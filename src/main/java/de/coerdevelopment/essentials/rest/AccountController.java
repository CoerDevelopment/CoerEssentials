package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.api.AccountLogin;
import de.coerdevelopment.essentials.api.FileMetadata;
import de.coerdevelopment.essentials.job.instances.AccountLoginHistoryJob;
import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.repository.LocalFileStorageRepository;
import de.coerdevelopment.essentials.security.CoerSecurity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/account")
public class AccountController {

    private static Map<String, Integer> failedLoginAttemptsPerMail = new HashMap<>();
    private static Map<String, Integer> passwordResetAttemptsPerMail = new HashMap<>();
    private static Map<String, LocalDateTime> accountLoginLocks = new HashMap<>();
    private static Map<String, LocalDateTime> passwordResetLocks = new HashMap<>();
    private static final long LOCK_DURATION_SECONDS = TimeUnit.MINUTES.toSeconds(5);

    /**
     * Creates a new account
     * @return a valid token for the new account
     */
    @PostMapping()
    public ResponseEntity<String> createAccount(@RequestBody AccountCredentialsRequest request) {
        if (getAccountModule().createAccount(request.mail, request.password, request.locale)) {
            return login(request);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to create the account. Maybe there is already an account with this mail.");
        }
    }

    @AuthentificationRequired
    @GetMapping()
    public ResponseEntity<Account> getAccount(@RequestAttribute("accountId") int accountId) {
        return ResponseEntity.ok(getAccountModule().getAccount(accountId));
    }

    @AuthentificationRequired
    @PutMapping()
    public ResponseEntity<Boolean> updateAccount(@RequestAttribute("accountId") int accountId, @RequestBody Account account) {
        return ResponseEntity.ok(getAccountModule().updateAccount(accountId, account));
    }

    @AuthentificationRequired
    @DeleteMapping()
    public ResponseEntity deleteAccount(@RequestAttribute("accountId") int accountId) {
        return getAccountModule().deleteAccount(accountId);
    }

    @PostMapping("/security/login")
    public ResponseEntity<String> login(@RequestBody AccountCredentialsRequest request) {
        if (accountLoginLocks.containsKey(request.mail)) {
            if (accountLoginLocks.get(request.mail).isAfter(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked temporarily due too many failed login attempts. Try again later.");
            } else {
                accountLoginLocks.remove(request.mail);
            }
        }
        int accountId = -1;
        try {
            accountId = getAccountModule().login(request.mail, request.password);
        } catch (Exception e) {
            CoerEssentials.getInstance().logError(e.getMessage());
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.mail, new Timestamp(System.currentTimeMillis()), false, e.getMessage()));
        }
        Account account = getAccountModule().getAccount(accountId);
        if (account != null && account.isLocked) {
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.mail, new Timestamp(System.currentTimeMillis()), false, "Account is locked"));
            return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked");
        }
        if (account != null) {
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.mail, new Timestamp(System.currentTimeMillis()), accountId != -1, ""));
            final long TOKEN_EXPIRATION = getAccountModule().refreshTokenExpiration;
            String refreshToken = CoerSecurity.getInstance().createToken(accountId, TOKEN_EXPIRATION);
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(getAccountModule().refreshTokenSecure)
                    .path("/api/account/security/refresh")
                    .maxAge(TOKEN_EXPIRATION / 1000)
                    .build();
            ResponseEntity<String> response = ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(getAccountModule().getToken(account));
            return response;
        } else {
            // save failed login attempts
            failedLoginAttemptsPerMail.put(request.mail, failedLoginAttemptsPerMail.getOrDefault(request.mail, 0) + 1);
            if (failedLoginAttemptsPerMail.get(request.mail) >= getAccountModule().maxLoginTriesInShortTime) {
                accountLoginLocks.put(request.mail, LocalDateTime.now().plusSeconds(LOCK_DURATION_SECONDS));
            }
            ScheduledExecutorService lockScheduler = Executors.newScheduledThreadPool(1);
            lockScheduler.schedule(() -> {
                int currentAttemps = failedLoginAttemptsPerMail.getOrDefault(request.mail, 0);
                if (currentAttemps <= 1) {
                    failedLoginAttemptsPerMail.remove(request.mail);
                } else {
                    failedLoginAttemptsPerMail.put(request.mail, Math.max(0, currentAttemps - 1));
                }
            }, LOCK_DURATION_SECONDS, TimeUnit.SECONDS);

            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.mail, new Timestamp(System.currentTimeMillis()), false, "Invalid credentials"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/security/refresh")
    public ResponseEntity<String> refreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    String refreshToken = cookie.getValue();
                    if (getAccountModule().isRefreshTokenBlacklisted(refreshToken)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is blacklisted");
                    }
                    int accountId = CoerSecurity.getInstance().getSubjectFromTokenAsInt(refreshToken);
                    Account account = getAccountModule().getAccount(accountId);
                    if (accountId > 0 && account != null && !account.isLocked) {
                        String newAccessToken = getAccountModule().getToken(account);

                        return ResponseEntity.ok(newAccessToken);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token missing");
    }

    @PostMapping("/security/requestpasswordreset/{mail}")
    public void requestPasswordReset(@PathVariable("mail") String mail) {
        if (passwordResetLocks.containsKey(mail)) {
            if (passwordResetLocks.get(mail).isAfter(LocalDateTime.now())) {
                return;
            } else {
                passwordResetLocks.remove(mail);
            }
        }
        getAccountModule().sendPasswordReset(mail);
        passwordResetAttemptsPerMail.put(mail, passwordResetAttemptsPerMail.getOrDefault(mail, 0) + 1);
        if (passwordResetAttemptsPerMail.get(mail) >= getAccountModule().maxPasswordResetTriesInShortTime) {
            passwordResetLocks.put(mail, LocalDateTime.now().plusSeconds(LOCK_DURATION_SECONDS));
        }
        ScheduledExecutorService lockScheduler = Executors.newScheduledThreadPool(1);
        lockScheduler.schedule(() -> {
            int currentAttemps = passwordResetAttemptsPerMail.getOrDefault(mail, 0);
            if (currentAttemps <= 1) {
                passwordResetAttemptsPerMail.remove(mail);
            } else {
                passwordResetAttemptsPerMail.put(mail, Math.max(0, currentAttemps - 1));
            }
        }, LOCK_DURATION_SECONDS, TimeUnit.SECONDS);
    }

    @CrossOrigin
    @PutMapping("/security/resetpassword/{token}")
    public ResponseEntity resetPassword(@PathVariable("token") String token, @RequestBody String newPassword) {
        if (getAccountModule().changePassword(token, newPassword)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
        }
    }

    @AuthentificationRequired
    @PostMapping("/mail/requestverification")
    public ResponseEntity requestMailVerification(@RequestAttribute("accountId") int accountId) {
        return getAccountModule().sendMailVerification(accountId);
    }

    @AuthentificationRequired
    @PutMapping("/mail/verify/{verificationCode}")
    public ResponseEntity verifyMail(@RequestAttribute("accountId") int accountId, @PathVariable("verificationCode") String verificationCode) {
        return getAccountModule().verifyMail(accountId, verificationCode);
    }

    @AuthentificationRequired
    @PostMapping("/profilepicture")
    public ResponseEntity<String> uploadProfilePicture(@RequestAttribute("accountId") int accountId, @RequestParam("file") MultipartFile file) {
        try {
            getAccountModule().uploadProfilePicture(accountId, file);
            return ResponseEntity.ok("Profile picture uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    @AuthentificationRequired
    @GetMapping("/profilepicture")
    public ResponseEntity<Resource> getProfilePicture(@RequestAttribute("accountId") int accountId) {
        try {
            Resource resource = getAccountModule().getProfilePicture(accountId, accountId);
            FileMetadata fileMetadata = LocalFileStorageRepository.getInstance().getFileMetadataByFileName(accountId, "profilePicture");

            MediaType mediaType = fileMetadata.mimeType.equals(MediaType.IMAGE_JPEG) ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @AuthentificationRequired
    @GetMapping("/profilepicture/{fileUUID}")
    public ResponseEntity<Resource> getProfilePicture(@RequestAttribute("accountId") int accountId, @PathVariable("fileUUID") String fileUUID) {
        try {
            FileMetadata metadata = LocalFileStorageRepository.getInstance().getFileMetadataByUUID(fileUUID);
            Resource resource = getAccountModule().getProfilePicture(accountId, metadata.accountId);
            FileMetadata fileMetadata = LocalFileStorageRepository.getInstance().getFileMetadataByFileName(accountId, "profilePicture");

            MediaType mediaType = fileMetadata.mimeType.equals(MediaType.IMAGE_JPEG) ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    public static class AccountCredentialsRequest {
        public String mail;
        public String password;
        public Locale locale;

        public AccountCredentialsRequest() {
        }
    }

    private AccountModule getAccountModule() {
        return CoerEssentials.getInstance().getAccountModule();
    }

}

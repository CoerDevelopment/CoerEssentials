package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.api.AccountLogin;
import de.coerdevelopment.essentials.api.FileMetadata;
import de.coerdevelopment.essentials.job.instances.AccountLoginHistoryJob;
import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.repository.LocalFileStorageRepository;
import de.coerdevelopment.essentials.security.CoerSecurity;
import de.coerdevelopment.essentials.utils.CoerCache;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/account")
public class AccountController {

    private static CoerCache<Integer> failedLoginAttemptsCache = new CoerCache<>("failedLoginAttempts", Duration.ofMinutes(10), Integer.class);
    private static CoerCache<Integer> passwordResetAttemptsCache = new CoerCache<>("passwordResetAttempts", Duration.ofMinutes(10), Integer.class);

    /**
     * Creates a new account
     * @return a valid token for the new account
     */
    @IdempotentRequest
    @PostMapping()
    public ResponseEntity<String> createAccount(@RequestBody AccountCreationRequest request) {
        request.email = request.email.toLowerCase(Locale.ROOT);
        if (getAccountModule().createAccount(request.email, request.password, request.locale, request.username, request.firstName, request.lastName)) {
            return login(new AccountLoginRequest(request.email, request.password));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to create the account. Maybe there is already an account with this mail.");
        }
    }

    @AuthentificationRequired
    @GetMapping()
    public ResponseEntity<Account> getAccount(@RequestAttribute("accountId") long accountId) {
        return ResponseEntity.ok(getAccountModule().getAccount(accountId));
    }

    @AuthentificationRequired
    @PutMapping()
    public ResponseEntity updateAccount(@RequestAttribute("accountId") long accountId, @RequestBody Account account) {
        try {
            Account updatedAccount = getAccountModule().updateAccount(accountId, account);
            return ResponseEntity.ok(updatedAccount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AuthentificationRequired
    @DeleteMapping()
    public ResponseEntity deleteAccount(@RequestAttribute("accountId") long accountId) {
        return getAccountModule().deleteAccount(accountId);
    }

    @PostMapping("/security/login")
    public ResponseEntity<String> login(@RequestBody AccountLoginRequest request) {
        if (request.emailOrUsername == null || request.emailOrUsername.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email/Username is required");
        }
        if (request.isEmail()) {
            request.emailOrUsername = request.emailOrUsername.toLowerCase(Locale.ROOT);
        }
        if (failedLoginAttemptsCache.contains(request.emailOrUsername) && failedLoginAttemptsCache.get(request.emailOrUsername) >= getAccountModule().maxLoginTriesInShortTime) {
            return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked temporarily due too many failed login attempts. Try again later.");
        }
        long accountId = -1;
        try {
            accountId = getAccountModule().login(request.emailOrUsername, request.password);
        } catch (Exception e) {
            CoerEssentials.getInstance().logError(e.getMessage());
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.emailOrUsername, OffsetDateTime.now(), false, e.getMessage()));
        }
        Account account = getAccountModule().getAccount(accountId);
        if (account != null && account.isLocked) {
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.emailOrUsername, OffsetDateTime.now(), false, "Account is locked"));
            return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked");
        }
        if (account != null) {
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.emailOrUsername, OffsetDateTime.now(), accountId != -1, ""));
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
            Integer currentAttempts = failedLoginAttemptsCache.get(request.emailOrUsername);
            if (currentAttempts == null) {
                currentAttempts = 0;
            }
            failedLoginAttemptsCache.put(request.emailOrUsername, currentAttempts + 1);
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(request.emailOrUsername, OffsetDateTime.now(), false, "Invalid credentials"));
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
                    long accountId = CoerSecurity.getInstance().getSubjectFromTokenAsInt(refreshToken);
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

    @IdempotentRequest
    @PostMapping("/security/requestpasswordreset/{email}")
    public void requestPasswordReset(@PathVariable("email") String email) {
        email = email.toLowerCase(Locale.ROOT);
        if (passwordResetAttemptsCache.contains(email)) {
            if (passwordResetAttemptsCache.get(email) >= getAccountModule().maxPasswordResetTriesInShortTime) {
                throw new RuntimeException("Too many password reset requests in a short time. Try again later.");
            }
            passwordResetAttemptsCache.put(email, passwordResetAttemptsCache.get(email) + 1);
        } else {
            passwordResetAttemptsCache.put(email, 1);
        }
        getAccountModule().sendPasswordReset(email);
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

    @IdempotentRequest
    @AuthentificationRequired
    @PostMapping("/mail/requestverification")
    public ResponseEntity requestMailVerification(@RequestAttribute("accountId") long accountId) {
        return getAccountModule().sendEmailVerification(accountId);
    }

    @AuthentificationRequired
    @PutMapping("/mail/verify/{verificationCode}")
    public ResponseEntity verifyMail(@RequestAttribute("accountId") long accountId, @PathVariable("verificationCode") String verificationCode) {
        return getAccountModule().verifyEmail(accountId, verificationCode);
    }

    @IdempotentRequest
    @AuthentificationRequired
    @PostMapping("/profilepicture")
    public ResponseEntity<String> uploadProfilePicture(@RequestAttribute("accountId") long accountId, @RequestParam("file") MultipartFile file) {
        try {
            getAccountModule().uploadProfilePicture(accountId, file);
            return ResponseEntity.ok("Profile picture uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    @AuthentificationRequired
    @GetMapping("/profilepicture")
    public ResponseEntity<Resource> getProfilePicture(@RequestAttribute("accountId") long accountId) {
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
    public ResponseEntity<Resource> getProfilePicture(@RequestAttribute("accountId") long accountId, @PathVariable("fileUUID") String fileUUID) {
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

    public static class AccountCreationRequest {
        public String email;
        public String password;
        public String firstName;
        public String lastName;
        public String username;
        public Locale locale;

        public AccountCreationRequest() {
        }
    }

    public static class AccountLoginRequest {
        public String emailOrUsername;
        public String password;

        public AccountLoginRequest() {
        }

        public AccountLoginRequest(String emailOrUsername, String password) {
            this.emailOrUsername = emailOrUsername;
            this.password = password;
        }

        public boolean isEmail() {
            return emailOrUsername.contains("@");
        }
    }

    private AccountModule getAccountModule() {
        return CoerEssentials.getInstance().getAccountModule();
    }

}

package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.module.AccountModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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
        if (getAccountModule().createAccount(request.mail, request.password)) {
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
        int accountId = getAccountModule().login(request.mail, request.password);
        if (accountId != -1) {
            return ResponseEntity.ok(getAccountModule().getToken(accountId));
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

            if (getAccountModule().isAccountLocked(accountId)) {
                return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        }
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

    public static class AccountCredentialsRequest {
        public String mail;
        public String password;

        public AccountCredentialsRequest() {
        }
    }

    private AccountModule getAccountModule() {
        return CoerEssentials.getInstance().getAccountModule();
    }

}

package de.coerdevelopment.essentials.rest;

import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.module.Module;
import de.coerdevelopment.essentials.module.ModuleType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    private static Map<String, Integer> failedLoginAttemptsPerMail = new HashMap<>();
    private static Map<String, Integer> passwordResetAttemptsPerMail = new HashMap<>();
    private static Map<String, LocalDateTime> accountLoginLocks = new HashMap<>();
    private static Map<String, LocalDateTime> passwordResetLocks = new HashMap<>();
    private static final long LOCK_DURATION_SECONDS = TimeUnit.MINUTES.toSeconds(5);

    private AccountModule accountModule;

    public AccountController() {
        if (Module.getModule(ModuleType.ACCOUNT) != null) {
            accountModule = (AccountModule) Module.getModule(ModuleType.ACCOUNT);
        }
    }

    /**
     * Creates a new account
     * @return a valid token for the new account
     */
    @PostMapping()
    public ResponseEntity<String> createAccount(@RequestBody AccountCredentialsRequest request) {
        if (accountModule.createAccount(request.mail, request.password)) {
            return login(request);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to create the account. Maybe there is already an account with this mail.");
        }
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestBody AccountCredentialsRequest request) {
        if (accountLoginLocks.containsKey(request.mail)) {
            if (accountLoginLocks.get(request.mail).isAfter(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked temporarily due too many failed login attempts. Try again later.");
            } else {
                accountLoginLocks.remove(request.mail);
            }
        }
        int accountId = accountModule.login(request.mail, request.password);
        if (accountId != -1) {
            return ResponseEntity.ok(accountModule.getToken(accountId));
        } else {
            // save failed login attempts
            failedLoginAttemptsPerMail.put(request.mail, failedLoginAttemptsPerMail.getOrDefault(request.mail, 0) + 1);
            if (failedLoginAttemptsPerMail.get(request.mail) >= accountModule.maxLoginTriesInShortTime) {
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

            if (accountModule.isAccountLocked(accountId)) {
                return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        }
    }

    @AuthentificationRequired
    @GetMapping()
    public ResponseEntity<Account> getAccount(@RequestAttribute("accountId") int accountId) {
        return ResponseEntity.ok(accountModule.getAccount(accountId));
    }

    @AuthentificationRequired
    @PostMapping("/update")
    public ResponseEntity<Boolean> updateAccount(@RequestAttribute("accountId") int accountId, @RequestBody Account account) {
        return ResponseEntity.ok(accountModule.updateAccount(accountId, account));
    }

    @AuthentificationRequired
    @DeleteMapping()
    public ResponseEntity deleteAccount(@RequestAttribute("accountId") int accountId) {
        return accountModule.deleteAccount(accountId);
    }

    @AuthentificationRequired
    @PostMapping("/mail/requestverification")
    public ResponseEntity requestMailVerification(@RequestAttribute("accountId") int accountId) {
        return accountModule.sendMailVerification(accountId);
    }

    @AuthentificationRequired
    @PostMapping("/mail/verify/{verificationCode}")
    public ResponseEntity verifyMail(@RequestAttribute("accountId") int accountId, @PathVariable("verificationCode") String verificationCode) {
        return accountModule.verifyMail(accountId, verificationCode);

    }

    @PostMapping("/password/requestreset/{mail}")
    public void requestPasswordReset(@PathVariable("mail") String mail) {
        if (passwordResetLocks.containsKey(mail)) {
            if (passwordResetLocks.get(mail).isAfter(LocalDateTime.now())) {
                return;
            } else {
                passwordResetLocks.remove(mail);
            }
        }
        accountModule.sendPasswordReset(mail);
        passwordResetAttemptsPerMail.put(mail, passwordResetAttemptsPerMail.getOrDefault(mail, 0) + 1);
        if (passwordResetAttemptsPerMail.get(mail) >= accountModule.maxPasswordResetTriesInShortTime) {
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
    @PostMapping("/password/reset/{token}")
    public ResponseEntity resetPassword(@PathVariable("token") String token, @RequestBody String newPassword) {
        if (accountModule.changePassword(token, newPassword)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
        }
    }

    public static class AccountCredentialsRequest {
        public String mail;
        public String password;

        public AccountCredentialsRequest() {
        }
    }

}

package de.coerdevelopment.essentials.module;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.api.AccountLogin;
import de.coerdevelopment.essentials.job.JobExecutor;
import de.coerdevelopment.essentials.job.JobOptions;
import de.coerdevelopment.essentials.job.JobOptionsConfig;
import de.coerdevelopment.essentials.job.instances.AccountLoginHistoryJob;
import de.coerdevelopment.essentials.repository.AccountLoginRepository;
import de.coerdevelopment.essentials.repository.AccountRepository;
import de.coerdevelopment.essentials.security.CoerSecurity;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AccountModule extends Module {

    private AccountRepository accountRepository;
    public AccountLoginRepository accountLoginRepository;
    private MailModule mailModule;
    private Map<Integer, Integer> restUsagePerAccountInShortTime;

    // Options
    private String tableName;
    private int saltLength;
    private String hashAlgorithm;
    private boolean mailConfirmationEnabled;
    private int mailVerificationCodeLength;
    private long mailVerificationCodeExpiration;
    private long passwordResetCodeExpiration;
    private long tokenExpiration;
    private String passwortResetUrl;
    public int maxLoginTriesInShortTime;
    public int maxPasswordResetTriesInShortTime;
    private boolean spamProtectionEnabled;
    private long spamProtectionTimeFrameMilliseconds;
    private int spamProtectionMaxRequests;

    public AccountModule() {
        super(ModuleType.ACCOUNT);
        this.tableName = getStringOption("tableName");
        this.saltLength = getIntOption("saltLength");
        this.hashAlgorithm = getStringOption("hashAlgorithm");
        this.mailConfirmationEnabled = getBooleanOption("mailConfirmationEnabled");
        this.mailVerificationCodeLength = getIntOption("mailConfirmationTokenLength");
        this.mailVerificationCodeExpiration = getLongOption("mailConfirmationTokenExpirationMilliseconds");
        this.passwordResetCodeExpiration = getLongOption("passwordResetExpirationMilliseconds");
        this.tokenExpiration = getLongOption("tokenExpirationMilliseconds");
        this.maxLoginTriesInShortTime = getIntOption("maxLoginTriesInShortTime");
        this.maxPasswordResetTriesInShortTime = getIntOption("maxPasswordResetTriesInShortTime");
        this.passwortResetUrl = getStringOption("resetPasswordUrl");
        this.spamProtectionEnabled = getBooleanOption("spamProtectionEnabled");
        this.spamProtectionTimeFrameMilliseconds = getLongOption("spamProtectionTimeFrameMilliseconds");
        this.spamProtectionMaxRequests = getIntOption("spamProtectionMaxRequests");

        this.accountRepository = new AccountRepository(tableName);
        this.accountLoginRepository = AccountLoginRepository.getInstance();
        SQLModule sqlModule = CoerEssentials.getInstance().getSQLModule();
        accountRepository.createTable(); // create the account table if it does not exist
        accountLoginRepository.createTable();
        CoerSecurity.newInstance(hashAlgorithm, saltLength, tokenExpiration);
        this.mailModule = CoerEssentials.getInstance().getMailModule();
        this.restUsagePerAccountInShortTime = new HashMap<>();

        JobExecutor.registerJob(new AccountLoginHistoryJob());
        JobOptions accountHistoryOptions = new JobOptions(
                "AccountLoginHistory",
                true,
                true,
                Duration.of(5, ChronoUnit.MINUTES),
                null,
                false,
                0,
                null);
        JobOptionsConfig.getInstance().saveOptions("AccountLoginHistory", accountHistoryOptions, false);
        JobExecutor.getInstance();
    }

    /**
     * Creates a new account if the mail is not already in use
     * @return true if the account was created, otherwise false
     */
    public boolean createAccount(String mail, String password) {
        // check if this mail is already associated with an account
        if (accountRepository.doesMailExists(mail)) {
            return false;
        }

        // Generate salt and hash the password again
        String salt = CoerSecurity.getInstance().generateSalt();
        String passwordHash = CoerSecurity.getInstance().hashPassword(password, salt);

        // create the account in database
        int accountId;
        try {
            accountId = accountRepository.insertAccount(mail, passwordHash, salt);
        } catch (Exception e) {
            CoerEssentials.getInstance().logWarning("Error creating account: " + e.getMessage());
            return false;
        }

        // send mail verification if enabled
        if (mailConfirmationEnabled) {
            sendMailVerification(accountId);
        }
        return true;
    }

    /**
     * Checks the provided credentials and returns the account id if they are correct
     */
    public int login(String mail, String passwordHash) {
        int accountId = -1;
        try {
            accountId = accountRepository.getAccountIdIfPasswortMatches(mail, passwordHash);
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(mail, new Timestamp(System.currentTimeMillis()), accountId != -1, ""));
        } catch (Exception e) {
            AccountLoginHistoryJob.loginsToBeProcessed.add(new AccountLogin(mail, new Timestamp(System.currentTimeMillis()), false, e.getMessage()));
        }
        return accountId;
    }

    /**
     * Returns a token for the given account
     */
    public String getToken(int accountId) {
        return CoerSecurity.getInstance().createToken(accountId);
    }

    /**
     * Checks if the given account has a verified mail
     */
    public boolean isMailVerified(int accountId) {
        return accountRepository.isMailVerified(accountId);
    }

    /**
     * Generates a new verification code and sends it to the mail of the given account
     */
    public ResponseEntity sendMailVerification(int accountId) {
        if (!mailConfirmationEnabled) {
            return ResponseEntity.badRequest().body("Unable to verify mail.");
        }
        // check if the mail is already verified
        if (isMailVerified(accountId)) {
            return ResponseEntity.badRequest().body("Mail is already verified.");
        }

        // check if account exists and get mail
        String mail = accountRepository.getMail(accountId);
        if (mail == null) {
            return ResponseEntity.badRequest().body("Unable to verify mail.");
        }

        // check if verification is pending
        if (accountRepository.isMailVerificationPending(accountId)) {
            return ResponseEntity.badRequest().body("Verification code already send.");
        }

        // generate new verification code
        String code = getVerificationCode();
        long expiration = System.currentTimeMillis() + mailVerificationCodeExpiration;
        accountRepository.setMailVerificationCode(accountId, code, expiration);

        // send mail
        String programName = CoerEssentials.getInstance().getProgramName();
        String text = "Your verification code is: " + code;
        mailModule.sendMail(mail, programName + " Verification Code", text);
        return ResponseEntity.ok("Verification code send.");
    }

    /**
     * Uses the verification code send by the client to verify the mail of the given account
     */
    public ResponseEntity verifyMail(int accountId, String verificationCode) {
        // check if the mail is already verified
        if (isMailVerified(accountId)) {
            return ResponseEntity.ok("Mail is already verified.");
        }
        // check if the verification code is correct
        // if the code is expired, send a new one
        try {
            if (accountRepository.doesMailVerificationCodeMatch(accountId, verificationCode)) {
                accountRepository.setMailVerified(accountId);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Invalid verification code.");
            }
        } catch (Exception e) {
            sendMailVerification(accountId);
            return ResponseEntity.badRequest().body("Verification code expired. New code send.");
        }
    }

    /**
     * Sends a link to the mail of the given account to reset the password
     */
    public void sendPasswordReset(String mail) {
        // check if the mail is associated with an account
        if (!accountRepository.doesMailExists(mail)) {
            return;
        }
        int accountId = accountRepository.getAccountIdByMail(mail);

        // generate new password reset token
        String token = CoerSecurity.getInstance().createToken("passwordReset" + accountId, passwordResetCodeExpiration);

        //send mail
        String programName = CoerEssentials.getInstance().getProgramName();
        String url = passwortResetUrl.replace("%token%", "token=" + token);
        String text = "Click the following link to reset your password: " + url;
        mailModule.sendMail(mail, programName + " Password Reset", text);
    }

    /**
     * Changes the password of the given account
     */
    public boolean changePassword(String token, String newPassword) {
        // check if token is valid
        String subject;
        try {
            subject = CoerSecurity.getInstance().getStringFromToken(token);
        } catch (Exception e) {
            return false;
        }
        if (!subject.startsWith("passwordReset")) {
            return false;
        }
        int accountId = Integer.parseInt(subject.replace("passwordReset", ""));

        // generate new salt and hash the password
        String salt = CoerSecurity.getInstance().generateSalt();
        String passwordHash = CoerSecurity.getInstance().hashPassword(newPassword, salt);
        accountRepository.changePassword(accountId, passwordHash, salt);
        return true;
    }

    /**
     * Checks if the account is spam protected
     * If the account is spam protected, the requests will not be processed
     */
    public boolean isAccountSpamProtected(int accountId) {
        if (!spamProtectionEnabled) {
            return false;
        }
        int requests = restUsagePerAccountInShortTime.getOrDefault(accountId, 0);
        if (requests >= spamProtectionMaxRequests) {
            return true;
        } else {
            restUsagePerAccountInShortTime.put(accountId, requests + 1);
            ScheduledExecutorService lockScheduler = Executors.newScheduledThreadPool(1);
            lockScheduler.schedule(() -> {
                int currentRequests = restUsagePerAccountInShortTime.getOrDefault(accountId, 0);
                restUsagePerAccountInShortTime.put(accountId, Math.max(0, currentRequests - 1));
            }, spamProtectionTimeFrameMilliseconds, TimeUnit.MILLISECONDS);
            return false;
        }
    }

    public void lockAccount(int accountId) {
        accountRepository.setProperty(accountId, "isLocked", true);
    }

    public void unlockAccount(int accountId) {
        accountRepository.setProperty(accountId, "isLocked", false);
    }

    public boolean isAccountLocked(int accountId) {
        return accountRepository.isAccountLocked(accountId);
    }

    public boolean updateAccount(int accountId, Account account) {
        return accountRepository.updateAccount(accountId, account);
    }

    /**
     * Sets the birthday of the account
     */
    public void setBirthday(int accountId, Date birthday) {
        accountRepository.setProperty(accountId, "birthday", birthday);
    }

    /**
     * Sets the first name of the account
     */
    public void setFirstName(int accountId, String firstName) {
        accountRepository.setProperty(accountId, "firstName", firstName);
    }

    /**
     * Sets the last name of the account
     */
    public void setLastName(int accountId, String lastName) {
        accountRepository.setProperty(accountId, "lastName", lastName);
    }

    /**
     * Sets the username of the account
     */
    public void setUsername(int accountId, String username) {
        accountRepository.setProperty(accountId, "username", username);
    }

    /**
     * Sets the nationality of the account
     */
    public void setNationality(int accountId, String nationality) {
        accountRepository.setProperty(accountId, "nationality", nationality);
    }

    /**
     * Sets the location of the account
     */
    public void setLocation(int accountId, String location) {
        accountRepository.setProperty(accountId, "location", location);
    }

    /**
     * Sets the instagram Url of the account
     */
    public void setInstagramUrl(int accountId, String instagramUrl) {
        accountRepository.setProperty(accountId, "instagramUrl", instagramUrl);
    }

    /**
     * Sets the twitter Url of the account
     */
    public void setTwitterUrl(int accountId, String twitterUrl) {
        accountRepository.setProperty(accountId, "twitterUrl", twitterUrl);
    }

    /**
     * Sets the facebook Url of the account
     */
    public void setFacebookUrl(int accountId, String facebookUrl) {
        accountRepository.setProperty(accountId, "facebookUrl", facebookUrl);
    }

    /**
     * Sets the linkedin Url of the account
     */
    public void setLinkedinUrl(int accountId, String linkedinUrl) {
        accountRepository.setProperty(accountId, "linkedinUrl", linkedinUrl);
    }

    /**
     * Sets the website Url of the account
     */
    public void setWebsiteUrl(int accountId, String websiteUrl) {
        accountRepository.setProperty(accountId, "websiteUrl", websiteUrl);
    }

    /**
     * Sets the about me text of the account
     */
    public void setAboutMe(int accountId, String aboutMe) {
        accountRepository.setProperty(accountId, "aboutMe", aboutMe);
    }

    /**
     * Sets the profile picture Url of the account
     */
    public void setProfilePictureUrl(int accountId, String profilePictureUrl) {
        accountRepository.setProperty(accountId, "profilePictureUrl", profilePictureUrl);
    }

    public void setPrivateStatus(int accountId, boolean isPrivate) {
        accountRepository.setProperty(accountId, "isPrivate", isPrivate);
    }

    /**
     * Returns the account with the given id
     */
    public Account getAccount(int accountId) {
        return accountRepository.getAccount(accountId);
    }

    /**
     * Deletes the account
     */
    public ResponseEntity deleteAccount(int accountId) {
        return accountRepository.deleteAccount(accountId) ?
                ResponseEntity.ok("Account have been deleted") :
                ResponseEntity.badRequest().body("Unable to delete account");
    }

    /**
     * Generates a random number with the given length
     */
    private String getVerificationCode() {
        String code = "";
        for (int i = 0; i < mailVerificationCodeLength; i++) {
            code += (int) (Math.random() * 10);
        }
        return code;
    }

}

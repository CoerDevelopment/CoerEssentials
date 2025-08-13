package de.coerdevelopment.essentials.module;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.filestorage.FileStorage;
import de.coerdevelopment.essentials.filestorage.LocalFileStorage;
import de.coerdevelopment.essentials.job.JobExecutor;
import de.coerdevelopment.essentials.job.instances.AccountCacheJob;
import de.coerdevelopment.essentials.job.instances.AccountLoginHistoryJob;
import de.coerdevelopment.essentials.repository.AccountLoginRepository;
import de.coerdevelopment.essentials.repository.AccountRepository;
import de.coerdevelopment.essentials.repository.LocalFileStorageRepository;
import de.coerdevelopment.essentials.security.CoerSecurity;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AccountModule extends Module {

    private AccountRepository accountRepository;
    public AccountLoginRepository accountLoginRepository;
    private MailModule mailModule;
    private ConcurrentHashMap<Long, Integer> restUsagePerAccountInShortTime;
    private FileStorage profilePictureStorage;
    private ConcurrentHashMap<Long, Account> accountsById;
    private List<String> blacklistedRefreshTokens;

    // Options
    public String tableName;
    private int saltLength;
    private String hashAlgorithm;
    private boolean mailConfirmationEnabled;
    private int mailVerificationCodeLength;
    private long mailVerificationCodeExpiration;
    private long passwordResetCodeExpiration;
    private long tokenExpiration;
    public long refreshTokenExpiration;
    public boolean refreshTokenSecure;
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
        this.refreshTokenExpiration = getLongOption("refreshTokenExpiration");
        this.refreshTokenSecure = getBooleanOption("refreshTokenSecure");
        this.maxLoginTriesInShortTime = getIntOption("maxLoginTriesInShortTime");
        this.maxPasswordResetTriesInShortTime = getIntOption("maxPasswordResetTriesInShortTime");
        this.passwortResetUrl = getStringOption("resetPasswordUrl");
        this.spamProtectionEnabled = getBooleanOption("spamProtectionEnabled");
        this.spamProtectionTimeFrameMilliseconds = getLongOption("spamProtectionTimeFrameMilliseconds");
        this.spamProtectionMaxRequests = getIntOption("spamProtectionMaxRequests");

        this.accountRepository = new AccountRepository(tableName);
        this.blacklistedRefreshTokens = new ArrayList<>();
        this.accountLoginRepository = AccountLoginRepository.getInstance();
        SQLModule sqlModule = CoerEssentials.getInstance().getSQLModule();
        accountRepository.createTable();
        accountLoginRepository.createTable();
        LocalFileStorageRepository.getInstance().createTable();
        CoerSecurity.newInstance(hashAlgorithm, saltLength, tokenExpiration);
        this.mailModule = CoerEssentials.getInstance().getMailModule();
        this.restUsagePerAccountInShortTime = new ConcurrentHashMap<>();
        // Initialize profile picture storage
        String profilePictureStorageType = getStringOption("profilePictureStorage");
        if (!profilePictureStorageType.equals("local")) {
            throw new IllegalArgumentException("Only local storage option is supported for account profile pictures");
        }
        Map<String, Object> profilePictureStorageOptions = (Map<String, Object>) getOption("profilePictureStorageOptions");
        List<String> supportedMimeTypes = (List<String>) profilePictureStorageOptions.get("supportedMimeTypes");
        long maxFileSizeBytes = Long.valueOf(String.valueOf(profilePictureStorageOptions.get("maxFileSizeBytes")));
        long maxStorageSizeBytes = Long.valueOf(String.valueOf(profilePictureStorageOptions.get("maxStorageSizeBytes")));
        String storageDirectory = (String) profilePictureStorageOptions.get("storageDirectory");

        Path storageDirectoryPath = Paths.get(CoerEssentials.getInstance().configDirectory + storageDirectory);
        try {
            this.profilePictureStorage = new LocalFileStorage(storageDirectoryPath, supportedMimeTypes, maxFileSizeBytes, maxStorageSizeBytes);
        } catch (Exception e) {
            CoerEssentials.getInstance().logError("Error initializing profile picture storage: " + e.getMessage());
            throw new RuntimeException("Failed to initialize profile picture storage", e);
        }

        accountsById = new ConcurrentHashMap<>();

        JobExecutor.registerJob(new AccountLoginHistoryJob());
        JobExecutor.registerJob(new AccountCacheJob());
    }

    /**
     * Creates a new account if the mail is not already in use
     * @return true if the account was created, otherwise false
     */
    public boolean createAccount(String email, String password, Locale locale) {
        // check if this mail is already associated with an account
        if (accountRepository.doesEmailExists(email)) {
            return false;
        }

        // Generate salt and hash the password again
        String salt = CoerSecurity.getInstance().generateSalt();
        String passwordHash = CoerSecurity.getInstance().hashPassword(password, salt);

        // create the account in database
        long accountId;
        try {
            accountId = accountRepository.insertAccount(email, passwordHash, salt, locale);
            Account account = accountRepository.getAccount(accountId);
            accountsById.put(accountId, account);
        } catch (Exception e) {
            CoerEssentials.getInstance().logWarning("Error creating account: " + e.getMessage());
            return false;
        }

        // send mail verification if enabled
        if (mailConfirmationEnabled) {
            sendEmailVerification(accountId);
        }
        return true;
    }

    /**
     * Checks the provided credentials and returns the account id if they are correct
     */
    public long login(String email, String passwordHash) throws Exception {
        return accountRepository.getAccountIdIfPasswortMatches(email, passwordHash);
    }

    /**
     * Returns a token for the given account
     */
    public String getToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", account.accountId);
        claims.put("email", account.email);
        claims.put("username", account.username);
        claims.put("createdAt", account.createdAt.toString());
        claims.put("locale", account.locale.toLanguageTag());
        claims.put("defaultCurrency", account.preferredCurrency.getCurrencyCode());
        claims.put("emailVerified", account.mailVerified);
        return CoerSecurity.getInstance().createToken(String.valueOf(account.accountId), claims);
    }

    public void blacklistRefreshToken(String token) {
        blacklistedRefreshTokens.add(token);
    }

    public boolean isRefreshTokenBlacklisted(String token) {
        return blacklistedRefreshTokens.contains(token);
    }

    /**
     * Checks if the given account has a verified mail
     */
    public boolean isEmailVerified(long accountId) {
        return accountRepository.isEmailVerified(accountId);
    }

    /**
     * Generates a new verification code and sends it to the mail of the given account
     */
    public ResponseEntity sendEmailVerification(long accountId) {
        if (!mailConfirmationEnabled) {
            return ResponseEntity.badRequest().body("Unable to verify email.");
        }
        // check if the mail is already verified
        if (isEmailVerified(accountId)) {
            return ResponseEntity.badRequest().body("Email is already verified.");
        }

        // check if account exists and get mail
        String email = accountRepository.getEmail(accountId);
        if (email == null) {
            return ResponseEntity.badRequest().body("Unable to verify mail.");
        }

        // check if verification is pending
        if (accountRepository.isEmailVerificationPending(accountId)) {
            return ResponseEntity.badRequest().body("Verification code already send.");
        }

        // generate new verification code
        String code = getVerificationCode();
        long expiration = System.currentTimeMillis() + mailVerificationCodeExpiration;
        accountRepository.setEmailVerificationCode(accountId, code, expiration);

        // send mail
        String programName = CoerEssentials.getInstance().getProgramName();
        String text = "Your verification code is: " + code;
        mailModule.sendMail(email, programName + " Verification Code", text);
        return ResponseEntity.ok("Verification code send.");
    }

    /**
     * Uses the verification code send by the client to verify the mail of the given account
     */
    public ResponseEntity verifyEmail(long accountId, String verificationCode) {
        // check if the mail is already verified
        if (isEmailVerified(accountId)) {
            return ResponseEntity.ok("Email is already verified.");
        }
        // check if the verification code is correct
        // if the code is expired, send a new one
        try {
            if (accountRepository.doesEmailVerificationCodeMatch(accountId, verificationCode)) {
                accountRepository.setEmailVerified(accountId);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Invalid verification code.");
            }
        } catch (Exception e) {
            sendEmailVerification(accountId);
            return ResponseEntity.badRequest().body("Verification code expired. New code send.");
        }
    }

    /**
     * Sends a link to the mail of the given account to reset the password
     */
    public void sendPasswordReset(String email) {
        // check if the mail is associated with an account
        if (!accountRepository.doesEmailExists(email)) {
            return;
        }
        long accountId = accountRepository.getAccountIdByEmail(email);

        // generate new password reset token
        String token = CoerSecurity.getInstance().createToken("passwordReset" + accountId, passwordResetCodeExpiration);

        //send mail
        String programName = CoerEssentials.getInstance().getProgramName();
        String url = passwortResetUrl.replace("%token%", "token=" + token);
        String text = "Click the following link to reset your password: " + url;
        mailModule.sendMail(email, programName + " Password Reset", text);
    }

    /**
     * Changes the password of the given account
     */
    public boolean changePassword(String token, String newPassword) {
        // check if token is valid
        String subject;
        try {
            subject = CoerSecurity.getInstance().getSubjectFromToken(token);
        } catch (Exception e) {
            return false;
        }
        if (!subject.startsWith("passwordReset")) {
            return false;
        }
        long accountId = Long.parseLong(subject.replace("passwordReset", ""));

        // generate new salt and hash the password
        String salt = CoerSecurity.getInstance().generateSalt();
        String passwordHash = CoerSecurity.getInstance().hashPassword(newPassword, salt);
        accountRepository.changePassword(accountId, passwordHash, salt);
        return true;
    }

    /**
     * Checks if the account is spam protected
     * If the account is spam protected, the requests will not be processed and the client call will be logged
     */
    public boolean isAccountSpamProtected(long accountId) {
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

    public void lockAccount(long accountId) {
        accountRepository.setProperty(accountId, "is_locked", true);
        accountsById.get(accountId).isLocked = true;
    }

    public void unlockAccount(long accountId) {
        accountRepository.setProperty(accountId, "is_locked", false);
        accountsById.get(accountId).isLocked = false;
    }

    public String uploadProfilePicture(long accountId, MultipartFile file) throws IOException {
        return profilePictureStorage.store(accountId, file, "profilePicture");
    }

    public Resource getProfilePicture(long accountId, long targetAccountId) {
        Account target = getAccount(targetAccountId);
        if (target.isPrivate && accountId != targetAccountId) {
            throw new RuntimeException("Target account is private.");
        }
        return profilePictureStorage.load(targetAccountId, "profilePicture");
    }

    public Account updateAccount(long accountId, Account account) throws Exception {
        Account currentState = getAccount(accountId);
        if (currentState.isLocked != account.isLocked) {
            throw new Exception("Unable to change lock status");
        }

        boolean success = accountRepository.updateAccount(accountId, account);
        if(success) {
            Account updatedAccount = accountRepository.getAccount(accountId);
            accountsById.put(accountId, updatedAccount);
            return updatedAccount;
        } else {
            throw new Exception("Unable to update account");
        }
    }

    /**
     * Sets the birthday of the account
     */
    public void setBirthday(long accountId, LocalDate birthday) {
        accountRepository.setProperty(accountId, "birthday", birthday);
    }

    /**
     * Sets the first name of the account
     */
    public void setFirstName(long accountId, String firstName) {
        accountRepository.setProperty(accountId, "first_name", firstName);
    }

    /**
     * Sets the last name of the account
     */
    public void setLastName(long accountId, String lastName) {
        accountRepository.setProperty(accountId, "last_name", lastName);
    }

    /**
     * Sets the username of the account
     */
    public void setUsername(long accountId, String username) {
        accountRepository.setProperty(accountId, "username", username);
    }

    /**
     * Sets the phone number of the account
     */
    public void setPhoneNumber(long accountId, String phoneNumber) {
        accountRepository.setProperty(accountId, "phone_number", phoneNumber);
    }

    /**
     * Sets the nationality of the account
     */
    public void setNationality(long accountId, String nationality) {
        accountRepository.setProperty(accountId, "nationality", nationality);
    }

    /**
     * Sets the location of the account
     */
    public void setLocation(long accountId, String location) {
        accountRepository.setProperty(accountId, "location", location);
    }

    /**
     * Sets the instagram Url of the account
     */
    public void setInstagramUrl(long accountId, String instagramUrl) {
        accountRepository.setProperty(accountId, "instagram_url", instagramUrl);
    }

    /**
     * Sets the twitter Url of the account
     */
    public void setTwitterUrl(long accountId, String twitterUrl) {
        accountRepository.setProperty(accountId, "twitter_url", twitterUrl);
    }

    /**
     * Sets the facebook Url of the account
     */
    public void setFacebookUrl(long accountId, String facebookUrl) {
        accountRepository.setProperty(accountId, "facebook_url", facebookUrl);
    }

    /**
     * Sets the linkedin Url of the account
     */
    public void setLinkedinUrl(long accountId, String linkedinUrl) {
        accountRepository.setProperty(accountId, "linked_in_url", linkedinUrl);
    }

    /**
     * Sets the website Url of the account
     */
    public void setWebsiteUrl(long accountId, String websiteUrl) {
        accountRepository.setProperty(accountId, "website_url", websiteUrl);
    }

    /**
     * Sets the about me text of the account
     */
    public void setAboutMe(long accountId, String aboutMe) {
        accountRepository.setProperty(accountId, "about_me", aboutMe);
    }

    /**
     * Sets the profile picture Url of the account
     */
    public void setProfilePictureUrl(long accountId, String profilePictureUrl) {
        accountRepository.setProperty(accountId, "profile_picture_url", profilePictureUrl);
    }

    public void setPrivateStatus(long accountId, boolean isPrivate) {
        accountRepository.setProperty(accountId, "is_private", isPrivate);
    }

    /**
     * Returns the account with the given id
     */
    public Account getAccount(long accountId) {
        return accountsById.get(accountId);
    }

    /**
     * Deletes the account
     */
    public ResponseEntity deleteAccount(long accountId) {
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

    public int updateAccounts() {
        accountsById.clear();
        accountsById = accountRepository.getAllAccountsById();
        return accountsById.size();
    }

}

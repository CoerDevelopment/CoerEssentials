package de.coerdevelopment.essentials.module;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.repository.AccountRepository;
import de.coerdevelopment.essentials.security.CoerSecurity;

public class AccountModule extends Module {

    private AccountRepository accountRepository;
    private MailModule mailModule;

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
    private int maxLoginTriesInShortTime;
    private int maxPasswordResetTriesInShortTime;

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

        this.accountRepository = new AccountRepository(tableName);
        SQLModule sqlModule = (SQLModule) Module.getModule(ModuleType.SQL);
        sqlModule.registerTableCreateRepository(accountRepository);
        CoerSecurity.newInstance(hashAlgorithm, saltLength, tokenExpiration);
        this.mailModule = (MailModule) Module.getModule(ModuleType.MAIL);
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
        String passwordHash = CoerSecurity.getInstance().stringToHash(password + salt);

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
        try {
            return accountRepository.getAccountIdIfPasswortMatches(mail, passwordHash);
        } catch (Exception e) {
            return -1;
        }
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
    public void sendMailVerification(int accountId) {
        // check if the mail is already verified
        if (isMailVerified(accountId)) {
            return;
        }

        // generate new verification code
        String code = getVerificationCode();
        long expiration = System.currentTimeMillis() + mailVerificationCodeExpiration;
        accountRepository.setMailVerificationCode(accountId, code, expiration);
        String mail = accountRepository.getMail(accountId);

        // send mail
        String programName = CoerEssentials.getInstance().getProgramName();
        String text = "Your verification code is: " + code;
        mailModule.sendMail(mail, programName + " Verification Code", text);
    }

    /**
     * Uses the verification code send by the client to verify the mail of the given account
     */
    public boolean verifyMail(int accountId, String verificationCode) {
        // check if the mail is already verified
        if (isMailVerified(accountId)) {
            return true;
        }
        // check if the verification code is correct
        // if the code is expired, send a new one
        try {
            if (accountRepository.doesMailVerificationCodeMatch(accountId, verificationCode)) {
                accountRepository.setMailVerified(accountId, true);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            sendMailVerification(accountId);
            return false;
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
        String url = passwortResetUrl.replace("%token%", token);
        String text = "Click the following link to reset your password: " + url;
        mailModule.sendMail(mail, programName + " Password Reset", text);
    }

    /**
     * Changes the password of the given account
     */
    public boolean changePassword(String token, String newPassword) {
        // check if token is valid
        String subject = CoerSecurity.getInstance().getStringFromToken(token);
        if (!subject.startsWith("passwordReset")) {
            return false;
        }
        int accountId = Integer.parseInt(subject.replace("passwordReset", ""));

        // generate new salt and hash the password
        String salt = CoerSecurity.getInstance().generateSalt();
        String passwordHash = CoerSecurity.getInstance().stringToHash(newPassword + salt);
        accountRepository.changePassword(accountId, passwordHash, salt);
        return true;
    }

    /**
     * Deletes the account
     */
    public void deleteAccount(int accountId) {
        accountRepository.deleteAccount(accountId);
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

package de.coerdevelopment.essentials.module;

import de.coerdevelopment.essentials.repository.AccountRepository;

public class AccountModule extends Module {

    private AccountRepository accountRepository;

    // Options
    private String tableName;
    private int saltLength;
    private String hashAlgorithm;
    private boolean mailConfirmationEnabled;
    private int mailVerificationCodeLength;
    private long mailVerificationCodeExpiration;
    private long passwordResetCodeExpiration;
    private long tokenExpiration;
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

        this.accountRepository = new AccountRepository(tableName);
        SQLModule sqlModule = (SQLModule) Module.getModule(ModuleType.SQL);
        sqlModule.registerTableCreateRepository(accountRepository);
    }

    /**
     * Creates a new account if the mail is not already in use
     */
    public void createAccount(String mail, String password) {

    }

    /**
     * Checks the provided credentials and returns the account id if they are correct
     */
    public int login(String mail, String passwordHash) {
        return -1;
    }

    /**
     * Returns a token for the given account
     */
    public String getToken(int accountId) {
        return null;
    }

    /**
     * Checks if the given account has a verified mail
     */
    public boolean isMailVerified(int accountId) {
        return false;
    }

    /**
     * Generates a new verification code and sends it to the mail of the given account
     */
    public void sendMailVerification(int accountId) {

    }

    /**
     * Uses the verification code send by the client to verify the mail of the given account
     */
    public void verifyMail(int accountId) {

    }

    /**
     * Sends a link to the mail of the given account to reset the password
     */
    public void sendPasswordReset(int accountId) {

    }

    /**
     * Changes the password of the given account
     */
    public void changePassword(int accountId) {

    }

    /**
     * Deletes the account
     */
    public void deleteAccount(int accountId) {

    }
}

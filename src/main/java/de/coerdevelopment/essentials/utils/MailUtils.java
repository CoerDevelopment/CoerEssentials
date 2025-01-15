package de.coerdevelopment.essentials.utils;

public class MailUtils {

    private static MailUtils instance;

    public static MailUtils getInstance() {
        if (instance == null) {
            instance = new MailUtils();
        }
        return instance;
    }

    private String fromMail;
    private String mailHost;
    private int mailPort;
    private String mailUsername;
    private String mailPassword;

    private final String protocol = "smtp";
    private final boolean useAuth = true;
    private final boolean tslEnabled = true;

    private MailUtils() {
        // private constructor used to secure singleton
    }

    public void setup(String fromMail, String mailHost, int mailPort, String mailUsername, String mailPassword) {
        this.fromMail = fromMail;
        this.mailHost = mailHost;
        this.mailPort = mailPort;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
    }

    public void sendMail(String recipient, String subject, String content) {

    }

    public boolean isSetupDone() {
        return false;
    }

}

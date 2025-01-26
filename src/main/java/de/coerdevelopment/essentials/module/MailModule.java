package de.coerdevelopment.essentials.module;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class MailModule extends Module {

    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;
    private String fromMail;

    public MailModule() {
        super(ModuleType.MAIL);
        this.host = getStringOption("host");
        this.port = getIntOption("port");
        this.username = getStringOption("username");
        this.password = getStringOption("password");
        this.fromMail = getStringOption("fromMail");
        this.session = createSession();
    }

    public void sendMail(String to, String subject, String body) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.addHeader("Content-type", "text/HTML; charset=UTF-8");
            message.addHeader("format", "flowed");
            message.addHeader("Content-Transfer-Encoding", "8bit");

            message.setFrom(fromMail);
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            message.setSentDate(new Date());
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Session createSession() {
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        return Session.getInstance(properties, auth);
    }

}

package de.coerdevelopment.essentials.api;

import java.sql.Timestamp;

public class AccountLogin {

    public int loginId;
    public String mail;
    public Timestamp timestamp;
    public boolean success;
    public String failureReason;

    public AccountLogin(int loginId, String mail, Timestamp timestamp, boolean success, String failureReason) {
        this(mail, timestamp, success, failureReason);
        this.loginId = loginId;
    }

    public AccountLogin(String mail, Timestamp timestamp, boolean success, String failureReason) {
        this.mail = mail;
        this.timestamp = timestamp;
        this.success = success;
        this.failureReason = failureReason;
    }
}

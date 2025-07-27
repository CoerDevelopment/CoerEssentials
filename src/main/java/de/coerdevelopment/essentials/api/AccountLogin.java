package de.coerdevelopment.essentials.api;

import java.time.OffsetDateTime;

public class AccountLogin {

    public int loginId;
    public String mail;
    public OffsetDateTime loginAt;
    public boolean success;
    public String failureReason;

    public AccountLogin(int loginId, String mail, OffsetDateTime loginAt, boolean success, String failureReason) {
        this(mail, loginAt, success, failureReason);
        this.loginId = loginId;
    }

    public AccountLogin(String mail, OffsetDateTime loginAt, boolean success, String failureReason) {
        this.mail = mail;
        this.loginAt = loginAt;
        this.success = success;
        this.failureReason = failureReason;
    }
}

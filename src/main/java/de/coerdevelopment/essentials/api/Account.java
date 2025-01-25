package de.coerdevelopment.essentials.api;

public class Account {

    public int accountId;
    public String mail;
    public String firstName;
    public String lastName;
    public String username;
    public boolean mailVerified;

    public Account(int accountId, String mail, String firstName, String lastName, String username, boolean mailVerified) {
        this.accountId = accountId;
        this.mail = mail;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.mailVerified = mailVerified;
    }
}

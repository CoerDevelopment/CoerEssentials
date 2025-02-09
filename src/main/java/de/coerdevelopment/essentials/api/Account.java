package de.coerdevelopment.essentials.api;

import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

public class Account {

    public int accountId;
    public String mail;
    public Date createdDate;
    public Date birthday;
    public int age;
    public String firstName;
    public String lastName;
    public String username;
    public String nationality;
    public String location;
    public String instagramUrl;
    public String twitterUrl;
    public String facebookUrl;
    public String linkedinUrl;
    public String websiteUrl;
    public String aboutMe;
    public String profilePictureUrl;
    public boolean isPrivate;
    public boolean mailVerified;

    public Account() {
    }

    public Account(int accountId, String mail, Date createdDate, Date birthday, String firstName, String lastName, String username, String nationality, String location, String instagramUrl, String twitterUrl, String facebookUrl, String linkedinUrl, String websiteUrl, String aboutMe, String profilePictureUrl, boolean isPrivate, boolean mailVerified) {
        this.accountId = accountId;
        this.mail = mail;
        this.createdDate = createdDate;
        this.birthday = birthday;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.nationality = nationality;
        this.location = location;
        this.instagramUrl = instagramUrl;
        this.twitterUrl = twitterUrl;
        this.facebookUrl = facebookUrl;
        this.linkedinUrl = linkedinUrl;
        this.websiteUrl = websiteUrl;
        this.aboutMe = aboutMe;
        this.profilePictureUrl = profilePictureUrl;
        this.isPrivate = isPrivate;
        this.mailVerified = mailVerified;
        calcAge();
    }

    private void calcAge() {
        if (birthday != null) {
            LocalDate birthdate = new java.sql.Date(birthday.getTime()).toLocalDate();
            LocalDate now = LocalDate.now();
            Period between = Period.between(birthdate, now);
            age = between.getYears();
        }
    }
}

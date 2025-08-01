package de.coerdevelopment.essentials.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.coerdevelopment.essentials.utils.CurrencyUnitDeserializer;
import de.coerdevelopment.essentials.utils.CurrencyUnitSerializer;

import javax.money.CurrencyUnit;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Locale;

public class Account {

    public int accountId;
    public String email;
    public OffsetDateTime createdAt;
    public LocalDate birthday;
    public int age;
    public String firstName;
    public String lastName;
    public String username;
    public String phoneNumber;
    public String nationality;
    public String location;
    public Locale locale;
    @JsonSerialize(using = CurrencyUnitSerializer.class)
    @JsonDeserialize(using = CurrencyUnitDeserializer.class)
    public CurrencyUnit preferredCurrency;
    public String instagramUrl;
    public String twitterUrl;
    public String facebookUrl;
    public String linkedinUrl;
    public String websiteUrl;
    public String aboutMe;
    public String profilePictureUrl;
    public boolean isPrivate;
    public boolean isLocked;
    public boolean mailVerified;

    public Account() {
    }

    public Account(int accountId, String email, OffsetDateTime createdAt, LocalDate birthday, String firstName, String lastName, String username, String phoneNumber, String nationality, String location, Locale locale, CurrencyUnit preferredCurrency, String instagramUrl, String twitterUrl, String facebookUrl, String linkedinUrl, String websiteUrl, String aboutMe, String profilePictureUrl, boolean isPrivate, boolean isLocked, boolean mailVerified) {
        this.accountId = accountId;
        this.email = email;
        this.createdAt = createdAt;
        this.birthday = birthday;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.nationality = nationality;
        this.location = location;
        this.locale = locale;
        this.preferredCurrency = preferredCurrency;
        this.instagramUrl = instagramUrl;
        this.twitterUrl = twitterUrl;
        this.facebookUrl = facebookUrl;
        this.linkedinUrl = linkedinUrl;
        this.websiteUrl = websiteUrl;
        this.aboutMe = aboutMe;
        this.profilePictureUrl = profilePictureUrl;
        this.isPrivate = isPrivate;
        this.isLocked = isLocked;
        this.mailVerified = mailVerified;
        calcAge();
    }

    private void calcAge() {
        if (birthday != null) {
            age = Period.between(birthday, LocalDate.now()).getYears();
        }
    }
}

package de.coerdevelopment.essentials.module;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ModuleType {

    SQL("SQL", Map.of(
            "host", "localhost",
            "username", "username",
            "password", "password",
            "database", "database",
            "port", 3306,
            "type", "mysql",
            "minPoolSize", 2,
            "maxPoolSize", 8
    ), List.of()),
    MAIL("Mail", Map.of(
            "host", "host",
            "port", 587,
            "username", "username",
            "password", "password",
            "fromMail", "from"
    ), List.of()),
    ACCOUNT("Account", getAccountOptions(), List.of(SQL, MAIL));

    public String name;
    public Map<String, Object> options;
    public List<ModuleType> dependencies;

    ModuleType(String name, Map<String, Object> options, List<ModuleType> dependencies) {
        this.name = name;
        this.options = options;
        this.dependencies = dependencies;
    }

    private static Map<String, Object> getAccountOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", "accounts");
        options.put("saltLength", 16);
        options.put("hashAlgorithm", "SHA-256");
        options.put("mailConfirmationEnabled", true);
        options.put("mailConfirmationTokenLength", 8);
        options.put("mailConfirmationTokenExpirationMilliseconds", 1000 * 60 * 60);
        options.put("passwordResetExpirationMilliseconds", 1000 * 60 * 30);
        options.put("tokenExpirationMilliseconds", Duration.ofMinutes(15).toMillis());
        options.put("refreshTokenExpiration", Duration.ofDays(7).toMillis());
        options.put("refreshTokenSecure", true);
        options.put("maxLoginTriesInShortTime", 10);
        options.put("maxPasswordResetTriesInShortTime", 3);
        options.put("resetPasswordUrl", "https://sample.com/resetPassword?%token%");
        options.put("spamProtectionEnabled", true);
        options.put("spamProtectionTimeFrameMilliseconds", 1000*60);
        options.put("spamProtectionMaxRequests", 30);
        options.put("profilePictureStorage", "local");
        options.put("profilePictureStorageOptions", Map.of(
                "storageDirectory", "profile_pictures",
                "supportedMimeTypes", List.of("image/jpeg", "image/png"),
                "maxFileSizeBytes", 1024 * 1024 * 5,
                "maxStorageSizeBytes", -1
        ));
        return options;
    }
}

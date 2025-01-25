package de.coerdevelopment.essentials.module;

import java.util.List;
import java.util.Map;

public enum ModuleType {

    SQL("SQL", Map.of(
            "host", "localhost",
            "username", "username",
            "password", "password",
            "database", "database",
            "port", 3306,
            "type", "mysql"
    ), List.of()),
    MAIL("Mail", Map.of(
            "host", "host",
            "port", 587,
            "username", "username",
            "password", "password",
            "fromMail", "from"
    ), List.of()),
    ACCOUNT("Account", Map.of(
            "tableName", "Account",
            "saltLenght", 16,
            "hashAlgorithm", "SHA-256",
            "mailConfirmationEnabled", true,
            "mailConfirmationTokenLength", 8,
            "mailConfirmationTokenExpirationMilliseconds", 1000 * 60 * 60,
            "passwordResetExpirationMilliseconds", 1000 * 60 * 30,
            "tokenExpirationMilliseconds", 1000 * 60 * 60 * 24,
            "maxLoginTriesInShortTime", 10,
            "maxPasswordResetTriesInShortTime", 3
    ), List.of(SQL, MAIL));

    public String name;
    public Map<String, Object> options;
    public List<ModuleType> dependencies;

    ModuleType(String name, Map<String, Object> options, List<ModuleType> dependencies) {
        this.name = name;
        this.options = options;
        this.dependencies = dependencies;
    }
}

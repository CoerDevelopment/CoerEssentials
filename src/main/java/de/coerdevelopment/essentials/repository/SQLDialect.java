package de.coerdevelopment.essentials.repository;

public enum SQLDialect {
    MYSQL("jdbc:mysql://"),
    MARIADB("jdbc:mariadb://"),
    POSTGRESQL("jdbc:postgresql://");

    public String driverUrl;

    SQLDialect(String driverUrl) {
        this.driverUrl = driverUrl;
    }
}

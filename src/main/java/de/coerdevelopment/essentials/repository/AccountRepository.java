package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.security.CoerSecurity;

import javax.money.Monetary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AccountRepository extends Repository {
    public AccountRepository(String tableName) {
        super(tableName);
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("account_id");
        table.addUniqueString("email", 256, false);
        table.addString("password", 64, false);
        table.addString("salt", 64, false);
        table.addDateTimeWithTimezone("created_at", false);
        table.addBooleanWithDefault("is_locked", false);
        table.addDate("birthday", true);
        table.addString("first_name", 64, true);
        table.addString("last_name", 64, true);
        table.addString("username", 32, true);
        table.addString("phone_number", 32, true);
        table.addString("nationality", 64, true);
        table.addString("location", 64, true);
        table.addString("locale", 20, false);
        table.addString("preferred_currency", 3, false);
        table.addString("instagram_url", 128, true);
        table.addString("twitter_url", 128, true);
        table.addString("facebook_url", 128, true);
        table.addString("linked_in_url", 128, true);
        table.addString("website_url", 128, true);
        table.addString("about_me", 256, true);
        table.addString("profile_picture_url", 256, true);
        table.addBooleanWithDefault("is_private", false);
        table.addBooleanWithDefault("email_verified", false);
        table.addString("email_verification_code", 64, true);
        table.addLong("email_verification_code_expiration", true);
        table.addCheck("email = LOWER(email)");
        table.addCheck("email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'");
        sql.executeQuery(table.getCreateTableStatement());
    }

    public long insertAccount(String email, String password, String salt, Locale locale, String username, String firstName, String lastName) {
        AtomicLong accountId = new AtomicLong(-1);
        sql.executeQueryReturningKeys("INSERT INTO " + tableName + " (email, password, salt, created_at, locale, preferred_currency, username, first_name, last_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    accountId.set(rs.getLong(1));
                }
            }
        }, email, password, salt, OffsetDateTime.now(), locale.toLanguageTag(), Monetary.getCurrency(locale).getCurrencyCode(), username, firstName, lastName);
        return accountId.get();
    }

    public long getAccountIdByEmail(String email) {
        AtomicLong accountId = new AtomicLong(-1);
        sql.executeQuery("SELECT account_id FROM " + tableName + " WHERE email = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    accountId.set(rs.getLong("account_id"));
                }
            }
        }, email);
        return accountId.get();
    }

    public boolean doesEmailExists(String email) {
        AtomicBoolean exists = new AtomicBoolean(false);
        sql.executeQuery("SELECT account_id FROM " + tableName + " WHERE email = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                exists.set(rs.next());
            }
        }, email);
        return exists.get();
    }

    public boolean doesUsernameExists(String username) {
        AtomicBoolean exists = new AtomicBoolean(false);
        sql.executeQuery("SELECT account_id FROM " + tableName + " WHERE username = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                exists.set(rs.next());
            }
        }, username);
        return exists.get();
    }

    public String getEmail(long accountId) {
        AtomicReference<String> email = new AtomicReference<>();
        sql.executeQuery("SELECT email FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    email.set(rs.getString("email"));
                }
            }
        }, accountId);
        return email.get();
    }

    public boolean isEmailVerified(long accountId) {
        AtomicBoolean verified = new AtomicBoolean(false);
        sql.executeQuery("SELECT email_verified FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    verified.set(rs.getBoolean("email_verified"));
                }
            }
        }, accountId);
        return verified.get();
    }

    public void setEmailVerified(long accountId) {
        sql.executeQuery("UPDATE " + tableName + " SET email_verified = ?, email_verification_code = null, email_verification_code_expiration = null  WHERE account_id = ?", true, accountId);
    }

    public void setEmailVerificationCode(long accountId, String emailVerificationCode, long emailVerificationCodeExpiration) {
        sql.executeQuery("UPDATE " + tableName + " SET email_verification_code = ?, email_verification_code_expiration = ? WHERE account_id = ?", emailVerificationCode, emailVerificationCodeExpiration, accountId);
    }

    public boolean doesEmailVerificationCodeMatch(long accountId, String emailVerificationCode) throws Exception {
        AtomicBoolean verified = new AtomicBoolean(false);
        AtomicBoolean expired = new AtomicBoolean(false);
        sql.executeQuery("SELECT email_verification_code, email_verification_code_expiration FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    String dbEmailVerificationCode = rs.getString("email_verification_code");
                    long dbEmailVerificationCodeExpiration = rs.getLong("email_verification_code_expiration");
                    if (System.currentTimeMillis() > dbEmailVerificationCodeExpiration) {
                        expired.set(true);
                        return;
                    }
                    verified.set(dbEmailVerificationCode.equals(emailVerificationCode));
                }
            }
        }, accountId);
        if (expired.get()) {
            throw new Exception("Email verification code expired");
        }
        return verified.get();
    }

    public boolean isEmailVerificationPending(long accountId) {
        AtomicBoolean verified = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT email_verification_code_expiration FROM " + tableName + " WHERE account_id = ? AND email_verified = false", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    verified.set(rs.getLong("email_verification_code_expiration") > System.currentTimeMillis());
                }
            }
        }, accountId);
        return verified.get();
    }

    /**
     * Checks if the given credentials are correct and returns the accountId if they are
     * Otherwise an exception is thrown
     */
    public long getAccountIdIfPasswortMatches(String emailOrUsername, String password) throws Exception {
        AtomicBoolean matches = new AtomicBoolean(false);
        AtomicBoolean accountExists = new AtomicBoolean(false);
        AtomicLong accountId = new AtomicLong(-1);
        PreparedStatement statement = sql.executeQuery("SELECT account_id, password, salt FROM " + tableName + " WHERE email = ? OR username = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    accountExists.set(true);
                    String dbPassword = rs.getString("password");
                    String dbSalt = rs.getString("salt");
                    String hashedPassword = CoerSecurity.getInstance().hashPassword(password, dbSalt);
                    if (dbPassword.equals(hashedPassword)) {
                        matches.set(true);
                        accountId.set(rs.getLong("account_id"));
                    }
                }
            }
        }, emailOrUsername, emailOrUsername);
        if (!accountExists.get()) {
            throw new Exception("Account does not exist");
        }
        if (!matches.get()) {
            throw new Exception("Invalid credentials");
        }
        return accountId.get();
    }

    public void changePassword(long accountId, String password, String salt) {
        sql.executeQuery("UPDATE " + tableName + " SET password = ?, salt = ? WHERE account_id = ?", password, salt, accountId);
    }

    public boolean updateAccount(long accountId, Account account) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET " +
                    "birthday = ?, first_name = ?, last_name = ?, username = ?, phone_number = ?, nationality = ?, location = ?, locale = ?, preferred_currency = ?, instagram_url = ?, twitter_url = ?, facebook_url = ?, linked_in_url = ?, website_url = ?, about_me = ?, profile_picture_url = ?, is_private = ? WHERE account_id = ?");
            int index = 1;
            ps.setObject(index++, account.birthday);
            ps.setString(index++, account.firstName);
            ps.setString(index++, account.lastName);
            ps.setString(index++, account.username);
            ps.setString(index++, account.phoneNumber);
            ps.setString(index++, account.nationality);
            ps.setString(index++, account.location);
            ps.setString(index++, account.locale.toLanguageTag());
            ps.setString(index++, account.preferredCurrency.getCurrencyCode());
            ps.setString(index++, account.instagramUrl);
            ps.setString(index++, account.twitterUrl);
            ps.setString(index++, account.facebookUrl);
            ps.setString(index++, account.linkedinUrl);
            ps.setString(index++, account.websiteUrl);
            ps.setString(index++, account.aboutMe);
            ps.setString(index++, account.profilePictureUrl);
            ps.setBoolean(index++, account.isPrivate);
            ps.setLong(index++, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setProperty(long accountId, String property, Object value) {
        sql.executeQuery("UPDATE " + tableName + " SET " + property + " = ? WHERE account_id = ?", value, accountId);
    }

    public ConcurrentHashMap<Long, Account> getAllAccountsById() {
        ConcurrentHashMap<Long, Account> accounts = new ConcurrentHashMap<>();
        sql.executeQuery("SELECT * FROM " + tableName, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                while (rs.next()) {
                    accounts.put(rs.getLong("account_id"), getColumnMapper().getObjectFromResultSetEntry(rs));
                }
            }
        });
        return accounts;
    }

    public Account getAccount(long accountId) {
        AtomicReference<Account> account = new AtomicReference<>();
        sql.executeQuery("SELECT * FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    account.set(getColumnMapper().getObjectFromResultSetEntry(rs));
                }
            }
        }, accountId);
        return account.get();
    }

    public boolean deleteAccount(long accountId) {
        AtomicBoolean deleted = new AtomicBoolean(false);
        sql.executeQuery("DELETE FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                deleted.set(statement.getUpdateCount() > 0);
            }
        }, accountId);
        return deleted.get();
    }

    private ColumnMapper<Account> getColumnMapper() {
        return new ColumnMapper<Account>() {
            @Override
            public Account getObjectFromResultSetEntry(ResultSet resultSet) throws SQLException {
                return new Account(
                        resultSet.getLong("account_id"),
                        resultSet.getString("email"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("birthday", LocalDate.class),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("username"),
                        resultSet.getString("phone_number"),
                        resultSet.getString("nationality"),
                        resultSet.getString("location"),
                        Locale.forLanguageTag(resultSet.getString("locale")),
                        Monetary.getCurrency(resultSet.getString("preferred_currency")),
                        resultSet.getString("instagram_url"),
                        resultSet.getString("twitter_url"),
                        resultSet.getString("facebook_url"),
                        resultSet.getString("linked_in_url"),
                        resultSet.getString("website_url"),
                        resultSet.getString("about_me"),
                        resultSet.getString("profile_picture_url"),
                        resultSet.getBoolean("is_private"),
                        resultSet.getBoolean("is_locked"),
                        resultSet.getBoolean("email_verified")
                );
            }
        };
    }

}

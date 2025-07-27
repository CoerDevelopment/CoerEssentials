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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AccountRepository extends Repository {
    public AccountRepository(String tableName) {
        super(tableName);
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("account_id");
        table.addUniqueString("mail", 128, false);
        table.addString("password", 64, false);
        table.addString("salt", 64, false);
        table.addDateTimeWithTimezone("created_at", false);
        table.addBooleanWithDefault("is_locked", false);
        table.addDate("birthday", true);
        table.addString("first_name", 64, true);
        table.addString("last_name", 64, true);
        table.addString("username", 32, true);
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
        table.addBooleanWithDefault("mail_verified", false);
        table.addString("mail_verification_code", 64, true);
        table.addLong("mail_verification_code_expiration", true);
        sql.executeQuery(table.getCreateTableStatement());

    }

    public int insertAccount(String mail, String password, String salt, Locale locale) {
        AtomicInteger accountId = new AtomicInteger(-1);
        PreparedStatement statement = sql.executeQueryReturningKeys("INSERT INTO " + tableName + " (mail, password, salt, created_at, locale, preferred_currency) VALUES (?, ?, ?, ?, ?, ?)", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    accountId.set(rs.getInt(1));
                }
            }
        }, mail, password, salt, OffsetDateTime.now(), locale.toLanguageTag(), Monetary.getCurrency(locale).getCurrencyCode());
        return accountId.get();
    }

    public int getAccountIdByMail(String mail) {
        AtomicInteger accountId = new AtomicInteger(-1);
        PreparedStatement statement = sql.executeQuery("SELECT account_id FROM " + tableName + " WHERE mail = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    accountId.set(rs.getInt("account_id"));
                }
            }
        }, mail);
        return accountId.get();
    }

    public boolean doesMailExists(String mail) {
        AtomicBoolean exists = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT account_id FROM " + tableName + " WHERE mail = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                exists.set(rs.next());
            }
        }, mail);
        return exists.get();
    }

    public String getMail(int accountId) {
        AtomicReference<String> mail = new AtomicReference<>();
        PreparedStatement statement = sql.executeQuery("SELECT mail FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    mail.set(rs.getString("mail"));
                }
            }
        }, accountId);
        return mail.get();
    }

    public boolean isMailVerified(int accountId) {
        AtomicBoolean verified = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT mail_verified FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    verified.set(rs.getBoolean("mail_verified"));
                }
            }
        }, accountId);
        return verified.get();
    }

    public void setMailVerified(int accountId) {
        sql.executeQuery("UPDATE " + tableName + " SET mail_verified = ?, mail_verification_code = null, mail_verification_code_expiration = null  WHERE account_id = ?", true, accountId);
    }

    public void setMailVerificationCode(int accountId, String mailVerificationCode, long mailVerificationCodeExpiration) {
        sql.executeQuery("UPDATE " + tableName + " SET mail_verification_code = ?, mail_verification_code_expiration = ? WHERE account_id = ?", mailVerificationCode, mailVerificationCodeExpiration, accountId);
    }

    public boolean doesMailVerificationCodeMatch(int accountId, String mailVerificationCode) throws Exception {
        AtomicBoolean verified = new AtomicBoolean(false);
        AtomicBoolean expired = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT mail_verification_code, mail_verification_code_expiration FROM " + tableName + " WHERE account_id = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    String dbMailVerificationCode = rs.getString("mail_verification_code");
                    long dbMailVerificationCodeExpiration = rs.getLong("mail_verification_code_expiration");
                    if (System.currentTimeMillis() > dbMailVerificationCodeExpiration) {
                        expired.set(true);
                        return;
                    }
                    verified.set(dbMailVerificationCode.equals(mailVerificationCode));
                }
            }
        }, accountId);
        if (expired.get()) {
            throw new Exception("Mail verification code expired");
        }
        return verified.get();
    }

    public boolean isMailVerificationPending(int accountId) {
        AtomicBoolean verified = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT mail_verification_code_expiration FROM " + tableName + " WHERE account_id = ? AND mail_verified = false", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    verified.set(rs.getLong("mail_verification_code_expiration") > System.currentTimeMillis());
                }
            }
        }, accountId);
        return verified.get();
    }

    /**
     * Checks if the given credentials are correct and returns the accountId if they are
     * Otherwise an exception is thrown
     */
    public int getAccountIdIfPasswortMatches(String mail, String password) throws Exception {
        AtomicBoolean matches = new AtomicBoolean(false);
        AtomicBoolean accountExists = new AtomicBoolean(false);
        AtomicInteger accountId = new AtomicInteger(-1);
        PreparedStatement statement = sql.executeQuery("SELECT account_id, password, salt FROM " + tableName + " WHERE mail = ?", new StatementCustomAction() {
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
                        accountId.set(rs.getInt("account_id"));
                    }
                }
            }
        }, mail);
        if (!accountExists.get()) {
            throw new Exception("Account does not exist");
        }
        if (!matches.get()) {
            throw new Exception("Invalid credentials");
        }
        return accountId.get();
    }

    public void changePassword(int accountId, String password, String salt) {
        sql.executeQuery("UPDATE " + tableName + " SET password = ?, salt = ? WHERE account_id = ?", password, salt, accountId);
    }

    public boolean updateAccount(int accountId, Account account) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET " +
                    "birthday = ?, first_name = ?, last_name = ?, username = ?, nationality = ?, location = ?, locale = ?, preferred_currency = ?, instagram_url = ?, twitter_url = ?, facebook_url = ?, linked_in_url = ?, website_url = ?, about_me = ?, profile_picture_url = ?, is_private = ? WHERE account_id = ?");
            int index = 1;
            ps.setObject(index++, account.birthday);
            ps.setString(index++, account.firstName);
            ps.setString(index++, account.lastName);
            ps.setString(index++, account.username);
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
            ps.setInt(index++, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setProperty(int accountId, String property, Object value) {
        sql.executeQuery("UPDATE " + tableName + " SET " + property + " = ? WHERE account_id = ?", value, accountId);
    }

    public Map<Integer, Account> getAllAccountsById() {
        Map<Integer, Account> accounts = new HashMap<>();
        sql.executeQuery("SELECT * FROM " + tableName, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                while (rs.next()) {
                    accounts.put(rs.getInt("account_id"), getColumnMapper().getObjectFromResultSetEntry(rs));
                }
            }
        });
        return accounts;
    }

    public Account getAccount(int accountId) {
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

    public boolean deleteAccount(int accountId) {
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
                        resultSet.getInt("account_id"),
                        resultSet.getString("mail"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("birthday", LocalDate.class),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("username"),
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
                        resultSet.getBoolean("mail_verified")
                );
            }
        };
    }

}

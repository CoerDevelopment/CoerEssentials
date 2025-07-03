package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.security.CoerSecurity;

import java.sql.*;
import java.util.HashMap;
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
        table.addAutoKey("accountId");
        table.addUniqueString("mail", 128, false);
        table.addString("password", 64, false);
        table.addString("salt", 64, false);
        table.addDateTime("createdDate", false);
        table.addBooleanWithDefault("isLocked", false);
        table.addDate("birthday", true);
        table.addString("firstName", 64, true);
        table.addString("lastName", 64, true);
        table.addString("username", 32, true);
        table.addString("nationality", 64, true);
        table.addString("location", 64, true);
        table.addString("instagramUrl", 128, true);
        table.addString("twitterUrl", 128, true);
        table.addString("facebookUrl", 128, true);
        table.addString("linkedInUrl", 128, true);
        table.addString("websiteUrl", 128, true);
        table.addString("aboutMe", 256, true);
        table.addString("profilePictureUrl", 256, true);
        table.addBooleanWithDefault("isPrivate", false);
        table.addBooleanWithDefault("mailVerified", false);
        table.addString("mailVerificationCode", 64, true);
        table.addLong("mailVerificationCodeExpiration", true);
        sql.executeQuery(table.getCreateTableStatement());

    }

    public int insertAccount(String mail, String password, String salt) {
        AtomicInteger accountId = new AtomicInteger(-1);
        PreparedStatement statement = sql.executeQueryReturningKeys("INSERT INTO " + tableName + " (mail, password, salt, createdDate) VALUES (?, ?, ?, ?)", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    accountId.set(rs.getInt(1));
                }
            }
        }, mail, password, salt, new Timestamp(System.currentTimeMillis()));
        return accountId.get();
    }

    public int getAccountIdByMail(String mail) {
        AtomicInteger accountId = new AtomicInteger(-1);
        PreparedStatement statement = sql.executeQuery("SELECT accountId FROM " + tableName + " WHERE mail = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    accountId.set(rs.getInt("accountId"));
                }
            }
        }, mail);
        return accountId.get();
    }

    public boolean doesMailExists(String mail) {
        AtomicBoolean exists = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT accountId FROM " + tableName + " WHERE mail = ?", new StatementCustomAction() {
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
        PreparedStatement statement = sql.executeQuery("SELECT mail FROM " + tableName + " WHERE accountId = ?", new StatementCustomAction() {
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
        PreparedStatement statement = sql.executeQuery("SELECT mailVerified FROM " + tableName + " WHERE accountId = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    verified.set(rs.getBoolean("mailVerified"));
                }
            }
        }, accountId);
        return verified.get();
    }

    public void setMailVerified(int accountId) {
        sql.executeQuery("UPDATE " + tableName + " SET mailVerified = ?, mailVerificationCode = null, mailVerificationCodeExpiration = null  WHERE accountId = ?", true, accountId);
    }

    public void setMailVerificationCode(int accountId, String mailVerificationCode, long mailVerificationCodeExpiration) {
        sql.executeQuery("UPDATE " + tableName + " SET mailVerificationCode = ?, mailVerificationCodeExpiration = ? WHERE accountId = ?", mailVerificationCode, mailVerificationCodeExpiration, accountId);
    }

    public boolean doesMailVerificationCodeMatch(int accountId, String mailVerificationCode) throws Exception {
        AtomicBoolean verified = new AtomicBoolean(false);
        AtomicBoolean expired = new AtomicBoolean(false);
        PreparedStatement statement = sql.executeQuery("SELECT mailVerificationCode, mailVerificationCodeExpiration FROM " + tableName + " WHERE accountId = ?", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    String dbMailVerificationCode = rs.getString("mailVerificationCode");
                    long dbMailVerificationCodeExpiration = rs.getLong("mailVerificationCodeExpiration");
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
        PreparedStatement statement = sql.executeQuery("SELECT mailVerificationCodeExpiration FROM " + tableName + " WHERE accountId = ? AND mailVerified = false", new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    verified.set(rs.getLong("mailVerificationCodeExpiration") > System.currentTimeMillis());
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
        PreparedStatement statement = sql.executeQuery("SELECT accountId, password, salt FROM " + tableName + " WHERE mail = ?", new StatementCustomAction() {
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
                        accountId.set(rs.getInt("accountId"));
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
        sql.executeQuery("UPDATE " + tableName + " SET password = ?, salt = ? WHERE accountId = ?", password, salt, accountId);
    }

    public boolean updateAccount(int accountId, Account account) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET " +
                    "birthday = ?, firstName = ?, lastName = ?, username = ?, nationality = ?, location = ?, instagramUrl = ?, twitterUrl = ?, facebookUrl = ?, linkedInUrl = ?, websiteUrl = ?, aboutMe = ?, profilePictureUrl = ?, isPrivate = ? WHERE accountId = ?");
            ps.setDate(1, account.birthday != null ? new Date(account.birthday.getTime()) : null);
            ps.setString(2, account.firstName);
            ps.setString(3, account.lastName);
            ps.setString(4, account.username);
            ps.setString(5, account.nationality);
            ps.setString(6, account.location);
            ps.setString(7, account.instagramUrl);
            ps.setString(8, account.twitterUrl);
            ps.setString(9, account.facebookUrl);
            ps.setString(10, account.linkedinUrl);
            ps.setString(11, account.websiteUrl);
            ps.setString(12, account.aboutMe);
            ps.setString(13, account.profilePictureUrl);
            ps.setBoolean(14, account.isPrivate);
            ps.setInt(15, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setProperty(int accountId, String property, Object value) {
        sql.executeQuery("UPDATE " + tableName + " SET " + property + " = ? WHERE accountId = ?", value, accountId);
    }

    public Map<Integer, Account> getAllAccountsById() {
        Map<Integer, Account> accounts = new HashMap<>();
        sql.executeQuery("SELECT * FROM " + tableName, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.getResultSet();
                while (rs.next()) {
                    accounts.put(rs.getInt("accountId"), getColumnMapper().getObjectFromResultSetEntry(rs));
                }
            }
        });
        return accounts;
    }

    public Account getAccount(int accountId) {
        AtomicReference<Account> account = new AtomicReference<>();
        sql.executeQuery("SELECT * FROM " + tableName + " WHERE accountId = ?", new StatementCustomAction() {
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
        sql.executeQuery("DELETE FROM " + tableName + " WHERE accountId = ?", new StatementCustomAction() {
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
                        resultSet.getInt("accountId"),
                        resultSet.getString("mail"),
                        resultSet.getDate("createdDate"),
                        resultSet.getDate("birthday"),
                        resultSet.getString("firstName"),
                        resultSet.getString("lastName"),
                        resultSet.getString("username"),
                        resultSet.getString("nationality"),
                        resultSet.getString("location"),
                        resultSet.getString("instagramUrl"),
                        resultSet.getString("twitterUrl"),
                        resultSet.getString("facebookUrl"),
                        resultSet.getString("linkedInUrl"),
                        resultSet.getString("websiteUrl"),
                        resultSet.getString("aboutMe"),
                        resultSet.getString("profilePictureUrl"),
                        resultSet.getBoolean("isPrivate"),
                        resultSet.getBoolean("isLocked"),
                        resultSet.getBoolean("mailVerified")
                );
            }
        };
    }

}

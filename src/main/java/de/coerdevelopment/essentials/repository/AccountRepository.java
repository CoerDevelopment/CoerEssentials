package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.security.CoerSecurity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Table structure:
 *
 * accountId INT PK
 * mail VARCHAR(128) NN UQ
 * password VARCHAR(64) NN
 * salt VARCHAR(64) NN
 * firstName VARCHAR(64)
 * lastName VARCHAR(64)
 * username VARCHAR(32)
 * mailVerified BOOLEAN NN
 * mailVerificationCode VARCHAR(64)
 * mailVerificationCodeExpiration BIGINT
 */
public class AccountRepository extends Repository {
    public AccountRepository(String tableName) {
        super(tableName);
    }

    @Override
    public void createTable() {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "accountId INT PRIMARY KEY AUTO_INCREMENT," +
                    "mail VARCHAR(128) NOT NULL UNIQUE," +
                    "password VARCHAR(64) NOT NULL," +
                    "salt VARCHAR(64) NOT NULL," +
                    "firstName VARCHAR(64)," +
                    "lastName VARCHAR(64)," +
                    "username VARCHAR(32) UNIQUE," +
                    "mailVerified BOOLEAN NOT NULL DEFAULT false," +
                    "mailVerificationCode VARCHAR(64)," +
                    "mailVerificationCodeExpiration BIGINT" +
                    ")");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int insertAccount(String mail, String password, String salt) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("INSERT INTO " + tableName + " (mail, password, salt, mailVerified) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, mail);
            ps.setString(2, password);
            ps.setString(3, salt);
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {

        }
        return -1;
    }

    public int getAccountIdByMail(String mail) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT accountId FROM " + tableName + " WHERE mail = ?");
            ps.setString(1, mail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("accountId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean doesMailExists(String mail) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT accountId FROM " + tableName + " WHERE mail = ?");
            ps.setString(1, mail);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getMail(int accountId) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT mail FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("mail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMailVerified(int accountId) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT mailVerified FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("mailVerified");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setMailVerified(int accountId, boolean mailVerified) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE " + tableName + " SET mailVerified = ? WHERE accountId = ?");
            ps.setBoolean(1, mailVerified);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setMailVerificationCode(int accountId, String mailVerificationCode, long mailVerificationCodeExpiration) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE " + tableName + " SET mailVerificationCode = ?, mailVerificationCodeExpiration = ? WHERE accountId = ?");
            ps.setString(1, mailVerificationCode);
            ps.setLong(2, mailVerificationCodeExpiration);
            ps.setInt(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean doesMailVerificationCodeMatch(int accountId, String mailVerificationCode) throws Exception {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT mailVerificationCode, mailVerificationCodeExpiration FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbMailVerificationCode = rs.getString("mailVerificationCode");
                long dbMailVerificationCodeExpiration = rs.getLong("mailVerificationCodeExpiration");
                if (System.currentTimeMillis() > dbMailVerificationCodeExpiration) {
                    throw new Exception("Mail verification code expired");
                }
                return dbMailVerificationCode.equals(mailVerificationCode);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the given credentials are correct and returns the accountId if they are
     * Otherwise an exception is thrown
     */
    public int getAccountIdIfPasswortMatches(String mail, String password) throws Exception {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT accountId, password, salt FROM " + tableName + " WHERE mail = ?");
            ps.setString(1, mail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                String dbSalt = rs.getString("salt");
                String hashedPassword = CoerSecurity.getInstance().stringToHash(password + dbSalt);
                if (dbPassword.equals(hashedPassword)) {
                    return rs.getInt("accountId");
                } else {
                    throw new Exception("Password does not match");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void changePassword(int accountId, String password, String salt) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE " + tableName + " SET password = ?, salt = ? WHERE accountId = ?");
            ps.setString(1, password);
            ps.setString(2, salt);
            ps.setInt(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setFirstName(int accountId, String firstName) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE " + tableName + " SET firstName = ? WHERE accountId = ?");
            ps.setString(1, firstName);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLastName(int accountId, String lastName) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE " + tableName + " SET lastName = ? WHERE accountId = ?");
            ps.setString(1, lastName);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setUsername(int accountId, String username) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE " + tableName + " SET username = ? WHERE accountId = ?");
            ps.setString(1, username);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Account getAccount(int accountId) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("SELECT * FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Account(
                        rs.getInt("accountId"),
                        rs.getString("mail"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("username"),
                        rs.getBoolean("mailVerified")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteAccount(int accountId) {
        try {
            PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

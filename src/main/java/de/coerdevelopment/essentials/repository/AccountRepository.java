package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.Account;
import de.coerdevelopment.essentials.security.CoerSecurity;

import java.sql.*;

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
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(table.getCreateTableStatement(sql.getDialect()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int insertAccount(String mail, String password, String salt) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName + " (mail, password, salt, createdDate) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, mail);
            ps.setString(2, password);
            ps.setString(3, salt);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getAccountIdByMail(String mail) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT accountId FROM " + tableName + " WHERE mail = ?");
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
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT accountId FROM " + tableName + " WHERE mail = ?");
            ps.setString(1, mail);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getMail(int accountId) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT mail FROM " + tableName + " WHERE accountId = ?");
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
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT mailVerified FROM " + tableName + " WHERE accountId = ?");
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

    public void setMailVerified(int accountId) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET mailVerified = ?, mailVerificationCode = null, mailVerificationCodeExpiration = null  WHERE accountId = ?");
            ps.setBoolean(1, true);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setMailVerificationCode(int accountId, String mailVerificationCode, long mailVerificationCodeExpiration) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET mailVerificationCode = ?, mailVerificationCodeExpiration = ? WHERE accountId = ?");
            ps.setString(1, mailVerificationCode);
            ps.setLong(2, mailVerificationCodeExpiration);
            ps.setInt(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean doesMailVerificationCodeMatch(int accountId, String mailVerificationCode) throws Exception {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT mailVerificationCode, mailVerificationCodeExpiration FROM " + tableName + " WHERE accountId = ?");
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

    public boolean isMailVerificationPending(int accountId) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT mailVerificationCodeExpiration FROM " + tableName + " WHERE accountId = ? AND mailVerified = false");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("mailVerificationCodeExpiration") > System.currentTimeMillis();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Checks if the given credentials are correct and returns the accountId if they are
     * Otherwise an exception is thrown
     */
    public int getAccountIdIfPasswortMatches(String mail, String password) throws Exception {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT accountId, password, salt FROM " + tableName + " WHERE mail = ? AND isLocked = false");
            ps.setString(1, mail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                String dbSalt = rs.getString("salt");
                String hashedPassword = CoerSecurity.getInstance().hashPassword(password, dbSalt);
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
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET password = ?, salt = ? WHERE accountId = ?");
            ps.setString(1, password);
            ps.setString(2, salt);
            ps.setInt(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET " + property + " = ? WHERE accountId = ?");
            ps.setObject(1, value);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isAccountLocked(int accountId) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT isLocked FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("isLocked");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Account getAccount(int accountId) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Account(
                        rs.getInt("accountId"),
                        rs.getString("mail"),
                        rs.getDate("createdDate"),
                        rs.getDate("birthday"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("username"),
                        rs.getString("nationality"),
                        rs.getString("location"),
                        rs.getString("instagramUrl"),
                        rs.getString("twitterUrl"),
                        rs.getString("facebookUrl"),
                        rs.getString("linkedInUrl"),
                        rs.getString("websiteUrl"),
                        rs.getString("aboutMe"),
                        rs.getString("profilePictureUrl"),
                        rs.getBoolean("isPrivate"),
                        rs.getBoolean("mailVerified")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteAccount(int accountId) {
        try (Connection connection = sql.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM " + tableName + " WHERE accountId = ?");
            ps.setInt(1, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}

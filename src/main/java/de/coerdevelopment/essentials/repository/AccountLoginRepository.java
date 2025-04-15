package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.AccountLogin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountLoginRepository extends Repository {

    private static AccountLoginRepository instance;

    public static AccountLoginRepository getInstance() {
        if (instance == null) {
            instance = new AccountLoginRepository();
        }
        return instance;
    }

    private AccountLoginRepository() {
        super("AccountLogin");
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable("AccountLogin");
        table.addAutoKey("loginId");
        table.addString("mail", 255, false);
        table.addDateTime("timestamp", false);
        table.addBoolean("success", false);
        table.addString("failureReason", 255, true);
        sql.executeQuery(table.getCreateTableStatement());
    }

    public void insertLogins(List<AccountLogin> logins) {
        try {
            sql.batchInsert(tableName, logins,
                    T -> Map.of("mail", T.mail, "timestamp", T.timestamp, "success", T.success, "failureReason", T.failureReason),
                    500);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccountLogin> getAccountLogins(String mail) {
        List<AccountLogin> accountLogins = new ArrayList<>();
        String query = "SELECT * FROM AccountLogin WHERE mail = ?";
        sql.executeQuery(query, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet resultSet = statement.getResultSet();
                while (resultSet.next()) {
                    accountLogins.add(new AccountLogin(
                            resultSet.getInt("loginId"),
                            resultSet.getString("mail"),
                            resultSet.getTimestamp("timestamp"),
                            resultSet.getBoolean("success"),
                            resultSet.getString("failureReason")
                    ));
                }
            }
        }, mail);
        return accountLogins;
    }

}

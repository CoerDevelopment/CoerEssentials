package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.AccountLogin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
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
        super("account_logins");
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("login_id");
        table.addString("mail", 255, false);
        table.addDateTimeWithTimezone("login_at", false);
        table.addBoolean("success", false);
        table.addString("failure_reason", 255, true);
        sql.executeQuery(table.getCreateTableStatement());
    }

    public void insertLogins(List<AccountLogin> logins) {
        try {
            sql.batchInsert(tableName, logins, getColumnMapper(),500);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccountLogin> getAccountLogins(String mail) {
        List<AccountLogin> accountLogins = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE mail = ?";
        ColumnMapper<AccountLogin> columnMapper = getColumnMapper();
        sql.executeQuery(query, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet resultSet = statement.getResultSet();
                while (resultSet.next()) {
                    accountLogins.add(columnMapper.getObjectFromResultSetEntry(resultSet));
                }
            }
        }, mail);
        return accountLogins;
    }

    private ColumnMapper<AccountLogin> getColumnMapper() {
        return new ColumnMapper<AccountLogin>() {
            @Override
            public Map<String, Object> mapColumns(AccountLogin obj) {
                return Map.of("mail", obj.mail,
                        "login_at", obj.loginAt,
                        "success", obj.success,
                        "failure_reason", obj.failureReason);
            }

            @Override
            public AccountLogin getObjectFromResultSetEntry(ResultSet resultSet) throws SQLException {
                return new AccountLogin(
                        resultSet.getLong("login_id"),
                        resultSet.getString("mail"),
                        resultSet.getObject("login_at", OffsetDateTime.class),
                        resultSet.getBoolean("success"),
                        resultSet.getString("failure_reason")
                );
            }
        };
    }

}

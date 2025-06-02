package de.coerdevelopment.essentials.module;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.job.JobRepository;
import de.coerdevelopment.essentials.repository.SQL;

public class SQLModule extends Module {

    public SQLModule() {
        super(ModuleType.SQL);
    }

    /**
     * Tries to establish a connection to the SQL database
     * @return true if the connection was successful, otherwise false
     */
    public boolean initSQL() {
        String host = getStringOption("host");
        String username = getStringOption("username");
        String password = getStringOption("password");
        String database = getStringOption("database");
        int port = getIntOption("port");
        String type = getStringOption("type");
        int minPoolSize = getIntOption("minPoolSize");
        int maxPoolSize = getIntOption("maxPoolSize");
        SQL sql = SQL.newSQL(host, username, password, database, port, type, minPoolSize, maxPoolSize);
        try {
            sql.connect();
            Runtime.getRuntime().addShutdownHook(disconnectOnShutdownThread());
            CoerEssentials.getInstance().logInfo("Successfully established connection to SQL database.");
            JobRepository.getInstance().createTable();
            return sql.isPoolConnected();
        } catch (Exception e) {
            CoerEssentials.getInstance().logError("Error establishing connection to SQL database.");
            CoerEssentials.getInstance().logError("Check the login data in the config file: " + moduleConfig.getFilePath());
            CoerEssentials.getInstance().logError("Error: " + e.getMessage());
            return false;
        }
    }

    private Thread disconnectOnShutdownThread() {
        return new Thread(() -> {
            try {
                SQL.getSQL().disconnect();
                CoerEssentials.getInstance().logInfo("Successfully disconnected from SQL database.");
            } catch (Exception e) {
                CoerEssentials.getInstance().logError("Error disconnecting from SQL database.");
                CoerEssentials.getInstance().logError("Error: " + e.getMessage());
            }
        });
    }

}

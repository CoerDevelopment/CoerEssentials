package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.module.*;
import de.coerdevelopment.essentials.module.Module;

public class CoerEssentials {

    private static CoerEssentials instance;

    public static CoerEssentials getInstance() {
        if (instance == null) {
            instance = new CoerEssentials("Sample Programm");
        }
        return instance;
    }

    public static CoerEssentials newInstance(String programName) {
        instance = new CoerEssentials(programName);
        return instance;
    }

    /**
     * Name of the program which uses this framework
     */
    private String programName;
    public String configDirectory;
    private System.Logger logger;

    private CoerEssentials(String programName) {
        configDirectory = System.getProperty("user.home") + "/CoerEssentials/";
        logger = System.getLogger("CoerEssentials");
        this.programName = programName;
    }

    public Module enableModule(ModuleType moduleType) {
        return switch (moduleType) {
            case SQL -> new SQLModule();
            case ACCOUNT -> new AccountModule();
            case MAIL -> new MailModule();
        };
    }

    public Module getModule(ModuleType moduleType) {
        for (Module module : Module.registeredModules) {
            if (module.type == moduleType) {
                return module;
            }
        }
        return null;
    }

    /**
     * Get the account module if it is enabled
     */
    public AccountModule getAccountModule() {
        return (AccountModule) getModule(ModuleType.ACCOUNT);
    }

    /**
     * Get the mail module if it is enabled
     */
    public MailModule getMailModule() {
        return (MailModule) getModule(ModuleType.MAIL);
    }

    /**
     * Get the SQL module if it is enabled
     */
    public SQLModule getSQLModule() {
        return (SQLModule) getModule(ModuleType.SQL);
    }


    public String getProgramName() {
        return programName;
    }

    public void setConfigDirectory(String directory) {
        this.configDirectory = directory;
    }

    // Logging

    public void logInfo(String message) {
        logger.log(System.Logger.Level.INFO, message);
    }

    public void logWarning(String message) {
        logger.log(System.Logger.Level.WARNING, message);
    }

    public void logError(String message) {
        logger.log(System.Logger.Level.ERROR, message);
    }

    public void logDebug(String message) {
        logger.log(System.Logger.Level.DEBUG, message);
    }

}

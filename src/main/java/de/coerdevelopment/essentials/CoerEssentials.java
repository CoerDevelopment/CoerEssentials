package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.module.MailModule;
import de.coerdevelopment.essentials.module.Module;
import de.coerdevelopment.essentials.module.ModuleType;
import de.coerdevelopment.essentials.module.SQLModule;

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

    public String getProgramName() {
        return programName;
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

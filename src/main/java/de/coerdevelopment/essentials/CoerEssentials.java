package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.job.JobExecutor;
import de.coerdevelopment.essentials.module.*;
import de.coerdevelopment.essentials.module.Module;

import java.util.HashMap;
import java.util.Map;

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
    private Map<ModuleType, Module> modulesCache;

    private CoerEssentials(String programName) {
        configDirectory = System.getProperty("user.home") + "/CoerEssentials/";
        logger = System.getLogger("CoerEssentials");
        this.programName = programName;
        this.modulesCache = new HashMap<>();
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
        Module genericModule = modulesCache.get(ModuleType.ACCOUNT);
        if (genericModule == null) {
            genericModule = getModule(ModuleType.ACCOUNT);
            modulesCache.put(ModuleType.ACCOUNT, genericModule);
        }
        return (AccountModule) genericModule;
    }

    /**
     * Get the mail module if it is enabled
     */
    public MailModule getMailModule() {
        Module genericModule = modulesCache.get(ModuleType.MAIL);
        if (genericModule == null) {
            genericModule = getModule(ModuleType.MAIL);
            modulesCache.put(ModuleType.MAIL, genericModule);
        }
        return (MailModule) genericModule;
    }

    /**
     * Get the SQL module if it is enabled
     */
    public SQLModule getSQLModule() {
        Module genericModule = modulesCache.get(ModuleType.SQL);
        if (genericModule == null) {
            genericModule = getModule(ModuleType.SQL);
            modulesCache.put(ModuleType.SQL, genericModule);
        }
        return (SQLModule) genericModule;
    }

    public void startExecutingJobs() {
        JobExecutor.getInstance().init();
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

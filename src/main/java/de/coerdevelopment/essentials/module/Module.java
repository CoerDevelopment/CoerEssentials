package de.coerdevelopment.essentials.module;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {

    public static final List<Module> registeredModules = new ArrayList<>();

    public static boolean isModuleEnabled(ModuleType type) {
        return registeredModules.stream().anyMatch(module -> module.type == type);
    }

    public static Module getModule(ModuleType type) {
        for (Module module : registeredModules) {
            if (module.type == type) {
                return module;
            }
        }
        return null;
    }

    public ModuleType type;
    protected Map<String, Object> options;
    protected Config moduleConfig;

    public Module(ModuleType type) {
        this.type = type;
        this.options = type.options;
        this.moduleConfig = new Config(CoerEssentials.getInstance().configDirectory, type.name + "-config");
        reloadOptions();
        setDefaultOptions();
        if (checkDependencies()) {
            CoerEssentials.getInstance().logInfo("Module " + type.name + " successfully enabled.");
            registeredModules.add(this);
        }
    }

    private boolean checkDependencies() {
        for (ModuleType dependency : type.dependencies) {
            if (registeredModules.stream().noneMatch(module -> module.type == dependency)) {
                CoerEssentials.getInstance().logError("Module " + type.name + " is missing dependency " + dependency.name);
                CoerEssentials.getInstance().logError("Module " + type.name + " may not work as expected.");
                CoerEssentials.getInstance().logInfo("Note: Please activate the modules in the correct order.");
                return false;
            }
        }
        return true;
    }

    /**
     * Reload the options from the config file
     */
    public void reloadOptions() {
        Map<String, Object> loadedOptions = new HashMap<>();
        for (String key : options.keySet()) {
            Object value = moduleConfig.getObject(key);
            if (value == null) {
                value = options.get(key);
            }
            loadedOptions.put(key, value);
        }
        this.options = loadedOptions;
    }

    public void setDefaultOptions() {
        for (String key : options.keySet()) {
            moduleConfig.addDefault(key, options.get(key));
        }
        moduleConfig.save();
    }

    public String getStringOption(String key) {
        return (String) options.get(key);
    }

    public int getIntOption(String key) {
        return (int) options.get(key);
    }

    public boolean getBooleanOption(String key) {
        return (boolean) options.get(key);
    }

    public Long getLongOption(String key) {
        return (long) options.get(key);
    }

    public Object getOption(String key) {
        return options.get(key);
    }

}

package de.coerdevelopment.essentials.job;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.config.Config;

public class JobConfig extends Config {

    public int schedulerPoolSize;
    public int workerPoolSize;

    public JobConfig() {
        super(CoerEssentials.getInstance().configDirectory, "JobConfig.yml");
        initConfig();
        loadConfig();
    }

    public void initConfig() {
        addDefault("schedulerPoolSize", 16);
        addDefault("workerPoolSize", 16);

        save();
    }

    public void loadConfig() {
        schedulerPoolSize = getInt("schedulerPoolSize");
        workerPoolSize = getInt("workerPoolSize");
    }

}

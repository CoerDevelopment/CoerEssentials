package de.coerdevelopment.essentials.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.coerdevelopment.essentials.CoerEssentials;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class JobOptionsConfig {

    private static JobOptionsConfig instance;

    public static JobOptionsConfig getInstance() {
        if (instance == null) {
            instance = new JobOptionsConfig();
        }
        return instance;
    }

    public final String dir;
    private final Gson gson;

    private JobOptionsConfig() {
        dir = CoerEssentials.getInstance().configDirectory + "/jobs";
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveOptions(String name, JobOptions options, boolean override) {
        String filePath = dir + "/" + name + ".json";
        if (!override && Files.exists(Paths.get(filePath))) {
            return;
        }
        String json = gson.toJson(options);
        try {
            Files.createDirectories(Paths.get(dir));
            Files.writeString(Paths.get(filePath), json);
        } catch (java.io.IOException e) {
            CoerEssentials.getInstance().logError("Failed to save job options for '" + name + "': " + e.getMessage());
        }
    }

    public List<JobOptions> loadOptions() {
        try {
            return Files.list(Paths.get(dir))
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> {
                        try {
                            String json = Files.readString(path);
                            return gson.fromJson(json, JobOptions.class);
                        } catch (java.io.IOException e) {
                            CoerEssentials.getInstance().logError("Failed to load job options from '" + path + "': " + e.getMessage());
                            return null;
                        }
                    })
                    .toList();
        } catch (java.io.IOException e) {
            CoerEssentials.getInstance().logError("Failed to load job options: " + e.getMessage());
            return List.of();
        }
    }
}

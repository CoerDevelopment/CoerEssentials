package de.coerdevelopment.essentials.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Author: Eric Dupont
 * Created: 2025
 *
 * Description: This class can be used to implement basic configuration from outside
 */
public class Config {

    private final Path directory;
    private final Path filePath;

    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private CommentedConfigurationNode rootNode;

    public Config(String directory, String filename) {
        filename = filename.endsWith(".yml") ? filename : filename + ".yml";
        this.directory = Paths.get(directory);
        this.filePath = this.directory.resolve(filename);

        try {
            if (!Files.exists(this.directory)) {
                Files.createDirectories(this.directory);
            }

            this.loader = YamlConfigurationLoader.builder()
                    .path(this.filePath)
                    .headerMode(HeaderMode.PRESET)
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            if (!Files.exists(this.filePath)) {
                Files.createFile(this.filePath);
            }

            this.rootNode = loader.load(ConfigurationOptions.defaults());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the current state of the configuration to the file of the given directory
     */
    public void save() {
        try {
            loader.save(rootNode);
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] splitKey(String key) {
        return key.split("\\.");
    }

    // SETTER

    public void setString(String key, String value) {
        setObject(key, value);
    }

    public void setInt(String key, int value) {
        setObject(key, value);
    }

    public void setDouble(String key, double value) {
        setObject(key, value);
    }

    public void setBoolean(String key, boolean value) {
        setObject(key, value);
    }

    public void setLong(String key, long value) {
        setObject(key, value);
    }

    public void setFloat(String key, float value) {
        setObject(key, value);
    }

    private void setObject(String key, Object value) {
        try {
            rootNode.node(splitKey(key)).set(value);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    // DEFAULTS

    public void addDefault(String key, String value) {
        addDefault(key, (Object) value);
    }

    public void addDefault(String key, int value) {
        addDefault(key, (Object) value);
    }

    public void addDefault(String key, double value) {
        addDefault(key, (Object) value);
    }

    public void addDefault(String key, boolean value) {
        addDefault(key, (Object) value);
    }

    public void addDefault(String key, long value) {
        addDefault(key, (Object) value);
    }

    public void addDefault(String key, float value) {
        addDefault(key, (Object) value);
    }

    public void addDefault(String key, Object value) {
        CommentedConfigurationNode node = rootNode.node(splitKey(key));
        if (node.virtual()) {
            try {
                node.set(value);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // GETTER

    public String getString(String key) {
        return rootNode.node(splitKey(key)).getString();
    }

    public int getInt(String key) {
        return rootNode.node(splitKey(key)).getInt();
    }

    public double getDouble(String key) {
        return rootNode.node(splitKey(key)).getDouble();
    }

    public boolean getBoolean(String key) {
        return rootNode.node(splitKey(key)).getBoolean();
    }

    public long getLong(String key) {
        return rootNode.node(splitKey(key)).getLong();
    }

    public float getFloat(String key) {
        return rootNode.node(splitKey(key)).getFloat();
    }

    public Object getObject(String key) {
        return rootNode.node(splitKey(key)).raw();
    }

    public String getFilePath() {
        return filePath.toString();
    }

}

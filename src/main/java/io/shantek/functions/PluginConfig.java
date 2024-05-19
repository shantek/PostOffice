package io.shantek.functions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;

import io.shantek.PostOffice;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
@SuppressWarnings("SameParameterValue")
public class PluginConfig {

    private final PostOffice postOffice;

    public PluginConfig(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    public void reloadConfigFile() {
        try {
            postOffice.getLogger().info("Reloading config file."); // Print to the console

            File configFile = new File(postOffice.getDataFolder(), "config.yml");

            // Check if the config file exists
            if (!configFile.exists()) {
                postOffice.getLogger().info("Config file not found. Creating a new one...");

                // Create a new config file based on a template from resources
                saveDefaultConfig("config.yml", configFile);
            } else {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                // Check for missing keys
                @SuppressWarnings("unused") boolean keysMissing = checkForMissingKeys(config);

                // Save existing values of missing keys
                Map<String, Object> missingKeyValues = saveMissingKeyValues(config);

                // Create a fresh config file
                saveDefaultConfig("config.yml", configFile);

                // Load the new config
                config = YamlConfiguration.loadConfiguration(configFile);

                // Update the new config with missing key values
                updateConfigWithMissingKeyValues(config, missingKeyValues);

                postOffice.customBarrelName = getString(config, "custom-barrel-name", "pobox");
                postOffice.language.cantStackItems = getString(config, "cant-stack-items", postOffice.language.cantStackItems);
                postOffice.language.removeItemError = getString(config, "remove-item-error", postOffice.language.removeItemError);
                postOffice.language.offHandError = getString(config, "offhand-error", postOffice.language.offHandError);
                postOffice.language.hotBarError = getString(config, "hotbar-error", postOffice.language.hotBarError);
                postOffice.language.sentMessage = getString(config, "sent-message", postOffice.language.sentMessage);
                postOffice.language.receivedMessage = getString(config, "received-message", postOffice.language.receivedMessage);
                postOffice.language.gotMailMessage = getString(config, "got-mail-message", postOffice.language.gotMailMessage);
                postOffice.language.createError = getString(config, "create-error", postOffice.language.createError);
                postOffice.language.breakError = getString(config, "break-error", postOffice.language.breakError);
                postOffice.language.postboxCreated = getString(config, "postbox-created", postOffice.language.postboxCreated);
                postOffice.language.pluginUpToDate = getString(config, "plugin-up-to-date", postOffice.language.pluginUpToDate);

                postOffice.postBoxProtection = getBoolean(config, "postbox-protection", true);
                postOffice.updateNotificationEnabled = getBoolean(config, "update-notification", true);
                postOffice.consoleLogs = getBoolean(config, "console-logs", true);
                postOffice.gotMailDelay = getBoolean(config, "got-mail-delay", true);

            }

        } catch (Exception e) {
            postOffice.getLogger().log(Level.SEVERE, "An error occurred while reloading the config file", e);
        }
    }

    private boolean checkForMissingKeys(FileConfiguration config) {
        boolean keysMissing = false;

        // List of keys to check
        List<String> keysToCheck = Arrays.asList(
                "custom-barrel-name", "cant-stack-items", "remove-item-error", "offhand-error", "hotbar-error", "drop-item-error",
                "sent-message", "received-message", "got-mail-message", "update-notification", "postbox-protection",
                "create-error", "break-error", "console-logs", "postbox-created", "plugin-up-to-date", "got-mail-delay");

        // Check for missing keys
        for (String key : keysToCheck) {
            if (!config.contains(key)) {
                postOffice.getLogger().warning("Key '" + key + "' not found in the configuration file, reverting to the default.");
                keysMissing = true;
            }
        }
        return keysMissing;
    }

    private Map<String, Object> saveMissingKeyValues(FileConfiguration config) {
        Map<String, Object> missingKeyValues = new HashMap<>();

        // List of keys to check
        List<String> keysToCheck = Arrays.asList(
                "custom-barrel-name", "cant-stack-items", "remove-item-error", "offhand-error", "hotbar-error", "drop-item-error",
                "sent-message", "received-message", "got-mail-message", "update-notification", "postbox-protection",
                "create-error", "break-error", "console-logs", "postbox-created", "plugin-up-to-date", "got-mail-delay");

        // Save existing values of missing keys
        for (String key : keysToCheck) {
            if (config.contains(key)) {
                Object value = config.get(key);
                missingKeyValues.put(key, value);
            }
        }
        return missingKeyValues;
    }

    private void updateConfigWithMissingKeyValues(FileConfiguration config, Map<String, Object> missingKeyValues) {
        // Update the new config with missing key values
        for (Map.Entry<String, Object> entry : missingKeyValues.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        // Save the updated config
        saveConfigSilently(config);
    }

    private void saveDefaultConfig(String resourceName, File destination) {
        try (InputStream resourceStream = getClass().getResourceAsStream("/config/" + resourceName)) {
            if (resourceStream != null) {
                Files.copy(resourceStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                //getLogger().info("Default config file created successfully.");
            } else {
                postOffice.getLogger().warning("Failed to create default config file. Resource not found.");
            }
        } catch (IOException e) {
            postOffice.getLogger().log(Level.SEVERE, "Error creating default config file", e);
        }
    }

    private String getString(FileConfiguration config, String key, String defaultValue) {
        if (config.contains(key) && config.isString(key)) {
            String originalValue = config.getString(key);
            assert originalValue != null;
            String updatedValue = originalValue.replaceAll("(?m)^\\s+|\\s+$", "")  // Remove leading/trailing spaces, tabs, and indentation
                    .replaceAll("\\s+", " ");  // Collapse multiple spaces into a single space

            // Log removal to the console if changes were made
            if (!originalValue.equals(updatedValue)) {
                if (postOffice.consoleLogs) {
                    postOffice.getLogger().info("Extra spaces removed from key '" + key + "'");
                }
            }


            // Check for a string split across two lines
            if (!originalValue.equals(updatedValue) && originalValue.contains("\n")) {
                updatedValue = originalValue.replace("\n", "");  // Remove newline characters
                if (postOffice.consoleLogs) {
                    postOffice.getLogger().info("Indentation removed from key '" + key + "'");
                }
            }



            // Save the updated value back to the config
            config.set(key, updatedValue);
            saveConfigSilently(config); // Custom method to save the configuration without logging exceptions

            return updatedValue;
        } else {
            // Log a warning if the key is not found or is of unexpected type
            postOffice.getLogger().warning("Key '" + key + "' not found in the configuration file, reverting to the default.");

            // Set the default value in the configuration
            config.set(key, defaultValue);

            // Save the configuration with the default value
            saveConfigSilently(config); // Custom method to save the configuration without logging exceptions

            return defaultValue;
        }
    }

    private boolean getBoolean(FileConfiguration config, String key, boolean defaultValue) {
        if (config.contains(key) && config.isBoolean(key)) {
            boolean originalValue = config.getBoolean(key);

            // Save the updated value back to the config
            config.set(key, originalValue);
            saveConfigSilently(config); // Custom method to save the configuration without logging exceptions

            return originalValue;
        } else {
            // Log a warning if the key is not found or is of unexpected type
            postOffice.getLogger().warning("Key '" + key + "' not found in the configuration file, reverting to the default.");

            // Set the default value in the configuration
            config.set(key, defaultValue);

            // Save the configuration with the default value
            saveConfigSilently(config); // Custom method to save the configuration without logging exceptions

            return defaultValue;
        }
    }

    private void saveConfigSilently(FileConfiguration config) {
        try {
            config.save(new File(postOffice.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            // Log the exception or handle it as needed (e.g., printStackTrace())
        }
    }

    public void setCustomBarrelName(String newCustomBarrelName) {
        try {
            File configFile = new File(postOffice.getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Update the custom barrel name in the configuration
            config.set("custom-barrel-name", newCustomBarrelName);

            // Save the updated configuration
            saveConfigSilently(config);

            // Update the custom barrel name in the PostOffice instance
            postOffice.customBarrelName = newCustomBarrelName;

        } catch (Exception e) {
            postOffice.getLogger().log(Level.SEVERE, "An error occurred while updating the custom barrel name", e);
        }
    }

}

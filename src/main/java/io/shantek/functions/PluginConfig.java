package io.shantek.functions;

import io.shantek.PostOffice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.nio.file.*;
import java.util.*;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PluginConfig {

    private final PostOffice postOffice;

    public PluginConfig(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    private FileConfiguration barrelsConfig = null;
    private File barrelsConfigFile = null;

    public void reloadConfigFile() {
        try {
            postOffice.getLogger().info("Reloading config file."); // Print to the console

            File configFile = new File(postOffice.getDataFolder(), "config.yml");

            // Check if the config file exists
            if (!configFile.exists()) {
                postOffice.getLogger().info("No config file found. Generating a new one.");

                // Create a new config file based on a template from resources
                saveDefaultConfig("config.yml", configFile);
            } else {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                // Check for missing keys
                boolean keysMissing = checkForMissingConfigKeys(config);

                // Save existing values of missing keys
                Map<String, Object> missingKeyValues = saveMissingConfigKey(config);

                // Create a fresh config file
                saveDefaultConfig("config.yml", configFile);

                // Load and update the config
                config = YamlConfiguration.loadConfiguration(configFile);
                updateConfigWithMissingKeyValues(config, missingKeyValues);

                // Boolean settings/config
                postOffice.customBarrelName = getString(config, "custom-barrel-name", "pobox");
                postOffice.postBoxProtection = getBoolean(config, "postbox-protection", true);
                postOffice.updateNotificationEnabled = getBoolean(config, "update-notification", true);
                postOffice.consoleLogs = getBoolean(config, "console-logs", true);
                postOffice.gotMailDelay = getBoolean(config, "got-mail-delay", true);
                postOffice.signNotification = getBoolean(config, "sign-notification", true);
            }
        } catch (Exception e) {
            postOffice.getLogger().log(Level.SEVERE, "An error occurred while reloading the config file", e);
        }

        try {
            postOffice.getLogger().info("Reloading lang file."); // Print to the console

            File langFile = new File(postOffice.getDataFolder(), "lang.yml");

            // Check if the config file exists
            if (!langFile.exists()) {
                postOffice.getLogger().info("No lang file not found. Generating a new one.");

                // Create a new config file based on a template from resources
                saveDefaultConfig("lang.yml", langFile);
            } else {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);

                // Check for missing keys
                boolean keysMissing = checkForMissingLangKeys(config);

                // Save existing values of missing keys
                Map<String, Object> missingKeyValues = saveMissingLangKey(config);

                // Create a fresh config file
                saveDefaultConfig("lang.yml", langFile);

                // Load and update the config
                config = YamlConfiguration.loadConfiguration(langFile);
                updateConfigWithMissingKeyValues(config, missingKeyValues);

                // Language strings
                postOffice.language.sentMessage = getString(config, "sent-message", postOffice.language.sentMessage);
                postOffice.language.receivedMessage = getString(config, "received-message", postOffice.language.receivedMessage);
                postOffice.language.gotMailMessage = getString(config, "got-mail-message", postOffice.language.gotMailMessage);
                postOffice.language.noPermission = getString(config, "no-permission", postOffice.language.noPermission);
                postOffice.language.denyAction = getString(config, "deny-action", postOffice.language.denyAction);
                postOffice.language.notRegistered = getString(config, "not-registered", postOffice.language.notRegistered);
                postOffice.language.postBoxRemoved = getString(config, "post-box-removed", postOffice.language.postBoxRemoved);
                postOffice.language.successfulRegistration = getString(config, "successful-registration", postOffice.language.successfulRegistration);
                postOffice.language.alreadyRegistered = getString(config, "already-registered", postOffice.language.alreadyRegistered);
                postOffice.language.postboxCreated = getString(config, "postbox-created", postOffice.language.postboxCreated);
                postOffice.language.removeFromConfig = getString(config, "remove-from-config", postOffice.language.removeFromConfig);
                postOffice.language.lookAtPostBox = getString(config, "look-at-post-box", postOffice.language.lookAtPostBox);
                postOffice.language.signOnBarrel = getString(config, "sign-on-barrel", postOffice.language.signOnBarrel);
                postOffice.language.alreadyClaimed = getString(config, "already-claimed", postOffice.language.alreadyClaimed);
                postOffice.language.invalidPostbox = getString(config, "invalid-postbox", postOffice.language.invalidPostbox);
                postOffice.language.successfullyClaimed = getString(config, "successfully-claimed", postOffice.language.successfullyClaimed);
                postOffice.language.modifySign = getString(config, "modify-sign", postOffice.language.modifySign);
                postOffice.language.unclaimedPostbox = getString(config, "unclaimed-postbox", postOffice.language.unclaimedPostbox);
                postOffice.language.userBanned = getString(config, "user-banned", postOffice.language.userBanned);
                postOffice.language.postBoxOwner = getString(config, "post-box-owner", postOffice.language.postBoxOwner);
                postOffice.language.claimedFor = getString(config, "claimed-for", postOffice.language.claimedFor);
                postOffice.language.alreadyHasPostBox = getString(config, "already-has-postbox", postOffice.language.alreadyHasPostBox);
                postOffice.language.notPlayedBefore = getString(config, "not-played-before", postOffice.language.notPlayedBefore);

                postOffice.language.pluginUpToDate = getString(config, "plugin-up-to-date", postOffice.language.pluginUpToDate);

            }
        } catch (Exception e) {
            postOffice.getLogger().log(Level.SEVERE, "An error occurred while reloading the lang file", e);
        }
    }

    private boolean checkForMissingConfigKeys(FileConfiguration config) {
        boolean keysMissing = false;

        // List of keys to check
        List<String> keysToCheck = Arrays.asList(
                "custom-barrel-name", "sign-notification", "got-mail-delay", "update-notification", "postbox-protection", "console-logs"
        );

        // Check for missing keys
        for (String key : keysToCheck) {
            if (!config.contains(key)) {
                postOffice.getLogger().warning("Key '" + key + "' not found in the configuration file, reverting to the default.");
                keysMissing = true;
            }
        }
        return keysMissing;
    }

    private boolean checkForMissingLangKeys(FileConfiguration config) {
        boolean keysMissing = false;

        // List of keys to check
        List<String> keysToCheck = Arrays.asList(
                "sent-message",
                "received-message",
                "got-mail-message",
                "no-permission", "deny-action",
                "not-registered",
                "post-box-removed",
                "successful-registration",
                "already-registered",
                "postbox-created",
                "remove-from-config",
                "look-at-post-box",
                "sign-on-barrel",
                "already-claimed",
                "invalid-postbox",
                "successfully-claimed",
                "modify-sign",
                "unclaimed-postbox",
                "user-banned",
                "post-box-owner",
                "claimed-for",
                "already-has-postbox",
                "not-played-before",
                "plugin-up-to-date"
        );


        // Check for missing keys
        for (String key : keysToCheck) {
            if (!config.contains(key)) {
                postOffice.getLogger().warning("Key '" + key + "' not found in the configuration file, reverting to the default.");
                keysMissing = true;
            }
        }
        return keysMissing;
    }

    private Map<String, Object> saveMissingLangKey(FileConfiguration config) {
        Map<String, Object> missingKeyValues = new HashMap<>();

        // List of keys to check
        List<String> keysToCheck = Arrays.asList(
                "sent-message",
                "received-message",
                "got-mail-message",
                "no-permission", "deny-action",
                "not-registered",
                "post-box-removed",
                "successful-registration",
                "already-registered",
                "postbox-created",
                "remove-from-config",
                "look-at-post-box",
                "sign-on-barrel",
                "already-claimed",
                "invalid-postbox",
                "successfully-claimed",
                "modify-sign",
                "unclaimed-postbox",
                "user-banned",
                "post-box-owner",
                "claimed-for",
                "already-has-postbox",
                "not-played-before",
                "plugin-up-to-date"
        );

        // Save existing values of missing keys
        for (String key : keysToCheck) {
            if (config.contains(key)) {
                Object value = config.get(key);
                missingKeyValues.put(key, value);
            }
        }
        return missingKeyValues;
    }

    private Map<String, Object> saveMissingConfigKey(FileConfiguration config) {
        Map<String, Object> missingKeyValues = new HashMap<>();

        // List of keys to check
        List<String> keysToCheck = Arrays.asList(
                "custom-barrel-name", "sign-notification", "got-mail-delay", "update-notification", "postbox-protection", "console-logs"
        );

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
        try (InputStream resourceStream = getClass().getResourceAsStream("/" + resourceName)) {
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
            postOffice.getLogger().log(Level.SEVERE, "An error occurred while updating the config file", e);
        }
    }

    public void reloadBarrelsConfig() {
        if (barrelsConfigFile == null) {
            barrelsConfigFile = new File(postOffice.getDataFolder(), "barrels.yml");
        }
        barrelsConfig = YamlConfiguration.loadConfiguration(barrelsConfigFile);

        // Check if the file exists, and if not, create it
        if (!barrelsConfigFile.exists()) {
            postOffice.saveResource("barrels.yml", false);
        }
    }

    // Get the barrels.yml configuration
    public FileConfiguration getBarrelsConfig() {
        if (barrelsConfig == null) {
            reloadBarrelsConfig();
        }
        return barrelsConfig;
    }

    // Save changes to barrels.yml
    public void saveBarrelsConfig() {
        if (barrelsConfig == null || barrelsConfigFile == null) {
            return;
        }
        try {
            barrelsConfig.save(barrelsConfigFile);
        } catch (IOException e) {
            postOffice.getLogger().severe("Could not save barrels.yml: " + e.getMessage());
        }
    }
}

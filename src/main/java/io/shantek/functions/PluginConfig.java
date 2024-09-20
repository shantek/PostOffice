package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class PluginConfig {

    private final PostOffice postOffice;

    public PluginConfig(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    private FileConfiguration barrelsConfig = null;
    private File barrelsConfigFile = null;

    public void reloadConfigFile() {
        // Handle config.yml
        handleFile("config.yml", this::loadConfigFile);

        // Handle lang.yml
        handleFile("lang.yml", this::loadLangFile);
    }

    private void handleFile(String fileName, Runnable loadAction) {
        File file = new File(postOffice.getDataFolder(), fileName);
        if (!file.exists()) {
            postOffice.getLogger().info(fileName + " not found. Creating a new one...");
            saveDefaultConfig(fileName, file);  // Create default file if not exists
        } else {
            postOffice.getLogger().info(fileName + " found. Reloading and checking for missing keys...");
            loadAction.run();
        }
    }

    private void loadConfigFile() {
        File configFile = new File(postOffice.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        if (checkForMissingConfigKeys(config)) {
            postOffice.getLogger().info("Updating config with missing keys...");
            updateConfigWithMissingKeyValues(config, saveMissingConfigKey(config), configFile);
        }
        postOffice.customBarrelName = getString(config, "custom-barrel-name", "pobox");
        postOffice.postBoxProtection = getBoolean(config, "postbox-protection", true);
        postOffice.updateNotificationEnabled = getBoolean(config, "update-notification", true);
        postOffice.consoleLogs = getBoolean(config, "console-logs", false);
        postOffice.gotMailDelay = getBoolean(config, "got-mail-delay", true);
        postOffice.signNotification = getBoolean(config, "sign-notification", true);
    }

    private void loadLangFile() {
        File langFile = new File(postOffice.getDataFolder(), "lang.yml");
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        if (checkForMissingLangKeys(langConfig)) {
            postOffice.getLogger().info("Updating lang with missing keys...");
            updateConfigWithMissingKeyValues(langConfig, saveMissingLangKey(langConfig), langFile);
        }
        postOffice.language.sentMessage = getString(langConfig, "sent-message", postOffice.language.sentMessage);
        postOffice.language.receivedMessage = getString(langConfig, "received-message", postOffice.language.receivedMessage);
        postOffice.language.gotMailMessage = getString(langConfig, "got-mail-message", postOffice.language.gotMailMessage);
        postOffice.language.noPermission = getString(langConfig, "no-permission", postOffice.language.noPermission);
        postOffice.language.denyAction = getString(langConfig, "deny-action", postOffice.language.denyAction);
        postOffice.language.notRegistered = getString(langConfig, "not-registered", postOffice.language.notRegistered);
        postOffice.language.postBoxRemoved = getString(langConfig, "post-box-removed", postOffice.language.postBoxRemoved);
        postOffice.language.successfulRegistration = getString(langConfig, "successful-registration", postOffice.language.successfulRegistration);
        postOffice.language.alreadyRegistered = getString(langConfig, "already-registered", postOffice.language.alreadyRegistered);
        postOffice.language.postboxCreated = getString(langConfig, "postbox-created", postOffice.language.postboxCreated);
        postOffice.language.removeFromConfig = getString(langConfig, "remove-from-config", postOffice.language.removeFromConfig);
        postOffice.language.lookAtPostBox = getString(langConfig, "look-at-post-box", postOffice.language.lookAtPostBox);
        postOffice.language.signOnBarrel = getString(langConfig, "sign-on-barrel", postOffice.language.signOnBarrel);
        postOffice.language.alreadyClaimed = getString(langConfig, "already-claimed", postOffice.language.alreadyClaimed);
        postOffice.language.invalidPostbox = getString(langConfig, "invalid-postbox", postOffice.language.invalidPostbox);
        postOffice.language.successfullyClaimed = getString(langConfig, "successfully-claimed", postOffice.language.successfullyClaimed);
        postOffice.language.modifySign = getString(langConfig, "modify-sign", postOffice.language.modifySign);
        postOffice.language.unclaimedPostbox = getString(langConfig, "unclaimed-postbox", postOffice.language.unclaimedPostbox);
        postOffice.language.userBanned = getString(langConfig, "user-banned", postOffice.language.userBanned);
        postOffice.language.postBoxOwner = getString(langConfig, "post-box-owner", postOffice.language.postBoxOwner);
        postOffice.language.claimedFor = getString(langConfig, "claimed-for", postOffice.language.claimedFor);
        postOffice.language.alreadyHasPostBox = getString(langConfig, "already-has-postbox", postOffice.language.alreadyHasPostBox);
        postOffice.language.notPlayedBefore = getString(langConfig, "not-played-before", postOffice.language.notPlayedBefore);
        postOffice.language.pluginUpToDate = getString(langConfig, "plugin-up-to-date", postOffice.language.pluginUpToDate);
    }

    private boolean checkForMissingConfigKeys(FileConfiguration config) {
        return checkForMissingKeys(config, Arrays.asList(
                "custom-barrel-name", "sign-notification", "got-mail-delay", "update-notification", "postbox-protection", "console-logs"
        ));
    }

    private boolean checkForMissingLangKeys(FileConfiguration config) {
        return checkForMissingKeys(config, Arrays.asList(
                "sent-message", "received-message", "got-mail-message", "no-permission", "deny-action",
                "not-registered", "post-box-removed", "successful-registration", "already-registered",
                "postbox-created", "remove-from-config", "look-at-post-box", "sign-on-barrel",
                "already-claimed", "invalid-postbox", "successfully-claimed", "modify-sign",
                "unclaimed-postbox", "user-banned", "post-box-owner", "claimed-for",
                "already-has-postbox", "not-played-before", "plugin-up-to-date"
        ));
    }

    private boolean checkForMissingKeys(FileConfiguration config, List<String> keysToCheck) {
        boolean keysMissing = false;
        for (String key : keysToCheck) {
            if (!config.contains(key)) {
                postOffice.getLogger().warning("Key '" + key + "' not found in the file, reverting to the default.");
                keysMissing = true;
            }
        }
        return keysMissing;
    }

    private Map<String, Object> saveMissingConfigKey(FileConfiguration config) {
        return saveMissingKeys(config, Arrays.asList(
                "custom-barrel-name", "sign-notification", "got-mail-delay", "update-notification", "postbox-protection", "console-logs"
        ));
    }

    private Map<String, Object> saveMissingLangKey(FileConfiguration config) {
        return saveMissingKeys(config, Arrays.asList(
                "sent-message", "received-message", "got-mail-message", "no-permission", "deny-action",
                "not-registered", "post-box-removed", "successful-registration", "already-registered",
                "postbox-created", "remove-from-config", "look-at-post-box", "sign-on-barrel",
                "already-claimed", "invalid-postbox", "successfully-claimed", "modify-sign",
                "unclaimed-postbox", "user-banned", "post-box-owner", "claimed-for",
                "already-has-postbox", "not-played-before", "plugin-up-to-date"
        ));
    }

    private Map<String, Object> saveMissingKeys(FileConfiguration config, List<String> keysToCheck) {
        Map<String, Object> missingKeyValues = new HashMap<>();
        for (String key : keysToCheck) {
            if (config.contains(key)) {
                missingKeyValues.put(key, config.get(key));
            }
        }
        return missingKeyValues;
    }

    private void updateConfigWithMissingKeyValues(FileConfiguration config, Map<String, Object> missingKeyValues, File destination) {
        for (Map.Entry<String, Object> entry : missingKeyValues.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        saveConfigSilently(config, destination);  // Pass the correct file to save to
    }

    private void saveDefaultConfig(String resourceName, File destination) {
        try (InputStream resourceStream = getClass().getResourceAsStream("/" + resourceName)) {
            if (resourceStream != null) {
                Files.copy(resourceStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                postOffice.getLogger().warning("Failed to create default " + resourceName + ". Resource not found.");
            }
        } catch (IOException e) {
            postOffice.getLogger().log(Level.SEVERE, "Error creating default file: " + resourceName, e);
        }
    }

    private String getString(FileConfiguration config, String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    private boolean getBoolean(FileConfiguration config, String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    private void saveConfigSilently(FileConfiguration config, File destination) {
        try {
            config.save(destination);
        } catch (IOException e) {
            postOffice.getLogger().log(Level.SEVERE, "An error occurred while saving the file " + destination.getName(), e);
        }
    }

    // Reload and manage barrels.yml
    public void reloadBarrelsConfig() {
        if (barrelsConfigFile == null) {
            barrelsConfigFile = new File(postOffice.getDataFolder(), "barrels.yml");
        }
        barrelsConfig = YamlConfiguration.loadConfiguration(barrelsConfigFile);
        if (!barrelsConfigFile.exists()) {
            postOffice.saveResource("barrels.yml", false);
        }
    }

    public FileConfiguration getBarrelsConfig() {
        if (barrelsConfig == null) {
            reloadBarrelsConfig();
        }
        return barrelsConfig;
    }

    public void saveBarrelsConfig() {
        if (barrelsConfig != null && barrelsConfigFile != null) {
            try {
                barrelsConfig.save(barrelsConfigFile);
            } catch (IOException e) {
                postOffice.getLogger().severe("Could not save barrels.yml: " + e.getMessage());
            }
        }
    }
}

package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;

public class PluginConfig {
    private final PostOffice postOffice;
    private final File configFile;
    private final File backupConfigFile;
    private final File backupLangFile;
    private final File langFile;
    private YamlConfiguration config;
    private YamlConfiguration langConfig;

    // Constructor
    public PluginConfig(PostOffice postOffice) {
        this.postOffice = postOffice;
        this.configFile = new File(postOffice.getDataFolder(), "config.yml");
        this.backupConfigFile = new File(postOffice.getDataFolder(), "config-backup.yml");
        this.backupLangFile = new File(postOffice.getDataFolder(), "lang-backup.yml");
        this.langFile = new File(postOffice.getDataFolder(), "lang.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    //region Configuration file

    public void initializeAndLoadConfig() {
        try {
            // Create config file if it's missing
            if (!configFile.exists()) {
                createDefaultConfig();
            }

            if (shouldReloadConfig()) {
                // Always create a proper backup
                createConfigBackup();

                // Reload the configuration to ensure it's up-to-date
                reloadConfig();
            } else {
                // Load the config values into memory directly if no reload is needed
                loadConfigIntoMemory();
            }
        } catch (Exception e) {
            postOffice.getLogger().severe("Error initializing and loading configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean shouldReloadConfig() {
        // Load the configuration from the file
        YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);

        // Add specific checks here to determine if reloading is necessary
        // For example, force a reload if the config version is outdated
        boolean needsReload = !isConfigValid(configFile)
                || newConfig.getInt("config-version") != getCurrentConfigVersion();

        if (needsReload) {
            postOffice.getLogger().info("Configuration needs reloading.");
        }

        return needsReload;
    }

    private int getCurrentConfigVersion() {
        // The version number in the default config file from the plugin resources
        try (InputStream defConfigStream = postOffice.getResource("config.yml")) {
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                return defConfig.getInt("config-version", 0);
            }
        } catch (IOException e) {
            postOffice.getLogger().severe("Failed to read the default configuration version: " + e.getMessage());
        }
        return 0; // Default version if the resource file cannot be read
    }

    private boolean isConfigValid(File configFile) {
        try {
            if (!configFile.exists()) {
                return false;
            }
            YamlConfiguration loadedConfig = new YamlConfiguration();
            loadedConfig.load(configFile);
            return loadedConfig.contains("config-version");
        } catch (IOException | InvalidConfigurationException e) {
            postOffice.getLogger().severe("Invalid configuration file detected: " + e.getMessage());
            return false;
        }
    }

    private void createConfigBackup() {
        try {
            if (configFile.exists()) {
                Files.copy(configFile.toPath(), backupConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                postOffice.getLogger().info("Configuration file backed up as config-backup.yml.");
            }
        } catch (IOException e) {
            postOffice.getLogger().severe("Failed to backup configuration file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultConfig() {
        try (InputStream defConfigStream = postOffice.getResource("config.yml")) {
            if (defConfigStream != null) {
                Files.copy(defConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                postOffice.getLogger().info("Configuration file created successfully");
            } else {
                postOffice.getLogger().severe("Default configuration file (config.yml) not found in resources!");
            }
        } catch (IOException e) {
            postOffice.getLogger().severe("An error occurred while creating the default configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {
            // Create default config if missing
            if (!configFile.exists()) {
                createDefaultConfig();
            }

            // Replace config.yml with the one from the resources folder first
            try (InputStream defConfigStream = postOffice.getResource("config.yml")) {
                if (defConfigStream == null) {
                    postOffice.getLogger().severe("Default configuration file (config.yml) not found in resources!");
                    return;
                }
                Files.copy(defConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
           }

            // Clear the in-memory configuration
            config = new YamlConfiguration();

            // Load the replaced default configuration file
            YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);

            // Load and merge values from the backup configuration if available
            if (isConfigValid(backupConfigFile)) {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupConfigFile);
                mergeConfigs(newConfig, backupConfig);
            }

            // Save the updated configuration file
            newConfig.save(configFile);
            postOffice.getLogger().info("Configuration file successfully migrated");

            // Reload the updated configuration into memory
            config.load(configFile);
            loadConfigIntoMemory();

        } catch (IOException | InvalidConfigurationException e) {
            postOffice.getLogger().severe("An error occurred while handling the configuration file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mergeConfigs(YamlConfiguration baseConfig, YamlConfiguration overrideConfig) {
        Set<String> keys = overrideConfig.getKeys(true);
        for (String key : keys) {
            if (!"config-version".equals(key)) {
                baseConfig.set(key, overrideConfig.get(key));
            }
        }
    }

    private void loadConfigIntoMemory() {

        // Load the configuration from the file
        YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);

        postOffice.customBarrelName = newConfig.getString("custom-barrel-name", "pobox");
        postOffice.signNotification = newConfig.getBoolean("sign-notification", true);
        postOffice.gotMailDelay = newConfig.getBoolean("got-mail-delay", true);
        postOffice.postBoxProtection = newConfig.getBoolean("postbox-protection", true);
        postOffice.hopperProtection = newConfig.getBoolean("hopper-protection", false);
        postOffice.updateNotificationEnabled = newConfig.getBoolean("update-notification", true);
        postOffice.consoleLogs = newConfig.getBoolean("console-logs", true);
        postOffice.debugLogs = newConfig.getBoolean("debug", false);
        postOffice.getLogger().info("Configuration file loaded into memory.");
    }

    //endregion

    //region Language file

    public void initializeAndLoadLang() {
        try {
            // Create lang file if it's missing
            if (!langFile.exists()) {
                createDefaultLang();
            }

            if (shouldReloadLang()) {
                // Always create a proper backup
                createLangBackup();

                // Reload the lang to ensure it's up-to-date
                reloadLang();
            } else {
                // Load the lang values into memory directly if no reload is needed
                loadLangIntoMemory();
            }
        } catch (Exception e) {
            postOffice.getLogger().severe("Error initializing and loading lang file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean shouldReloadLang() {
        // Load the lang from the file
        YamlConfiguration newLang = YamlConfiguration.loadConfiguration(langFile);

        // Add specific checks here to determine if reloading is necessary
        // For example, force a reload if the lang version is outdated
        boolean needsReload = !isLangValid(langFile)
                || newLang.getInt("lang-version") != getCurrentLangVersion();

        if (needsReload) {
            postOffice.getLogger().info("Lang file needs reloading.");
        }

        return needsReload;
    }

    private int getCurrentLangVersion() {
        // The version number in the default lang file from the plugin resources
        try (InputStream defLangStream = postOffice.getResource("lang.yml")) {
            if (defLangStream != null) {
                YamlConfiguration defLang = YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
                return defLang.getInt("lang-version", 0);
            }
        } catch (IOException e) {
            postOffice.getLogger().severe("Failed to read the default lang version: " + e.getMessage());
        }
        return 0; // Default version if the resource file cannot be read
    }

    private boolean isLangValid(File langFile) {
        try {
            if (!langFile.exists()) {
                return false;
            }
            YamlConfiguration loadedLang = new YamlConfiguration();
            loadedLang.load(langFile);
            return loadedLang.contains("lang-version");
        } catch (IOException | InvalidConfigurationException e) {
            postOffice.getLogger().severe("Invalid lang file detected: " + e.getMessage());
            return false;
        }
    }

    private void createLangBackup() {
        try {
            if (langFile.exists()) {
                Files.copy(langFile.toPath(), backupLangFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                postOffice.getLogger().info("Lang file backed up as lang-backup.yml.");
            }
        } catch (IOException e) {
            postOffice.getLogger().severe("Failed to backup lang file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultLang() {
        try (InputStream defLangStream = postOffice.getResource("lang.yml")) {
            if (defLangStream != null) {
                Files.copy(defLangStream, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                postOffice.getLogger().info("Lang file created successfully");
            } else {
                postOffice.getLogger().severe("Default lang file (lang.yml) not found in resources!");
            }
        } catch (IOException e) {
            postOffice.getLogger().severe("An error occurred while creating the default lang file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadLang() {
        try {
            // Create default lang if missing
            if (!langFile.exists()) {
                createDefaultLang();
            }

            // Replace lang.yml with the one from the resources folder first
            try (InputStream defLangStream = postOffice.getResource("lang.yml")) {
                if (defLangStream == null) {
                    postOffice.getLogger().severe("Default lang file (lang.yml) not found in resources!");
                    return;
                }
                Files.copy(defLangStream, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Clear the in-memory lang
            langConfig = new YamlConfiguration();

            // Load the replaced default lang file
            YamlConfiguration newLang = YamlConfiguration.loadConfiguration(langFile);

            // Load and merge values from the backup lang if available
            if (isLangValid(backupLangFile)) {
                YamlConfiguration backupLang = YamlConfiguration.loadConfiguration(backupLangFile);
                mergeLangs(newLang, backupLang);
            }

            // Save the updated lang file
            newLang.save(langFile);
            postOffice.getLogger().info("Lang file successfully migrated");

            // Reload the updated lang file into memory
            langConfig.load(langFile);
            loadLangIntoMemory();

        } catch (IOException | InvalidConfigurationException e) {
            postOffice.getLogger().severe("An error occurred while handling the lang file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mergeLangs(YamlConfiguration baseConfig, YamlConfiguration overrideConfig) {
        Set<String> keys = overrideConfig.getKeys(true);
        for (String key : keys) {
            if (!"lang-version".equals(key)) {
                baseConfig.set(key, overrideConfig.get(key));
            }
        }
    }

    private void loadLangIntoMemory() {

        // Load the lang from the file
        YamlConfiguration newLang = YamlConfiguration.loadConfiguration(langFile);

        postOffice.language.sentMessage = newLang.getString("sent-message", "&a[Post Office] &aMail sent to %receiver%.");
        postOffice.language.receivedMessage = newLang.getString("received-message", "&a[Post Office] &eYou received mail from %sender%!");
        postOffice.language.gotMailMessage = newLang.getString("got-mail-message","&a[Post Office] &fYou got mail!");
        postOffice.language.noPermission = newLang.getString("no-permission", "&a[Post Office] &4You don't have permission to do that.");
        postOffice.language.denyAction = newLang.getString("deny-action","&a[Post Office] &4You can't do that here!");
        postOffice.language.notRegistered = newLang.getString("not-registered","&a[Post Office] &4This isn't a registered post office box.");
        postOffice.language.postBoxRemoved = newLang.getString("post-box-removed","&a[Post Office] &aPost box removed successfully.");
        postOffice.language.successfulRegistration = newLang.getString("successful-registration","&a[Post Office] &aPost box registered successfully.");
        postOffice.language.alreadyRegistered = newLang.getString("already-registered","&a[Post Office] &4This post box is already registered.");
        postOffice.language.postboxCreated = newLang.getString("postbox-created","&a[Post Office] &4 Box successfully created for %username%");
        postOffice.language.removeFromConfig = newLang.getString("remove-from-config", "&a[Post Office] &aPost box successfully removed from the config.");
        postOffice.language.lookAtPostBox = newLang.getString("look-at-post-box","&a[Post Office] &4You must be looking at a barrel or a sign attached to a barrel.");
        postOffice.language.signOnBarrel = newLang.getString("sign-on-barrel","&a[Post Office] &4The sign must be attached to a barrel.");
        postOffice.language.alreadyClaimed = newLang.getString("already-claimed","&a[Post Office] &4This post box has already been claimed.");
        postOffice.language.invalidPostbox = newLang.getString("invalid-postbox","&a[Post Office] &4This isn't a valid post box.");
        postOffice.language.successfullyClaimed = newLang.getString("successfully-claimed","&a[Post Office] &aYou have successfully registered this post box.");
        postOffice.language.modifySign = newLang.getString("modify-sign","&a[Post Office] &4You cannot modify a post box sign.");
        postOffice.language.unclaimedPostbox = newLang.getString("unclaimed-postbox","&a[Post Office] &4This post box is unclaimed.");
        postOffice.language.userBanned = newLang.getString("user-banned","&a[Post Office] &4You aren't able to interact with this post box.");
        postOffice.language.postBoxOwner = newLang.getString("post-box-owner","&a[Post Office] &aThis post box is owned by %owner%");
        postOffice.language.claimedFor = newLang.getString("claimed-for", "&a[Post Office] &aThis post box has been claimed for %owner%");
        postOffice.language.alreadyHasPostBox = newLang.getString("already-has-postbox","&a[Post Office] &4%player% already has a post box at: %location%");
        postOffice.language.notPlayedBefore = newLang.getString("not-played-before","&a[Post Office] &4The player %player% has not played on this server.");
        postOffice.language.claimedForOtherPlayer = newLang.getString("claimed-for-other-player","&a[Post Office] &aA post box has been created for you.");
        postOffice.language.pluginUpToDate = newLang.getString("plugin-up-to-date","Your plugin is up-to-date.");
        postOffice.language.blacklistedItem = newLang.getString("blacklisted-item", "&a[Post Office] &4This is a blacklisted item.");
        postOffice.getLogger().info("Lang file loaded into memory.");
    }

    //endregion

}
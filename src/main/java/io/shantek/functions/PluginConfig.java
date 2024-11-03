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
    private final File backupFile;
    private final File langFile;
    private YamlConfiguration config;
    private YamlConfiguration langConfig;

    // Constructor
    public PluginConfig(PostOffice postOffice) {
        this.postOffice = postOffice;
        this.configFile = new File(postOffice.getDataFolder(), "config.yml");
        this.backupFile = new File(postOffice.getDataFolder(), "config-backup.yml");
        this.langFile = new File(postOffice.getDataFolder(), "lang.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void initializeAndLoadConfig() {
        try {
            // Create config file if it's missing
            if (!configFile.exists()) {
                createDefaultConfig();
            }

            if (shouldReloadConfig()) {
                // Always create a proper backup
                createBackup();

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

    private void createBackup() {
        try {
            if (configFile.exists()) {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
            if (isConfigValid(backupFile)) {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
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
        postOffice.getLogger().info("Configuration values loaded into memory.");
    }

    public void checkAndUpdateLang() {
        try (InputStream defLangStream = postOffice.getResource("lang.yml")) {
            if (defLangStream == null) {
                postOffice.getLogger().severe("Default language file (lang.yml) not found in resources!");
                return;
            }

            YamlConfiguration defLang = YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
            boolean needsUpdate = !langConfig.contains("lang-version") || langConfig.getInt("lang-version") != defLang.getInt("lang-version");

            if (needsUpdate) {
                reloadLangAndMerge(defLang);
            }

        } catch (IOException e) {
            postOffice.getLogger().severe("An error occurred while reading the default language configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void reloadLangAndMerge(YamlConfiguration defLang) {
        try {
            // Create a backup of current configurations
            YamlConfiguration currentLang = YamlConfiguration.loadConfiguration(langFile);
            // Replace lang file with the one from resources
            try (InputStream inputStream = postOffice.getResource("lang.yml")) {
                if (inputStream != null) {
                    Files.copy(inputStream, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    this.langConfig = YamlConfiguration.loadConfiguration(langFile);

                    // Merge old values back into the new configuration
                    mergeConfigs(langConfig, currentLang);
                    saveLangConfig();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLangConfig() {
        try {
            this.langConfig.save(langFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
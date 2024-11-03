package io.shantek;

import io.shantek.functions.*;
import io.shantek.listeners.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class PostOffice extends JavaPlugin {

    public UpdateChecker updateChecker;
    public Commands commands;
    public Language language;
    public Metrics metrics;
    public PluginConfig pluginConfig;
    public Helpers helpers;
    public BarrelProtection barrelProtection;
    public TabCompleter tabCompleter;
    public static PostOffice instance;
    public String customBarrelName = "pobox";
    public File mailFile;
    public int previousItemCount = 0;
    public int newItemCount = 0;
    public Set<String> playersWithMail = new HashSet<>();
    public boolean updateNotificationEnabled = true;
    public boolean postBoxProtection = true;
    public boolean consoleLogs = true;
    public boolean gotMailDelay = true;
    public boolean signNotification = true;
    public boolean hopperProtection = false;
    public boolean debugLogs = false;

    public void onEnable() {

        instance = this;
        commands = new Commands(this);
        language = new Language(this);
        barrelProtection = new BarrelProtection(this);
        helpers = new Helpers(this);
        TabCompleter tabCompleter = new TabCompleter(this);

        Objects.requireNonNull(getCommand("postoffice")).setTabCompleter(new TabCompleter(this));

        // Check for a data folder, create it if needed
        helpers.checkForDataFolder();

        this.mailFile = new File(getDataFolder(), "hasmail.txt");

        Objects.requireNonNull(getCommand("postoffice")).setExecutor(new Commands(this));

        // Create an instance of UpdateChecker
        this.updateChecker = new UpdateChecker();

        // Create an instance of PluginConfig
        this.pluginConfig = new PluginConfig(this);
        pluginConfig.initializeAndLoadConfig();
        pluginConfig.checkAndUpdateLang();

        if (this.mailFile.exists()) {
            try (Stream<String> lines = Files.lines(mailFile.toPath())) {
                this.playersWithMail = lines
                        .filter(line -> !line.isEmpty()) // Only non-empty lines
                        .collect(Collectors.toCollection(HashSet::new));
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not read mail file", e);
            }
        } else {
            try {
                boolean fileCreated = this.mailFile.createNewFile();
                if (fileCreated) {
                    getLogger().info("Mail file created successfully.");
                } else {
                    getLogger().warning("Mail file creation failed. It may already exist.");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create mail file", e);
            }
        }

        int pluginId = 20173;
        Metrics metrics = new Metrics(this, pluginId);

        // CHECK FOR PLUGIN UPDATES IF ENABLED
        if (updateNotificationEnabled) {
            UpdateChecker.checkForUpdatesAsync(getDescription().getVersion(), this);
        }
        // Register event listeners
        registerEventListeners();
    }

    public void registerEventListeners() {
        Bukkit.getPluginManager().registerEvents(new InventoryClick(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryClose(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryOpen(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);
        Bukkit.getPluginManager().registerEvents(new BarrelProtection(this), this);
    }

    public static PostOffice getInstance() {
        if (instance == null) {
            instance = new PostOffice();
        }
        return instance;
    }

    @Override
    public void onDisable() {
        // Save the cache to file when the plugin is disabled
        helpers.saveCacheToFile();
    }

    public void printInfoMessage(String message) {
        getLogger().info(message); // Print to the console
    }

}
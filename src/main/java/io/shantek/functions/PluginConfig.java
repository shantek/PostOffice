package io.shantek.functions;

import io.shantek.PostOffice;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PluginConfig {

    private final PostOffice postOffice;
    private final File configFile;
    private final File langFile;
    private FileConfiguration config;
    private FileConfiguration langConfig;

    public PluginConfig(PostOffice postOffice) {
        this.postOffice = postOffice;
        this.configFile = new File(postOffice.getDataFolder(), "config.yml");
        this.langFile = new File(postOffice.getDataFolder(), "lang.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("custom-barrel-name", "pobox");
        defaults.put("sign-notification", true);
        defaults.put("got-mail-delay", true);
        defaults.put("postbox-protection", true);
        defaults.put("hopper-protection", false);
        defaults.put("update-notification", true);
        defaults.put("console-logs", false);
        return defaults;
    }

    private Map<String, String> defaultLangConfig() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("sent-message", "&a[Post Office] &aMail sent to %receiver%.");
        defaults.put("received-message", "&a[Post Office] &eYou received mail from %sender%!");
        defaults.put("got-mail-message", "&a[Post Office] &fYou got mail!");
        defaults.put("no-permission", "&a[Post Office] &4You don't have permission to do that.");
        defaults.put("deny-action", "&a[Post Office] &4You can't do that here!");
        defaults.put("not-registered", "&a[Post Office] &4This isn't a registered post office box.");
        defaults.put("post-box-removed", "&a[Post Office] &aPost box removed successfully.");
        defaults.put("successful-registration", "&a[Post Office] &aPost box registered successfully.");
        defaults.put("already-registered", "&a[Post Office] &4This post box is already registered.");
        defaults.put("postbox-created", "&a[Post Office] &2 Box successfully created for %username%");
        defaults.put("remove-from-config", "&a[Post Office] &aPost box successfully removed from the config.");
        defaults.put("look-at-post-box", "&a[Post Office] &4You must be looking at a barrel or a sign attached to a barrel.");
        defaults.put("sign-on-barrel", "&a[Post Office] &4The sign must be attached to a barrel.");
        defaults.put("already-claimed", "&a[Post Office] &4This post box has already been claimed.");
        defaults.put("invalid-postbox", "&a[Post Office] &4This isn't a valid post box.");
        defaults.put("successfully-claimed", "&a[Post Office] &aYou have successfully claimed this post box.");
        defaults.put("modify-sign", "&a[Post Office] &4You cannot modify a post box sign.");
        defaults.put("unclaimed-postbox", "&a[Post Office] &4This post box is unclaimed.");
        defaults.put("user-banned", "&a[Post Office] &4You aren't able to interact with this post box.");
        defaults.put("post-box-owner", "&a[Post Office] &aThis post box is owned by %owner%");
        defaults.put("claimed-for", "&a[Post Office] &aThis post box has been claimed for %owner%");
        return defaults;
    }

    public void loadConfig() {
        Map<String, Object> defaults = defaultConfig();
        boolean saveNeeded = false;

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                System.out.println("Missing key added: " + entry.getKey());
                saveNeeded = true;
            }
        }

        if (saveNeeded) {
            saveConfig();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
            System.out.println("Configuration saved to " + configFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadLangConfig() {
        Map<String, String> defaults = defaultLangConfig();
        boolean saveNeeded = false;

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!langConfig.contains(entry.getKey())) {
                langConfig.set(entry.getKey(), entry.getValue());
                System.out.println("Missing lang key added: " + entry.getKey());
                saveNeeded = true;
            }
        }

        if (saveNeeded) {
            saveLangConfig();
        }
    }

    public void saveLangConfig() {
        try {
            langConfig.save(langFile);
            System.out.println("Language configuration saved to " + langFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
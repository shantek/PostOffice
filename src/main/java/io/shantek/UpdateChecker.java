package io.shantek;

import org.bukkit.plugin.Plugin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    @SuppressWarnings("unused")
    public String currentVersion = null;
    public static String remoteVersion = null;
    public static void checkForUpdatesAsync(String currentVersion, Plugin plugin) {
        CompletableFuture.runAsync(() -> checkForUpdates(currentVersion, plugin));
    }

    private static void checkForUpdates(String currentVersion, Plugin plugin) {
        String updateUrl = "https://api.shantek.io/postoffice.txt"; // Replace with your actual URL

        try {
            URL url = new URL(updateUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                remoteVersion = reader.readLine();

                // Check if the retrieved version is in the expected format (e.g., 1.6.0)
                if (remoteVersion.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                    if (isNewVersionAvailable(currentVersion, remoteVersion)) {
                        plugin.getLogger().log(Level.WARNING, "Plugin outdated. Installed version: " + currentVersion + ", Latest version: " + remoteVersion);
                        // Notify users about the update
                    } else {
                        plugin.getLogger().info("Your plugin is up-to-date.");
                    }
                } else {
                    // Treat non-version responses as failed attempts
                    plugin.getLogger().log(Level.WARNING, "Failed to check for updates. Ignoring.");
                    remoteVersion = currentVersion; // Set remote version to current version
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates. Response Code: " + responseCode);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error checking for updates.");
        }
    }

    public static boolean isNewVersionAvailable(String currentVersion, String remoteVersion) {
        // Directly compare the version strings
        return !currentVersion.equals(remoteVersion);
    }
}
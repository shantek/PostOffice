package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.plugin.Plugin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    public static String remoteVersion = null;
    public static void checkForUpdatesAsync(String currentVersion, Plugin plugin) {
        CompletableFuture.runAsync(() -> checkForUpdates(currentVersion, plugin));
    }

    private static void checkForUpdates(String currentVersion, Plugin plugin) {

        PostOffice postOffice = PostOffice.getInstance();

        String updateUrl = "https://api.shantek.dev/postoffice.txt";
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
                        postOffice.printInfoMessage(postOffice.language.pluginUpToDate);
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
        // Check if the remote version is null, return false in that case
        if (remoteVersion == null || remoteVersion.isEmpty()) {
            return false;
        }

        // Split the version strings by '.' to compare major, minor, and patch
        String[] currentVersionParts = currentVersion.split("\\.");
        String[] remoteVersionParts = remoteVersion.split("\\.");

        // Check if current version is a dev build (contains "-SNAPSHOT" or other pre-release indicators)
        boolean isDevBuild = currentVersion.toLowerCase().contains("snapshot") || currentVersion.toLowerCase().contains("dev");

        // Compare major, minor, and patch versions
        for (int i = 0; i < Math.min(currentVersionParts.length, remoteVersionParts.length); i++) {
            try {
                int currentPart = Integer.parseInt(currentVersionParts[i]);
                int remotePart = Integer.parseInt(remoteVersionParts[i]);

                // If the current version part is less than the remote, a new version is available
                if (currentPart < remotePart) {
                    return true;
                }
                // If the current version part is greater, the current version is newer (possibly a dev build)
                if (currentPart > remotePart) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // In case of non-numeric version parts (like "1.6.2-SNAPSHOT"), treat dev build as newer
                if (isDevBuild) {
                    return false; // Consider dev builds to be newer than any stable release
                }
            }
        }

        // If all version parts are equal, but one version has more parts (e.g., "1.6" vs "1.6.1"), compare lengths
        return currentVersionParts.length < remoteVersionParts.length;
        // Remote version has more parts (e.g., 1.6 -> 1.6.1), so a new version is available
        // No new version available
    }

}
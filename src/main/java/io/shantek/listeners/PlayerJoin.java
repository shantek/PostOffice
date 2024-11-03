package io.shantek.listeners;

import io.shantek.PostOffice;
import io.shantek.functions.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    public PostOffice postOffice;

    public PlayerJoin(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (postOffice.playersWithMail.contains(player.getUniqueId().toString())) {

            // Set initial delay of 1 second
            long messageDelay = 20L;

            // If the additional delay is active, change to 10 seconds
            if (postOffice.gotMailDelay) {
                messageDelay = 200L;
            }

            Bukkit.getScheduler().runTaskLater(postOffice, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.gotMailMessage));
            }, messageDelay);

            if (postOffice.debugLogs) {
                postOffice.getLogger().severe("Sent mail notification to player " + player.getName() + " (" + player.getUniqueId() + ")");
            }
        }

        if (postOffice.updateNotificationEnabled && (player.isOp() || player.hasPermission("shantek.postoffice.updatenotification"))) {
            String currentVersion = postOffice.getDescription().getVersion();
            String remoteVersion = UpdateChecker.remoteVersion; // Assuming UpdateChecker retrieves the remote version

            // Only notify if a newer version is available
            if (UpdateChecker.isNewVersionAvailable(currentVersion, remoteVersion)) {
                player.sendMessage("[Post Office] An update is available! New version: " + remoteVersion);
            }
        }

    }

}



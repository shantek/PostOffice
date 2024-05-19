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

        if (postOffice.playersWithMail.contains(player.getName())) {

            // If message delay is enabled, delay it by 5 seconds
            long messageDelay = 10L;
            if (postOffice.gotMailDelay) {
                messageDelay = 100L;
            }

            Bukkit.getScheduler().runTaskLater(postOffice, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.gotMailMessage));
            }, messageDelay); // Delay showing the title by 10 ticks
        }

        if (postOffice.updateNotificationEnabled && (player.isOp() || player.hasPermission("shantek.postoffice.updatenotification"))) {
            if (UpdateChecker.isNewVersionAvailable(postOffice.getDescription().getVersion(), UpdateChecker.remoteVersion)) {
                player.sendMessage("[Post Office] An update is available! New version: " + UpdateChecker.remoteVersion);
            }
        }

    }
}



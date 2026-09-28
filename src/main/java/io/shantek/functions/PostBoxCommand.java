package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PostBoxCommand implements CommandExecutor {

    private final PostOffice postOffice;

    public PostBoxCommand(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("postbox")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Opening another player's box
        if (args.length == 1) {
            return handleOpenOthers(player, args[0]);
        }

        // Opening own box
        return handleOpenOwn(player);
    }

    private boolean handleOpenOwn(Player player) {
        if (!player.hasPermission("shantek.postoffice.openremote")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.remoteOpenNoPermission));
            return true;
        }

        Block primaryBox = postOffice.helpers.getPrimaryBoxForPlayer(player.getUniqueId());

        if (primaryBox == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.noPostBoxClaimed));
            return true;
        }

        openBoxForPlayer(player, primaryBox, player.getUniqueId());
        return true;
    }

    private boolean handleOpenOthers(Player player, String targetName) {
        if (!player.hasPermission("shantek.postoffice.openremote.others")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.remoteOpenOthersNoPermission));
            return true;
        }

        // Resolve target player (online or offline)
        OfflinePlayer target = resolvePlayer(targetName);

        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.remoteOpenPlayerNotFound));
            return true;
        }

        Block primaryBox = postOffice.helpers.getPrimaryBoxForPlayer(target.getUniqueId());

        if (primaryBox == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.remoteOpenTargetNoBox
                            .replace("%player%", target.getName() != null ? target.getName() : targetName)));
            return true;
        }

        openBoxForPlayer(player, primaryBox, target.getUniqueId());
        return true;
    }

    private void openBoxForPlayer(Player player, Block primaryBox, UUID ownerUUID) {
        if (!(primaryBox.getState() instanceof Barrel)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.invalidPostbox));
            return;
        }

        Barrel barrel = (Barrel) primaryBox.getState();

        // Track item count for change detection (consistent with normal open path)
        postOffice.previousItemCount = postOffice.helpers.countNonNullItems(
                barrel.getInventory().getContents());

        player.openInventory(barrel.getInventory());
        postOffice.holdRemoteBoxChunk(player, primaryBox);

        if (postOffice.debugLogs) {
            String ownerName = postOffice.helpers.getPlayerName(ownerUUID);
            postOffice.getLogger().info("Player " + player.getName() +
                    " opened post box for " + ownerName + " via /postbox command.");
        }
    }

    /**
     * Resolve a player by name, checking online players first then offline.
     */
    private OfflinePlayer resolvePlayer(String name) {
        // Check online players first (exact name match)
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        // Fall back to offline players
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline;
    }
}

package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.Objects;
import java.util.UUID;

public class InventoryClose implements Listener {

    public PostOffice postOffice;

    public InventoryClose(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        PostOffice plugin = PostOffice.getInstance();
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        if (isBarrelInventory(inventory, event)) {
            Block barrelBlock = getBarrelBlock(event);

            if (postOffice.helpers.isBarrelInConfig(barrelBlock)) {
                processBarrelInventoryClose(plugin, inventory, player, barrelBlock);
            }
        }
    }

    private boolean isBarrelInventory(Inventory inventory, InventoryCloseEvent event) {
        return inventory.getType() == InventoryType.BARREL
                && Objects.requireNonNull(event.getInventory().getLocation()).getBlock().getType() == Material.BARREL;
    }

    private void processBarrelInventoryClose(PostOffice plugin, Inventory inventory, Player player, Block barrelBlock) {

        UUID boxOwnerUUID = postOffice.helpers.getOwnerUUID(barrelBlock);

        if (boxOwnerUUID == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.language.unclaimedPostbox));
        } else {
            logValidPostBox(plugin, postOffice.helpers.getPlayer(boxOwnerUUID));
        }

        if (boxOwnerUUID != null && postOffice.helpers.isPostBoxOwner(barrelBlock, player)) {
            clearSignAndNotifications(plugin, barrelBlock, player);
        } else if (boxOwnerUUID != null) {
            checkForItemChanges(plugin, inventory, barrelBlock, player, boxOwnerUUID);
        }
    }

    private void logValidPostBox(PostOffice plugin, OfflinePlayer boxOwner) {
        if (plugin.consoleLogs) {
            plugin.getLogger().info("Closing valid post box. Owner: " + boxOwner.getName());
        }
    }

    private void clearSignAndNotifications(PostOffice plugin, Block barrelBlock, Player player) {
        Block signBlock = postOffice.helpers.getSignFromConfig(barrelBlock);

        if (signBlock != null && signBlock.getState() instanceof Sign) {
            Sign sign = (Sign) signBlock.getState();
            sign.setLine(2, "");
            sign.update();
        }

        plugin.playersWithMail.remove(player.getUniqueId().toString());
        postOffice.helpers.saveMailFile();
    }

    private void checkForItemChanges(PostOffice plugin, Inventory inventory, Block barrelBlock, Player player, UUID boxOwnerUUID) {
        plugin.newItemCount = postOffice.helpers.countNonNullItems(inventory.getContents());

        if (plugin.newItemCount > plugin.previousItemCount) {
            updateSignAndNotifyOwner(plugin, barrelBlock, player, boxOwnerUUID);
        }
    }

    private void updateSignAndNotifyOwner(PostOffice plugin, Block barrelBlock, Player player, UUID boxOwnerUUID) {
        Block signBlock = plugin.helpers.getSignFromConfig(barrelBlock);

        if (plugin.signNotification && signBlock != null && signBlock.getState() instanceof Sign) {
            Sign sign = (Sign) signBlock.getState();
            sign.setLine(2, ChatColor.GREEN + "You have mail");
            sign.update();
        }

        sendPlayerMessage(plugin, player, boxOwnerUUID);
        plugin.playersWithMail.add(boxOwnerUUID.toString());
        plugin.helpers.saveMailFile();

        notifyOwner(plugin, player, boxOwnerUUID);
    }

    private void sendPlayerMessage(PostOffice plugin, Player player, UUID boxOwnerUUID) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.language.sentMessage
                        .replace("%sender%", player.getName())
                        .replace("%receiver%", Objects.requireNonNull(Bukkit.getOfflinePlayer(boxOwnerUUID).getName()))));

        if (plugin.consoleLogs) {
            plugin.getLogger().info(player.getName() + " added mail for " + Bukkit.getOfflinePlayer(boxOwnerUUID).getName());
        }
    }

    private void notifyOwner(PostOffice plugin, Player player, UUID boxOwnerUUID) {
        Player owner = Bukkit.getPlayer(boxOwnerUUID);
        if (owner != null) {
            owner.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.language.receivedMessage
                            .replace("%sender%", player.getName())
                            .replace("%receiver%", owner.getName())));
        }
    }

    private Block getBarrelBlock(InventoryCloseEvent event) {
        return Objects.requireNonNull(event.getInventory().getLocation()).getBlock();
    }
}




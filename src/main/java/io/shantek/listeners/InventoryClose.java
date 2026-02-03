package io.shantek.listeners;

import io.shantek.PostOffice;
import io.shantek.functions.Helpers;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
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

        // Skip any non-barrel or virtual inventories (no location)
        if (inventory.getType() != InventoryType.BARREL || inventory.getLocation() == null) {
            return;
        }

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
            return;
        }
        
        logValidPostBox(plugin, postOffice.helpers.getPlayer(boxOwnerUUID));

        // Get the snapshot of what the inventory looked like when opened
        String barrelLocation = postOffice.helpers.getBlockLocationString(barrelBlock);
        ItemStack[] beforeSnapshot = plugin.inventoryTracker.getAndRemoveSnapshot(player.getUniqueId(), barrelLocation);
        
        // Log inventory changes
        if (beforeSnapshot != null) {
            logInventoryChanges(plugin, beforeSnapshot, inventory.getContents(), boxOwnerUUID, player);
        }

        if (postOffice.helpers.isPostBoxOwner(barrelBlock, player)) {
            clearSignAndNotifications(plugin, barrelBlock, player);
        } else {
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

        // Also clear all secondary box signs for this owner
        UUID playerUUID = player.getUniqueId();
        List<Helpers.BarrelInfo> allBoxes = plugin.helpers.getAllBoxesForPlayer(playerUUID);
        for (Helpers.BarrelInfo boxInfo : allBoxes) {
            if ("secondary".equals(boxInfo.state)) {
                // Get the secondary box's barrel block
                World world = Bukkit.getWorld(boxInfo.world);
                if (world != null) {
                    Block secondaryBarrel = world.getBlockAt(boxInfo.x, boxInfo.y, boxInfo.z);
                    Block secondarySign = plugin.helpers.getSignFromConfig(secondaryBarrel);
                    
                    if (secondarySign != null && secondarySign.getState() instanceof Sign) {
                        Sign secSign = (Sign) secondarySign.getState();
                        secSign.setLine(2, "");
                        secSign.update();
                    }
                }
            }
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

        // Also update all secondary box signs for this owner
        if (plugin.signNotification && boxOwnerUUID != null) {
            List<Helpers.BarrelInfo> allBoxes = plugin.helpers.getAllBoxesForPlayer(boxOwnerUUID);
            for (Helpers.BarrelInfo boxInfo : allBoxes) {
                if ("secondary".equals(boxInfo.state)) {
                    // Get the secondary box's barrel block
                    World world = Bukkit.getWorld(boxInfo.world);
                    if (world != null) {
                        Block secondaryBarrel = world.getBlockAt(boxInfo.x, boxInfo.y, boxInfo.z);
                        Block secondarySign = plugin.helpers.getSignFromConfig(secondaryBarrel);
                        
                        if (secondarySign != null && secondarySign.getState() instanceof Sign) {
                            Sign secSign = (Sign) secondarySign.getState();
                            secSign.setLine(2, ChatColor.GREEN + "You have mail");
                            secSign.update();
                        }
                    }
                }
            }
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

    /**
     * Compare before and after snapshots to log what items were deposited or withdrawn
     */
    private void logInventoryChanges(PostOffice plugin, ItemStack[] before, ItemStack[] after, UUID boxOwnerUUID, Player player) {
        List<ItemStack> deposited = new ArrayList<>();
        List<ItemStack> withdrawn = new ArrayList<>();

        // Create maps to track item counts
        java.util.Map<String, Integer> beforeCounts = new java.util.HashMap<>();
        java.util.Map<String, Integer> afterCounts = new java.util.HashMap<>();

        // Count items in before snapshot
        for (ItemStack item : before) {
            if (item != null && item.getType() != Material.AIR) {
                String key = item.getType().name();
                beforeCounts.put(key, beforeCounts.getOrDefault(key, 0) + item.getAmount());
            }
        }

        // Count items in after snapshot
        for (ItemStack item : after) {
            if (item != null && item.getType() != Material.AIR) {
                String key = item.getType().name();
                afterCounts.put(key, afterCounts.getOrDefault(key, 0) + item.getAmount());
            }
        }

        // Compare to find what changed
        java.util.Set<String> allKeys = new java.util.HashSet<>();
        allKeys.addAll(beforeCounts.keySet());
        allKeys.addAll(afterCounts.keySet());

        for (String key : allKeys) {
            int beforeCount = beforeCounts.getOrDefault(key, 0);
            int afterCount = afterCounts.getOrDefault(key, 0);
            int diff = afterCount - beforeCount;

            if (diff > 0) {
                // Items were added
                deposited.add(new ItemStack(Material.valueOf(key), diff));
            } else if (diff < 0) {
                // Items were removed
                withdrawn.add(new ItemStack(Material.valueOf(key), -diff));
            }
        }

        // Log deposited items
        if (!deposited.isEmpty()) {
            boolean isOwner = player.getUniqueId().equals(boxOwnerUUID);
            if (!isOwner) {
                // Someone else deposited items into this box
                plugin.logManager.logItemsDeposited(boxOwnerUUID, player.getUniqueId(), deposited);
            }
        }

        // Log withdrawn items
        if (!withdrawn.isEmpty()) {
            boolean isAdmin = player.hasPermission("shantek.postoffice.register") && !player.getUniqueId().equals(boxOwnerUUID);
            plugin.logManager.logItemsWithdrawn(boxOwnerUUID, player.getUniqueId(), withdrawn, isAdmin);
        }
    }
}




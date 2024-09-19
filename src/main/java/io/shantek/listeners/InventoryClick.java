package io.shantek.listeners;

import io.shantek.PostOffice;
import io.shantek.functions.BarrelData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

public class InventoryClick implements Listener {

    private final PostOffice postOffice;

    public InventoryClick(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        Inventory clickedInventory = event.getClickedInventory();

        // If the clicked inventory isn't a barrel, ignore it
        if (clickedInventory == null || inventory.getType() != InventoryType.BARREL) {
            return;
        }

        Block clickedBlock = Objects.requireNonNull(inventory.getLocation()).getBlock();
        if (clickedBlock.getType() != Material.BARREL) {
            return;
        }

        // Get the location string for the barrel
        String barrelLocationString = postOffice.helpers.getBlockLocationString(clickedBlock);

        // Log barrel location
        if (postOffice.consoleLogs) {
            postOffice.getLogger().info("Player is interacting with a barrel at location: " + barrelLocationString);
        }

        // Fetch the barrel data from the cache
        BarrelData barrelData = postOffice.helpers.barrelsCache.get(barrelLocationString);  // Use barrelsCache directly

        // Check if barrel data is null (not registered)
        if (barrelData == null) {
            player.sendMessage(ChatColor.RED + "This is not a registered postbox.");
            return;
        }

        // Log the owner and the player interacting
        UUID ownerUUID = barrelData.getOwnerUUID();
        if (postOffice.consoleLogs) {
            postOffice.getLogger().info("Owner of the barrel: " + (ownerUUID != null ? ownerUUID.toString() : "None"));
            postOffice.getLogger().info("Player interacting: " + player.getUniqueId());
        }

        // Determine if the player is the owner or has permission
        boolean isOwner = ownerUUID != null && ownerUUID.equals(player.getUniqueId());
        boolean hasPermissionToRemove = player.hasPermission("shantek.postoffice.removeitems");

        // Log the result of the ownership and permission checks
        if (postOffice.consoleLogs) {
            postOffice.getLogger().info("Is owner: " + isOwner);
            postOffice.getLogger().info("Has permission to remove items: " + hasPermissionToRemove);
        }

        // Prevent non-owners without permission from interacting with the post box
        if (!isOwner && !hasPermissionToRemove) {
            if (postOffice.consoleLogs) {
                postOffice.getLogger().info("Player " + player.getName() + " tried to interact without permission.");
            }
            player.sendMessage(ChatColor.RED + postOffice.language.removeItemError);
            event.setCancelled(true);
            return;
        }

        // Prevent double-clicking items in player's own inventory to remove items
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            if (clickedInventory == player.getInventory() || clickedInventory.getType() == InventoryType.PLAYER) {
                player.sendMessage(ChatColor.RED + "You cannot double-click items into this postbox.");
                event.setCancelled(true);
                return;
            }
        }

        // Prevent item swapping using hotbar
        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null) {
                player.sendMessage(ChatColor.RED + "You cannot use number keys to swap items in this postbox.");
                event.setCancelled(true);
                return;
            }
        }

        // Prevent swapping items from a barrel while having another item in hand
        if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR || event.getAction() == InventoryAction.HOTBAR_SWAP) {
            player.sendMessage(ChatColor.RED + "You cannot swap items with the cursor in this postbox.");
            event.setCancelled(true);
            return;
        }

        // Prevent the player from dropping items out of the postbox
        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            player.sendMessage(ChatColor.RED + "You cannot drop items out of this postbox.");
            event.setCancelled(true);
            return;
        }

        // Check if the player is adding items to an existing stack in the barrel
        ItemStack[] contents = inventory.getContents();
        for (ItemStack item : contents) {
            if (item != null && event.getCurrentItem() != null && item.isSimilar(event.getCurrentItem())) {
                // If the player is not the owner and trying to add to an existing stack, cancel the event
                if (!isOwner && event.getAction() == InventoryAction.PLACE_ALL && item.getAmount() < item.getMaxStackSize()) {
                    player.sendMessage(ChatColor.RED + "You cannot stack items in this postbox.");
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }
    }
}

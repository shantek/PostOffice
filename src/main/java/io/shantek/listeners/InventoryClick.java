package io.shantek.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import io.shantek.PostOffice;

import java.util.Objects;

public class InventoryClick implements Listener {

    public PostOffice postOffice;
    public InventoryClick(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // IF THEY DON'T HAVE A BARREL OPEN, IGNORE IT
        if (event.getClickedInventory() == null || inventory.getType() != InventoryType.BARREL) {
            return;
        }

        Block clickedBlock = Objects.requireNonNull(event.getClickedInventory().getLocation()).getBlock();

        if (clickedBlock.getType() == Material.BARREL) {
            BlockState blockState = clickedBlock.getState();

            if (blockState instanceof Barrel) {
                Barrel barrel = (Barrel) blockState;

                if (barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(postOffice.customBarrelName)) {

                    String ownerName = "";

                    for (BlockFace blockFace : BlockFace.values()) {
                        Block relativeBlock = clickedBlock.getRelative(blockFace);

                        if (relativeBlock.getType().name().toUpperCase().contains("SIGN")) {
                            Sign sign = (Sign) relativeBlock.getState();
                            Location signLoc = relativeBlock.getLocation().subtract(blockFace.getDirection());

                            if (sign.getLine(1).equalsIgnoreCase(player.getName()) && signLoc.equals(clickedBlock.getLocation())) {
                                ownerName = sign.getLine(1);
                                break;
                            }
                        }
                    }

                    boolean isNotOwner = !ownerName.equalsIgnoreCase(player.getName());

                    // Prevent non-owners who do not have OP from taking items or shift-clicking
                    if (!player.isOp() && isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && (event.getAction().name().contains("PICKUP") || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.removeItemError));
                        return;
                    }

                    // If player is not the owner and trying to take an item, cancel the event
                    if (isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.removeItemError));
                        return;
                    }

                    if (isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && !player.isOp()) {
                        // CHECK IF THE PLAYER DOESN'T HAVE PERMISSION TO USE THIS BARREL, RESTRICT NUMBER KEY CLICKING TO MOVE TO HOTBAR
                        if (event.getWhoClicked() instanceof Player && event.getClickedInventory() != null) {
                            ItemStack hotbarItem = event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : null;

                            if (hotbarItem != null) {
                                event.setCancelled(true);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.hotBarError));
                                return;
                            }
                        }

                        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.hotBarError));
                            return;
                        }
                    }

                    // Check if item is already in the barrel
                    ItemStack[] contents = inventory.getContents();
                    for (ItemStack item : contents) {
                        if (item != null && event.getCurrentItem() != null && item.isSimilar(event.getCurrentItem())) {
                            // If the player is not the owner and trying to add to an existing stack, cancel the event
                            if (isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && event.getAction() == InventoryAction.PLACE_ALL && item.getAmount() < item.getMaxStackSize()) {
                                event.setCancelled(true);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.cantStackItems));
                                return;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

}

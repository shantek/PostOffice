package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

public class InventoryClick implements Listener {

    public PostOffice postOffice;

    public InventoryClick(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (shouldIgnoreEvent(clickedInventory, event.getInventory())) {
            return;
        }

        if (isDoubleClickOnPlayerInventory(event, clickedInventory, player)) {
            cancelIfBarrelNameMatches(event, player, player.getOpenInventory().getTopInventory());
            return;
        }

        handleBarrelInteractions(event, player, clickedInventory);
    }

    private boolean shouldIgnoreEvent(Inventory clickedInventory, Inventory inventory) {
        return clickedInventory == null || inventory.getType() != InventoryType.BARREL;
    }

    private boolean isDoubleClickOnPlayerInventory(InventoryClickEvent event, Inventory clickedInventory, Player player) {
        return event.getClick() == ClickType.DOUBLE_CLICK &&
                (clickedInventory == player.getInventory() || clickedInventory.getType() == InventoryType.PLAYER);
    }

    private void cancelIfBarrelNameMatches(InventoryClickEvent event, Player player, Inventory inventory) {
        Block clickedBlock = Objects.requireNonNull(inventory.getLocation()).getBlock();
        if (isBarrelWithName(clickedBlock, PostOffice.instance.customBarrelName)) {
            event.setCancelled(true);
        }
    }

    private boolean isBarrelWithName(Block block, String name) {
        if (block.getType() == Material.BARREL) {
            BlockState blockState = block.getState();
            if (blockState instanceof Barrel) {
                Barrel barrel = (Barrel) blockState;
                return name.equalsIgnoreCase(barrel.getCustomName());
            }
        }
        return false;
    }

    private void handleBarrelInteractions(InventoryClickEvent event, Player player, Inventory clickedInventory) {
        Block clickedBlock = Objects.requireNonNull(clickedInventory.getLocation()).getBlock();
        if (isBarrelWithName(clickedBlock, PostOffice.instance.customBarrelName)) {
            String ownerName = findOwnerName(clickedBlock, player);
            boolean isNotOwner = !ownerName.equalsIgnoreCase(player.getName());

            if (shouldCancelEvent(event, player, isNotOwner)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', PostOffice.instance.language.denyAction));
                return;
            }

            if (isItemAlreadyInBarrel(event, clickedInventory)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', PostOffice.instance.language.denyAction));
            }
        }
    }

    private String findOwnerName(Block clickedBlock, Player player) {
        for (BlockFace blockFace : BlockFace.values()) {
            Block relativeBlock = clickedBlock.getRelative(blockFace);
            if (relativeBlock.getType().name().toUpperCase().contains("SIGN")) {
                Sign sign = (Sign) relativeBlock.getState();
                Location signLoc = relativeBlock.getLocation().subtract(blockFace.getDirection());
                if (sign.getLine(1).equalsIgnoreCase(player.getName()) && signLoc.equals(clickedBlock.getLocation())) {
                    return sign.getLine(1);
                }
            }
        }
        return "";
    }

    private boolean shouldCancelEvent(InventoryClickEvent event, Player player, boolean isNotOwner) {
        return (!player.isOp() && isNotOwner && !player.hasPermission("shantek.postoffice.removeitems")) &&
                (event.getAction().name().contains("PICKUP") || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                        event.getClick() == ClickType.NUMBER_KEY || event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
                        event.getAction() == InventoryAction.HOTBAR_SWAP || event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP);
    }

    private boolean isItemAlreadyInBarrel(InventoryClickEvent event, Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (ItemStack item : contents) {
            if (item != null && event.getCurrentItem() != null && item.isSimilar(event.getCurrentItem())) {
                if (item.getAmount() < item.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }
}

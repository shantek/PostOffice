package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

public class BarrelProtection implements Listener {

    private final PostOffice postOffice;

    public BarrelProtection(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        Player player = event.getPlayer();
        Block signBlock = event.getBlock();

        if (hasBarrelNearby(signBlock)) {

            if (!player.isOp() && !player.hasPermission("shantek.postoffice.create")) {

                if (!postOffice.postBoxProtection) {
                    return;
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.createError));
                    event.setCancelled(true);
                }
            } else {
                // Check if line 2 of the sign has text
                String line2 = event.getLine(1);
                if (!line2.isEmpty()) {
                    // Get the player name
                    String playerName = line2;


                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            postOffice.language.postboxCreated
                                    .replace("%username%", playerName)));

                }

            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();

        // Check if the broken block is a barrel
        if (brokenBlock.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) brokenBlock.getState();
            String barrelCustomName = barrel.getCustomName();

            // Check if it's a custom post box and the player has permission
            if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {

                if (!player.isOp() && !player.hasPermission("shantek.postoffice.break")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.breakError));
                    event.setCancelled(true);
                }

            }

        }

        // Check if the broken block is a sign
        if (brokenBlock.getState() instanceof Sign) {

            if (hasBarrelNearby(brokenBlock))


                if (!player.isOp() && !player.hasPermission("shantek.postoffice.break")) {

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.breakError));
                    event.setCancelled(true);
                }


        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        Player player = event.getPlayer();
        Block placedBlock = event.getBlockPlaced();

        if (!(placedBlock.getState() instanceof Sign)) {
            return; // Ignore blocks that are not signs
        }

        if (hasBarrelNearby(placedBlock)) {
            if (!player.isOp() && !player.hasPermission("shantek.postoffice.create")) {
                // Player doesn't have permission to place a sign on the barrel
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.createError));

                placedBlock.breakNaturally();
                // event.setCancelled(true);
            }
        }

    }

    private boolean hasBarrelNearby(Block block) {
        // Check if the given block is a barrel with the custom name
        if (block.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) block.getState();
            String barrelCustomName = barrel.getCustomName();

            if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                return true;
            }
        }

        // Check if any nearby block is a barrel with the custom name
        for (BlockFace blockFace : BlockFace.values()) {
            Block relativeBlock = block.getRelative(blockFace);

            if (relativeBlock.getType() == Material.BARREL) {
                Barrel barrel = (Barrel) relativeBlock.getState();
                String barrelCustomName = barrel.getCustomName();

                if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private BlockFace getAttachedFace(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            BlockFace facing = directional.getFacing();
            return facing != null ? facing.getOppositeFace() : null;
        }
        return null;
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        InventoryHolder sourceHolder = event.getSource().getHolder();
        InventoryHolder destinationHolder = event.getDestination().getHolder();

        // Check if the source is a barrel and the destination is a hopper
        if (sourceHolder instanceof Barrel && destinationHolder instanceof Hopper) {
            Barrel barrel = (Barrel) sourceHolder;

            // Check if the barrel has the custom name
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                event.setCancelled(true);
            }
        }
    }

}

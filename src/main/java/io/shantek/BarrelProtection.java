package io.shantek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

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
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.createError));
                event.setCancelled(true);
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

                    event.setCancelled(true);
                }

            }

        }

        // Check if the broken block is a sign
        if (brokenBlock.getState() instanceof Sign) {

            if (hasBarrelNearby(brokenBlock))


                if (!player.isOp() && !player.hasPermission("shantek.postoffice.break")) {

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.breakError));
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
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.createError));

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

}

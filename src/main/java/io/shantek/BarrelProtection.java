package io.shantek;

import org.bukkit.ChatColor;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

public class BarrelProtection implements Listener {

    private final PostOffice postOffice;

    public BarrelProtection(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block signBlock = event.getBlock();
        Block attachedBlock = signBlock.getRelative(getAttachedFace(signBlock));

        if (!(attachedBlock.getState() instanceof Barrel)) {
            return;
        }

        Barrel barrel = (Barrel) attachedBlock.getState();
        String barrelCustomName = barrel.getCustomName();

        if (barrelCustomName == null || (!barrelCustomName.equals(postOffice.customBarrelName))) {
            return;
        }

        if (!player.isOp() && !player.hasPermission("shantek.postoffice.create")) {
            player.sendMessage(ChatColor.RED + "[Post Office] " + ChatColor.RED + "You don't have permission to create a Post Box.");
            //signBlock.breakNaturally();
            event.setCancelled(true);
            return;
        }


    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();

        if (brokenBlock.getState() instanceof Sign) {
            // Check if the broken block is a sign
            Sign sign = (Sign) brokenBlock.getState();
            Block attachedBlock = brokenBlock.getRelative(getAttachedFace(brokenBlock));

            if (attachedBlock.getState() instanceof Barrel) {
                // Check if the sign is attached to a barrel
                Barrel barrel = (Barrel) attachedBlock.getState();
                String barrelCustomName = barrel.getCustomName();

                if (barrelCustomName != null && barrelCustomName.equals(postOffice.customBarrelName)) {
                    // Check if the barrel has the custom name
                    if (!player.isOp() && !player.hasPermission("shantek.postoffice.breaksign")) {
                        // Player doesn't have permission to break the sign
                        player.sendMessage(ChatColor.RED + "[Post Office] " + ChatColor.RED + "You don't have permission to break this Post Box sign.");
                        event.setCancelled(true);
                    }
                }
            }
        } else if (brokenBlock.getState() instanceof Barrel) {
            // Check if the broken block is a barrel
            Barrel barrel = (Barrel) brokenBlock.getState();
            String barrelCustomName = barrel.getCustomName();

            if (barrelCustomName != null && barrelCustomName.equals(postOffice.customBarrelName)) {
                // Check if the barrel has the custom name
                if (!player.isOp() && !player.hasPermission("shantek.postoffice.breakbox")) {
                    // Player doesn't have permission to break the barrel
                    player.sendMessage(ChatColor.RED + "[Post Office] " + ChatColor.RED + "You don't have permission to break this Post Box.");
                    event.setCancelled(true);
                }
            }
        }
    }



    private BlockFace getAttachedFace(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            return directional.getFacing().getOppositeFace();
        }
        return null;
    }

}

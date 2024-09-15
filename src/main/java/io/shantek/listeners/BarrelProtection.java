package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.ChatColor;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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

        if (postOffice.helpers.hasBarrelNearby(signBlock)) {
            if (!player.isOp() && !player.hasPermission("shantek.postoffice.create")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.createError));
                event.setCancelled(true);
            } else {
                String line2 = event.getLine(1);
                if (line2 != null && !line2.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            postOffice.language.postboxCreated
                                    .replace("%username%", line2)));
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

            if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                if (!player.isOp() && !player.hasPermission("shantek.postoffice.break")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.breakError));
                    event.setCancelled(true);
                }
            }
        }

        // Check if the broken block is a sign
        if (brokenBlock.getState() instanceof Sign) {
            if (postOffice.helpers.hasBarrelNearby(brokenBlock)) {
                if (!player.isOp() && !player.hasPermission("shantek.postoffice.break")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.breakError));
                    event.setCancelled(true);
                }
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

        if (placedBlock.getState() instanceof Sign && postOffice.helpers.hasBarrelNearby(placedBlock)) {
            if (!player.isOp() && !player.hasPermission("shantek.postoffice.create")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.createError));
                placedBlock.breakNaturally();
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        InventoryHolder sourceHolder = event.getSource().getHolder();
        InventoryHolder destinationHolder = event.getDestination().getHolder();

        // Check if the source is a barrel and the destination is a hopper or hopper minecart
        if (sourceHolder instanceof Barrel && (destinationHolder instanceof Hopper || destinationHolder instanceof HopperMinecart)) {
            Barrel barrel = (Barrel) sourceHolder;
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }
        event.blockList().removeIf(postOffice.helpers::isProtectedPostBox);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }
        event.blockList().removeIf(postOffice.helpers::isProtectedPostBox);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        if (event.getEntityType() == EntityType.WITHER) {
            Block block = event.getBlock();
            if (postOffice.helpers.isProtectedPostBox(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }

        if (event.getEntity() instanceof WitherSkull) {
            Block hitBlock = event.getHitBlock();
            if (hitBlock != null && postOffice.helpers.isProtectedPostBox(hitBlock)) {
                // Cancel explosion effect by removing the wither skull
                event.getEntity().remove();
            }
        }
    }
}

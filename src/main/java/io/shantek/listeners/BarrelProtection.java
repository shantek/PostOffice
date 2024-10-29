package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
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
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

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
        Block attachedBarrel = postOffice.helpers.getAttachedBarrel(signBlock);

        if (attachedBarrel != null && attachedBarrel.getType() == Material.BARREL
                && postOffice.helpers.isBarrelInConfig(attachedBarrel)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.modifySign));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();

        if (postOffice.helpers.isProtectedPostBox(brokenBlock)) {
            if (player.isOp() || player.hasPermission("shantek.postoffice.break")) {
                Block barrelBlock = brokenBlock.getType() == Material.BARREL
                        ? brokenBlock
                        : postOffice.helpers.getAttachedBarrel(brokenBlock);

                if (barrelBlock == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.notRegistered));
                    return;
                }

                if (postOffice.helpers.isBarrelInConfig(barrelBlock)) {
                    postOffice.helpers.removeBarrelFromCache(barrelBlock);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.removeFromConfig));
                }
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.noPermission));
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

        if (placedBlock.getState() instanceof Sign && postOffice.helpers.hasBarrelNearby(placedBlock)) {
            if (!player.isOp() && !player.hasPermission("shantek.postoffice.create")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.noPermission));
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

        if (sourceHolder instanceof Barrel) {
            InventoryHolder destinationHolder = event.getDestination().getHolder();

            if (destinationHolder instanceof Hopper || destinationHolder instanceof HopperMinecart) {
                Barrel barrel = (Barrel) sourceHolder;
                if (postOffice.helpers.isProtectedPostBox(barrel.getBlock())) {
                    event.setCancelled(true);
                }
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
                event.getEntity().remove();
            }
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        if (!postOffice.postBoxProtection) {
            return;
        }
        List<BlockState> blocksToRemove = new ArrayList<>();
        for (BlockState blockState : event.getBlocks()) {
            Block block = blockState.getBlock();
            if (postOffice.helpers.isProtectedPostBox(block)) {
                blocksToRemove.add(blockState);
            }
        }
        event.getBlocks().removeAll(blocksToRemove);
    }
}
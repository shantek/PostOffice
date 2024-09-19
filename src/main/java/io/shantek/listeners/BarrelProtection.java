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

        // Get the attached barrel from the sign
        Block attachedBarrel = postOffice.helpers.getAttachedBarrel(signBlock);

        // Ensure the attached block is a barrel and that it is in the config
        if (attachedBarrel != null && attachedBarrel.getType() == Material.BARREL) {
            if (postOffice.helpers.isBarrelInConfig(attachedBarrel)) {
                // Cancel the sign change event if the barrel is in the config (protected post box)
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.modifySign));
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

        // Check if the broken block is a protected post box (either a barrel or a sign)
        if (postOffice.helpers.isProtectedPostBox(brokenBlock)) {
            if (player.isOp() || player.hasPermission("shantek.postoffice.break")) {
                Block barrelBlock = null;

                // Check if the player is breaking a sign
                if (Tag.SIGNS.isTagged(brokenBlock.getType())) {
                    // Retrieve the attached barrel if the broken block is a sign
                    barrelBlock = postOffice.helpers.getAttachedBarrel(brokenBlock);
                } else if (brokenBlock.getType() == Material.BARREL) {
                    // If breaking a barrel directly, set it as the barrelBlock
                    barrelBlock = brokenBlock;
                }

                // Ensure barrelBlock is valid before proceeding
                if (barrelBlock == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.notRegistered));
                    return;
                }

                // Check if the barrel exists in the config (registered post box)
                if (postOffice.helpers.isBarrelInConfig(barrelBlock)) {
                    // Call the helper to remove the barrel from the cache and config
                    postOffice.helpers.removeBarrelFromCache(barrelBlock);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.removeFromConfig));
                }

            } else {
                // Prevent the player from breaking the post box if they don't have permission
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
            if (postOffice.helpers.isProtectedPostBox(barrel.getBlock())) {
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

package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.Bukkit;
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
import java.util.Optional;

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

        if (attachedBarrel != null && attachedBarrel.getType() == Material.BARREL) {
            if (postOffice.helpers.isBarrelInConfig(attachedBarrel)) {

                if (player.hasPermission("shantek.postoffice.register")) {
                    String rawLine = Optional.ofNullable(event.getLine(0)).orElse("");
                    String attemptedLine0 = ChatColor.translateAlternateColorCodes('&', rawLine);

                    // Cancel the event to prevent default update
                    event.setCancelled(true);

                    // Manually apply the player's input (even if blank)
                    Bukkit.getScheduler().runTaskLater(postOffice, () -> {
                        BlockState state = signBlock.getState();
                        if (state instanceof Sign) {
                            Sign sign = (Sign) state;
                            sign.setLine(0, attemptedLine0); // May be ""
                            sign.update();
                        }
                    }, 1L);
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.modifySign));
                    event.setCancelled(true);
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

        // Check if the broken block is a protected post box (either a barrel or a sign)
        if (postOffice.helpers.isProtectedPostBox(brokenBlock)) {
            // Check if it's actually registered in the config
            Block barrelBlock = null;

            // Check if the player is breaking a sign
            if (Tag.SIGNS.isTagged(brokenBlock.getType())) {
                // Retrieve the attached barrel if the broken block is a sign
                barrelBlock = postOffice.helpers.getAttachedBarrel(brokenBlock);
            } else if (brokenBlock.getType() == Material.BARREL) {
                // If breaking a barrel directly, set it as the barrelBlock
                barrelBlock = brokenBlock;
            }

            // If the barrel is registered, prevent breaking
            if (barrelBlock != null && postOffice.helpers.isBarrelInConfig(barrelBlock)) {
                // Nobody can break it - must use /postoffice remove command
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.useRemoveCommand));
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

        if (!postOffice.hopperProtection) {
            return;
        }

        InventoryHolder sourceHolder = event.getSource().getHolder();
        if (!(sourceHolder instanceof Barrel)) {
            return;
        }

        InventoryHolder destinationHolder = event.getDestination().getHolder();
        if (!(destinationHolder instanceof Hopper || destinationHolder instanceof HopperMinecart)) {
            return;
        }

        Barrel barrel = (Barrel) sourceHolder;
        if (postOffice.helpers.isProtectedPostBox(barrel.getBlock())) {
            event.setCancelled(true);
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

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {

        if (!postOffice.postBoxProtection) {
            return;
        }

        // Create a list to store blocks to be removed
        List<BlockState> blocksToRemove = new ArrayList<>();

        // Iterate through the blocks the structure is going to replace
        for (BlockState blockState : event.getBlocks()) {
            Block block = blockState.getBlock();

            // Check if the block is a protected post box (e.g., a sign attached to a barrel)
            if (postOffice.helpers.isProtectedPostBox(block)) {
                // Add the block to the list for removal
                blocksToRemove.add(blockState);
            }
        }

        // Remove the protected blocks from the event after iteration
        event.getBlocks().removeAll(blocksToRemove);
    }

}
package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.ChatColor;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import java.util.Objects;

public class InventoryOpen implements Listener {

    public PostOffice postOffice;

    public InventoryOpen(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        // Skip any non-barrel or virtual inventories (no location)
        if (inventory.getType() != InventoryType.BARREL || inventory.getLocation() == null) {
            return;
        }

        Block clickedBlock = Objects.requireNonNull(inventory.getLocation()).getBlock();

        BlockState blockState = clickedBlock.getState();

        if (blockState instanceof Barrel) {
            Barrel barrel = (Barrel) blockState;
            String barrelName = barrel.getCustomName();

            // Check if it's a primary post box (custom name from config) or secondary (hardcoded "secondary")
            boolean isPrimaryBox = barrelName != null && barrelName.equalsIgnoreCase(postOffice.customBarrelName);
            boolean isSecondaryBox = barrelName != null && barrelName.equalsIgnoreCase(PostOffice.SECONDARY_BARREL_NAME);

            if (isPrimaryBox || isSecondaryBox) {

                if (player.hasPermission("shantek.postoffice.use")) {

                    // Check if this is a secondary box
                    if (isSecondaryBox && postOffice.helpers.isSecondaryBox(clickedBlock)) {
                        // Get the owner's primary box
                        java.util.UUID ownerUUID = postOffice.helpers.getOwnerUUID(clickedBlock);
                        if (ownerUUID != null) {
                            Block primaryBox = postOffice.helpers.getPrimaryBoxForPlayer(ownerUUID);
                            
                            if (primaryBox != null && primaryBox.getState() instanceof Barrel) {
                                Barrel primaryBarrel = (Barrel) primaryBox.getState();
                                
                                // Cancel the secondary box open
                                event.setCancelled(true);
                                
                                // Open the primary box inventory instead
                                // This bypasses any WorldGuard/claim protections on the primary location
                                // since we're opening a virtual inventory, not interacting with the actual block
                                player.openInventory(primaryBarrel.getInventory());
                                
                                // Set item count from the primary box
                                postOffice.previousItemCount = postOffice.helpers.countNonNullItems(
                                        primaryBarrel.getInventory().getContents());
                                
                                if (postOffice.debugLogs) {
                                    postOffice.getLogger().info("Player " + player.getName() + 
                                            " opened secondary box, redirected to primary at " + 
                                            postOffice.helpers.getBlockLocationString(primaryBox));
                                }
                                return;
                            } else {
                                // Primary box not found - owner needs to claim a new primary box
                                String ownerName = postOffice.helpers.getPlayerName(ownerUUID);
                                if (player.getUniqueId().equals(ownerUUID)) {
                                    // It's the owner trying to open their own secondary
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            postOffice.language.primaryBoxMissingOwner));
                                } else {
                                    // Someone else trying to deliver mail
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            postOffice.language.primaryBoxMissing
                                                    .replace("%owner%", ownerName != null ? ownerName : "Unknown")));
                                }
                                event.setCancelled(true);
                                
                                if (postOffice.debugLogs) {
                                    postOffice.getLogger().warning("Secondary box found but primary box missing for player " + 
                                            ownerUUID + ". Owner needs to claim a new primary box.");
                                }
                                return;
                            }
                        }
                    }

                    // Normal post box - let them open it and count items
                    postOffice.previousItemCount = postOffice.helpers.countNonNullItems(inventory.getContents());
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.userBanned));
                    event.setCancelled(true);

                    if (postOffice.debugLogs) {
                        postOffice.getLogger().severe("Player " + player.getName() + " (" + player.getUniqueId() + ") tried to use a post box but doesn't have the use permission/is banned.");
                    }
                }

            }
        }

    }
}



package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.Objects;
import java.util.UUID;

public class InventoryClose implements Listener {

    public PostOffice postOffice;

    public InventoryClose(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        PostOffice plugin = PostOffice.getInstance();
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        // Check if the inventory is a barrel
        if (inventory.getType() == InventoryType.BARREL) {
            Block clickedBlock = Objects.requireNonNull(event.getInventory().getLocation()).getBlock();

            if (clickedBlock.getType() == Material.BARREL) {
                if (postOffice.helpers.isBarrelInConfig(clickedBlock)) {

                    // This barrel is in the config, treat it as a valid post box
                    UUID boxOwnerUUID = postOffice.helpers.getOwnerUUID(clickedBlock);

                    if (boxOwnerUUID == null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.unclaimedPostbox));
                    } else {

                        if (postOffice.consoleLogs) {
                            OfflinePlayer boxOwner = postOffice.helpers.getPlayer(boxOwnerUUID);
                            plugin.getLogger().info("Closing valid post box. Owner: " + boxOwner.getName());
                        }

                    }

                    // Check if the player owns the post box
                    if (boxOwnerUUID != null && postOffice.helpers.isPostBoxOwner(clickedBlock, player)) {
                        // Player owns the post box - clear the "You have mail" message from the sign
                        Block signBlock = postOffice.helpers.getSignFromConfig(clickedBlock);
                        if (signBlock != null && signBlock.getState() instanceof Sign) {
                            Sign sign = (Sign) signBlock.getState();
                            sign.setLine(2, ""); // Clear the 3rd line
                            sign.update();
                        }

                        // Remove any mail notifications (from the internal mail list)
                        postOffice.playersWithMail.remove(player.getUniqueId().toString());
                        postOffice.helpers.saveMailFile();

                    } else if (boxOwnerUUID != null) {
                        // Player does not own the post box - check for item changes
                        postOffice.newItemCount = postOffice.helpers.countNonNullItems(inventory.getContents());
                        if (postOffice.newItemCount > postOffice.previousItemCount) {
                            // Update the sign to notify the owner of mail
                            Block signBlock = postOffice.helpers.getSignFromConfig(clickedBlock);
                            if (postOffice.signNotification && signBlock != null && signBlock.getState() instanceof Sign) {
                                Sign sign = (Sign) signBlock.getState();
                                sign.setLine(2, ChatColor.GREEN + "You have mail"); // Set "You have mail" on the 3rd line
                                sign.update();
                            }

                            // Send message to the player
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    postOffice.language.sentMessage
                                            .replace("%sender%", player.getName())
                                            .replace("%receiver%", Bukkit.getOfflinePlayer(boxOwnerUUID).getName())));

                            // Add the owner to the mail list
                            if (postOffice.consoleLogs) {
                                plugin.getLogger().info(player.getName() + " added mail for " + Bukkit.getOfflinePlayer(boxOwnerUUID).getName());
                            }
                            postOffice.playersWithMail.add(boxOwnerUUID.toString());
                            postOffice.helpers.saveMailFile();

                            // Notify the owner if they are online
                            Player owner = Bukkit.getPlayer(boxOwnerUUID);
                            if (owner != null) {
                                owner.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        postOffice.language.receivedMessage
                                                .replace("%sender%", player.getName())
                                                .replace("%receiver%", owner.getName())));
                            }
                        }
                    }
                }
            }
        }
    }

}




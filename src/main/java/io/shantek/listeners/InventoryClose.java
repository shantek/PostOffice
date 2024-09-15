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
        if (inventory.getType() == InventoryType.BARREL) {
            Block clickedBlock = Objects.requireNonNull(event.getInventory().getLocation()).getBlock();

            if (clickedBlock.getType() == Material.BARREL) {
                BlockState blockState = clickedBlock.getState();

                if (blockState instanceof Barrel) {
                    Barrel barrel = (Barrel) blockState;

                    if (barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(postOffice.customBarrelName)) {
                        boolean isOwner = false;
                        String ownerName = "";

                        Sign sign = null;
                        boolean foundSign = false;

                        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                            Block relativeBlock = clickedBlock.getRelative(face);

                            if (relativeBlock.getType().name().toUpperCase().contains("SIGN")) {
                                foundSign = true;
                                sign = (Sign) relativeBlock.getState();
                                ownerName = sign.getLine(1);

                                if (sign.getLine(1).equalsIgnoreCase(event.getPlayer().getName())) {
                                    isOwner = true;
                                    break;
                                }
                            }
                        }


                        if (!ownerName.isEmpty()) {
                            if (!isOwner) {
                                postOffice.newItemCount = postOffice.helpers.countNonNullItems(inventory.getContents());
                                if (postOffice.newItemCount > postOffice.previousItemCount) {

                                    // Set their sign to notify them they have mail
                                    if (postOffice.signNotification && foundSign) {

                                        sign.setLine(2, ChatColor.GREEN + "You have mail");
                                        sign.update();

                                    }

                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            postOffice.language.sentMessage
                                                    .replace("%sender%", player.getName())
                                                    .replace("%receiver%", ownerName)));

                                    // Get owner UUID
                                    OfflinePlayer postBoxOwner = Bukkit.getOfflinePlayer(ownerName);
                                    UUID ownerUUID = postBoxOwner.getUniqueId();

                                    // Add owners to mail list if someone else is adding items
                                    if (postOffice.consoleLogs) {
                                        plugin.getLogger().info(player.getName() + " added mail for " + ownerName);
                                    }
                                    postOffice.playersWithMail.add(ownerName);
                                    postOffice.helpers.saveMailFile();

                                    Player owner = Bukkit.getPlayer(ownerName);
                                    if (owner != null) {
                                        owner.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                postOffice.language.receivedMessage
                                                        .replace("%sender%", player.getName())
                                                        .replace("%receiver%", owner.getName())));
                                    }
                                }
                            } else {

                                // Set their sign to notify them they have mail
                                if (postOffice.signNotification && foundSign) {

                                    sign.setLine(2, "");
                                    sign.update();

                                }

                                // If they were the owner, and it was their barrel, remove them from the mail list
                                postOffice.playersWithMail.remove(event.getPlayer().getName());
                                postOffice.helpers.saveMailFile();
                            }
                        }
                    }
                }
            }
        }
    }

}
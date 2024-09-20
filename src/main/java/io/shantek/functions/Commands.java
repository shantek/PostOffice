package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Commands implements CommandExecutor {

    public PostOffice postOffice;

    public Commands(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("postoffice")) {

            // Postoffice remove command
            if (args[0].equalsIgnoreCase("remove")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;

                    // Ensure they have the proper permission
                    if (player.hasPermission("shantek.postoffice.register") || player.isOp()) {

                        // Get the block the player is looking at (sign or barrel)
                        Block targetBlock = player.getTargetBlock(null, 10);

                        Block barrelBlock = null;

                        // Check if the player is looking at a sign
                        if (Tag.SIGNS.isTagged(targetBlock.getType())) {
                            // Retrieve the attached barrel
                            barrelBlock = postOffice.helpers.getAttachedBarrel(targetBlock);
                        } else if (targetBlock.getType() == Material.BARREL) {
                            // Player is looking directly at a barrel
                            barrelBlock = targetBlock;
                        }

                        // Ensure we have a valid barrel block
                        if (barrelBlock == null || barrelBlock.getType() != Material.BARREL) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                            return true;
                        }

                        // Check if the barrel exists in the config (registered post box)
                        if (!postOffice.helpers.isBarrelInConfig(barrelBlock)) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.notRegistered));
                            return true;
                        }

                        // Optionally clear the sign associated with the post box
                        Block signBlock = postOffice.helpers.getSignForBarrel(barrelBlock);
                        if (signBlock != null && signBlock.getState() instanceof Sign) {
                            Sign sign = (Sign) signBlock.getState();
                            sign.setLine(1, ""); // Clear the second line
                            sign.setLine(2, ""); // Clear the third line
                            sign.update(); // Update the sign
                        }

                        // Call the helper to remove the barrel from the cache and config
                        postOffice.helpers.removeBarrelFromCache(barrelBlock);

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.postBoxRemoved));

                        return true;
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.noPermission));
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
            }

            else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shantek.postoffice.reload") || sender.isOp()) {

                    // Reload all the config and language file
                    postOffice.pluginConfig.reloadConfigFile();
                    postOffice.helpers.saveCacheToFile();  // Save cache instead of config directly
                    postOffice.helpers.reloadBarrelsConfig();
                    sender.sendMessage(ChatColor.GREEN + "Post Office config file has been reloaded.");
                    return true;
                } else {
                    // Player doesn't have permission to use the command
                    sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("info")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;

                    Block targetBlock = player.getTargetBlock(null, 10); // Get the block the player is looking at

                    Block barrelBlock = null;

                    // Check if the player is looking at a sign
                    if (Tag.SIGNS.isTagged(targetBlock.getType())) {
                        // Look up the barrel using the sign's location from the config
                        barrelBlock = postOffice.helpers.getBarrelFromSign(targetBlock);

                        // If there's no attached barrel or it's not a valid post box, show unregistered
                        if (barrelBlock == null || barrelBlock.getType() != Material.BARREL) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.notRegistered));
                            return true;
                        }

                    } else if (targetBlock.getType() == Material.BARREL) {
                        // The player is directly looking at a barrel
                        barrelBlock = targetBlock;
                    }

                    // Ensure we have a valid barrel block
                    if (barrelBlock == null || barrelBlock.getType() != Material.BARREL) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.notRegistered));
                        return true;
                    }

                    // Use the helper to get the owner and state information
                    String barrelLocation = postOffice.helpers.getBlockLocationString(barrelBlock);
                    String owner = postOffice.helpers.getOwnerNameFromConfig(barrelLocation); // Get the owner name
                    String state = postOffice.helpers.getStateFromConfig(barrelLocation); // Get the post box state

                    // If there's an owner, print it. Otherwise, print the state.
                    if (owner != null && !owner.equals("none")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                postOffice.language.postBoxOwner
                                        .replace("%owner%", owner)
                                        ));
                    } else if (state != null && state.equals("registered")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.unclaimedPostbox));
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.invalidPostbox));
                    }

                    return true;
                }
            } else if (args[0].equalsIgnoreCase("register")) {

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("shantek.postoffice.register") || sender.isOp()) {

                        // Ensure they are looking at a sign
                        Block targetBlock = player.getTargetBlock(null, 10); // Max distance 10 blocks
                        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                            return true;
                        }

                        // Ensure the sign is attached to a barrel
                        Block attachedBarrel = postOffice.helpers.getAttachedBarrel(targetBlock);
                        if (attachedBarrel == null || attachedBarrel.getType() != Material.BARREL) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.signOnBarrel));
                            return true;
                        }

                        // Get the location of the barrel and check its state
                        String barrelLocation = postOffice.helpers.getBlockLocationString(attachedBarrel);
                        String currentOwner = postOffice.helpers.getOwnerNameFromConfig(barrelLocation); // Get the owner name
                        String currentState = postOffice.helpers.getStateFromConfig(barrelLocation); // Get the post box state

                        // Check if the post box is already registered or claimed
                        if (currentOwner != null && !currentOwner.equals("none")) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    postOffice.language.postBoxOwner
                                            .replace("%owner%", currentOwner)
                                            ));
                            return true;
                        }
                        if (currentState != null && currentState.equals("registered")) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.alreadyRegistered));
                            return true;
                        }

                        // Register the post box and update the barrel name
                        Barrel barrel = (Barrel) attachedBarrel.getState();
                        barrel.setCustomName(postOffice.customBarrelName);
                        barrel.update();

                        // Register the barrel and sign in the plugin config
                        UUID barrelOwnerUUID = null; // No owner yet
                        postOffice.helpers.addOrUpdateBarrelInCache(attachedBarrel, targetBlock, barrelOwnerUUID, "registered");

                        // Update the sign with "Unclaimed" on the second line in red text
                        Sign sign = (Sign) targetBlock.getState();
                        sign.setLine(1, ChatColor.RED + "Unclaimed");
                        sign.update(); // Make sure to update the sign to apply the changes

                        postOffice.helpers.saveCacheToFile(); // Save the cache to disk

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.successfulRegistration));
                        return true;

                    } else {
                        // Player doesn't have permission to use the command
                        sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                        return false;

                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("claim") && args.length == 2) {
                if (sender.hasPermission("shantek.postoffice.claim.others") || sender.isOp()) {

                    // Ensure they are looking at a sign
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                        return true;
                    }

                    Player player = (Player) sender;
                    Block targetBlock = player.getTargetBlock(null, 10); // Max distance 10 blocks
                    if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                        return true;
                    }

                    // Ensure the sign is attached to a barrel
                    Block attachedBarrel = postOffice.helpers.getAttachedBarrel(targetBlock);
                    if (attachedBarrel == null || attachedBarrel.getType() != Material.BARREL) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.signOnBarrel));
                        return true;
                    }

                    // Check if the sign exists in the config (if the post box has been registered)
                    Block savedSign = postOffice.helpers.getSignForBarrel(attachedBarrel); // Retrieve saved sign
                    if (savedSign == null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.signOnBarrel));
                        return true;
                    }

                    // Get the location of the barrel
                    String barrelLocation = postOffice.helpers.getBlockLocationString(attachedBarrel);

                    // Get the target player's name and UUID
                    String targetPlayerName = args[1];
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

                    if (!targetPlayer.hasPlayedBefore()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                postOffice.language.notPlayedBefore
                                        .replace("%player%", targetPlayer.toString())
                                        ));
                        return true;
                    }

                    UUID targetPlayerUUID = targetPlayer.getUniqueId();

                    // Check if the player already has a post box
                    if (postOffice.helpers.doesPlayerHavePostBox(targetPlayerUUID)) {
                        String existingPostBoxLocation = postOffice.helpers.getPlayerPostBoxLocation(targetPlayerUUID); // Get world and coordinates
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                postOffice.language.alreadyHasPostBox
                                        .replace("%player%", targetPlayerName)
                                        .replace("%location%", existingPostBoxLocation)));
                        return true;
                    }

                    // Check if the post box is already claimed
                    String currentOwner = postOffice.helpers.getOwnerNameFromConfig(barrelLocation);
                    if (currentOwner != null && !currentOwner.equals("none")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.alreadyClaimed));
                        return true;
                    }

                    // Claim the post box for the target player and update the state to 'claimed'
                    postOffice.helpers.addOrUpdateBarrelInCache(attachedBarrel, targetBlock, targetPlayerUUID, "claimed");

                    // Update the sign to display the player's name on the second line
                    if (targetBlock != null && targetBlock.getState() instanceof Sign) {
                        Sign sign = (Sign) targetBlock.getState();
                        sign.setLine(1, targetPlayer.getName()); // Set the player's name on the 2nd line
                        sign.update();
                    }

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            postOffice.language.claimedFor
                                    .replace("%owner%", targetPlayerName)));

                    postOffice.helpers.saveCacheToFile(); // Save the cache to disk
                    return true;
                }
                else {
                    // Player doesn't have permission to use the command
                    sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("claim")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    UUID playerUUID = player.getUniqueId();

                    if (sender.hasPermission("shantek.postoffice.claim") || sender.isOp()) {

                        // Ensure they are looking at a sign
                        Block targetBlock = player.getTargetBlock(null, 10); // Max distance 10 blocks
                        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                            return true;
                        }

                        // Ensure the sign is attached to a barrel
                        Block attachedBarrel = postOffice.helpers.getAttachedBarrel(targetBlock);
                        if (attachedBarrel == null || attachedBarrel.getType() != Material.BARREL) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.signOnBarrel));
                            return true;
                        }

                        // Check if the sign exists in the config (if the post box has been registered)
                        Block savedSign = postOffice.helpers.getSignForBarrel(attachedBarrel); // Retrieve saved sign
                        if (savedSign == null) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.notRegistered));
                            return true;
                        }

                        // Get the location of the barrel
                        String barrelLocation = postOffice.helpers.getBlockLocationString(attachedBarrel);

                        // Check if the post box is already claimed
                        String currentOwner = postOffice.helpers.getOwnerNameFromConfig(barrelLocation);
                        if (currentOwner != null && !currentOwner.equals("none")) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.alreadyClaimed));
                            return true;
                        }

                        // Check if the player already has a post box
                        if (postOffice.helpers.doesPlayerHavePostBox(playerUUID)) {
                            String existingPostBoxLocation = postOffice.helpers.getPlayerPostBoxLocation(playerUUID); // Get world and coordinates

                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    postOffice.language.alreadyHasPostBox
                                            .replace("%player%", player.getName())
                                            .replace("%location%", existingPostBoxLocation)));

                            return true;
                        }

                        // Claim the post box for the player and update the state to 'claimed'
                        postOffice.helpers.addOrUpdateBarrelInCache(attachedBarrel, targetBlock, playerUUID, "claimed");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.successfullyClaimed));

                        // Update the sign to display the player's name on the second line
                        if (targetBlock != null && targetBlock.getState() instanceof Sign) {
                            Sign sign = (Sign) targetBlock.getState();
                            sign.setLine(1, player.getName()); // Set the player's name on the 2nd line
                            sign.update();
                        }

                        postOffice.helpers.saveCacheToFile(); // Save the cache to disk
                        return true;
                    } else {
                        // Player doesn't have permission to use the command
                        sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                        return false;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
            } else {
                // Invalid command format
                sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
                return false;
            }
        }
        return false;
    }
}

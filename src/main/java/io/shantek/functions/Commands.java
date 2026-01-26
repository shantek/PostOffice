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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Commands implements CommandExecutor {

    public PostOffice postOffice;

    public Commands(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("postoffice")) {
            return false;
        }

        if (args.length == 0) {
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            return onCommandRemove(sender);

        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            return onCommandReload(sender);

        } else if (args[0].equalsIgnoreCase("info")) {
            return onCommandInfo(sender);

        } else if (args[0].equalsIgnoreCase("register")) {
            return onCommandRegister(sender);

        } else if (args[0].equalsIgnoreCase("claim") && args.length == 2) {
            return onCommandClaimOthers(sender, args);

        } else if (args[0].equalsIgnoreCase("claim")) {
            return onCommandClaim(sender);

        } else if (args[0].equalsIgnoreCase("secondary")) {
            return onCommandSecondary(sender);

        } else if (args[0].equalsIgnoreCase("list") && args.length == 2) {
            return onCommandList(sender, args);

        } else if (args[0].equalsIgnoreCase("removesecondary") && args.length == 2) {
            return onCommandRemoveSecondary(sender, args);

        } else {
            sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
            return true;
        }
    }


    public boolean onCommandRemove(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Get the block the player is looking at (sign or barrel)
            Block targetBlock = postOffice.helpers.getBlockLookingAt(player, 6);

            // Exit out if they aren't looking at a block or are too far away
            if (targetBlock == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                return true;
            }

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

            // Get the barrel state and owner
            String barrelState = postOffice.helpers.getBarrelState(barrelBlock);
            UUID boxOwnerUUID = postOffice.helpers.getOwnerUUID(barrelBlock);

            // Permission check: Must be owner OR have admin permission
            boolean isOwner = boxOwnerUUID != null && player.getUniqueId().equals(boxOwnerUUID);
            boolean hasAdminPerm = player.hasPermission("shantek.postoffice.register") || player.isOp();

            if (!isOwner && !hasAdminPerm) {
                invalidPermission(sender);
                return false;
            }

            // Optionally clear the sign associated with the post box
            Block signBlock = postOffice.helpers.getSignForBarrel(barrelBlock);
            if (signBlock != null && signBlock.getState() instanceof Sign) {
                Sign sign = (Sign) signBlock.getState();

                // Clear all lines of the sign (if you want to fully reset it)
                for (int i = 0; i < 4; i++) {
                    sign.setLine(i, "");
                }

                // Finally, update the sign
                boolean signUpdated = sign.update();

                // Log if the sign was successfully updated
                if (!signUpdated) {
                    player.sendMessage(ChatColor.RED + "There was an issue updating the sign.");
                }
            }

            // If removing a primary box, warn about secondary boxes
            if ("claimed".equals(barrelState) && boxOwnerUUID != null) {
                int secondaryCount = postOffice.helpers.countPlayerSecondaryBoxes(boxOwnerUUID);
                if (secondaryCount > 0) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        postOffice.language.secondaryBoxesWillNotWork
                                .replace("%count%", String.valueOf(secondaryCount))));
                }
            }

            // Call the helper to remove the barrel from the cache and config
            postOffice.helpers.removeBarrelFromCache(barrelBlock);

            // Send appropriate message based on box type
            if ("secondary".equals(barrelState)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.secondaryBoxRemoved));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.postBoxRemoved));
            }

            return true;

        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

    }

    public boolean onCommandReload(CommandSender sender) {
        if (sender.hasPermission("shantek.postoffice.reload") || sender.isOp()) {

            // Reload all the config and language file
            postOffice.pluginConfig.initializeAndLoadConfig();
            postOffice.pluginConfig.initializeAndLoadLang();
            postOffice.helpers.saveCacheToFile();  // Save cache instead of config directly
            postOffice.helpers.reloadBarrelsConfig();
            postOffice.helpers.loadBlacklist();

            sender.sendMessage(ChatColor.GREEN + "Post Office config file has been reloaded.");
            return true;
        } else {
            // Player doesn't have permission to use the command
            invalidPermission(sender);
            return false;
        }
    }

    public boolean onCommandInfo(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Get the block the player is looking at (sign or barrel)
            Block targetBlock = postOffice.helpers.getBlockLookingAt(player, 6);

            // Exit out if they aren't looking at a block or are too far away
            if (targetBlock == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                return true;
            }

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

            // If it's a secondary box, show different info
            if (state != null && state.equals("secondary")) {
                if (owner != null && !owner.equals("none")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            postOffice.language.secondaryBoxInfo
                                    .replace("%owner%", owner)
                    ));
                    
                    // Show primary box location
                    UUID ownerUUID = postOffice.helpers.getOwnerUUID(barrelBlock);
                    if (ownerUUID != null) {
                        Block primaryBox = postOffice.helpers.getPrimaryBoxForPlayer(ownerUUID);
                        if (primaryBox != null) {
                            String primaryLocation = String.format("%s [%d, %d, %d]",
                                primaryBox.getWorld().getName(),
                                primaryBox.getX(),
                                primaryBox.getY(),
                                primaryBox.getZ());
                            player.sendMessage(ChatColor.GRAY + "Primary box: " + ChatColor.WHITE + primaryLocation);
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.invalidPostbox));
                }
            }
            // If there's an owner, print it. Otherwise, print the state.
            else if (owner != null && !owner.equals("none")) {
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
        } else {
            invalidPermission(sender);
            return false;
        }
    }

    public boolean onCommandRegister(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (sender.hasPermission("shantek.postoffice.register") || sender.isOp()) {

                // Get the block the player is looking at (sign or barrel)
                Block targetBlock = postOffice.helpers.getBlockLookingAt(player, 6);


                // Exit out if they aren't looking at a block or are too far away
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
                invalidPermission(sender);
                return false;

            }
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
    }

    public boolean onCommandClaim(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();

            if (sender.hasPermission("shantek.postoffice.claim") || sender.isOp()) {

                // Get the block the player is looking at (sign or barrel)
                Block targetBlock = postOffice.helpers.getBlockLookingAt(player, 6);

                // Exit out if they aren't looking at a block or are too far away
                if (targetBlock == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                    return true;
                }

                if (!(targetBlock.getState() instanceof Sign)) {
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
                if (targetBlock.getState() instanceof Sign) {
                    Sign sign = (Sign) targetBlock.getState();
                    sign.setLine(1, player.getName()); // Set the player's name on the 2nd line
                    sign.update();
                }

                postOffice.helpers.saveCacheToFile(); // Save the cache to disk
                return true;
            } else {
                // Player doesn't have permission to use the command
                invalidPermission(sender);
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
    }

    public boolean onCommandClaimOthers(CommandSender sender, String[] args) {
        if (sender.hasPermission("shantek.postoffice.claim.others") || sender.isOp()) {

            // Ensure they are looking at a sign
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;
            // Get the block the player is looking at (sign or barrel)
            Block targetBlock = postOffice.helpers.getBlockLookingAt(player, 6);

            // Exit out if they aren't looking at a block or are too far away
            if (targetBlock == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
                return true;
            }

            if (!(targetBlock.getState() instanceof Sign)) {
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
            if (targetBlock.getState() instanceof Sign) {
                Sign sign = (Sign) targetBlock.getState();
                sign.setLine(1, targetPlayer.getName()); // Set the player's name on the 2nd line
                sign.update();
            }

            // Confirm to the person running the command that it worked
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.claimedFor
                            .replace("%owner%", targetPlayerName)));

            // Let the owner know they now have a post box, if they're online
            if (targetPlayer.isOnline()) {
                Objects.requireNonNull(targetPlayer.getPlayer()).sendMessage(ChatColor.translateAlternateColorCodes('&',
                        postOffice.language.claimedForOtherPlayer));
            }
            postOffice.helpers.saveCacheToFile(); // Save the cache to disk
            return true;
        }
        else {
            // Player doesn't have permission to use the command
            invalidPermission(sender);
            return false;
        }
    }

    public boolean onCommandSecondary(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Permission check - players can register their own secondary boxes
        if (!player.hasPermission("shantek.postoffice.claim") && !player.isOp()) {
            invalidPermission(sender);
            return false;
        }

        // Check if player has a primary box first
        if (!postOffice.helpers.doesPlayerHavePostBox(playerUUID)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.noPrimaryBox));
            return true;
        }

        // Check max secondary boxes limit (if not unlimited)
        if (postOffice.maxSecondaryBoxes != -1) {
            int currentCount = postOffice.helpers.countPlayerSecondaryBoxes(playerUUID);
            if (currentCount >= postOffice.maxSecondaryBoxes) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        postOffice.language.maxSecondaryBoxes.replace("%max%", 
                                String.valueOf(postOffice.maxSecondaryBoxes))));
                return true;
            }
        }

        // Get the block the player is looking at
        Block targetBlock = postOffice.helpers.getBlockLookingAt(player, 6);

        if (targetBlock == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
            return true;
        }

        if (!(targetBlock.getState() instanceof Sign)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.lookAtPostBox));
            return true;
        }

        // Ensure the sign is attached to a barrel
        Block attachedBarrel = postOffice.helpers.getAttachedBarrel(targetBlock);
        if (attachedBarrel == null || attachedBarrel.getType() != Material.BARREL) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.signOnBarrel));
            return true;
        }

        // Check that the barrel is named "secondary"
        if (attachedBarrel.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) attachedBarrel.getState();
            if (barrel.getCustomName() == null || !barrel.getCustomName().equalsIgnoreCase(PostOffice.SECONDARY_BARREL_NAME)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    postOffice.language.barrelMustBeNamedSecondary));
                return true;
            }
        }

        // Check if barrel is already registered
        if (postOffice.helpers.isBarrelInConfig(attachedBarrel)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.alreadyRegistered));
            return true;
        }

        // Register the secondary box
        postOffice.helpers.addOrUpdateBarrelInCache(attachedBarrel, targetBlock, playerUUID, "secondary");

        // Update the sign to display the player's name
        if (targetBlock.getState() instanceof Sign) {
            Sign sign = (Sign) targetBlock.getState();
            sign.setLine(1, player.getName());
            sign.update();
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.secondaryBoxRegistered));
        postOffice.helpers.saveCacheToFile();

        return true;
    }

    public boolean onCommandList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shantek.postoffice.register") && !sender.isOp()) {
            invalidPermission(sender);
            return false;
        }

        String targetPlayerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.playerNotFound));
            return true;
        }

        UUID playerUUID = targetPlayer.getUniqueId();
        List<Helpers.BarrelInfo> boxes = postOffice.helpers.getAllBoxesForPlayer(playerUUID);

        if (boxes.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.noPostBoxes.replace("%player%", targetPlayerName)));
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "=== Post Boxes for " + ChatColor.WHITE + targetPlayerName + ChatColor.GREEN + " ===");

        for (Helpers.BarrelInfo box : boxes) {
            String type = box.state.equals("claimed") ? "PRIMARY" : 
                         box.state.equals("secondary") ? "SECONDARY" : "REGISTERED";
            String location = String.format("%s [%d, %d, %d]",
                    box.world, box.x, box.y, box.z);

            sender.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + type +
                    ChatColor.GRAY + " - " + ChatColor.AQUA + location);
        }

        return true;
    }

    public boolean onCommandRemoveSecondary(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shantek.postoffice.register") && !sender.isOp()) {
            invalidPermission(sender);
            return false;
        }

        String targetPlayerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.playerNotFound));
            return true;
        }

        UUID playerUUID = targetPlayer.getUniqueId();
        int secondaryCount = postOffice.helpers.countPlayerSecondaryBoxes(playerUUID);

        if (secondaryCount == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    postOffice.language.noSecondaryBoxes.replace("%player%", targetPlayerName)));
            return true;
        }

        // Remove all secondary boxes
        postOffice.helpers.removeAllSecondaryBoxesForPlayer(playerUUID);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                postOffice.language.allSecondaryBoxesRemovedAdmin
                        .replace("%count%", String.valueOf(secondaryCount))
                        .replace("%player%", targetPlayerName)));

        return true;
    }

    public void invalidPermission(CommandSender sender)
    {
        // Player doesn't have permission to use the command
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', postOffice.language.noPermission));
    }

}

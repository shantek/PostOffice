package io.shantek;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class PostOffice extends JavaPlugin implements Listener {

    //region Configuration Variables
    public String customBarrelName = "pobox";
    private File mailFile;
    int previousItemCount = 0;
    int newItemCount = 0;
    private Set<String> playersWithMail = new HashSet<>();

    public String sentMessage = "&a[Post Office] &aMail sent to %receiver%.";
    public String receivedMessage = "&a[Post Office] &eYou received mail from %sender%!";
    public String gotMailMessage = "&a[Post Office] &fYou got mail!";
    public String cantStackItems = "&a[Post Office] &4You don't have permission to do that.";
    public String removeItemError = "&a[Post Office] &4You don't have permission to remove items.";
    public String offHandError = "&a[Post Office] &4No offhand usage while in a Post Box!";
    public String hotBarError = "&a[Post Office] &4No hot bar usage while in a Post Box!";
    public String breakError = "&a[Post Office] &4You can't break a Post Box.";
    public String createError = "&a[Post Office] &4You can't create a Post Box.";
    public String postboxCreated = "&a[Post Office] &4 Box successfully created for %username%";
    public UpdateChecker updateChecker;
    public PluginConfig pluginConfig;

    public PostOffice() {

        // Other initialization code
    }

    //endregion

    public boolean updateNotificationEnabled = true;
    public boolean postBoxProtection = true;
    public boolean consoleLogs = true;

    public void onEnable() {

        // Ensure the data folder exists
        if (!getDataFolder().exists()) {
            if (getDataFolder().mkdir()) {
                getLogger().info("Data folder created successfully.");
            } else {
                getLogger().warning("Failed to create data folder.");
            }
        }


        this.mailFile = new File(getDataFolder(), "mail.txt");
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new BarrelProtection(this), this);

        PluginCommand barrelNameCommand = this.getCommand("postoffice");
        if (barrelNameCommand != null) {
            barrelNameCommand.setExecutor(this);
        } else {
            getLogger().warning("Command 'postoffice' not found!");
        }

        // Create an instance of UpdateChecker
        this.updateChecker = new UpdateChecker();

        // Create an instance of PluginConfig
        this.pluginConfig = new PluginConfig(this);

        // Register the permission node
        Permission removeItemsPermission = new Permission("shantek.postoffice.removeitems");
        PluginManager pm = getServer().getPluginManager();
        pm.addPermission(removeItemsPermission);

        // Permission for breaking Post Boxes
        Permission breakPermission = new Permission("shantek.postoffice.break");
        pm.addPermission(breakPermission);

        // Permission for creating Post Boxes
        Permission createBoxPermission = new Permission("shantek.postoffice.create");
        pm.addPermission(createBoxPermission);

        // Permission for breaking Post Boxes
        Permission updateNotificationPermission = new Permission("shantek.postoffice.updatenotification");
        pm.addPermission(updateNotificationPermission);

        pluginConfig.reloadConfigFile();

        if (this.mailFile.exists()) {
            try (Stream<String> lines = Files.lines(mailFile.toPath())) {
                this.playersWithMail = lines
                        .filter(line -> !line.isEmpty()) // Only non-empty lines
                        .collect(Collectors.toCollection(HashSet::new));
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not read mail file", e);
            }
        } else {
            try {
                boolean fileCreated = this.mailFile.createNewFile();
                if (fileCreated) {
                    getLogger().info("Mail file created successfully.");
                } else {
                    getLogger().warning("Mail file creation failed. It may already exist.");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create mail file", e);
            }
        }

        this.getServer().getPluginManager().registerEvents(new PlayerLoginListener(), this);

        int pluginId = 20173; // <-- Replace with the id of your plugin!
        @SuppressWarnings("unused") Metrics metrics = new Metrics(this, pluginId);

        // CHECK FOR PLUGIN UPDATES IF ENABLED
        if (updateNotificationEnabled) {
            UpdateChecker.checkForUpdatesAsync(getDescription().getVersion(), this);
        }
    }

    private class PlayerLoginListener implements Listener {
        @EventHandler
        public void onPlayerLogin(PlayerJoinEvent event) {
            Player player = event.getPlayer();

            if (playersWithMail.contains(player.getName())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', gotMailMessage));
            }

            if (updateNotificationEnabled && (player.isOp() || player.hasPermission("shantek.postoffice.updatenotification"))) {
                if (UpdateChecker.isNewVersionAvailable(getDescription().getVersion(), UpdateChecker.remoteVersion)) {
                    player.sendMessage("[Post Office] An update is available! New version: " + UpdateChecker.remoteVersion);
                }
            }

        }
    }

    public void onDisable() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    // File created successfully
                    getLogger().info("Config file created successfully.");
                } else {
                    // File already exists
                    getLogger().warning("Config file already exists.");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create config file", e);
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("custom-barrel-name", this.customBarrelName);

        try {
            config.save(configFile);
        } catch (IOException var4) {
            getLogger().log(Level.SEVERE, "Could not save config file", var4);
        }

    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("postoffice")) {
            if (args.length == 2 && args[0].equalsIgnoreCase("barrelname")) {
                // Check if the second argument is present
                String name = args[1];
                this.customBarrelName = name;

                pluginConfig.setCustomBarrelName(this.customBarrelName);
                sender.sendMessage("Custom barrel name set to " + this.customBarrelName);
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("barrelname")) {
                sender.sendMessage(ChatColor.RED + "Invalid barrel name, use /postoffice barrelname <name>");
                return true;

            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shantek.postoffice.reload") || sender.isOp()) {
                    // Reload logic
                    pluginConfig.reloadConfigFile();
                    sender.sendMessage(ChatColor.GREEN + "PostOffice config reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                }
                return true;
            } else {
                // Invalid command format
                sender.sendMessage(ChatColor.RED + "Invalid command");
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("postoffice")) {
            if (args.length == 1) {
                // Check the first argument
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
                if ("barrelname".startsWith(args[0].toLowerCase()) && sender.hasPermission("shantek.postoffice.setname")) {
                    completions.add("barrelname");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("barrelname") && sender.hasPermission("shantek.postoffice.setname")) {
                // Check the second argument for "barrelname" subcommand
                if (args[1].isEmpty()) {
                    completions.add("<name>");
                }
            }
        }

        return completions;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        Inventory inventory = event.getInventory();

        if (inventory.getType() == InventoryType.BARREL) {

            Block clickedBlock = Objects.requireNonNull(inventory.getLocation()).getBlock();
            if (clickedBlock.getType() == Material.BARREL) {
                BlockState blockState = clickedBlock.getState();

                if (blockState instanceof Barrel) {
                    Barrel barrel = (Barrel) blockState;

                    if (barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(this.customBarrelName)) {

                        previousItemCount = countNonNullItems(inventory.getContents());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // IF THEY DON'T HAVE A BARREL OPEN, IGNORE IT
        if (event.getClickedInventory() == null || inventory.getType() != InventoryType.BARREL) {
            return;
        }

        Block clickedBlock = Objects.requireNonNull(event.getClickedInventory().getLocation()).getBlock();

        if (clickedBlock.getType() == Material.BARREL) {
            BlockState blockState = clickedBlock.getState();

            if (blockState instanceof Barrel) {
                Barrel barrel = (Barrel) blockState;

                if (barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(this.customBarrelName)) {

                    String ownerName = "";

                    for (BlockFace blockFace : BlockFace.values()) {
                        Block relativeBlock = clickedBlock.getRelative(blockFace);

                        if (relativeBlock.getType().name().toUpperCase().contains("SIGN")) {
                            Sign sign = (Sign) relativeBlock.getState();
                            Location signLoc = relativeBlock.getLocation().subtract(blockFace.getDirection());

                            if (sign.getLine(1).equalsIgnoreCase(player.getName()) && signLoc.equals(clickedBlock.getLocation())) {
                                ownerName = sign.getLine(1);
                                break;
                            }
                        }
                    }

                    boolean isNotOwner = !ownerName.equalsIgnoreCase(player.getName());

                    // Prevent non-owners who do not have OP from taking items or shift-clicking
                    if (!player.isOp() && isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && (event.getAction().name().contains("PICKUP") || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', removeItemError));
                        return;
                    }

                    // If player is not the owner and trying to take an item, cancel the event
                    if (isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', removeItemError));
                        return;
                    }

                    if (isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && !player.isOp()) {
                        // CHECK IF THE PLAYER DOESN'T HAVE PERMISSION TO USE THIS BARREL, RESTRICT NUMBER KEY CLICKING TO MOVE TO HOTBAR
                        if (event.getWhoClicked() instanceof Player && event.getClickedInventory() != null) {
                            ItemStack hotbarItem = event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : null;

                            if (hotbarItem != null) {
                                event.setCancelled(true);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', hotBarError));
                                return;
                            }
                        }

                        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', hotBarError));
                            return;
                        }
                    }

                    // Check if item is already in the barrel
                    ItemStack[] contents = inventory.getContents();
                    for (ItemStack item : contents) {
                        if (item != null && event.getCurrentItem() != null && item.isSimilar(event.getCurrentItem())) {
                            // If the player is not the owner and trying to add to an existing stack, cancel the event
                            if (isNotOwner && !player.hasPermission("shantek.postoffice.removeitems") && event.getAction() == InventoryAction.PLACE_ALL && item.getAmount() < item.getMaxStackSize()) {
                                event.setCancelled(true);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', cantStackItems));
                                return;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        Player player = (Player) event.getPlayer();
        if (inventory.getType() == InventoryType.BARREL) {
            Block clickedBlock = Objects.requireNonNull(event.getInventory().getLocation()).getBlock();

            if (clickedBlock.getType() == Material.BARREL) {
                BlockState blockState = clickedBlock.getState();

                if (blockState instanceof Barrel) {
                    Barrel barrel = (Barrel) blockState;

                    if (barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(this.customBarrelName)) {

                        boolean isOwner = false;
                        String ownerName = "";

                        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                            Block relativeBlock = clickedBlock.getRelative(face);

                            if (relativeBlock.getType().name().toUpperCase().contains("SIGN")) {
                                Sign sign = (Sign) relativeBlock.getState();
                                ownerName = sign.getLine(1);

                                if (sign.getLine(1).equalsIgnoreCase(event.getPlayer().getName())) {
                                    isOwner = true;
                                    break;
                                }
                            }
                        }

                        // Only process the logic if the ownerName is not empty
                        if (!ownerName.isEmpty()) {
                            if (!isOwner) {
                                newItemCount = countNonNullItems(inventory.getContents());
                                if (newItemCount > previousItemCount) {

                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            this.sentMessage
                                                    .replace("%sender%", player.getName())
                                                    .replace("%receiver%", ownerName)));

                                    // Add owners to mail list if someone else is adding items
                                    if (consoleLogs) {
                                        getLogger().info(player.getName() + " added mail for " + ownerName);
                                    }
                                    playersWithMail.add(ownerName);
                                    saveMailFile();

                                    Player owner = Bukkit.getPlayer(ownerName);
                                    if (owner != null) {
                                        owner.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                this.receivedMessage
                                                        .replace("%sender%", player.getName())
                                                        .replace("%receiver%", owner.getName())));
                                    }
                                }
                            } else {
                                // If they were the owner, and it was their barrel, remove them from the mail list
                                playersWithMail.remove(event.getPlayer().getName());
                                saveMailFile();
                            }
                        }
                    }
                }
            }
        }
    }

    private void saveMailFile() {
        try {
            if (consoleLogs) {
                getLogger().info("Mail list updated: " + playersWithMail);
            }
            Files.write(this.mailFile.toPath(), this.playersWithMail);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save mail file", e);
        }
    }

    private int countNonNullItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                //count++;
                count += item.getAmount();
            }
        }
        return count;
    }
}
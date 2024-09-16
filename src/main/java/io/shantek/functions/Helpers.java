package io.shantek.functions;

import io.shantek.PostOffice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Helpers {

    private final PostOffice postOffice;

    public Helpers(PostOffice postOffice) {
        this.postOffice = postOffice;
        barrelsCache = new HashMap<>();
        loadBarrelsIntoCache(); // Load data from barrels.yml into the cache at startup
    }

    // In-memory cache to store barrel data (key: block location, value: owner UUID)
    private Map<String, BarrelData> barrelsCache;

    private FileConfiguration barrelsConfig = null;
    private File barrelsConfigFile = null;

    //region Barrel Operations

    // Check if there's a barrel with the custom name nearby
    public boolean hasBarrelNearby(Block block) {
        // Check if the given block is a barrel with the custom name
        if (block.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) block.getState();
            String barrelCustomName = barrel.getCustomName();

            if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                return true;
            }
        }

        // Check if any nearby block is a barrel with the custom name
        for (BlockFace blockFace : BlockFace.values()) {
            Block relativeBlock = block.getRelative(blockFace);

            if (relativeBlock.getType() == Material.BARREL) {
                Barrel barrel = (Barrel) relativeBlock.getState();
                String barrelCustomName = barrel.getCustomName();

                if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Method to check if the block the player is looking at is a custom barrel
    public boolean isLookingAtCustomBarrel(Player player) {

        // Get the block the player is looking at (with a max distance of 10)
        Block targetBlock = player.getTargetBlock((Set<Material>) null, 10);

        if (targetBlock.getType() == Material.BARREL) {
            BlockState state = targetBlock.getState();

            if (state instanceof Barrel) {
                Barrel barrel = (Barrel) state;
                String barrelCustomName = barrel.getCustomName();

                // Check if the barrel has the custom name
                return barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName);
            }
        }

        return false; // Not looking at a valid custom barrel
    }

    // Check if the block is a protected post box
    public boolean isProtectedPostBox(Block block) {
        if (block.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) block.getState();
            String barrelCustomName = barrel.getCustomName();
            return barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName);
        } else if (Tag.SIGNS.isTagged(block.getType())) {
            return isSignNextToProtectedBarrel(block);
        }
        return false;
    }

    // Check if a sign is next to a protected barrel
    public boolean isSignNextToProtectedBarrel(Block signBlock) {
        BlockFace[] adjacentFaces = {
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        };

        for (BlockFace face : adjacentFaces) {
            Block adjacentBlock = signBlock.getRelative(face);
            if (adjacentBlock.getType() == Material.BARREL) {
                Barrel barrel = (Barrel) adjacentBlock.getState();
                String barrelCustomName = barrel.getCustomName();
                if (barrelCustomName != null && barrelCustomName.equalsIgnoreCase(postOffice.customBarrelName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Check for a sign attached to a barrel
    public Block getAttachedSign(Block barrelBlock) {
        for (BlockFace face : BlockFace.values()) {
            Block relativeBlock = barrelBlock.getRelative(face);
            if (Tag.SIGNS.isTagged(relativeBlock.getType())) {
                return relativeBlock; // Return the block if it's a sign
            }
        }
        return null; // No sign found
    }

    //region Barrel Caching and Config Operations

    private void loadBarrelsIntoCache() {
        barrelsCache.clear(); // Clear the cache before reloading

        FileConfiguration barrelsConfig = getBarrelsConfig();
        if (barrelsConfig.contains("barrels")) {
            Set<String> keys = barrelsConfig.getConfigurationSection("barrels").getKeys(false);
            postOffice.getLogger().info("Loading barrels into cache. Found keys: " + keys);

            for (String key : keys) {
                String path = "barrels." + key;
                String ownerUUIDString = barrelsConfig.getString(path + ".owner");
                String state = barrelsConfig.getString(path + ".state", "unregistered"); // Default to "unregistered"
                String signLocation = barrelsConfig.getString(path + ".sign");

                UUID ownerUUID = null;
                if (ownerUUIDString != null && !ownerUUIDString.equalsIgnoreCase("none")) {
                    try {
                        ownerUUID = UUID.fromString(ownerUUIDString);
                    } catch (IllegalArgumentException e) {
                        postOffice.getLogger().warning("Invalid UUID found for barrel at " + key);
                        continue; // Skip this barrel if UUID is invalid
                    }
                }

                // Create a BarrelData object and store it in the cache
                BarrelData barrelData = new BarrelData(ownerUUID, state, signLocation);
                barrelsCache.put(key, barrelData); // Add to cache

                postOffice.getLogger().info("Added barrel to cache at location: " + key + ", Owner: "
                        + (ownerUUID != null ? ownerUUID : "none") + ", State: " + state + ", Sign: " + signLocation);
            }
        } else {
            postOffice.getLogger().warning("No barrels found in barrels.yml during cache load.");
        }
    }

    public void addBarrelWithSign(Block barrelBlock, Block signBlock, UUID newOwnerUUID, String state) {
        if (barrelBlock.getType() != Material.BARREL || !Tag.SIGNS.isTagged(signBlock.getType())) {
            return; // Only proceed if the block is a barrel and the sign is valid
        }

        String barrelLocationString = getBlockLocationString(barrelBlock);
        String signLocationString = getBlockLocationString(signBlock);

        // Create or update the BarrelData
        BarrelData barrelData = new BarrelData(newOwnerUUID, state, signLocationString);

        // Add or update the barrel data in the cache
        barrelsCache.put(barrelLocationString, barrelData);

        // Log for debugging purposes
        postOffice.getLogger().info("Adding/Updating barrel at: " + barrelLocationString);
        postOffice.getLogger().info("Sign location for barrel: " + signLocationString);

        // Save the updated cache to disk
        saveBarrelsToDisk();
    }

    // Remove barrel from in-memory cache and save to disk if necessary
    public void removeBarrel(Block block) {
        String blockLocationString = getBlockLocationString(block);

        // Check if the barrel exists in the cache
        if (barrelsCache.containsKey(blockLocationString)) {
            // Barrel exists, remove it from the cache
            barrelsCache.remove(blockLocationString);

            // Remove the barrel from the config as well
            barrelsConfig.set("barrels." + blockLocationString, null);

            // Save the updated cache to barrels.yml
            saveBarrelsToDisk();
        }
    }

    // Save the in-memory cache to barrels.yml
    public void saveBarrelsToDisk() {
        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Clear existing entries in the file
        barrelsConfig.set("barrels", null);

        // Iterate over the cache and save each barrel's data
        for (Map.Entry<String, BarrelData> entry : barrelsCache.entrySet()) {
            String barrelLocationString = entry.getKey();
            BarrelData barrelData = entry.getValue();

            String path = "barrels." + barrelLocationString;

            // Set the owner UUID, or "none" if no owner is set
            barrelsConfig.set(path + ".owner", barrelData.getOwnerUUID() != null ? barrelData.getOwnerUUID().toString() : "none");

            // Set the state (e.g., "claimed", "registered")
            barrelsConfig.set(path + ".state", barrelData.getState());

            // Set the sign location if it exists
            barrelsConfig.set(path + ".sign", barrelData.getSignLocation());

            // Parse and store the world and coordinates from the barrel location string
            String[] parts = barrelLocationString.split("_");
            if (parts.length == 4) {
                barrelsConfig.set(path + ".world", parts[0]);
                barrelsConfig.set(path + ".x", Integer.parseInt(parts[1]));
                barrelsConfig.set(path + ".y", Integer.parseInt(parts[2]));
                barrelsConfig.set(path + ".z", Integer.parseInt(parts[3]));
            }
        }

        // Save the updated configuration to barrels.yml
        saveBarrelsConfig();
    }


    public void registerBarrelWithSign(Block barrelBlock, Block signBlock, UUID ownerUUID, String state) {
        String barrelLocationString = getBlockLocationString(barrelBlock);
        String signLocationString = getBlockLocationString(signBlock);

        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Register barrel and sign in barrels.yml with proper world and coordinates
        barrelsConfig.set("barrels." + barrelLocationString + ".state", state); // Set the state to registered/claimed
        barrelsConfig.set("barrels." + barrelLocationString + ".owner", ownerUUID != null ? ownerUUID.toString() : "none");
        barrelsConfig.set("barrels." + barrelLocationString + ".sign", signLocationString);
        barrelsConfig.set("barrels." + barrelLocationString + ".world", barrelBlock.getWorld().getName());
        barrelsConfig.set("barrels." + barrelLocationString + ".x", barrelBlock.getX());
        barrelsConfig.set("barrels." + barrelLocationString + ".y", barrelBlock.getY());
        barrelsConfig.set("barrels." + barrelLocationString + ".z", barrelBlock.getZ());

        // Log the barrel and sign registration for debugging purposes
        postOffice.getLogger().info("Registering barrel at: " + barrelLocationString);
        postOffice.getLogger().info("Sign location for barrel: " + signLocationString);

        saveBarrelsConfig(); // Save changes to barrels.yml
        reloadBarrelsConfig();
    }

    public Block getSignFromConfig(Block barrelBlock) {
        String barrelLocationString = getBlockLocationString(barrelBlock); // Convert block to location string
        String path = "barrels." + barrelLocationString + ".sign"; // Use this location string in the config

        // Look up the sign location in the config
        if (barrelsConfig.contains(path)) {
            String signLocation = barrelsConfig.getString(path);
            String[] parts = signLocation.split("_");

            if (parts.length == 4) {
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    return world.getBlockAt(x, y, z); // Return the block at the saved sign location
                }
            }
        }
        return null; // Sign not found in the config
    }

    // Helper method to get block location string
    public String getBlockLocationString(Block block) {
        return block.getWorld().getName() + "_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
    }

    // Get the owner name from the barrels.yml config
    public String getOwnerNameFromConfig(String barrelLocationString) {
        FileConfiguration barrelsConfig = getBarrelsConfig();

        String ownerUUIDString = barrelsConfig.getString("barrels." + barrelLocationString + ".owner");

        if (ownerUUIDString != null && !ownerUUIDString.equals("none")) {
            UUID ownerUUID = UUID.fromString(ownerUUIDString);
            return getPlayerName(ownerUUID); // Retrieve player's name from UUID
        }

        return "none"; // No owner
    }

    // Get the state of the post box from the barrels.yml config
    public String getStateFromConfig(String barrelLocationString) {
        FileConfiguration barrelsConfig = getBarrelsConfig();
        return barrelsConfig.getString("barrels." + barrelLocationString + ".state", "unregistered"); // Default to "registered"
    }

    public boolean doesPlayerHavePostBox(UUID playerUUID) {
        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Check if the 'barrels' section exists before accessing it
        if (barrelsConfig.contains("barrels")) {
            ConfigurationSection barrelsSection = barrelsConfig.getConfigurationSection("barrels");

            // Loop through all barrels to check if any are owned by this player
            for (String barrelLocation : barrelsSection.getKeys(false)) {
                String ownerUUIDString = barrelsConfig.getString("barrels." + barrelLocation + ".owner");
                if (ownerUUIDString != null && ownerUUIDString.equals(playerUUID.toString())) {
                    return true; // The player already owns a post box
                }
            }
        }
        return false; // The player does not have a post box
    }


    public String getPlayerPostBoxLocation(UUID playerUUID) {
        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Loop through all barrels to find the one owned by the player
        for (String barrelLocation : barrelsConfig.getConfigurationSection("barrels").getKeys(false)) {
            String ownerUUIDString = barrelsConfig.getString("barrels." + barrelLocation + ".owner");
            if (ownerUUIDString != null && ownerUUIDString.equals(playerUUID.toString())) {

                // Check if world, x, y, z are stored properly
                String worldName = barrelsConfig.getString("barrels." + barrelLocation + ".world", "Unknown World");
                int x = barrelsConfig.getInt("barrels." + barrelLocation + ".x", Integer.MIN_VALUE);
                int y = barrelsConfig.getInt("barrels." + barrelLocation + ".y", Integer.MIN_VALUE);
                int z = barrelsConfig.getInt("barrels." + barrelLocation + ".z", Integer.MIN_VALUE);

                // Check if coordinates are valid (i.e., not the fallback value)
                if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
                    return "Unknown location"; // Coordinates are not valid
                }

                // Return formatted location string
                return worldName + " [" + x + ", " + y + ", " + z + "]";
            }
        }
        return "Unknown location"; // Fallback if no post box is found
    }


    //endregion

    //region Retrieving Sign Information

    // Retrieve the location of the sign attached to a barrel
    public Block getSignForBarrel(Block barrelBlock) {
        String barrelLocationString = getBlockLocationString(barrelBlock);

        // Retrieve the sign's location from the config
        String signLocationString = barrelsConfig.getString("barrels." + barrelLocationString + ".sign");

        if (signLocationString != null) {
            String[] parts = signLocationString.split("_");
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            return postOffice.getServer().getWorld(worldName).getBlockAt(x, y, z);
        }

        return null; // No sign found for this barrel
    }

    // Method to check if the sign is attached to a barrel
    public Block getAttachedBarrel(Block signBlock) {
        for (BlockFace face : BlockFace.values()) {
            Block attachedBlock = signBlock.getRelative(face);
            if (attachedBlock.getType() == Material.BARREL) {
                return attachedBlock;
            }
        }
        return null; // No barrel found attached to the sign
    }

    public Block getBarrelFromSign(Block signBlock) {
        String signLocationString = getBlockLocationString(signBlock);
        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Ensure we have a valid configuration section for barrels
        ConfigurationSection section = barrelsConfig.getConfigurationSection("barrels");
        if (section == null) {
            postOffice.getLogger().severe("Barrels section is missing in the config");
            return null;
        }

        // Loop through all stored barrels to find one with this sign location
        for (String barrelLocation : section.getKeys(false)) {
            String storedSignLocation = barrelsConfig.getString("barrels." + barrelLocation + ".sign");

            if (signLocationString.equals(storedSignLocation)) {
                // The sign matches, get the barrel block
                String[] parts = barrelLocation.split("_");
                if (parts.length == 4) {
                    String worldName = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);

                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        return world.getBlockAt(x, y, z); // Return the barrel block
                    }
                }
            }
        }
        return null; // No barrel found for this sign
    }

    //endregion

    //region Plugin Configuration (YAML Management)

    // Load the barrels.yml configuration
    public void reloadBarrelsConfig() {
        if (barrelsConfigFile == null) {
            barrelsConfigFile = new File(postOffice.getDataFolder(), "barrels.yml");
        }
        barrelsConfig = YamlConfiguration.loadConfiguration(barrelsConfigFile);

        // Create the file if it doesn't exist
        if (!barrelsConfigFile.exists()) {
            postOffice.saveResource("barrels.yml", false);
        }

        // Debugging to check if it's loading correctly
        if (barrelsConfig.contains("barrels")) {
            Set<String> keys = barrelsConfig.getConfigurationSection("barrels").getKeys(false);
            postOffice.getLogger().info("Keys in barrels.yml after reload: " + keys.toString());
        } else {
            postOffice.getLogger().warning("No barrels found in barrels.yml during reload.");
        }
    }

    // Get the barrels.yml configuration
    public FileConfiguration getBarrelsConfig() {
        if (barrelsConfig == null) {
            reloadBarrelsConfig();
        }
        return barrelsConfig;
    }

    // Save changes to barrels.yml
    public void saveBarrelsConfig() {
        if (barrelsConfig == null || barrelsConfigFile == null) {
            return;
        }
        try {
            barrelsConfig.save(barrelsConfigFile);
        } catch (IOException e) {
            postOffice.getLogger().severe("Could not save barrels.yml: " + e.getMessage());
        }
    }

    //endregion

    //region Mail and Item Operations

    public void saveMailFile() {
        try {
            if (postOffice.consoleLogs) {
                postOffice.getLogger().info("The mail list has been updated.");
            }
            Files.write(postOffice.mailFile.toPath(), postOffice.playersWithMail);
        } catch (IOException e) {
            postOffice.getLogger().log(Level.SEVERE, "Error updating the mail file.", e);
        }
    }

    public int countNonNullItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public void checkForDataFolder() {
        if (!postOffice.getDataFolder().exists()) {
            if (postOffice.getDataFolder().mkdir()) {
                postOffice.getLogger().info("Data folder created successfully.");
            } else {
                postOffice.getLogger().warning("Error creating the data folder.");
            }
        }
    }

    public OfflinePlayer getPlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public OfflinePlayer getPlayerReference(UUID ownerUuid) {
        return Bukkit.getOfflinePlayer(ownerUuid);
    }

    public boolean isPostBoxOwner(Block block, Player player) {
        UUID playerUUID = player.getUniqueId();
        return getOwnerUUID(block).equals(playerUUID);

    }

    public boolean isBarrelInConfig(Block block) {
        String blockLocationString = getBlockLocationString(block);
        return barrelsCache.containsKey(blockLocationString);
    }

    public UUID getOwnerUUID(Block block) {
        String blockLocationString = getBlockLocationString(block);
        BarrelData barrelData = barrelsCache.get(blockLocationString);
        return barrelData != null ? barrelData.getOwnerUUID() : null;
    }
    // Method to get a player's name by UUID, checking both online and offline players
    public String getPlayerName(UUID uuid) {
        // Check if the player is online
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        // If the player is offline, use getOfflinePlayer
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName(); // This may return null if the player's name is not available
    }

    public UUID getPlayerUUID(Player player) {
        return player.getUniqueId();
    }

    // Method to check if a player is online by UUID
    public boolean isPlayerOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null;
    }

    //endregion



    public void addOrUpdateBarrelInCache(Block barrelBlock, Block signBlock, UUID ownerUUID, String state) {
        String barrelLocationString = getBlockLocationString(barrelBlock);
        String signLocationString = getBlockLocationString(signBlock);

        // Update the cache with the new data
        barrelsCache.put(barrelLocationString, new BarrelData(ownerUUID, signLocationString, state));

        // Optionally, you could save the cache to file immediately here:
        saveCacheToFile();
    }

    public void removeBarrelFromCache(Block barrelBlock) {
        String barrelLocationString = getBlockLocationString(barrelBlock);

        // Remove the barrel from the cache
        barrelsCache.remove(barrelLocationString);

        // Optionally, you could save the cache to file immediately here:
        // saveCacheToFile();
    }

    public void saveCacheToFile() {
        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Clear the existing barrels section in the config
        barrelsConfig.set("barrels", null);

        // Iterate over the cache and save each barrel to the config
        for (Map.Entry<String, BarrelData> entry : barrelsCache.entrySet()) {
            String barrelLocationString = entry.getKey();
            BarrelData barrelData = entry.getValue();

            String path = "barrels." + barrelLocationString;
            barrelsConfig.set(path + ".owner", barrelData.getOwnerUUID() != null ? barrelData.getOwnerUUID().toString() : "none");
            barrelsConfig.set(path + ".sign", barrelData.getSignLocation());
            barrelsConfig.set(path + ".state", barrelData.getState());

            // Parse location from the key (world, x, y, z)
            String[] parts = barrelLocationString.split("_");
            if (parts.length == 4) {
                barrelsConfig.set(path + ".world", parts[0]);
                barrelsConfig.set(path + ".x", Integer.parseInt(parts[1]));
                barrelsConfig.set(path + ".y", Integer.parseInt(parts[2]));
                barrelsConfig.set(path + ".z", Integer.parseInt(parts[3]));
            }
        }

        // Save the config to disk
        saveBarrelsConfig();
    }




}

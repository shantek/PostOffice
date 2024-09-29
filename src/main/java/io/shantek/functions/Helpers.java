package io.shantek.functions;

import io.shantek.PostOffice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

public class Helpers {

    private final PostOffice postOffice;

    public Helpers(PostOffice postOffice) {
        this.postOffice = postOffice;
        barrelsCache = new HashMap<>();
        loadBarrelsIntoCache(); // Load data from barrels.yml into the cache at startup
    }

    public Map<String, BarrelData> barrelsCache;
    private FileConfiguration barrelsConfig = null;
    private File barrelsConfigFile = null;

    //region Post box management

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

    public boolean doesPlayerHavePostBox(UUID playerUUID) {
        FileConfiguration barrelsConfig = getBarrelsConfig();

        // Check if the 'barrels' section exists before accessing it
        if (barrelsConfig.contains("barrels")) {
            ConfigurationSection barrelsSection = barrelsConfig.getConfigurationSection("barrels");

            // Loop through all barrels to check if any are owned by this player
            assert barrelsSection != null;
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
        for (String barrelLocation : Objects.requireNonNull(barrelsConfig.getConfigurationSection("barrels")).getKeys(false)) {
            String ownerUUIDString = barrelsConfig.getString("barrels." + barrelLocation + ".owner");
            if (ownerUUIDString != null && ownerUUIDString.equals(playerUUID.toString())) {
                Block barrelBlock = getBlockFromLocationString(barrelLocation);
                if (barrelBlock != null) {
                    return barrelBlock.getWorld().getName() + " [" + barrelBlock.getX() + ", " + barrelBlock.getY() + ", " + barrelBlock.getZ() + "]";
                }
            }
        }
        return "Unknown location";
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

    public OfflinePlayer getPlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public boolean isPostBoxOwner(Block block, Player player) {
        UUID playerUUID = player.getUniqueId();
        return getOwnerUUID(block).equals(playerUUID);

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

    public Block getSignForBarrel(Block barrelBlock) {
        String barrelLocationString = getBlockLocationString(barrelBlock);

        // Retrieve the sign's location from the config
        String signLocationString = barrelsConfig.getString("barrels." + barrelLocationString + ".sign");

        if (signLocationString != null) {
            // Use the helper method to get the sign block
            return getBlockFromLocationString(signLocationString);
        }

        postOffice.getLogger().warning("Sign location not found for barrel: " + barrelLocationString);
        return null; // Sign not found or invalid format
    }

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
                // The sign matches, get the barrel block using the updated method
                return getBlockFromLocationString(barrelLocation);
            }
        }
        return null; // No barrel found for this sign
    }

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

    public Block getBlockLookingAt(Player player, double maxDistance) {

        // Perform a ray trace to get the block the player is directly looking at
        Block targetBlock = null;
        RayTraceResult result = player.rayTraceBlocks(maxDistance);

        // Check if the ray trace hit a block
        if (result != null && result.getHitBlock() != null) {
            targetBlock = result.getHitBlock();
        }

        return targetBlock;
    }

    //endregion

    //region Configuration loading/saving

    public void removeBarrelFromCache(Block barrelBlock) {
        String barrelLocationString = getBlockLocationString(barrelBlock);

        BarrelData barrelData = barrelsCache.get(barrelLocationString);
        if (barrelData != null) {
            String signLocationString = barrelData.getSignLocation();
            if (signLocationString != null) {
                Block signBlock = getBlockFromLocationString(signLocationString);
                if (signBlock != null && signBlock.getState() instanceof Sign) {
                    Sign sign = (Sign) signBlock.getState();
                    for (int i = 0; i < 4; i++) {
                        sign.setLine(i, ""); // Clear sign text
                    }
                    sign.update();
                }
            }
        }

        barrelsCache.remove(barrelLocationString);
        saveCacheToFile();
    }

    // Get the state of the post box from the barrels.yml config
    public String getStateFromConfig(String barrelLocationString) {
        FileConfiguration barrelsConfig = getBarrelsConfig();
        return barrelsConfig.getString("barrels." + barrelLocationString + ".state", "unregistered"); // Default to "registered"
    }

    public boolean isBarrelInConfig(Block block) {
        String blockLocationString = getBlockLocationString(block);
        return barrelsCache.containsKey(blockLocationString);
    }

    public void loadBarrelsIntoCache() {
        barrelsCache.clear(); // Clear the cache before reloading

        FileConfiguration barrelsConfig = getBarrelsConfig();
        if (barrelsConfig.contains("barrels")) {
            Set<String> keys = Objects.requireNonNull(barrelsConfig.getConfigurationSection("barrels")).getKeys(false);

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

                // Create a BarrelData object with corrected constructor parameters
                BarrelData barrelData = new BarrelData(ownerUUID, state, signLocation);
                barrelsCache.put(key, barrelData); // Add to cache
            }
        } else {
            postOffice.getLogger().warning("No barrels found in barrels.yml during cache load.");
        }
    }

    public void addOrUpdateBarrelInCache(Block barrelBlock, Block signBlock, UUID ownerUUID, String state) {
        String barrelLocationString = getBlockLocationString(barrelBlock);
        String signLocationString = getBlockLocationString(signBlock);

        // Create the BarrelData object with correct order of parameters
        BarrelData barrelData = new BarrelData(ownerUUID, state, signLocationString);

        // Add or update the barrel data in the cache
        barrelsCache.put(barrelLocationString, barrelData);

        // Log for debugging purposes
        postOffice.getLogger().info("Adding/Updating barrel at: " + barrelLocationString);
        postOffice.getLogger().info("Sign location for barrel: " + signLocationString);
        postOffice.getLogger().info("Post box state: " + state);

        // Optionally save the cache to disk immediately
        saveCacheToFile();
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

    public void checkForDataFolder() {
        if (!postOffice.getDataFolder().exists()) {
            if (postOffice.getDataFolder().mkdir()) {
                postOffice.getLogger().info("Data folder created successfully.");
            } else {
                postOffice.getLogger().warning("Error creating the data folder.");
            }
        }
    }

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
            Set<String> keys = Objects.requireNonNull(barrelsConfig.getConfigurationSection("barrels")).getKeys(false);
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

    public Block getBlockFromLocationString(String locationString) {
        // Split the string into parts by "_"
        String[] parts = locationString.split("_");

        // Ensure there are at least 4 parts (world and coordinates)
        if (parts.length < 4) {
            postOffice.getLogger().warning("Invalid location string format: " + locationString);
            return null;
        }

        try {
            // The last three parts are x, y, and z
            int x = Integer.parseInt(parts[parts.length - 3]);
            int y = Integer.parseInt(parts[parts.length - 2]);
            int z = Integer.parseInt(parts[parts.length - 1]);

            // The rest is the world name, so join everything before the last 3 parts
            String worldName = String.join("_", Arrays.copyOf(parts, parts.length - 3));

            // Get the world and return the block at the given coordinates
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return world.getBlockAt(x, y, z);
            } else {
                postOffice.getLogger().warning("World not found: " + worldName);
            }
        } catch (NumberFormatException e) {
            postOffice.getLogger().warning("Invalid number format in location string: " + locationString + " - Error: " + e.getMessage());
        }

        return null;
    }


    public Block getSignFromConfig(Block barrelBlock) {
        String barrelLocationString = getBlockLocationString(barrelBlock); // Convert block to location string
        String path = "barrels." + barrelLocationString + ".sign"; // Use this location string in the config

        // Look up the sign location in the config
        if (barrelsConfig.contains(path)) {
            String signLocation = barrelsConfig.getString(path);
            assert signLocation != null;
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

    //endregion

}

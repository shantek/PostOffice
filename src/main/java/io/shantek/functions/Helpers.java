package io.shantek.functions;

import io.shantek.PostOffice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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
    private Map<String, UUID> barrelsCache;

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

    // Load the contents of barrels.yml into memory (cache)
    private void loadBarrelsIntoCache() {
        FileConfiguration barrelsConfig = getBarrelsConfig();
        if (barrelsConfig.contains("barrels")) {
            for (String key : barrelsConfig.getConfigurationSection("barrels").getKeys(false)) {
                String path = "barrels." + key;
                UUID ownerUUID = UUID.fromString(barrelsConfig.getString(path + ".owner"));
                barrelsCache.put(key, ownerUUID); // Add to cache
            }
        }
    }

    // Add barrel and sign to in-memory cache and save to disk if necessary
    public void addBarrelWithSign(Block barrelBlock, Block signBlock, UUID ownerUUID) {
        if (barrelBlock.getType() != Material.BARREL || !Tag.SIGNS.isTagged(signBlock.getType())) {
            return; // Only proceed if the block is a barrel and the sign is valid
        }

        String barrelLocationString = getBlockLocationString(barrelBlock);
        String signLocationString = getBlockLocationString(signBlock);

        // Check if barrel already exists in the cache
        if (!barrelsCache.containsKey(barrelLocationString)) {
            // Barrel does not exist, add it to the cache with owner UUID
            barrelsCache.put(barrelLocationString, ownerUUID);

            // Also store the sign's location in the config
            barrelsConfig.set("barrels." + barrelLocationString + ".sign", signLocationString);

            // Save the updated cache and config to barrels.yml
            saveBarrelsToDisk();
        }
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

        barrelsConfig.set("barrels", null); // Clear existing entries in the file
        for (Map.Entry<String, UUID> entry : barrelsCache.entrySet()) {
            String path = "barrels." + entry.getKey();
            UUID ownerUUID = entry.getValue();

            barrelsConfig.set(path + ".owner", ownerUUID.toString());

            // You may want to store coordinates if needed as well
            String[] parts = entry.getKey().split("_");
            barrelsConfig.set(path + ".world", parts[0]);
            barrelsConfig.set(path + ".x", Integer.parseInt(parts[1]));
            barrelsConfig.set(path + ".y", Integer.parseInt(parts[2]));
            barrelsConfig.set(path + ".z", Integer.parseInt(parts[3]));

            // Ensure the sign's location is stored if present
            String signLocation = barrelsConfig.getString(path + ".sign");
            if (signLocation != null) {
                barrelsConfig.set(path + ".sign", signLocation);
            }
        }

        saveBarrelsConfig(); // Save changes to barrels.yml
    }

    // Get the string representing the block's location
    private String getBlockLocationString(Block block) {
        return block.getWorld().getName() + "_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
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

    public String getOwnerName(UUID ownerUuid) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);

        String ownerName = owner.getName();
        if (ownerName != null) {
            return ownerName;
        } else {
            return ownerUuid.toString();
        }

    }

    public boolean isBarrelInConfig(Block block) {
        if (block.getType() != Material.BARREL) {
            return false; // Only check if it's a barrel
        }

        // Check if it matches the custom barrel name
        BlockState blockState = block.getState();
        Barrel barrel = (Barrel) blockState;

        if (!Objects.requireNonNull(barrel.getCustomName()).equalsIgnoreCase(postOffice.customBarrelName)) {
            return false;
        }


        String blockLocationString = getBlockLocationString(block); // Get the block's location string

        // Check if the location exists in the cache or config
        return barrelsCache.containsKey(blockLocationString);
    }

    public UUID getOwnerUUID(Block block) {
        if (block.getType() != Material.BARREL) {
            return null; // Only work with barrels
        }

        String blockLocationString = getBlockLocationString(block); // Get the block's location string

        // Return the owner's UUID if the block exists in the cache
        return barrelsCache.get(blockLocationString);
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
}

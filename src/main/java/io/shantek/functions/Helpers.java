package io.shantek.functions;

import io.shantek.PostOffice;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

public class Helpers {

    private final PostOffice postOffice;

    public Helpers(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

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

    public int countNonNullItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                count += item.getAmount();
            }
        }
        return count;
    }

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

    //region Plugin Configuration

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

    //endregion
}

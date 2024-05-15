package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class Helpers {

    public PostOffice postOffice;
    public Helpers(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    public void saveMailFile() {
        try {
            if (postOffice.consoleLogs) {
                postOffice.getLogger().info("Mail list updated.");
            }
            Files.write(postOffice.mailFile.toPath(), postOffice.playersWithMail);
        } catch (IOException e) {
            postOffice.getLogger().log(Level.SEVERE, "Could not save mail file", e);
        }
    }

    public int countNonNullItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                //count++;
                count += item.getAmount();
            }
        }
        return count;
    }

    public void checkForDataFolder() {
        // Ensure the data folder exists
        if (!postOffice.getDataFolder().exists()) {
            if (postOffice.getDataFolder().mkdir()) {
                postOffice.getLogger().info("Data folder created successfully.");
            } else {
                postOffice.getLogger().warning("Failed to create data folder.");
            }
        }
    }

}

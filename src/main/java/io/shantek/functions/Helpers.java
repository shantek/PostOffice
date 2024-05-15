package io.shantek.functions;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class Helpers {

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

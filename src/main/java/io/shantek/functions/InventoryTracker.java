package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InventoryTracker implements Listener {

    private final PostOffice postOffice;
    
    // Track the inventory state when players open boxes
    // Key: Player UUID, Value: Map of barrel location -> ItemStack snapshot
    private final Map<UUID, Map<String, ItemStack[]>> inventorySnapshots = new HashMap<>();

    public InventoryTracker(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        // Only track barrel inventories
        if (inventory.getType() != InventoryType.BARREL || inventory.getLocation() == null) {
            return;
        }

        Block barrelBlock = inventory.getLocation().getBlock();
        
        // Only track if this is a registered post box
        if (!postOffice.helpers.isBarrelInConfig(barrelBlock)) {
            return;
        }

        // Create a snapshot of the current inventory
        String barrelLocation = postOffice.helpers.getBlockLocationString(barrelBlock);
        ItemStack[] snapshot = inventory.getContents().clone();
        
        // Deep clone each ItemStack
        for (int i = 0; i < snapshot.length; i++) {
            if (snapshot[i] != null) {
                snapshot[i] = snapshot[i].clone();
            }
        }

        // Store the snapshot
        inventorySnapshots.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .put(barrelLocation, snapshot);
    }

    /**
     * Get and remove the inventory snapshot for a player and barrel
     */
    public ItemStack[] getAndRemoveSnapshot(UUID playerUUID, String barrelLocation) {
        Map<String, ItemStack[]> playerSnapshots = inventorySnapshots.get(playerUUID);
        if (playerSnapshots == null) {
            return null;
        }
        return playerSnapshots.remove(barrelLocation);
    }

    /**
     * Clear all snapshots for a player
     */
    public void clearPlayerSnapshots(UUID playerUUID) {
        inventorySnapshots.remove(playerUUID);
    }
}

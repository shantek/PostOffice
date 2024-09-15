package io.shantek.listeners;

import io.shantek.PostOffice;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import java.util.Objects;

public class InventoryOpen implements Listener {

    public PostOffice postOffice;

    public InventoryOpen(PostOffice postOffice) {
        this.postOffice = postOffice;
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

                    if (barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(postOffice.customBarrelName)) {
                        postOffice.previousItemCount = postOffice.helpers.countNonNullItems(inventory.getContents());
                    }
                }
            }
        }
    }

}

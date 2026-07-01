package org.atrimilan.sidastuffsmp.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling shulker boxes and their contents.
 */
public class ShulkerUtil {

    private ShulkerUtil() {} // Prevent instantiation

    /**
     * Check if an item is a shulker box.
     *
     * @param item The ItemStack to check
     * @return true if the item is a shulker box
     */
    public static boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        String typeName = item.getType().name();
        return typeName.endsWith("_SHULKER_BOX") || typeName.equals("SHULKER_BOX");
    }

    /**
     * Check if a material is a shulker box.
     *
     * @param material The Material to check
     * @return true if the material is a shulker box
     */
    public static boolean isShulkerBox(Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }
        
        String typeName = material.name();
        return typeName.endsWith("_SHULKER_BOX") || typeName.equals("SHULKER_BOX");
    }

    /**
     * Extract all items from a shulker box.
     *
     * @param shulkerItem The shulker box item
     * @return List of ItemStacks that were inside the shulker, or empty list if not a shulker or empty
     */
    public static List<ItemStack> extractItemsFromShulker(ItemStack shulkerItem) {
        List<ItemStack> items = new ArrayList<>();
        
        if (!isShulkerBox(shulkerItem)) {
            return items;
        }
        
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            return items;
        }
        
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return items;
        }
        
        Inventory shulkerInventory = shulkerBox.getInventory();
        for (ItemStack item : shulkerInventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        
        return items;
    }

    /**
     * Get an empty shulker box of the same type as the input.
     *
     * @param shulkerItem The shulker box item
     * @return An empty shulker box ItemStack, or null if not a shulker
     */
    public static ItemStack getEmptyShulker(ItemStack shulkerItem) {
        if (!isShulkerBox(shulkerItem)) {
            return null;
        }
        
        ItemStack emptyShulker = new ItemStack(shulkerItem.getType(), 1);
        
        // Copy display name and lore if present
        if (shulkerItem.hasItemMeta()) {
            ItemStack emptyMetaShulker = emptyShulker.clone();
            ItemStack source = shulkerItem.clone();
            source.setAmount(1);
            emptyMetaShulker.setItemMeta(source.getItemMeta());
            return emptyMetaShulker;
        }
        
        return emptyShulker;
    }

    /**
     * Get an empty shulker box of a specific type.
     *
     * @param material The shulker box material
     * @return An empty shulker box ItemStack
     */
    public static ItemStack getEmptyShulker(Material material) {
        if (!isShulkerBox(material)) {
            return null;
        }
        return new ItemStack(material, 1);
    }

    /**
     * Clear all items from a shulker box.
     *
     * @param shulkerItem The shulker box item to clear
     * @return The cleared (now empty) shulker box
     */
    public static ItemStack clearShulker(ItemStack shulkerItem) {
        if (!isShulkerBox(shulkerItem)) {
            return shulkerItem;
        }
        
        ItemStack emptyShulker = getEmptyShulker(shulkerItem);
        if (emptyShulker == null) {
            return shulkerItem;
        }
        
        // Copy the amount from the original
        emptyShulker.setAmount(shulkerItem.getAmount());
        return emptyShulker;
    }

    /**
     * Process a list of items and extract items from any shulkers found.
     * Returns two lists: one with extracted items (from shulkers), and one with non-shulker items.
     * Also returns a list of empty shulkers to be returned to the player.
     *
     * @param items The list of items to process
     * @return A ShulkerProcessResult containing extracted items, non-shulker items, and empty shulkers
     */
    public static ShulkerProcessResult processItemsForShulkers(List<ItemStack> items) {
        List<ItemStack> extractedItems = new ArrayList<>();
        List<ItemStack> nonShulkerItems = new ArrayList<>();
        List<ItemStack> emptyShulkers = new ArrayList<>();
        
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            if (isShulkerBox(item)) {
                // Extract items from shulker
                List<ItemStack> shulkerContents = extractItemsFromShulker(item);
                extractedItems.addAll(shulkerContents);
                
                // Add empty shulkers (same amount as original)
                ItemStack emptyShulker = getEmptyShulker(item);
                if (emptyShulker != null) {
                    emptyShulker.setAmount(item.getAmount());
                    emptyShulkers.add(emptyShulker);
                }
            } else {
                nonShulkerItems.add(item);
            }
        }
        
        return new ShulkerProcessResult(extractedItems, nonShulkerItems, emptyShulkers);
    }

    /**
     * Result class for shulker processing.
     */
    public static class ShulkerProcessResult {
        private final List<ItemStack> extractedItems;
        private final List<ItemStack> nonShulkerItems;
        private final List<ItemStack> emptyShulkers;
        
        public ShulkerProcessResult(List<ItemStack> extractedItems, List<ItemStack> nonShulkerItems, List<ItemStack> emptyShulkers) {
            this.extractedItems = extractedItems;
            this.nonShulkerItems = nonShulkerItems;
            this.emptyShulkers = emptyShulkers;
        }
        
        public List<ItemStack> getExtractedItems() {
            return extractedItems;
        }
        
        public List<ItemStack> getNonShulkerItems() {
            return nonShulkerItems;
        }
        
        public List<ItemStack> getEmptyShulkers() {
            return emptyShulkers;
        }
    }
}

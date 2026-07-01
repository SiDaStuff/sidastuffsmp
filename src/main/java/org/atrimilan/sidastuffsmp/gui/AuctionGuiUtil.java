package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.auction.AuctionSortMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AuctionGuiUtil {

    public static final int BROWSER_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int CONTROL_BAR_START = 45;

    public static final int SLOT_PREV_PAGE = 45;
    public static final int SLOT_SORT = 46;
    public static final int SLOT_REFRESH = 47;
    public static final int SLOT_SEARCH = 48;
    public static final int SLOT_MY_LISTINGS = 49;
    public static final int SLOT_NEXT_PAGE = 51;
    public static final int SLOT_CLOSE = 52;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);

    private AuctionGuiUtil() {}

    public static ItemStack addEnchantmentGlow(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (meta.hasEnchants()) return item;
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createControlItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack prevPageItem(boolean hasPrev) {
        if (!hasPrev) return null;
        return createControlItem(Material.ARROW, "Previous Page", List.of(
                Component.text("Click to go back", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack nextPageItem(boolean hasNext) {
        if (!hasNext) return null;
        return createControlItem(Material.ARROW, "Next Page", List.of(
                Component.text("Click to go forward", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack sortItem(AuctionSortMode current) {
        return createControlItem(Material.HOPPER, "Sort: " + current.displayName(), List.of(
                Component.text("Click to cycle sort mode", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack refreshItem() {
        return createControlItem(Material.ANVIL, "Refresh", List.of(
                Component.text("Click to refresh listings", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack searchItem() {
        return createControlItem(Material.NAME_TAG, "Search", List.of(
                Component.text("Click to search by item name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack myListingsItem() {
        return createControlItem(Material.CHEST, "My Listings", List.of(
                Component.text("View your auction listings", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack closeItem() {
        return createControlItem(Material.BARRIER, "Close", null);
    }

    public static ItemStack fillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillEmptySlots(Inventory inv, ItemStack filler) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, filler);
            }
        }
    }

    public static ItemStack confirmItem(String label, Material material) {
        return createControlItem(material, label, List.of(
                Component.text("Click to confirm", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack cancelButtonItem() {
        return createControlItem(Material.RED_STAINED_GLASS_PANE, "Cancel", List.of(
                Component.text("Click to go back", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack createBrowserListingItem(AuctionListing listing) {
        ItemStack displayItem = AuctionManager.deserializeItem(listing.itemBase64());
        if (displayItem == null) {
            displayItem = new ItemStack(Material.BARRIER);
        }

        ItemStack clone = displayItem.clone();
        if (clone.hasItemMeta() && clone.getItemMeta() != null && clone.getItemMeta().hasEnchants()) {
            addEnchantmentGlow(clone);
        }
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
            }
            lore.add(Component.empty());
            lore.add(Component.text("Seller: " + listing.sellerName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Price: " + AuctionManager.formatPrice(listing.price()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Expires: " + formatExpiry(listing.expiresAt()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to purchase", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public static ItemStack createMyActiveItem(AuctionListing listing) {
        ItemStack displayItem = AuctionManager.deserializeItem(listing.itemBase64());
        if (displayItem == null) {
            displayItem = new ItemStack(Material.BARRIER);
        }
        ItemStack clone = displayItem.clone();
        if (clone.hasItemMeta() && clone.getItemMeta() != null && clone.getItemMeta().hasEnchants()) {
            addEnchantmentGlow(clone);
        }
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
            }
            lore.add(Component.empty());
            lore.add(Component.text("Price: " + AuctionManager.formatPrice(listing.price()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Expires: " + formatExpiry(listing.expiresAt()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to CANCEL", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public static ItemStack createMySoldItem(AuctionListing listing) {
        ItemStack displayItem = AuctionManager.deserializeItem(listing.itemBase64());
        if (displayItem == null) {
            displayItem = new ItemStack(Material.BARRIER);
        }
        ItemStack clone = displayItem.clone();
        if (clone.hasItemMeta() && clone.getItemMeta() != null && clone.getItemMeta().hasEnchants()) {
            addEnchantmentGlow(clone);
        }
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
            }
            lore.add(Component.empty());
            lore.add(Component.text("Sold to: " + (listing.buyerName() != null ? listing.buyerName() : "Unknown"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Price: " + AuctionManager.formatPrice(listing.price()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to COLLECT money", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public static ItemStack createMyExpiredItem(AuctionListing listing) {
        ItemStack displayItem = AuctionManager.deserializeItem(listing.itemBase64());
        if (displayItem == null) {
            displayItem = new ItemStack(Material.BARRIER);
        }
        ItemStack clone = displayItem.clone();
        if (clone.hasItemMeta() && clone.getItemMeta() != null && clone.getItemMeta().hasEnchants()) {
            addEnchantmentGlow(clone);
        }
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
            }
            lore.add(Component.empty());
            lore.add(Component.text("Price: " + AuctionManager.formatPrice(listing.price()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to COLLECT item", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public static ItemStack sectionSeparator(String label, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String formatExpiry(long expiresAtMillis) {
        long diff = expiresAtMillis - System.currentTimeMillis();
        if (diff <= 0) return "Expired";
        long hours = diff / (60 * 60 * 1000);
        long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);
        if (hours > 24) {
            long days = hours / 24;
            return days + "d " + (hours % 24) + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static int getTotalPages(int totalItems) {
        return (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
    }
}

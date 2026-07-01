package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry;
import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.order.OrderSortMode;
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

public final class OrderGuiUtil {

    public static final int BROWSER_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int CONTROL_BAR_START = 45;

    public static final int SLOT_PREV_PAGE = 45;
    public static final int SLOT_SORT = 46;
    public static final int SLOT_REFRESH = 47;
    public static final int SLOT_SEARCH = 48;
    public static final int SLOT_NEW_ORDER = 49;
    public static final int SLOT_MY_ORDERS = 50;
    public static final int SLOT_NEXT_PAGE = 51;
    public static final int SLOT_CLOSE = 52;

    public static final int DELIVER_INPUT_START = 0;
    public static final int DELIVER_INPUT_SIZE = 45;
    public static final int DELIVER_SLOT_CONFIRM = 49;
    public static final int DELIVER_SLOT_SET_AMOUNT = 50;
    public static final int DELIVER_SLOT_INFO = 51;

    public static final int CONFIRM_SLOT_CONFIRM = 11;
    public static final int CONFIRM_SLOT_ITEM = 13;
    public static final int CONFIRM_SLOT_CANCEL = 15;
    public static final int CONFIRM_SIZE = 27;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);

    private OrderGuiUtil() {}

    public static Material resolveMaterialFromName(String materialName) {
        if (materialName == null) return Material.BARRIER;
        Material mat = Material.matchMaterial(materialName);
        if (mat != null) return mat;
        org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem mcItem =
                org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.findItemByName(materialName);
        if (mcItem != null) return mcItem.material();
        return Material.BARRIER;
    }

    public static ItemStack createDisplayItemFromMaterialName(String materialName) {
        if (materialName == null) return new ItemStack(Material.BARRIER);
        org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem mcItem =
                org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.findItemByName(materialName);
        if (mcItem != null && mcItem.hasPotionEffect()) {
            ItemStack item = org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.createDisplayItemStack(mcItem);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                item.setItemMeta(meta);
            }
            return item;
        }
        Material mat = Material.matchMaterial(materialName);
        if (mat != null) return new ItemStack(mat);
        if (mcItem != null) return new ItemStack(mcItem.material());
        return new ItemStack(Material.BARRIER);
    }

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

    public static ItemStack refreshItem() {
        return createControlItem(Material.ANVIL, "Refresh", List.of(
                Component.text("Click to refresh orders", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack myOrdersItem() {
        return createControlItem(Material.CHEST, "My Orders", List.of(
                Component.text("View your buy orders", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack newOrderItem() {
        return createControlItem(Material.LIME_STAINED_GLASS_PANE, "New Order", List.of(
                Component.text("Create a new buy order", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack sortItem(OrderSortMode current) {
        return createControlItem(Material.HOPPER, "Sort: " + current.displayName(), List.of(
                Component.text("Click to cycle sort mode", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack searchItem() {
        return createControlItem(Material.NAME_TAG, "Search", List.of(
                Component.text("Click to search by item name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack searchItemWithTerm(String term) {
        return createControlItem(Material.NAME_TAG, "Search: " + term, List.of(
                Component.text("Click to search by item name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
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

    public static ItemStack createBrowserOrderItem(OrderListing order) {
        ItemStack displayItem = createDisplayItemFromMaterialName(order.materialName());
        if (order.hasNbtRequirement()) {
            ItemStack template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
            if (template != null && template.hasItemMeta()) {
                ItemMeta templateMeta = template.getItemMeta();
                if (templateMeta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta && potionMeta.hasCustomEffects()) {
                    displayItem = applyNbtDisplay(displayItem, order);
                    addEnchantmentGlow(displayItem);
                }
            }
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            if (order.hasNbtRequirement() && !MinecraftDataRegistry.isPotionItem(displayItem.getType())) {
                addEnchantmentGlow(displayItem);
                meta = displayItem.getItemMeta();
            }
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Buyer: " + order.buyerName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Remaining: " + order.getRemainingQuantity() + " / " + order.quantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Total Value: " + OrderManager.formatPrice(order.getRemainingQuantity() * order.pricePerUnit()), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Expires: " + formatExpiry(order.expiresAt()), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (order.hasNbtRequirement()) {
            ItemStack template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
            if (template != null && template.hasItemMeta()) {
                ItemMeta templateMeta = template.getItemMeta();
                lore.add(Component.empty());
                lore.add(Component.text("Requires:", NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false));
                if (templateMeta.hasEnchants()) {
                    for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : templateMeta.getEnchants().entrySet()) {
                        String enchName = formatEnchName(entry.getKey().getKey().getKey());
                        String level = toRoman(entry.getValue());
                        lore.add(Component.text("  " + enchName + " " + level, NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                if (templateMeta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta && potionMeta.hasCustomEffects()) {
                    for (org.bukkit.potion.PotionEffect effect : potionMeta.getCustomEffects()) {
                        String effectName = formatEnchName(effect.getType().getKey().getKey());
                        int level = effect.getAmplifier() + 1;
                        int durationSeconds = effect.getDuration() / 20;
                        String duration = formatDuration(durationSeconds);
                        lore.add(Component.text("  " + effectName + " " + toRoman(level) + " (" + duration + ")", NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
            }
        }
        lore.add(Component.empty());
            lore.add(Component.text("Click to fulfill", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public static ItemStack createMyActiveOrderItem(OrderListing order) {
        ItemStack displayItem = createDisplayItemFromMaterialName(order.materialName());
        if (order.hasNbtRequirement()) {
            displayItem = applyNbtDisplay(displayItem, order);
            addEnchantmentGlow(displayItem);
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Filled: " + order.filledQuantity() + " / " + order.quantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Escrowed: " + OrderManager.formatPrice(order.getRemainingEscrow()), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Expires: " + formatExpiry(order.expiresAt()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            boolean hasStash = OrderManager.hasUncollectedStashForOrder(order.id());
            if (hasStash) {
                lore.add(Component.empty());
                lore.add(Component.text("Delivered items in stash!", NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click for details", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public static ItemStack createMyCompletedOrderItem(OrderListing order) {
        ItemStack displayItem = createDisplayItemFromMaterialName(order.materialName());
        if (order.hasNbtRequirement()) {
            displayItem = applyNbtDisplay(displayItem, order);
            addEnchantmentGlow(displayItem);
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Filled: " + order.filledQuantity() + " / " + order.quantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click for details", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public static ItemStack createMyExpiredOrderItem(OrderListing order) {
        ItemStack displayItem = createDisplayItemFromMaterialName(order.materialName());
        if (order.hasNbtRequirement()) {
            displayItem = applyNbtDisplay(displayItem, order);
            addEnchantmentGlow(displayItem);
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Filled: " + order.filledQuantity() + " / " + order.quantity(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Refunded", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click for details", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public static ItemStack createMyCancelledOrderItem(OrderListing order) {
        ItemStack displayItem = createDisplayItemFromMaterialName(order.materialName());
        if (order.hasNbtRequirement()) {
            displayItem = applyNbtDisplay(displayItem, order);
            addEnchantmentGlow(displayItem);
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Filled: " + order.filledQuantity() + " / " + order.quantity(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Refunded", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click for details", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public static ItemStack createMaterialPickerItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Click to create order for this item", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
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

    public static String formatDuration(int seconds) {
        if (seconds <= 0) return "Instant";
        long minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        return seconds + "s";
    }

    public static int getTotalPages(int totalItems) {
        return (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
    }

    static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }

    static String formatEnchName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static ItemStack applyNbtDisplay(ItemStack displayItem, OrderListing order) {
        if (displayItem == null || !order.hasNbtRequirement()) return displayItem;
        ItemStack template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
        if (template == null || !template.hasItemMeta()) return displayItem;
        ItemMeta templateMeta = template.getItemMeta();
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta == null) return displayItem;

        if (templateMeta.hasEnchants()) {
            for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : templateMeta.getEnchants().entrySet()) {
                displayMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        if (templateMeta instanceof org.bukkit.inventory.meta.PotionMeta templatePotion
                && displayMeta instanceof org.bukkit.inventory.meta.PotionMeta displayPotion) {
            if (templatePotion.hasCustomEffects()) {
                for (org.bukkit.potion.PotionEffect effect : templatePotion.getCustomEffects()) {
                    displayPotion.addCustomEffect(effect, true);
                }
            }
            if (templatePotion.hasColor()) {
                displayPotion.setColor(templatePotion.getColor());
            }
            if (templatePotion.hasCustomEffects() && templatePotion.getCustomEffects().size() > 0) {
                String effectName = formatEnchName(templatePotion.getCustomEffects().get(0).getType().getKey().getKey());
                displayPotion.displayName(Component.text(effectName + " Potion", NamedTextColor.WHITE)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        displayItem.setItemMeta(displayMeta);
        return displayItem;
    }
}

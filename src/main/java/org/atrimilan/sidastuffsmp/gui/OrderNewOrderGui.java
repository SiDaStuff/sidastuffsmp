package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.economy.EconomyShopGuiPriceLoader;
import org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderNewOrderGui {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_SEARCH = 47;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;

    private OrderNewOrderGui() {}

    public static Inventory open(Player player, int page, String searchTerm) {
        OrderGuiHolder.BrowserState state = OrderGuiHolder.getBrowserState(player.getUniqueId());
        state.setNewItemPage(page);
        state.setNewItemSearch(searchTerm);

        List<MinecraftDataRegistry.MinecraftItem> allItems = MinecraftDataRegistry.getAllItems();
        List<MinecraftDataRegistry.MinecraftItem> filtered = new ArrayList<>();

        for (MinecraftDataRegistry.MinecraftItem item : allItems) {
            if (searchTerm != null && !searchTerm.isBlank()) {
                String lower = searchTerm.toLowerCase(Locale.ROOT);
                if (!item.name().toLowerCase(Locale.ROOT).contains(lower)
                        && !item.displayName().toLowerCase(Locale.ROOT).contains(lower)
                        && !item.name().replace('_', ' ').toLowerCase(Locale.ROOT).contains(lower)) {
                    continue;
                }
            }
            filtered.add(item);
        }

        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(page, 0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalItems);

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.NEW_ORDER_ITEM_PICKER);
        holder.setViewerUuid(player.getUniqueId());
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Select Item for Order"));

        for (int i = start; i < end; i++) {
            MinecraftDataRegistry.MinecraftItem mcItem = filtered.get(i);
            inv.setItem(i - start, createItemPickerItem(mcItem));
        }

        

        inv.setItem(SLOT_PREV, OrderGuiUtil.prevPageItem(page > 0));
        inv.setItem(SLOT_SEARCH, OrderGuiUtil.createControlItem(Material.NAME_TAG,
                "Search" + (searchTerm != null ? ": " + searchTerm : ""), List.of(
                        Component.text("Click to search by item name", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false))));
        inv.setItem(SLOT_BACK, OrderGuiUtil.createControlItem(Material.ARROW,
                "Back to Create Order", List.of(
                        Component.text("Return without selecting", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false))));
        inv.setItem(SLOT_NEXT, OrderGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, OrderGuiUtil.closeItem());

        ItemStack newItemFiller = OrderGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, newItemFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    public static Inventory open(Player player, int page) {
        return open(player, page, null);
    }

    private static ItemStack createItemPickerItem(MinecraftDataRegistry.MinecraftItem mcItem) {
        ItemStack item = MinecraftDataRegistry.createDisplayItemStack(mcItem);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(mcItem.displayName(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            
            // Show shop price if available
            double shopPrice = EconomyShopGuiPriceLoader.getSellPrice(mcItem.material());
            if (shopPrice > 0) {
                lore.add(Component.text("Shop Price: " + OrderManager.formatPrice(shopPrice), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            
            // Show potion duration for potion items
            String duration = MinecraftDataRegistry.getPotionDuration(mcItem);
            if (duration != null) {
                lore.add(Component.text("Duration: " + duration, NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }
            
            lore.add(Component.text("Stack Size: " + mcItem.stackSize(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (!mcItem.enchantCategories().isEmpty()) {
                lore.add(Component.text("Enchantable", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click to create order", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            if (mcItem.hasPotionEffect()) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderStashGui {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_COLLECT = 47;
    private static final int SLOT_DROP_ALL = 48;
    private static final int SLOT_SELL_ALL = 49;
    private static final int SLOT_BACK = 50;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;
    private static final DateTimeFormatter STASH_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm", java.util.Locale.US)
            .withZone(ZoneId.systemDefault());

    private OrderStashGui() {}

    public static Inventory open(Player player, int orderId, int page) {
        OrderListing order = OrderManager.getOrderById(orderId);
        if (order == null) return null;

        List<OrderManager.StashEntry> allStash = OrderManager.getOrderStash(orderId);
        List<OrderManager.StashEntry> uncollected = new ArrayList<>();
        for (OrderManager.StashEntry entry : allStash) {
            if (!entry.collected()) {
                uncollected.add(entry);
            }
        }

        List<StashStack> stacks = buildStacks(uncollected, order);

        int totalEntries = stacks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(page, 0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalEntries);

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.ORDER_STASH);
        holder.setViewerUuid(player.getUniqueId());
        holder.setOrderId(orderId);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Order #" + orderId + " Stash (p" + (page + 1) + "/" + totalPages + ")"));

        for (int i = start; i < end; i++) {
            StashStack stack = stacks.get(i);
            int slot = i - start;
            inv.setItem(slot, stack.displayItem);
        }

        

        inv.setItem(SLOT_PREV, OrderGuiUtil.prevPageItem(page > 0));

        int totalStashItems = 0;
        for (OrderManager.StashEntry e : uncollected) {
            totalStashItems += e.quantity();
        }

        if (totalStashItems > 0) {
            ItemStack collectItem = OrderGuiUtil.createControlItem(Material.HOPPER,
                    "Collect All (" + totalStashItems + ")",
                    List.of(Component.text("Collect all stash items to inventory", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(SLOT_COLLECT, collectItem);

            ItemStack dropItem = OrderGuiUtil.createControlItem(Material.DROPPER,
                    "Drop All (" + totalStashItems + ")",
                    List.of(Component.text("Drop all stash items at your feet", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(SLOT_DROP_ALL, dropItem);

            ItemStack sellItem = OrderGuiUtil.createControlItem(Material.EMERALD,
                    "Sell All (" + totalStashItems + ")",
                    List.of(Component.text("Sell all stash items now", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(SLOT_SELL_ALL, sellItem);
        } else {
            ItemStack emptyItem = OrderGuiUtil.createControlItem(Material.HOPPER,
                    "Stash Empty",
                    List.of(Component.text("No items to collect", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(SLOT_COLLECT, emptyItem);
            inv.setItem(SLOT_DROP_ALL, null);
            inv.setItem(SLOT_SELL_ALL, null);
        }

        ItemStack backItem = OrderGuiUtil.createControlItem(Material.ARROW, "Back to Order",
                List.of(Component.text("Return to order details", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        inv.setItem(SLOT_BACK, backItem);
        inv.setItem(SLOT_NEXT, OrderGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, OrderGuiUtil.closeItem());

        ItemStack stashFiller = OrderGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, stashFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    private static List<StashStack> buildStacks(List<OrderManager.StashEntry> entries, OrderListing order) {
        Material material = OrderGuiUtil.resolveMaterialFromName(order.materialName());

        int totalQuantity = 0;
        for (OrderManager.StashEntry entry : entries) {
            totalQuantity += entry.quantity();
        }

        List<StashStack> stacks = new ArrayList<>();
        int maxStackSize = material.getMaxStackSize();
        int remaining = totalQuantity;
        int entryIdx = 0;
        int consumedFromEntry = 0;

        while (remaining > 0 && entryIdx < entries.size()) {
            OrderManager.StashEntry currentEntry = entries.get(entryIdx);
            int availableInEntry = currentEntry.quantity() - consumedFromEntry;

            if (availableInEntry <= 0) {
                entryIdx++;
                consumedFromEntry = 0;
                continue;
            }

            int stackSize = Math.min(remaining, maxStackSize);
            int takeFromCurrent = Math.min(stackSize, availableInEntry);

            ItemStack displayItem = new ItemStack(material, stackSize);
            if (order.hasNbtRequirement()) {
                displayItem = OrderGuiUtil.applyNbtDisplay(displayItem, order);
                OrderGuiUtil.addEnchantmentGlow(displayItem);
            }

            long timestamp = currentEntry.createdAt();

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("Amount: " + stackSize, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Delivered: " + formatTimestamp(timestamp), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Value: " + OrderManager.formatPrice(stackSize * order.pricePerUnit()), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }

            stacks.add(new StashStack(displayItem));

            remaining -= stackSize;
            consumedFromEntry += takeFromCurrent;
        }

        return stacks;
    }

    private record StashStack(ItemStack displayItem) {}

    private static String formatTimestamp(long millis) {
        return STASH_DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}

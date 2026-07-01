package org.atrimilan.sidastuffsmp.gui;

import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.order.OrderSortMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class OrderBrowserGui {

    private static final java.util.Map<java.util.UUID, Long> REFRESH_COOLDOWNS = new java.util.HashMap<>();
    private static final long REFRESH_COOLDOWN_MS = 2000;

    private OrderBrowserGui() {}

    public static Inventory open(Player player) {
        return open(player, 0, OrderSortMode.NEWEST, null);
    }

    public static Inventory open(Player player, int page, OrderSortMode sortMode, String searchTerm) {
        OrderGuiHolder.BrowserState state = OrderGuiHolder.getBrowserState(player.getUniqueId());
        state.setPage(page);
        state.setSortMode(sortMode);
        state.setSearchTerm(searchTerm);

        int offset = page * OrderGuiUtil.ITEMS_PER_PAGE;
        int totalItems = OrderManager.getBrowserOrderCount(searchTerm);
        int totalPages = OrderGuiUtil.getTotalPages(totalItems);

        List<OrderListing> orders = OrderManager.getBrowserOrders(sortMode, searchTerm, offset, OrderGuiUtil.ITEMS_PER_PAGE);

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.BROWSER);
        holder.setViewerUuid(player.getUniqueId());
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, OrderGuiUtil.BROWSER_SIZE,
                net.kyori.adventure.text.Component.text("Orders Market"));

        for (int i = 0; i < orders.size() && i < OrderGuiUtil.ITEMS_PER_PAGE; i++) {
            inv.setItem(i, OrderGuiUtil.createBrowserOrderItem(orders.get(i)));
        }

        

        inv.setItem(OrderGuiUtil.SLOT_PREV_PAGE, OrderGuiUtil.prevPageItem(page > 0));
        inv.setItem(OrderGuiUtil.SLOT_SORT, OrderGuiUtil.sortItem(sortMode));
        inv.setItem(OrderGuiUtil.SLOT_REFRESH, OrderGuiUtil.refreshItem());
        inv.setItem(OrderGuiUtil.SLOT_SEARCH, searchTerm != null && !searchTerm.isBlank()
                ? OrderGuiUtil.searchItemWithTerm(searchTerm)
                : OrderGuiUtil.searchItem());
        inv.setItem(OrderGuiUtil.SLOT_NEW_ORDER, OrderGuiUtil.newOrderItem());
        inv.setItem(OrderGuiUtil.SLOT_MY_ORDERS, OrderGuiUtil.myOrdersItem());
        inv.setItem(OrderGuiUtil.SLOT_NEXT_PAGE, OrderGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(OrderGuiUtil.SLOT_CLOSE, OrderGuiUtil.closeItem());

        ItemStack browserFiller = OrderGuiUtil.fillerItem();
        for (int i = OrderGuiUtil.CONTROL_BAR_START; i < OrderGuiUtil.BROWSER_SIZE; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, browserFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    public static void refresh(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastRefresh = REFRESH_COOLDOWNS.get(uuid);
        if (lastRefresh != null && (now - lastRefresh) < REFRESH_COOLDOWN_MS) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("Please wait before refreshing.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        REFRESH_COOLDOWNS.put(uuid, now);
        OrderGuiHolder.BrowserState state = OrderGuiHolder.getBrowserState(uuid);
        open(player, state.getPage(), state.getSortMode(), state.getSearchTerm());
    }

    public static Long getRefreshCooldown(java.util.UUID uuid) {
        return REFRESH_COOLDOWNS.get(uuid);
    }

    public static void setRefreshCooldown(java.util.UUID uuid, long time) {
        REFRESH_COOLDOWNS.put(uuid, time);
    }
}

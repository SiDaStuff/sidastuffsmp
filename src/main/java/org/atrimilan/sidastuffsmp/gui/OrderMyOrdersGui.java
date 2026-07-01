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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderMyOrdersGui {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;

    private OrderMyOrdersGui() {}

    public static Inventory open(Player player, int page) {
        UUID uuid = player.getUniqueId();
        List<OrderListing> allOrders = OrderManager.getMyOrders(uuid);

        List<OrderListing> activeOrders = new ArrayList<>();
        List<OrderListing> completedOrders = new ArrayList<>();
        List<OrderListing> expiredOrders = new ArrayList<>();
        List<OrderListing> cancelledOrders = new ArrayList<>();

        for (OrderListing o : allOrders) {
            switch (o.status()) {
                case "ACTIVE" -> activeOrders.add(o);
                case "COMPLETED" -> completedOrders.add(o);
                case "EXPIRED" -> expiredOrders.add(o);
                case "CANCELLED" -> cancelledOrders.add(o);
            }
        }

        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry(Entry.Type.SEPARATOR, null, "Active (" + activeOrders.size() + ")", Material.YELLOW_STAINED_GLASS_PANE));
        for (OrderListing o : activeOrders) {
            entries.add(new Entry(Entry.Type.ACTIVE, o, null, null));
        }

        entries.add(new Entry(Entry.Type.SEPARATOR, null, "Completed (" + completedOrders.size() + ")", Material.GREEN_STAINED_GLASS_PANE));
        for (OrderListing o : completedOrders) {
            entries.add(new Entry(Entry.Type.COMPLETED, o, null, null));
        }

        entries.add(new Entry(Entry.Type.SEPARATOR, null, "Expired (" + expiredOrders.size() + ")", Material.GRAY_STAINED_GLASS_PANE));
        for (OrderListing o : expiredOrders) {
            entries.add(new Entry(Entry.Type.EXPIRED, o, null, null));
        }

        entries.add(new Entry(Entry.Type.SEPARATOR, null, "Cancelled (" + cancelledOrders.size() + ")", Material.RED_STAINED_GLASS_PANE));
        for (OrderListing o : cancelledOrders) {
            entries.add(new Entry(Entry.Type.CANCELLED, o, null, null));
        }

        int totalEntries = entries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(page, 0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalEntries);

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.MY_ORDERS);
        holder.setViewerUuid(uuid);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("My Orders"));

        for (int i = start; i < end; i++) {
            Entry entry = entries.get(i);
            int slot = i - start;
            switch (entry.type) {
                case SEPARATOR -> inv.setItem(slot, OrderGuiUtil.sectionSeparator(entry.label, entry.material));
                case ACTIVE -> inv.setItem(slot, OrderGuiUtil.createMyActiveOrderItem(entry.order));
                case COMPLETED -> inv.setItem(slot, OrderGuiUtil.createMyCompletedOrderItem(entry.order));
                case EXPIRED -> inv.setItem(slot, OrderGuiUtil.createMyExpiredOrderItem(entry.order));
                case CANCELLED -> inv.setItem(slot, OrderGuiUtil.createMyCancelledOrderItem(entry.order));
            }
        }

        inv.setItem(SLOT_PREV, OrderGuiUtil.prevPageItem(page > 0));
        inv.setItem(SLOT_BACK, OrderGuiUtil.createControlItem(Material.ARROW, "Back to Market", null));
        inv.setItem(SLOT_NEXT, OrderGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, OrderGuiUtil.closeItem());

        ItemStack myOrdersFiller = OrderGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, myOrdersFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    private record Entry(Type type, OrderListing order, String label, Material material) {
        enum Type { SEPARATOR, ACTIVE, COMPLETED, EXPIRED, CANCELLED }
    }
}

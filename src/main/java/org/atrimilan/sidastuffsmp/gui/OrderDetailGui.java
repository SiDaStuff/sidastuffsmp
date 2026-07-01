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

import java.util.ArrayList;
import java.util.List;

public class OrderDetailGui {

    private static final int SLOT_CANCEL = 11;
    private static final int SLOT_DISPLAY = 13;
    private static final int SLOT_STASH = 15;
    private static final int SLOT_REFRESH = 19;
    private static final int SLOT_BACK = 22;

    private OrderDetailGui() {}

    public static Inventory open(Player player, int orderId) {
        OrderListing order = OrderManager.getOrderById(orderId);
        if (order == null) return null;

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.ORDER_DETAIL);
        holder.setViewerUuid(player.getUniqueId());
        holder.setOrderId(orderId);

        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Order #" + orderId));

        Material material = OrderGuiUtil.resolveMaterialFromName(order.materialName());

        ItemStack displayItem = OrderGuiUtil.createDisplayItemFromMaterialName(order.materialName());
        if (order.hasNbtRequirement()) {
            displayItem = OrderGuiUtil.applyNbtDisplay(displayItem, order);
            OrderGuiUtil.addEnchantmentGlow(displayItem);
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Buyer: " + order.buyerName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Price Each: " + OrderManager.formatPrice(order.pricePerUnit()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Quantity: " + order.quantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Filled: " + order.filledQuantity() + " / " + order.quantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Remaining: " + order.getRemainingQuantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Total Escrow: " + OrderManager.formatPrice(order.getTotalEscrow()), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            if (order.status().equals("ACTIVE")) {
                lore.add(Component.text("Remaining Escrow: " + OrderManager.formatPrice(order.getRemainingEscrow()), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Status: " + order.status(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Created: " + formatTimestamp(order.createdAt()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Expires: " + OrderGuiUtil.formatExpiry(order.expiresAt()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (order.completedAt() != null) {
                lore.add(Component.text("Completed: " + formatTimestamp(order.completedAt()), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        inv.setItem(SLOT_DISPLAY, displayItem);

        if (order.status().equals("ACTIVE") && order.buyerUuid().equals(player.getUniqueId())) {
            boolean hasStash = OrderManager.hasUncollectedStashForOrder(orderId);
            if (hasStash) {
                inv.setItem(SLOT_CANCEL, OrderGuiUtil.createControlItem(Material.RED_STAINED_GLASS_PANE,
                        "Cannot Cancel", List.of(
                                Component.text("Collect stash items first!", NamedTextColor.RED)
                                        .decoration(TextDecoration.BOLD, true)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.text("Use Order Stash to collect", NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false)
                        )));
            } else {
                inv.setItem(SLOT_CANCEL, OrderGuiUtil.createControlItem(Material.RED_STAINED_GLASS_PANE,
                        "Cancel Order", List.of(
                                Component.text("Cancel and refund " + OrderManager.formatPrice(order.getRemainingEscrow()), NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false)
                        )));
            }
        } else {
            inv.setItem(SLOT_CANCEL, null);
        }

        int stashItemCount = 0;
        List<OrderManager.StashEntry> stashEntries = OrderManager.getOrderStash(orderId);
        for (OrderManager.StashEntry entry : stashEntries) {
            if (!entry.collected()) {
                stashItemCount += entry.quantity();
            }
        }

        if (stashItemCount > 0) {
            inv.setItem(SLOT_STASH, OrderGuiUtil.createControlItem(Material.HOPPER,
                    "Order Stash (" + stashItemCount + ")", List.of(
                            Component.text("Click to view and collect items", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        } else {
            inv.setItem(SLOT_STASH, OrderGuiUtil.createControlItem(Material.HOPPER,
                    "Order Stash", List.of(
                            Component.text("No items in stash", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        }

        inv.setItem(SLOT_REFRESH, OrderGuiUtil.createControlItem(Material.ANVIL, "Refresh", List.of(
                Component.text("Click to refresh order details", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        )));

        inv.setItem(SLOT_BACK, OrderGuiUtil.createControlItem(Material.ARROW, "Back", List.of(
                Component.text("Back to My Orders", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        )        ));

        OrderGuiUtil.fillEmptySlots(inv, OrderGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    private static String formatTimestamp(long millis) {
        return new java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(new java.util.Date(millis));
    }
}

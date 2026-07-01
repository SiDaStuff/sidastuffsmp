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

public class OrderConfirmCancelGui {

    private OrderConfirmCancelGui() {}

    public static Inventory open(Player player, int orderId) {
        OrderListing order = OrderManager.getOrderById(orderId);
        if (order == null) return null;

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.CONFIRM_CANCEL);
        holder.setViewerUuid(player.getUniqueId());
        holder.setOrderId(orderId);

        Inventory inv = Bukkit.createInventory(holder, OrderGuiUtil.CONFIRM_SIZE, Component.text("Confirm Cancel"));

        ItemStack displayItem = OrderGuiUtil.createDisplayItemFromMaterialName(order.materialName());
        var displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Order #" + orderId, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Refund: " + OrderManager.formatPrice(order.getRemainingEscrow()), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Filled: " + order.filledQuantity() + " / " + order.quantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            displayMeta.lore(lore);
            displayItem.setItemMeta(displayMeta);
        }
        inv.setItem(OrderGuiUtil.CONFIRM_SLOT_ITEM, displayItem);

        inv.setItem(OrderGuiUtil.CONFIRM_SLOT_CONFIRM,
                OrderGuiUtil.createControlItem(Material.ORANGE_STAINED_GLASS_PANE, "Confirm Cancel", List.of(
                        Component.text("Refund: " + OrderManager.formatPrice(order.getRemainingEscrow()), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Click to confirm", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                )));

        inv.setItem(OrderGuiUtil.CONFIRM_SLOT_CANCEL, OrderGuiUtil.cancelButtonItem());

        OrderGuiUtil.fillEmptySlots(inv, OrderGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }
}

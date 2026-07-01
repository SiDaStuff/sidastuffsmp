package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.order.OrderConfig;
import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OrderConfirmDeliveryGui {

    private OrderConfirmDeliveryGui() {}

    public static Inventory open(Player player, int orderId, int itemCount) {
        OrderListing order = OrderManager.getOrderById(orderId);
        if (order == null) return null;

        Material requiredMaterial = OrderGuiUtil.resolveMaterialFromName(order.materialName());

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.CONFIRM_DELIVERY);
        holder.setViewerUuid(player.getUniqueId());
        holder.setOrderId(orderId);

        Inventory inv = Bukkit.createInventory(holder, OrderGuiUtil.CONFIRM_SIZE,
                Component.text("Confirm Delivery"));

        ItemStack displayItem = OrderGuiUtil.createDisplayItemFromMaterialName(order.materialName());
        displayItem.setAmount(Math.min(itemCount, requiredMaterial.getMaxStackSize()));
        if (order.hasNbtRequirement()) {
            displayItem = OrderGuiUtil.applyNbtDisplay(displayItem, order);
            OrderGuiUtil.addEnchantmentGlow(displayItem);
        }
        var meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Delivering: " + itemCount + "x", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Payment: " + OrderManager.formatPrice(itemCount * order.pricePerUnit()), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Buyer: " + order.buyerName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click Confirm to deliver", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        inv.setItem(OrderGuiUtil.CONFIRM_SLOT_ITEM, displayItem);

        inv.setItem(OrderGuiUtil.CONFIRM_SLOT_CONFIRM,
                OrderGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE,
                        "Confirm - " + OrderManager.formatPrice(itemCount * order.pricePerUnit()), List.of(
                                Component.text("Click to deliver items", NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false)
                        )));

        inv.setItem(OrderGuiUtil.CONFIRM_SLOT_CANCEL, OrderGuiUtil.cancelButtonItem());

        OrderGuiUtil.fillEmptySlots(inv, OrderGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }
}

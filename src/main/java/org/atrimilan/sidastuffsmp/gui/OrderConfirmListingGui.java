package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

public class OrderConfirmListingGui {

    private OrderConfirmListingGui() {}

    public static Inventory open(Player player) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.CONFIRM_LISTING);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Confirm Listing"));

        Material selectedMat = state.getSelectedMaterial();
        int quantity = state.getQuantity();
        double pricePerUnit = state.getPricePerUnit();

        ItemStack displayItem;
        if (state.getSelectedItemName() != null) {
            MinecraftDataRegistry.MinecraftItem mcItem = MinecraftDataRegistry.findItemByName(state.getSelectedItemName());
            if (mcItem != null && mcItem.hasPotionEffect()) {
                displayItem = MinecraftDataRegistry.createDisplayItemStack(mcItem);
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    displayItem.setItemMeta(meta);
                }
            } else {
                displayItem = new ItemStack(selectedMat != null ? selectedMat : Material.STONE);
            }
        } else {
            displayItem = new ItemStack(selectedMat != null ? selectedMat : Material.STONE);
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(quantity + "x " + OrderManager.formatMaterialName(
                    state.getSelectedItemName() != null ? state.getSelectedItemName() : selectedMat.name()),
                    NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Price: " + OrderManager.formatPrice(pricePerUnit) + " each",
                    NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            double total = quantity * pricePerUnit;
            lore.add(Component.text("Total: " + OrderManager.formatPrice(total),
                    NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

            List<MinecraftDataRegistry.MinecraftEnchantment> selEnch = state.getSelectedEnchantments();
            if (selEnch != null && !selEnch.isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Enchantments:", NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
                for (MinecraftDataRegistry.MinecraftEnchantment ench : selEnch) {
                    int level = state.getEnchantmentLevel(ench.name());
                    lore.add(Component.text("  " + ench.displayName() + " " + toRoman(level), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            if (state.getSelectedEffect() != null && !state.getSelectedEffect().isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Effect: " + MinecraftDataRegistry.getPotionEffectDisplay(state.getSelectedEffect()), NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        inv.setItem(13, displayItem);

        inv.setItem(11, OrderGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE,
                "Confirm & Publish", List.of(
                        Component.text("Click to publish order", NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false)
                )));

        inv.setItem(15, OrderGuiUtil.createControlItem(Material.RED_STAINED_GLASS_PANE,
                "Cancel", List.of(
                        Component.text("Go back to edit", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )));

        OrderGuiUtil.fillEmptySlots(inv, OrderGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    private static String toRoman(int level) {
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
}

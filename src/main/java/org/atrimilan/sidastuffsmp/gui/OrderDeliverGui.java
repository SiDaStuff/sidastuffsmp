package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDeliverGui {

    private OrderDeliverGui() {}

    public static Inventory open(Player player, int orderId) {
        OrderListing order = OrderManager.getOrderById(orderId);
        if (order == null) return null;

        Material requiredMaterial = OrderGuiUtil.resolveMaterialFromName(order.materialName());

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.DELIVER_ITEMS);
        holder.setViewerUuid(player.getUniqueId());
        holder.setOrderId(orderId);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Deliver: " + OrderManager.formatMaterialName(requiredMaterial)));

        for (int i = OrderGuiUtil.DELIVER_INPUT_START; i < OrderGuiUtil.DELIVER_INPUT_START + OrderGuiUtil.DELIVER_INPUT_SIZE; i++) {
            inv.setItem(i, null);
        }

        inv.setItem(OrderGuiUtil.DELIVER_SLOT_CONFIRM, OrderGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE, "Confirm Delivery", List.of(
                Component.text("Place items above, then click", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Or click 'Set Amount' to deliver", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("a specific quantity directly", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        )));

        inv.setItem(OrderGuiUtil.DELIVER_SLOT_SET_AMOUNT, OrderGuiUtil.createControlItem(Material.NAME_TAG, "Set Amount", List.of(
                Component.text("Click to type how many", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("items to deliver directly", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        )));

        ItemStack infoItem;
        if (order.hasNbtRequirement()) {
            ItemStack template = AuctionManager.deserializeItem(order.requiredNbt());
            if (template != null) {
                infoItem = template.clone();
                OrderGuiUtil.addEnchantmentGlow(infoItem);
            } else {
                infoItem = new ItemStack(requiredMaterial);
            }
        } else {
            infoItem = new ItemStack(requiredMaterial);
        }
        if (infoItem.hasItemMeta() && infoItem.getItemMeta() != null) {
            ItemMeta infoMeta = infoItem.getItemMeta();
            if (order.hasNbtRequirement() && infoMeta instanceof PotionMeta potionMeta) {
                if (potionMeta.hasCustomEffects()) {
                    for (PotionEffect effect : potionMeta.getCustomEffects()) {
                        String effectName = formatEffectName(effect.getType().getKey().getKey());
                        infoMeta.displayName(Component.text(effectName + " Potion", NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
            }
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Order #" + orderId, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Price: " + OrderManager.formatPrice(order.pricePerUnit()) + " each", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Remaining: " + order.getRemainingQuantity(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Buyer: " + order.buyerName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            if (order.hasNbtRequirement()) {
                ItemStack template = AuctionManager.deserializeItem(order.requiredNbt());
                if (template != null && template.hasItemMeta()) {
                    ItemMeta templateMeta = template.getItemMeta();
                    lore.add(Component.empty());
                    lore.add(Component.text("Required:", NamedTextColor.AQUA)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false));

                    if (templateMeta.hasEnchants()) {
                        for (Map.Entry<Enchantment, Integer> entry : templateMeta.getEnchants().entrySet()) {
                            String enchName = formatEnchName(entry.getKey().getKey().getKey());
                            String level = toRoman(entry.getValue());
                            lore.add(Component.text(" " + enchName + " " + level, NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                    }

                    if (templateMeta instanceof PotionMeta potionMeta && potionMeta.hasCustomEffects()) {
                        for (PotionEffect effect : potionMeta.getCustomEffects()) {
                            String effectName = formatEffectName(effect.getType().getKey().getKey());
                            lore.add(Component.text(" " + effectName, NamedTextColor.LIGHT_PURPLE)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                    }
                }
            }

            infoMeta.lore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(OrderGuiUtil.DELIVER_SLOT_INFO, infoItem);

        inv.setItem(52, OrderGuiUtil.cancelButtonItem());
        inv.setItem(53, OrderGuiUtil.closeItem());

        ItemStack deliverFiller = OrderGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, deliverFiller);
            }
        }

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

    private static String formatEnchName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String formatEffectName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}

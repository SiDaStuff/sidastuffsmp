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
import java.util.Map;

public class CreateOrderGui {

    private static final int SLOT_CANCEL = 9;
    private static final int SLOT_ITEM_SELECT = 11;
    private static final int SLOT_AMOUNT = 13;
    private static final int SLOT_PRICE = 15;
    private static final int SLOT_ENCHANT = 16;
    private static final int SLOT_PUBLISH = 17;

    private CreateOrderGui() {}

    public static Inventory open(Player player) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());

        if (state.getSelectedMaterial() == null) {
            state.setSelectedMaterial(Material.STONE);
        }

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.CREATE_ORDER);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Create Order"));

        inv.setItem(SLOT_CANCEL, OrderGuiUtil.createControlItem(Material.RED_STAINED_GLASS_PANE,
                "Cancel", List.of(
                        Component.text("Go back without creating", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false))));

        Material selectedMat = state.getSelectedMaterial();
        int quantity = state.getQuantity();
        double pricePerUnit = state.getPricePerUnit();

        if (selectedMat != null) {
            ItemStack itemDisplay = findMinecraftItemDisplay(state.getSelectedItemName(), selectedMat);
            ItemMeta meta = itemDisplay.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text(getItemDisplayName(state.getSelectedItemName(), selectedMat), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                
                // Show shop price if available
                double shopPrice = EconomyShopGuiPriceLoader.getSellPrice(selectedMat);
                if (shopPrice > 0) {
                    lore.add(Component.text("Shop Price: " + OrderManager.formatPrice(shopPrice), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }
                
                // Show potion duration for potion items
                if (MinecraftDataRegistry.isPotionItem(selectedMat)) {
                    MinecraftDataRegistry.MinecraftItem mcItem = state.getSelectedItemName() != null
                            ? MinecraftDataRegistry.findItemByName(state.getSelectedItemName())
                            : null;
                    if (mcItem != null) {
                        String duration = MinecraftDataRegistry.getPotionDuration(mcItem);
                        if (duration != null) {
                            lore.add(Component.text("Duration: " + duration, NamedTextColor.LIGHT_PURPLE)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                    }
                }
                
                lore.add(Component.text("Click to change item", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                if (MinecraftDataRegistry.isPotionItem(selectedMat)) {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                }
                meta.lore(lore);
                itemDisplay.setItemMeta(meta);
            }
            inv.setItem(SLOT_ITEM_SELECT, itemDisplay);
        } else {
            inv.setItem(SLOT_ITEM_SELECT, OrderGuiUtil.createControlItem(Material.COMPASS,
                    "Select Item", List.of(
                            Component.text("Click to choose an item", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false))));
        }

        if (quantity > 0) {
            inv.setItem(SLOT_AMOUNT, OrderGuiUtil.createControlItem(Material.NAME_TAG,
                    "Amount: " + quantity, List.of(
                            Component.text("Click to set amount", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false))));
        } else {
            inv.setItem(SLOT_AMOUNT, OrderGuiUtil.createControlItem(Material.NAME_TAG,
                    "Set Amount", List.of(
                            Component.text("Click to set amount", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false))));
        }

        if (pricePerUnit > 0) {
            inv.setItem(SLOT_PRICE, OrderGuiUtil.createControlItem(Material.EMERALD,
                    "Price: " + OrderManager.formatPrice(pricePerUnit) + " each", List.of(
                            Component.text("Click to set price via sign", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false))));
        } else {
            inv.setItem(SLOT_PRICE, OrderGuiUtil.createControlItem(Material.EMERALD,
                    "Set Price", List.of(
                            Component.text("Click to set price per unit", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false))));
        }

        if (selectedMat != null && MinecraftDataRegistry.isEnchantable(selectedMat)) {
            List<MinecraftDataRegistry.MinecraftEnchantment> selectedEnchants = state.getSelectedEnchantments();
            if (selectedEnchants != null && !selectedEnchants.isEmpty()) {
                List<Component> enchLore = new ArrayList<>();
                enchLore.add(Component.empty());
                for (MinecraftDataRegistry.MinecraftEnchantment ench : selectedEnchants) {
                    int level = state.getEnchantmentLevel(ench.name());
                    enchLore.add(Component.text(ench.displayName() + " " + toRoman(level), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
                enchLore.add(Component.empty());
                enchLore.add(Component.text("Click to toggle enchantments", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                inv.setItem(SLOT_ENCHANT, OrderGuiUtil.createControlItem(Material.ENCHANTED_BOOK,
                        "Enchantments (" + selectedEnchants.size() + ")", enchLore));
            } else {
                inv.setItem(SLOT_ENCHANT, OrderGuiUtil.createControlItem(Material.ENCHANTED_BOOK,
                        "Add Enchantments", List.of(
                                Component.text("Click to add enchantments", NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false))));
            }
        }



        if (selectedMat != null && quantity > 0 && pricePerUnit > 0) {
            double totalCost = quantity * pricePerUnit;
            List<Component> publishLore = new ArrayList<>();
            publishLore.add(Component.text("Total Cost: " + OrderManager.formatPrice(totalCost), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            publishLore.add(Component.text(quantity + "x " + getItemDisplayName(state.getSelectedItemName(), selectedMat)
                    + " @ " + OrderManager.formatPrice(pricePerUnit) + " each", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            
            // Show shop price comparison if available
            double shopPrice = EconomyShopGuiPriceLoader.getSellPrice(selectedMat);
            if (shopPrice > 0) {
                publishLore.add(Component.empty());
                publishLore.add(Component.text("Market Price: " + OrderManager.formatPrice(shopPrice), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                if (pricePerUnit < shopPrice) {
                    publishLore.add(Component.text("Below market price!", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                } else if (pricePerUnit > shopPrice) {
                    publishLore.add(Component.text("Above market price!", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    publishLore.add(Component.text("At market price!", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            
            List<MinecraftDataRegistry.MinecraftEnchantment> selEnch = state.getSelectedEnchantments();
            if (selEnch != null && !selEnch.isEmpty()) {
                publishLore.add(Component.empty());
                publishLore.add(Component.text("Enchanted:", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                for (MinecraftDataRegistry.MinecraftEnchantment ench : selEnch) {
                    int level = state.getEnchantmentLevel(ench.name());
                    publishLore.add(Component.text("  " + ench.displayName() + " " + toRoman(level), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            if (state.getSelectedEffect() != null && !state.getSelectedEffect().isEmpty()) {
                publishLore.add(Component.empty());
                publishLore.add(Component.text("Effect: " + MinecraftDataRegistry.getPotionEffectDisplay(state.getSelectedEffect()), NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }
            publishLore.add(Component.empty());
            publishLore.add(Component.text("Click to publish", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            inv.setItem(SLOT_PUBLISH, OrderGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE,
                    "Publish Order", publishLore));
        } else {
            inv.setItem(SLOT_PUBLISH, OrderGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE,
                    "Publish Order", List.of(
                            Component.text("Select item, amount, and price first", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false))));
        }

        OrderGuiUtil.fillEmptySlots(inv, OrderGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    private static ItemStack findMinecraftItemDisplay(String itemName, Material fallbackMaterial) {
        if (itemName != null) {
            MinecraftDataRegistry.MinecraftItem mcItem = MinecraftDataRegistry.findItemByName(itemName);
            if (mcItem != null && mcItem.hasPotionEffect()) {
                return MinecraftDataRegistry.createDisplayItemStack(mcItem);
            }
        }
        return new ItemStack(fallbackMaterial);
    }

    private static String getItemDisplayName(String itemName, Material fallbackMaterial) {
        if (itemName != null) {
            MinecraftDataRegistry.MinecraftItem mcItem = MinecraftDataRegistry.findItemByName(itemName);
            if (mcItem != null) return mcItem.displayName();
        }
        return OrderManager.formatMaterialName(fallbackMaterial);
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

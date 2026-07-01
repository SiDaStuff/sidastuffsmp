package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentSelectionGui {

    private static final int PAGE_SIZE = 36;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;

    private EnchantmentSelectionGui() {}

    public static Inventory open(Player player, int page) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());
        Material selectedMat = state.getSelectedMaterial();

        List<MinecraftDataRegistry.MinecraftEnchantment> allEnchants;
        if (selectedMat != null) {
            allEnchants = MinecraftDataRegistry.getApplicableEnchantments(selectedMat);
        } else {
            allEnchants = MinecraftDataRegistry.getAllEnchantments();
        }

        int totalEntries = allEnchants.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(page, 0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalEntries);

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.ENCHANTMENT_SELECT);
        holder.setViewerUuid(player.getUniqueId());
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Select Enchantments"));

        for (int i = start; i < end; i++) {
            MinecraftDataRegistry.MinecraftEnchantment ench = allEnchants.get(i);
            int slot = i - start;
            boolean isSelected = state.getSelectedEnchantments().stream()
                    .anyMatch(e -> e.name().equals(ench.name()));
            boolean isConflicting = !isSelected && MinecraftDataRegistry.hasConflictingEnchantment(
                    state.getSelectedEnchantments(), ench);

            Material icon;
            if (isSelected) {
                icon = Material.ENCHANTED_BOOK;
            } else if (isConflicting) {
                icon = Material.BARRIER;
            } else {
                icon = Material.BOOK;
            }

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                int level = state.getEnchantmentLevel(ench.name());
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("Max Level: " + ench.maxLevel(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                if (ench.treasureOnly()) {
                    lore.add(Component.text("Treasure Only", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (ench.curse()) {
                    lore.add(Component.text("Curse", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (!ench.exclude().isEmpty()) {
                    lore.add(Component.empty());
                    lore.add(Component.text("Excludes:", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    for (String ex : ench.exclude()) {
                        String exDisplay = formatEnchName(ex);
                        lore.add(Component.text("  " + exDisplay, NamedTextColor.DARK_RED)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                lore.add(Component.empty());
                if (isSelected) {
                    lore.add(Component.text("Level: " + toRoman(level), NamedTextColor.AQUA)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Left-click to increase level", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Right-click to remove", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                } else if (isConflicting) {
                    lore.add(Component.text("Conflicts with selected", NamedTextColor.RED)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("Click to add", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                NamedTextColor nameColor = isSelected ? NamedTextColor.AQUA
                        : isConflicting ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
                meta.displayName(Component.text(ench.displayName() + (isSelected ? " " + toRoman(level) : ""),
                                nameColor)
                        .decoration(TextDecoration.BOLD, isSelected)
                        .decoration(TextDecoration.ITALIC, false));
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }

        

        inv.setItem(SLOT_PREV, OrderGuiUtil.prevPageItem(page > 0));
        inv.setItem(SLOT_BACK, OrderGuiUtil.createControlItem(Material.ARROW,
                "Back to Create Order", List.of(
                        Component.text("Return to order creation", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false))));
        inv.setItem(SLOT_NEXT, OrderGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, OrderGuiUtil.closeItem());

        ItemStack enchFiller = OrderGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, enchFiller);
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
}

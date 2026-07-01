package org.atrimilan.sidastuffsmp.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class RtpGui {

    private static final int SLOT_OVERWORLD = 11;
    private static final int SLOT_NETHER = 13;
    private static final int SLOT_END = 15;

    private static final int GUI_SIZE = 27;

    private RtpGui() {}

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(new RtpGuiHolder(), GUI_SIZE,
                Component.text("Random Teleport"));

        inv.setItem(SLOT_OVERWORLD, createDimensionItem(Material.GRASS_BLOCK, "Overworld",
                List.of(
                        Component.empty(),
                        Component.text("Click to RTP to the Overworld", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(SLOT_NETHER, createDimensionItem(Material.NETHERRACK, "Nether",
                List.of(
                        Component.empty(),
                        Component.text("Click to RTP to the Nether", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(SLOT_END, createDimensionItem(Material.END_STONE, "End",
                List.of(
                        Component.empty(),
                        Component.text("Click to RTP to the End", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false))));

        player.openInventory(inv);
    }

    private static ItemStack createDimensionItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class RtpGuiHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
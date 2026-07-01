package org.atrimilan.sidastuffsmp.sus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class SusDetailGui {

    private static final int GUI_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_NEXT = 53;
    public static final int SLOT_BROADCAST = 47;

    private SusDetailGui() {}

    public static void open(Player viewer, String playerName, int page) {
        SusManager.SusEntry entry = null;
        List<SusManager.SusEntry> all = SusManager.getAllEntries();
        for (SusManager.SusEntry e : all) {
            if (e.playerName().equalsIgnoreCase(playerName)) {
                entry = e;
                break;
            }
        }
        if (entry == null) {
            viewer.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Player not found on sus list.", NamedTextColor.RED));
            return;
        }

        List<String> flags = entry.flags();
        int totalPages = Math.max(1, (int) Math.ceil((double) flags.size() / ITEMS_PER_PAGE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(0, page);

        SusDetailGuiHolder holder = new SusDetailGuiHolder(playerName, page);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE,
                Component.text("Sus: " + playerName, NamedTextColor.DARK_RED));

        int offset = page * ITEMS_PER_PAGE;
        int end = Math.min(offset + ITEMS_PER_PAGE, flags.size());
        for (int i = offset; i < end; i++) {
            String flag = flags.get(i);
            inv.setItem(i - offset, createFlagPaper(flag));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, createControlItem(Material.ARROW, "Previous Page",
                    List.of(Component.text("Page " + page, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, createControlItem(Material.ARROW, "Next Page",
                    List.of(Component.text("Page " + (page + 2), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        }

        inv.setItem(SLOT_BROADCAST, createControlItem(Material.OBSERVER, "Broadcast Flags",
                List.of(Component.text("Click to report in chat", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));

        inv.setItem(SLOT_BACK, createControlItem(Material.BARRIER, "Back",
                List.of(Component.text("Return to sus list", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));

        viewer.openInventory(inv);
    }

    static void broadcastFlags(Player viewer, String playerName) {
        SusManager.SusEntry entry = null;
        for (SusManager.SusEntry e : SusManager.getAllEntries()) {
            if (e.playerName().equalsIgnoreCase(playerName)) {
                entry = e;
                break;
            }
        }
        if (entry == null) return;

        StringBuilder flagsList = new StringBuilder();
        for (String flag : entry.flags()) {
            if (flagsList.length() > 0) flagsList.append(", ");
            flagsList.append(flag);
        }

        Component message = org.atrimilan.sidastuffsmp.utils.Chat.prefixed(
                Component.text(entry.playerName(), NamedTextColor.YELLOW)
                        .append(Component.text(" - Flags: ", NamedTextColor.WHITE))
                        .append(Component.text(flagsList.toString(), NamedTextColor.RED))
        );
        Bukkit.broadcast(message, "sidastuffsmp.admin");
    }

    private static ItemStack createFlagPaper(String flag) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(flag, NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Reported flag", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createControlItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

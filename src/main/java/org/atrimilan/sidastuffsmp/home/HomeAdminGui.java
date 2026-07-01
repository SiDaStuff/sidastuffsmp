package org.atrimilan.sidastuffsmp.home;

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
import java.util.UUID;

public class HomeAdminGui {

    private static final int[] HOME_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final int[] DELETE_SLOTS = {18, 19, 20, 21, 23, 24, 25, 26};

    private HomeAdminGui() {}

    public static void open(Player admin, UUID ownerUuid, String ownerName, int page) {
        Inventory inv = Bukkit.createInventory(new HomeAdminGuiHolder(ownerUuid, ownerName, page), 36,
                Component.text("Homes: " + ownerName));

        List<Home> homes = HomeManager.getHomes(ownerUuid);
        List<Home> paged = new ArrayList<>();
        for (int i = page * 8; i < (page + 1) * 8 && i < HomeConfig.maxHomes(); i++) {
            int homeSlot = i + 1;
            Home match = null;
            for (Home h : homes) {
                if (h.getSlot() == homeSlot) { match = h; break; }
            }
            if (match != null) paged.add(match);
        }

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            int homeSlot = i + 1 + (page * 8);
            if (homeSlot > HomeConfig.maxHomes()) {
                inv.setItem(HOME_SLOTS[i], new ItemStack(Material.AIR));
                continue;
            }
            Home home = null;
            for (Home h : paged) {
                if (h.getSlot() == homeSlot) { home = h; break; }
            }

            Material bedMat = home != null ? getBedColor(i) : Material.LIGHT_GRAY_BED;
            ItemStack bed = new ItemStack(bedMat);
            ItemMeta meta = bed.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                if (home != null) {
                    meta.displayName(Component.text("Slot " + home.getSlot() + ": " + home.getName(),
                            NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("World: " + home.getFriendlyWorldName(), NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text(String.format("Coords: %.1f, %.1f, %.1f",
                            home.getX(), home.getY(), home.getZ()), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Click to teleport", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    meta.displayName(Component.text("Slot " + homeSlot + " (empty)", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                bed.setItemMeta(meta);
            }
            inv.setItem(HOME_SLOTS[i], bed);

            ItemStack del = new ItemStack(home != null ? Material.BARRIER : Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta delMeta = del.getItemMeta();
            if (delMeta != null) {
                if (home != null) {
                    delMeta.displayName(Component.text("Delete " + home.getName(), NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                    List<Component> dlore = new ArrayList<>();
                    dlore.add(Component.text("Admin delete this home.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    delMeta.lore(dlore);
                } else {
                    delMeta.displayName(Component.text("Empty", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
                del.setItemMeta(delMeta);
            }
            inv.setItem(DELETE_SLOTS[i], del);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            close.setItemMeta(closeMeta);
        }
        inv.setItem(35, close);

        // Back arrow
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Previous Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(backMeta);
        }
        inv.setItem(36 - 9, back);

        // Forward arrow
        ItemStack forward = new ItemStack(Material.ARROW);
        ItemMeta fwdMeta = forward.getItemMeta();
        if (fwdMeta != null) {
            fwdMeta.displayName(Component.text("Next Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            forward.setItemMeta(fwdMeta);
        }
        inv.setItem(36 - 5, forward);

        // Info
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("Homes: " + ownerName, NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("UUID: " + ownerUuid, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Total homes: " + HomeManager.getHomes(ownerUuid).size(),
                    NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click a bed to teleport. Click", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("the barrier to delete a home.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        ItemStack adminFiller = createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, adminFiller);
            }
        }

        admin.openInventory(inv);
    }

    public static void openConfirmDelete(Player admin, UUID ownerUuid, String ownerName, int slot, String homeName) {
        Inventory inv = Bukkit.createInventory(new HomeAdminGuiHolder(ownerUuid, ownerName, 0), 27,
                Component.text("Confirm Delete"));
        ((HomeAdminGuiHolder) inv.getHolder()).setPendingDelete(slot);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Confirm Delete", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Delete home: " + homeName, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Owner: " + ownerName, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            confirm.setItemMeta(meta);
        }
        inv.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cmeta = cancel.getItemMeta();
        if (cmeta != null) {
            cmeta.displayName(Component.text("Cancel", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            cancel.setItemMeta(cmeta);
        }
        inv.setItem(15, cancel);

        ItemStack confirmFiller = createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, confirmFiller);
            }
        }

        admin.openInventory(inv);
    }

    private static ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material getBedColor(int index) {
        return switch (index) {
            case 0 -> Material.RED_BED;
            case 1 -> Material.ORANGE_BED;
            case 2 -> Material.YELLOW_BED;
            case 3 -> Material.LIME_BED;
            case 4 -> Material.GREEN_BED;
            case 5 -> Material.CYAN_BED;
            case 6 -> Material.BLUE_BED;
            case 7 -> Material.PURPLE_BED;
            default -> Material.WHITE_BED;
        };
    }
}
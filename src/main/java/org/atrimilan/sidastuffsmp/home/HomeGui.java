package org.atrimilan.sidastuffsmp.home;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.gui.OrderGuiUtil;
import org.atrimilan.sidastuffsmp.utils.AnvilInput;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HomeGui {

    public static final int GUI_SIZE = 54;
    public static final int[] HOME_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };
    public static final int MAX_HOMES = HOME_SLOTS.length;

    public static final int SLOT_SEARCH = 49;
    public static final int SLOT_CLOSE = 53;

    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_ITEM_DISPLAY = 13;
    public static final int SLOT_CANCEL = 15;
    public static final int CONFIRM_SIZE = 27;

    public static final int SLOT_EDIT_NAME = 11;
    public static final int SLOT_EDIT_ICON = 15;
    public static final int EDIT_MENU_SIZE = 27;

    private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material BORDER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;

    private HomeGui() {}

    public static void open(Player player) {
        open(player, null);
    }

    public static void open(Player player, String searchTerm) {
        List<Home> allHomes = HomeManager.getHomes(player.getUniqueId());

        HomeGuiHolder holder = new HomeGuiHolder();
        holder.setViewerUuid(player.getUniqueId());
        holder.setSearchTerm(searchTerm);

        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, Component.text("My Homes"));

        ItemStack border = createFiller(BORDER_MATERIAL);
        ItemStack filler = createFiller(FILLER_MATERIAL);

        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inv.setItem(slot, border);
        }

        int[] fillerSlots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int slot : fillerSlots) {
            inv.setItem(slot, filler);
        }

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            int homeSlotNum = i + 1;
            Home home = findHomeAtSlot(allHomes, homeSlotNum);
            inv.setItem(HOME_SLOTS[i], createHomeItem(home, homeSlotNum));
        }

        if (searchTerm != null && !searchTerm.isBlank()) {
            inv.setItem(SLOT_SEARCH, OrderGuiUtil.createControlItem(Material.NAME_TAG, "Search: " + searchTerm, List.of(
                    Component.text("Click to search again", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        } else {
            inv.setItem(SLOT_SEARCH, OrderGuiUtil.createControlItem(Material.NAME_TAG, "Search", List.of(
                    Component.text("Search your homes by name", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        }

        inv.setItem(SLOT_CLOSE, OrderGuiUtil.createControlItem(Material.BARRIER, "Close", null));

        player.openInventory(inv);
    }

    private static Home findHomeAtSlot(List<Home> homes, int slot) {
        for (Home h : homes) {
            if (h.getSlot() == slot) return h;
        }
        return null;
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

    private static ItemStack createHomeItem(Home home, int homeSlot) {
        Material mat = resolveIconMaterial(home);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<Component> lore = new ArrayList<>();
        if (home != null) {
            meta.displayName(Component.text(home.getName(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            lore.add(Component.text("World: " + home.getFriendlyWorldName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Slot " + home.getSlot(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Left-click: Teleport", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Right-click: Edit Home", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Shift-click: Delete", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.text("Empty Slot " + homeSlot, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            lore.add(Component.text("Left-click to set home", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static Material resolveIconMaterial(Home home) {
        if (home == null) return Material.GRAY_BED;
        if (home.getIcon() != null && !home.getIcon().isBlank()) {
            Material custom = Material.matchMaterial(home.getIcon().toUpperCase(Locale.ROOT));
            if (custom != null && custom.isItem()) return custom;
        }
        return Material.WHITE_BED;
    }

    public static void openEditMenu(Player player, int homeSlot, String homeName) {
        HomeGuiHolder holder = new HomeGuiHolder();
        holder.setPendingDelete(homeSlot);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, EDIT_MENU_SIZE, Component.text("Edit Home"));

        ItemStack filler = createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < EDIT_MENU_SIZE; i++) {
            inv.setItem(i, filler);
        }

        ItemStack display = new ItemStack(resolveIconMaterial(HomeManager.getHome(player.getUniqueId(), homeSlot)));
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.displayName(Component.text(homeName, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            displayMeta.lore(List.of(
                    Component.text("Slot " + homeSlot, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            display.setItemMeta(displayMeta);
        }
        inv.setItem(SLOT_ITEM_DISPLAY, display);

        ItemStack editName = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = editName.getItemMeta();
        if (nameMeta != null) {
            nameMeta.displayName(Component.text("Edit Name", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            nameMeta.lore(List.of(
                    Component.text("Rename this home", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Current: " + homeName, NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false)));
            editName.setItemMeta(nameMeta);
        }
        inv.setItem(SLOT_EDIT_NAME, editName);

        ItemStack editIcon = new ItemStack(Material.PAINTING);
        ItemMeta iconMeta = editIcon.getItemMeta();
        if (iconMeta != null) {
            iconMeta.displayName(Component.text("Edit Icon", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            iconMeta.lore(List.of(
                    Component.text("Change the home's icon", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            editIcon.setItemMeta(iconMeta);
        }
        inv.setItem(SLOT_EDIT_ICON, editIcon);

        ItemStack back = OrderGuiUtil.createControlItem(Material.ARROW, "Back to Homes", List.of(
                Component.text("Return without changes", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    public static void openIconPicker(Player player, int homeSlot) {
        HomeIconGui.open(player, homeSlot, 0, null);
    }

    public static void openConfirmDelete(Player player, int slot, String name) {
        HomeGuiHolder holder = new HomeGuiHolder();
        holder.setPendingDelete(slot);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, CONFIRM_SIZE, Component.text("Delete Home"));

        ItemStack filler = createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < CONFIRM_SIZE; i++) {
            inv.setItem(i, filler);
        }

        ItemStack display = new ItemStack(resolveIconMaterial(HomeManager.getHome(player.getUniqueId(), slot)));
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            displayMeta.lore(List.of(
                    Component.text("This home will be deleted!", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)));
            display.setItemMeta(displayMeta);
        }
        inv.setItem(SLOT_ITEM_DISPLAY, display);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text("Confirm Delete", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> clore = new ArrayList<>();
            clore.add(Component.text("Delete home: " + name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            clore.add(Component.text("This cannot be undone!", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            confirmMeta.lore(clore);
            confirm.setItemMeta(confirmMeta);
        }

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("Cancel", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            cancelMeta.lore(List.of(
                    Component.text("Return to My Homes", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            cancel.setItemMeta(cancelMeta);
        }

        inv.setItem(SLOT_CONFIRM, confirm);
        inv.setItem(SLOT_CANCEL, cancel);

        player.openInventory(inv);
    }

    public static void requestSetHomeName(Player player, int homeSlot) {
        AnvilInput.request(player, "Set Home Name", "Enter home name:", "home",
                name -> {
                    if (HomeConfig.isWorldBlocked(player.getLocation().getWorld().getName())) {
                        player.sendMessage(Chat.prefixed(
                                "You cannot set homes in this world!", NamedTextColor.RED));
                        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                            if (player.isOnline()) open(player);
                        }, 2L);
                        return;
                    }
                    HomeManager.createHomeFromLocation(
                            player.getUniqueId(), homeSlot, name, player.getLocation());
                    player.sendMessage(Chat.prefixed(
                            "Home '" + name + "' set!", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                        if (player.isOnline()) open(player);
                    }, 2L);
                },
                () -> Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                    if (player.isOnline()) open(player);
                }, 1L));
    }

    public static void requestRenameHome(Player player, int homeSlot, String currentName) {
        AnvilInput.request(player, "Rename Home", "Enter new home name:", currentName,
                newName -> {
                    HomeManager.renameHome(player.getUniqueId(), homeSlot, newName);
                    player.sendMessage(Chat.prefixed(
                            "Home renamed to '" + newName + "'.", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                        if (player.isOnline()) open(player);
                    }, 2L);
                },
                () -> Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                    if (player.isOnline()) open(player);
                }, 1L));
    }
}

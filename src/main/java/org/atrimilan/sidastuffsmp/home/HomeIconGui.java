package org.atrimilan.sidastuffsmp.home;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.gui.OrderGuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class HomeIconGui {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_SEARCH = 46;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;

    private static final List<String> ALL_MATERIALS = new ArrayList<>();

    static {
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.name().contains("POTION") && !mat.name().contains("SPLASH") &&
                    !mat.name().contains("LINGERING") && !mat.name().contains("TIPPED_ARROW") &&
                    !mat.name().contains("ELDER_GUARDIAN") && !mat.name().contains("ZOMBIE_VILLAGER") &&
                    !mat.name().contains("SKELETON_HORSE") && !mat.name().contains("ZOMBIE_HORSE") &&
                    !mat.name().contains("MULE") && !mat.name().contains("DONKEY") && !mat.name().contains("LLAMA") &&
                    !mat.name().contains("TRADER_LLAMA") && !mat.name().contains("WANDERING_TRADER") &&
                    !mat.name().contains("FOX") && !mat.name().contains("BEE") && !mat.name().contains("PANDA") &&
                    !mat.name().contains("PIGLIN") && !mat.name().contains("PIGLIN_BRUTE") && !mat.name().contains("HOGLIN") &&
                    !mat.name().contains("ZOGLIN") && !mat.name().contains("WARDEN") && !mat.name().contains("ALLAY") &&
                    !mat.name().contains("FROG") && !mat.name().contains("TADPOLE") && !mat.name().contains("WARDEN") &&
                    !mat.name().contains("CAMEL") && !mat.name().contains("SNIFFER") && !mat.name().contains("BREEZE") &&
                    !mat.name().contains("CHEST_BOAT") && !mat.name().contains("SKELETAL") &&
                    !mat.name().contains("_SPAWN_EGG") && !mat.name().contains("MUSIC_DISC") &&
                    !mat.name().contains("SHULKER_BOX") && !mat.name().contains("undyed_shulker")) {
                ALL_MATERIALS.add(mat.name());
            }
        }
        ALL_MATERIALS.sort(String::compareToIgnoreCase);
    }

    static final class IconBrowserState {
        private int page = 0;
        private String searchTerm = null;
        private int homeSlot = 0;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public String getSearchTerm() { return searchTerm; }
        public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
        public int getHomeSlot() { return homeSlot; }
        public void setHomeSlot(int homeSlot) { this.homeSlot = homeSlot; }
    }

    private static final java.util.Map<UUID, IconBrowserState> BROWSER_STATES = new java.util.HashMap<>();

    private static IconBrowserState getState(UUID uuid) {
        return BROWSER_STATES.computeIfAbsent(uuid, k -> new IconBrowserState());
    }

    public static IconBrowserState getBrowserState(UUID uuid) {
        return getState(uuid);
    }

    private HomeIconGui() {}

    public static Inventory open(Player player, int homeSlot, int page, String searchTerm) {
        IconBrowserState state = getState(player.getUniqueId());
        state.setHomeSlot(homeSlot);
        state.setPage(page);
        state.setSearchTerm(searchTerm);

        List<String> filtered = getFiltered(searchTerm);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        if (page < 0) page = 0;
        state.setPage(page);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalItems);

        HomeGuiHolder holder = new HomeGuiHolder();
        holder.setHomeIconSelectionSlot(homeSlot);
        holder.setPage(page);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Select Home Icon"));

        for (int i = start; i < end; i++) {
            String matName = filtered.get(i);
            Material mat = Material.matchMaterial(matName);
            if (mat != null) {
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(formatName(matName), NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
                    item.setItemMeta(meta);
                }
                inv.setItem(i - start, item);
            }
        }

        ItemStack filler = createFiller(Material.GRAY_STAINED_GLASS_PANE);
        int[] fillerSlots = {47, 48, 50, 51};
        for (int s : fillerSlots) {
            inv.setItem(s, filler);
        }

        inv.setItem(SLOT_PREV, prevPageItem(page > 0));
        inv.setItem(SLOT_SEARCH, searchTerm != null && !searchTerm.isBlank()
                ? searchItemWithTerm(searchTerm)
                : searchItem());
        inv.setItem(SLOT_BACK, backItem());
        inv.setItem(SLOT_NEXT, nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, OrderGuiUtil.closeItem());

        player.openInventory(inv);
        return inv;
    }

    public static void refresh(Player player) {
        IconBrowserState state = getState(player.getUniqueId());
        open(player, state.getHomeSlot(), state.getPage(), state.getSearchTerm());
    }

    private static List<String> getFiltered(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return ALL_MATERIALS.stream().filter(HomeIconGui::isValid).collect(Collectors.toList());
        }
        String[] keywords = searchTerm.toLowerCase(Locale.ROOT).split("\\s+");
        return ALL_MATERIALS.stream()
                .filter(name -> {
                    String lowered = name.toLowerCase(Locale.ROOT);
                    String friendly = formatName(name).toLowerCase(Locale.ROOT);
                    for (String keyword : keywords) {
                        if (keyword.isEmpty()) continue;
                        if (!lowered.contains(keyword) && !friendly.contains(keyword)) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(HomeIconGui::isValid)
                .collect(Collectors.toList());
    }

    private static boolean isValid(String name) {
        Material mat = Material.matchMaterial(name);
        return mat != null && mat.isItem();
    }

    private static String formatName(String name) {
        return Arrays.stream(name.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
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

    static ItemStack searchItem() {
        return OrderGuiUtil.createControlItem(Material.NAME_TAG, "Search", List.of(
                Component.text("Click to search by icon name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
    }

    static ItemStack searchItemWithTerm(String term) {
        return OrderGuiUtil.createControlItem(Material.NAME_TAG, "Search: " + term, List.of(
                Component.text("Click to search again", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
    }

    static ItemStack backItem() {
        return OrderGuiUtil.createControlItem(Material.ARROW, "Back to Homes", List.of(
                Component.text("Return without selecting", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
    }

    static ItemStack prevPageItem(boolean hasPrev) {
        if (!hasPrev) return createFiller(Material.GRAY_STAINED_GLASS_PANE);
        return OrderGuiUtil.createControlItem(Material.ARROW, "Previous Page", List.of(
                Component.text("Click to go back", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
    }

    static ItemStack nextPageItem(boolean hasNext) {
        if (!hasNext) return createFiller(Material.GRAY_STAINED_GLASS_PANE);
        return OrderGuiUtil.createControlItem(Material.ARROW, "Next Page", List.of(
                Component.text("Click to go forward", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
    }
}

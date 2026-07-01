package org.atrimilan.sidastuffsmp.gui;

import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.auction.AuctionSortMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionBrowserGui {

    private static final java.util.Map<java.util.UUID, Long> REFRESH_COOLDOWNS = new java.util.HashMap<>();
    private static final long REFRESH_COOLDOWN_MS = 2000;

    private AuctionBrowserGui() {}

    public static Inventory open(Player player) {
        return open(player, 0, AuctionSortMode.NEWEST, null);
    }

    public static Inventory open(Player player, int page, AuctionSortMode sortMode, String searchTerm) {
        AuctionGuiHolder.BrowserState state = AuctionGuiHolder.getBrowserState(player.getUniqueId());
        state.setPage(page);
        state.setSortMode(sortMode);
        state.setSearchTerm(searchTerm);

        int offset = page * AuctionGuiUtil.ITEMS_PER_PAGE;
        int totalItems = AuctionManager.getBrowserListingCount(null, searchTerm);
        int totalPages = AuctionGuiUtil.getTotalPages(totalItems);

        List<AuctionListing> listings = AuctionManager.getBrowserListings(sortMode, null, searchTerm, offset, AuctionGuiUtil.ITEMS_PER_PAGE);

        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.GuiType.BROWSER);
        holder.setViewerUuid(player.getUniqueId());
        holder.setPage(page);
        holder.setSortMode(sortMode);
        holder.setSearchTerm(searchTerm);

        Inventory inv = Bukkit.createInventory(holder, AuctionGuiUtil.BROWSER_SIZE,
                net.kyori.adventure.text.Component.text("Auction House"));

        for (int i = 0; i < listings.size() && i < AuctionGuiUtil.ITEMS_PER_PAGE; i++) {
            inv.setItem(i, AuctionGuiUtil.createBrowserListingItem(listings.get(i)));
        }

        

        inv.setItem(AuctionGuiUtil.SLOT_PREV_PAGE, AuctionGuiUtil.prevPageItem(page > 0));
        inv.setItem(AuctionGuiUtil.SLOT_SORT, AuctionGuiUtil.sortItem(sortMode));
        inv.setItem(AuctionGuiUtil.SLOT_REFRESH, AuctionGuiUtil.refreshItem());
        inv.setItem(AuctionGuiUtil.SLOT_SEARCH, AuctionGuiUtil.searchItem());
        inv.setItem(AuctionGuiUtil.SLOT_MY_LISTINGS, AuctionGuiUtil.myListingsItem());
        inv.setItem(AuctionGuiUtil.SLOT_NEXT_PAGE, AuctionGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(AuctionGuiUtil.SLOT_CLOSE, AuctionGuiUtil.closeItem());

        ItemStack auctionFiller = AuctionGuiUtil.fillerItem();
        for (int i = AuctionGuiUtil.CONTROL_BAR_START; i < AuctionGuiUtil.BROWSER_SIZE; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, auctionFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    public static void refresh(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastRefresh = REFRESH_COOLDOWNS.get(uuid);
        if (lastRefresh != null && (now - lastRefresh) < REFRESH_COOLDOWN_MS) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("Please wait before refreshing.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        REFRESH_COOLDOWNS.put(uuid, now);
        AuctionGuiHolder.BrowserState state = AuctionGuiHolder.getBrowserState(uuid);
        open(player, state.getPage(), state.getSortMode(), state.getSearchTerm());
    }
}

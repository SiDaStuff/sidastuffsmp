package org.atrimilan.sidastuffsmp.gui;

import org.atrimilan.sidastuffsmp.auction.AuctionSortMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionGuiHolder implements InventoryHolder {

    public enum GuiType {
        BROWSER,
        CONFIRM_PURCHASE,
        CONFIRM_CANCEL,
        CREATE,
        MY_LISTINGS
    }

    private final GuiType guiType;
    private int listingId = -1;
    private int page = 0;
    private AuctionSortMode sortMode = AuctionSortMode.NEWEST;
    private String searchTerm = null;
    private UUID viewerUuid;

    public AuctionGuiHolder(GuiType guiType) {
        this.guiType = guiType;
    }

    public GuiType getGuiType() {
        return guiType;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public AuctionSortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(AuctionSortMode sortMode) {
        this.sortMode = sortMode;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public void setViewerUuid(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public static class BrowserState {
        private int page = 0;
        private AuctionSortMode sortMode = AuctionSortMode.NEWEST;
        private String searchTerm = null;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public AuctionSortMode getSortMode() { return sortMode; }
        public void setSortMode(AuctionSortMode sortMode) { this.sortMode = sortMode; }
        public String getSearchTerm() { return searchTerm; }
        public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    }

    private static final Map<UUID, BrowserState> BROWSER_STATES = new HashMap<>();

    public static BrowserState getBrowserState(UUID playerUuid) {
        return BROWSER_STATES.computeIfAbsent(playerUuid, k -> new BrowserState());
    }

    public static void clearBrowserState(UUID playerUuid) {
        BROWSER_STATES.remove(playerUuid);
    }

    private static final Map<UUID, Integer> PENDING_SEARCH = new HashMap<>();

    public static void setPendingSearch(UUID playerUuid, int page) {
        PENDING_SEARCH.put(playerUuid, page);
    }

    public static int consumePendingSearch(UUID playerUuid) {
        Integer val = PENDING_SEARCH.remove(playerUuid);
        return val != null ? val : 0;
    }

    public static boolean hasPendingSearch(UUID playerUuid) {
        return PENDING_SEARCH.containsKey(playerUuid);
    }
}

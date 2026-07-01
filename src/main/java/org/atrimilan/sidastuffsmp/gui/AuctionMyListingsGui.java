package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.auction.AuctionCategory;
import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.auction.AuctionSortMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionMyListingsGui {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_CLOSE = 53;

    private AuctionMyListingsGui() {}

    public static Inventory open(Player player, int page) {
        UUID uuid = player.getUniqueId();
        List<AuctionListing> allListings = AuctionManager.getMyListings(uuid);

        List<AuctionListing> activeListings = new ArrayList<>();
        List<AuctionListing> soldListings = new ArrayList<>();
        List<AuctionListing> expiredListings = new ArrayList<>();

        for (AuctionListing l : allListings) {
            switch (l.status()) {
                case "ACTIVE" -> activeListings.add(l);
                case "SOLD" -> soldListings.add(l);
                case "EXPIRED" -> expiredListings.add(l);
            }
        }

        List<InventoryEntry> entries = new ArrayList<>();

        entries.add(new InventoryEntry(InventoryEntry.Type.SEPARATOR, null,
                "Active Listings (" + activeListings.size() + ")", Material.YELLOW_STAINED_GLASS_PANE));
        for (AuctionListing l : activeListings) {
            entries.add(new InventoryEntry(InventoryEntry.Type.ACTIVE, l, null, null));
        }

        entries.add(new InventoryEntry(InventoryEntry.Type.SEPARATOR, null,
                "Sold (" + soldListings.size() + ")", Material.GREEN_STAINED_GLASS_PANE));
        for (AuctionListing l : soldListings) {
            entries.add(new InventoryEntry(InventoryEntry.Type.SOLD, l, null, null));
        }

        entries.add(new InventoryEntry(InventoryEntry.Type.SEPARATOR, null,
                "Expired (" + expiredListings.size() + ")", Material.GRAY_STAINED_GLASS_PANE));
        for (AuctionListing l : expiredListings) {
            entries.add(new InventoryEntry(InventoryEntry.Type.EXPIRED, l, null, null));
        }

        int totalEntries = entries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(page, 0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalEntries);

        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.GuiType.MY_LISTINGS);
        holder.setViewerUuid(uuid);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("My Listings"));

        for (int i = start; i < end; i++) {
            InventoryEntry entry = entries.get(i);
            int slot = i - start;
            switch (entry.type) {
                case SEPARATOR -> inv.setItem(slot, AuctionGuiUtil.sectionSeparator(entry.label, entry.material));
                case ACTIVE -> inv.setItem(slot, AuctionGuiUtil.createMyActiveItem(entry.listing));
                case SOLD -> inv.setItem(slot, AuctionGuiUtil.createMySoldItem(entry.listing));
                case EXPIRED -> inv.setItem(slot, AuctionGuiUtil.createMyExpiredItem(entry.listing));
            }
        }

        inv.setItem(SLOT_PREV, AuctionGuiUtil.prevPageItem(page > 0));
        inv.setItem(SLOT_BACK, AuctionGuiUtil.createControlItem(Material.ARROW, "Back to Browser", null));
        inv.setItem(SLOT_NEXT, AuctionGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, AuctionGuiUtil.closeItem());

        ItemStack myListingsFiller = AuctionGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, myListingsFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    private record InventoryEntry(Type type, AuctionListing listing, String label, Material material) {
        enum Type {
            SEPARATOR,
            ACTIVE,
            SOLD,
            EXPIRED
        }
    }
}

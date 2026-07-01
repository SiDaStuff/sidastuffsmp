package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.auction.AuctionSortMode;
import org.atrimilan.sidastuffsmp.utils.AnvilInput;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionGuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AuctionGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.getViewerUuid().equals(player.getUniqueId())) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        switch (holder.getGuiType()) {
            case BROWSER -> handleBrowserClick(player, holder, slot);
            case CONFIRM_PURCHASE -> handleConfirmPurchaseClick(player, holder, slot);
            case CONFIRM_CANCEL -> handleConfirmCancelClick(player, holder, slot);
            case CREATE -> handleCreateClick(player, holder, slot);
            case MY_LISTINGS -> handleMyListingsClick(player, holder, slot, event);
        }
    }

    private void handleBrowserClick(Player player, AuctionGuiHolder holder, int slot) {
        AuctionGuiHolder.BrowserState state = AuctionGuiHolder.getBrowserState(player.getUniqueId());

        if (slot == AuctionGuiUtil.SLOT_PREV_PAGE) {
            if (state.getPage() > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
                AuctionBrowserGui.open(player, state.getPage() - 1, state.getSortMode(), state.getSearchTerm());
            }
            return;
        }

        if (slot == AuctionGuiUtil.SLOT_NEXT_PAGE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
            AuctionBrowserGui.open(player, state.getPage() + 1, state.getSortMode(), state.getSearchTerm());
            return;
        }

        if (slot == AuctionGuiUtil.SLOT_SORT) {
            AuctionSortMode next = state.getSortMode().next();
            AuctionBrowserGui.open(player, state.getPage(), next, state.getSearchTerm());
            return;
        }

        if (slot == AuctionGuiUtil.SLOT_REFRESH) {
            AuctionBrowserGui.refresh(player);
            return;
        }

        if (slot == AuctionGuiUtil.SLOT_SEARCH) {
            AuctionGuiHolder.setPendingSearch(player.getUniqueId(), state.getPage());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Search Auction", "Type item name or material:",
                    state.getSearchTerm(),
                    text -> {
                        AuctionGuiHolder.consumePendingSearch(player.getUniqueId());
                        Bukkit.getScheduler().runTaskLater(
                                SiDaStuffSmp.getInstance(),
                                () -> AuctionBrowserGui.open(player, 0, AuctionSortMode.NEWEST, text),
                                2L);
                    },
                    () -> AuctionBrowserGui.open(player));
            return;
        }

        if (slot == AuctionGuiUtil.SLOT_MY_LISTINGS) {
            AuctionMyListingsGui.open(player, 0);
            return;
        }

        if (slot == AuctionGuiUtil.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < AuctionGuiUtil.ITEMS_PER_PAGE) {
            int offset = state.getPage() * AuctionGuiUtil.ITEMS_PER_PAGE;
            List<AuctionListing> listings = AuctionManager.getBrowserListings(
                    state.getSortMode(), null, state.getSearchTerm(),
                    offset, AuctionGuiUtil.ITEMS_PER_PAGE);
            if (slot < listings.size()) {
                int listingId = listings.get(slot).id();
                org.atrimilan.sidastuffsmp.sync.DataSync.refreshThenRun(player, "auction",
                        () -> {
                            if (!player.isOnline()) return;
                            AuctionListing fresh = AuctionManager.getListingById(listingId);
                            if (fresh == null || !"ACTIVE".equals(fresh.status())) {
                                player.sendMessage(Chat.prefixed(
                                        "That listing is no longer available.", NamedTextColor.RED));
                                player.playSound(player.getLocation(),
                                        org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                                AuctionBrowserGui.refresh(player);
                                return;
                            }
                            AuctionConfirmGui.openPurchaseConfirm(player, fresh.id());
                        });
            }
        }
    }

    private void handleConfirmPurchaseClick(Player player, AuctionGuiHolder holder, int slot) {
        if (slot == AuctionConfirmGui.SLOT_CONFIRM) {
            AuctionManager.PurchaseResult result = AuctionManager.purchaseListing(player, holder.getListingId());
            player.closeInventory();
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (result.success()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        } else if (slot == AuctionConfirmGui.SLOT_CANCEL) {
            player.closeInventory();
            AuctionBrowserGui.refresh(player);
        }
    }

    private void handleConfirmCancelClick(Player player, AuctionGuiHolder holder, int slot) {
        if (slot == AuctionConfirmGui.SLOT_CONFIRM) {
            AuctionManager.CancelResult result = AuctionManager.cancelListing(player, holder.getListingId());
            player.closeInventory();
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            AuctionMyListingsGui.open(player, 0);
        } else if (slot == AuctionConfirmGui.SLOT_CANCEL) {
            player.closeInventory();
            AuctionMyListingsGui.open(player, 0);
        }
    }

    private void handleMyListingsCollect(Player player, AuctionListing listing) {
        boolean success = false;
        switch (listing.status()) {
            case "SOLD" -> {
                AuctionManager.CollectResult result = AuctionManager.collectSoldMoney(player, listing.id());
                success = result.success();
                player.sendMessage(Chat.prefixed(result.message(), success ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            case "EXPIRED" -> {
                AuctionManager.CollectResult result = AuctionManager.collectExpiredItem(player, listing.id());
                success = result.success();
                player.sendMessage(Chat.prefixed(result.message(), success ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
        }
        if (success) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    private void handleCreateClick(Player player, AuctionGuiHolder holder, int slot) {
        if (slot == AuctionConfirmGui.SLOT_CONFIRM) {
            double price = AuctionCreateGui.getPendingPrice(player.getUniqueId());
            if (price <= 0) {
                player.closeInventory();
                player.sendMessage(Chat.prefixed("Invalid listing request. Use /ah sell <price>.", NamedTextColor.RED));
                return;
            }
            AuctionManager.ListResult result = AuctionManager.createListing(player, player.getInventory().getItemInMainHand(), price);
            player.closeInventory();
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (result.success()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        } else if (slot == AuctionConfirmGui.SLOT_CANCEL) {
            AuctionCreateGui.getPendingPrice(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(Chat.prefixed("Listing cancelled.", NamedTextColor.YELLOW));
        }
    }

    private void handleMyListingsClick(Player player, AuctionGuiHolder holder, int slot, InventoryClickEvent event) {
        if (slot == 45) {
            if (holder.getPage() > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
                AuctionMyListingsGui.open(player, holder.getPage() - 1);
            }
            return;
        }
        if (slot == 49) {
            AuctionBrowserGui.refresh(player);
            return;
        }
        if (slot == 52) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
            AuctionMyListingsGui.open(player, holder.getPage() + 1);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

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

            List<Object> entries = new ArrayList<>();
            entries.add(new Object());
            entries.addAll(activeListings);
            entries.add(new Object());
            entries.addAll(soldListings);
            entries.add(new Object());
            entries.addAll(expiredListings);

            int page = holder.getPage();
            int start = page * 45;
            int entryIndex = start + slot;

            if (entryIndex < entries.size()) {
                Object entry = entries.get(entryIndex);
                if (entry instanceof AuctionListing listing) {
                    switch (listing.status()) {
                        case "ACTIVE" -> AuctionConfirmGui.openCancelConfirm(player, listing.id());
                        case "SOLD" -> {
                            handleMyListingsCollect(player, listing);
                            AuctionMyListingsGui.open(player, page);
                        }
                        case "EXPIRED" -> {
                            handleMyListingsCollect(player, listing);
                            AuctionMyListingsGui.open(player, page);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        AnvilInput.cancel(event.getPlayer());
    }
}
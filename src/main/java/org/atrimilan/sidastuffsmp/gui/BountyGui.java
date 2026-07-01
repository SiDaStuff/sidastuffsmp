package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.bounty.BountyConfig;
import org.atrimilan.sidastuffsmp.bounty.BountyManager;
import org.atrimilan.sidastuffsmp.bounty.BountyManager.BountyEntry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountyGui {

    private BountyGui() {}

    public static Inventory open(Player player) {
        return open(player, 0, null);
    }

    public static Inventory open(Player player, int page, String searchTerm) {
        BountyGuiHolder.BrowserState state = BountyGuiHolder.getBrowserState(player.getUniqueId());
        state.setPage(page);
        state.setSearchTerm(searchTerm);

        int offset = page * BountyGuiUtil.ITEMS_PER_PAGE;
        List<BountyEntry> bounties = getFilteredBounties(searchTerm);
        int totalPages = (int) Math.ceil((double) bounties.size() / BountyGuiUtil.ITEMS_PER_PAGE);

        BountyGuiHolder holder = new BountyGuiHolder(BountyGuiHolder.GuiType.BOUNTY_BROWSER);
        holder.setViewerUuid(player.getUniqueId());
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, BountyGuiUtil.BROWSER_SIZE,
                Component.text("Bounty Board", NamedTextColor.GOLD));

        // Add bounty items
        int endIndex = Math.min(offset + BountyGuiUtil.ITEMS_PER_PAGE, bounties.size());
        for (int i = offset; i < endIndex; i++) {
            BountyEntry entry = bounties.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(entry.targetUuid());
            inv.setItem(i - offset, BountyGuiUtil.createBountyItem(target, entry.amount(), i + 1));
        }

        // Control bar
        inv.setItem(BountyGuiUtil.SLOT_PREV_PAGE, BountyGuiUtil.prevPageItem(page > 0));
        inv.setItem(BountyGuiUtil.SLOT_SEARCH, searchTerm != null && !searchTerm.isBlank()
                ? BountyGuiUtil.searchItemWithTerm(searchTerm) : BountyGuiUtil.searchItem());
        inv.setItem(BountyGuiUtil.SLOT_MY_BOUNTIES, BountyGuiUtil.myBountiesItem());
        inv.setItem(BountyGuiUtil.SLOT_REFRESH, BountyGuiUtil.refreshItem());
        inv.setItem(BountyGuiUtil.SLOT_NEXT_PAGE, BountyGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(BountyGuiUtil.SLOT_CLOSE, BountyGuiUtil.closeItem());

        ItemStack bountyFiller = BountyGuiUtil.fillerItem();
        for (int i = BountyGuiUtil.CONTROL_BAR_START; i < BountyGuiUtil.BROWSER_SIZE; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, bountyFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    public static Inventory openPlayerDetail(Player player, UUID targetUuid) {
        BountyGuiHolder holder = new BountyGuiHolder(BountyGuiHolder.GuiType.BOUNTY_DETAIL);
        holder.setViewerUuid(player.getUniqueId());
        holder.setTargetUuid(targetUuid);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        double bounty = BountyManager.getTotalBounty(targetUuid);

        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Bounty: " + target.getName(), NamedTextColor.GOLD));

        // Player head in center
        inv.setItem(13, BountyGuiUtil.createPlayerHeadItem(target, bounty));

        // Add/Remove buttons
        inv.setItem(11, BountyGuiUtil.addBountyItem(0));
        if (bounty > 0) {
            inv.setItem(15, BountyGuiUtil.removeBountyItem(0));
        }

        inv.setItem(18, BountyGuiUtil.createControlItem(Material.ARROW, "Back", List.of(
                Component.text("Click to go back", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        )));
        inv.setItem(26, BountyGuiUtil.closeItem());

        BountyGuiUtil.fillEmptySlots(inv, BountyGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    public static Inventory openMyBounties(Player player) {
        BountyGuiHolder holder = new BountyGuiHolder(BountyGuiHolder.GuiType.MY_BOUNTIES);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, BountyGuiUtil.BROWSER_SIZE,
                Component.text("My Bounties", NamedTextColor.GOLD));

        List<BountyEntry> bounties = BountyManager.getBountiesSetBy(player.getUniqueId(), BountyGuiUtil.ITEMS_PER_PAGE);
        for (int i = 0; i < bounties.size(); i++) {
            BountyEntry entry = bounties.get(i);
            inv.setItem(i, BountyGuiUtil.createBountyItem(Bukkit.getOfflinePlayer(entry.targetUuid()), entry.amount(), i + 1));
        }

        inv.setItem(BountyGuiUtil.SLOT_PREV_PAGE, BountyGuiUtil.createControlItem(Material.ARROW, "Back", List.of(
                Component.text("Return to bounty board", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        inv.setItem(BountyGuiUtil.SLOT_CLOSE, BountyGuiUtil.closeItem());

        ItemStack myBountiesFiller = BountyGuiUtil.fillerItem();
        for (int i = BountyGuiUtil.CONTROL_BAR_START; i < BountyGuiUtil.BROWSER_SIZE; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, myBountiesFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    public static Inventory openAddBounty(Player player, UUID targetUuid) {
        BountyGuiHolder holder = new BountyGuiHolder(BountyGuiHolder.GuiType.ADD_BOUNTY);
        holder.setViewerUuid(player.getUniqueId());
        holder.setTargetUuid(targetUuid);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Add Bounty: " + target.getName(), NamedTextColor.GOLD));

        int[] slots = {10, 12, 14, 16};
        List<Double> presets = BountyConfig.presetAmounts();
        for (int i = 0; i < slots.length && i < presets.size(); i++) {
            inv.setItem(slots[i], BountyGuiUtil.addBountyItem(presets.get(i)));
        }
        inv.setItem(18, BountyGuiUtil.createControlItem(Material.ARROW, "Back", List.of(
                Component.text("Return to details", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        inv.setItem(26, BountyGuiUtil.closeItem());

        BountyGuiUtil.fillEmptySlots(inv, BountyGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    public static Inventory openConfirmAdd(Player player, UUID targetUuid, double amount) {
        BountyGuiHolder holder = new BountyGuiHolder(BountyGuiHolder.GuiType.CONFIRM_ADD);
        holder.setViewerUuid(player.getUniqueId());
        holder.setTargetUuid(targetUuid);
        holder.setPendingAmount(amount);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text(BountyConfig.message("confirm-title", "Confirm Bounty"), NamedTextColor.GOLD));
        inv.setItem(13, BountyGuiUtil.createPlayerHeadItem(target, BountyManager.getTotalBounty(targetUuid)));
        inv.setItem(11, BountyGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE, "Confirm " + BountyManager.formatPrice(amount), List.of(
                Component.text("Place bounty on " + target.getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))));
        inv.setItem(15, BountyGuiUtil.createControlItem(Material.RED_STAINED_GLASS_PANE, "Cancel", List.of(
                Component.text("Return without placing bounty", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));

        BountyGuiUtil.fillEmptySlots(inv, BountyGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    private static List<BountyEntry> getFilteredBounties(String searchTerm) {
        List<BountyEntry> all = BountyManager.getTopBounties(100);
        if (searchTerm == null || searchTerm.isBlank()) {
            return all;
        }
        String term = searchTerm.toLowerCase();
        List<BountyEntry> filtered = new ArrayList<>();
        for (BountyEntry entry : all) {
            if (entry.targetName().toLowerCase().contains(term)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
}

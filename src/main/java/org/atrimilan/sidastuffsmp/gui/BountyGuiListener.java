package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.bounty.BountyConfig;
import org.atrimilan.sidastuffsmp.bounty.BountyManager;
import org.atrimilan.sidastuffsmp.utils.AnvilInput;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class BountyGuiListener implements Listener {

    public BountyGuiListener() {}

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof BountyGuiHolder holder)) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        switch (holder.getGuiType()) {
            case BOUNTY_BROWSER -> handleBrowserClick(player, holder, slot);
            case BOUNTY_DETAIL -> handleDetailClick(player, holder, slot);
            case MY_BOUNTIES -> handleMyBountiesClick(player, slot);
            case ADD_BOUNTY -> handleAddBountyClick(player, holder, slot);
            case CONFIRM_ADD -> handleConfirmAddClick(player, holder, slot);
            case REMOVE_BOUNTY -> {}
        }
    }

    private void handleBrowserClick(Player player, BountyGuiHolder holder, int slot) {
        if (slot == BountyGuiUtil.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot == BountyGuiUtil.SLOT_PREV_PAGE) {
            int newPage = holder.getPage() - 1;
            if (newPage >= 0) {
                BountyGui.open(player, newPage, holder.getViewerUuid() != null
                        ? BountyGuiHolder.getBrowserState(holder.getViewerUuid()).getSearchTerm() : null);
            }
            return;
        }

        if (slot == BountyGuiUtil.SLOT_NEXT_PAGE) {
            int newPage = holder.getPage() + 1;
            BountyGui.open(player, newPage, holder.getViewerUuid() != null
                    ? BountyGuiHolder.getBrowserState(holder.getViewerUuid()).getSearchTerm() : null);
            return;
        }

        if (slot == BountyGuiUtil.SLOT_SEARCH) {
            String currentTerm = holder.getViewerUuid() != null
                    ? BountyGuiHolder.getBrowserState(holder.getViewerUuid()).getSearchTerm() : null;
            AnvilInput.request(player, "Search Bounties", "Type player name:",
                    currentTerm,
                    text -> {
                        Bukkit.getScheduler().runTaskLater(
                                org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance(),
                                () -> BountyGui.open(player, 0, text), 2L);
                    },
                    () -> BountyGui.open(player));
            return;
        }

        if (slot == BountyGuiUtil.SLOT_MY_BOUNTIES) {
            BountyGui.openMyBounties(player);
            return;
        }

        if (slot == BountyGuiUtil.SLOT_REFRESH) {
            BountyGui.open(player, holder.getPage(), holder.getViewerUuid() != null
                    ? BountyGuiHolder.getBrowserState(holder.getViewerUuid()).getSearchTerm() : null);
            return;
        }

        // Clicked on a bounty item (slots 0-44)
        if (slot >= 0 && slot < BountyGuiUtil.ITEMS_PER_PAGE) {
            int index = holder.getPage() * BountyGuiUtil.ITEMS_PER_PAGE + slot;
            java.util.List<BountyManager.BountyEntry> bounties = BountyManager.getTopBounties(100);
            if (index < bounties.size()) {
                BountyManager.BountyEntry entry = bounties.get(index);
                BountyGui.openPlayerDetail(player, entry.targetUuid());
            }
        }
    }

    private void handleMyBountiesClick(Player player, int slot) {
        if (slot == BountyGuiUtil.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == BountyGuiUtil.SLOT_PREV_PAGE) {
            BountyGui.open(player);
            return;
        }
        if (slot >= 0 && slot < BountyGuiUtil.ITEMS_PER_PAGE) {
            java.util.List<BountyManager.BountyEntry> bounties = BountyManager.getBountiesSetBy(player.getUniqueId(), 100);
            if (slot < bounties.size()) {
                BountyGui.openPlayerDetail(player, bounties.get(slot).targetUuid());
            }
        }
    }

    private void handleDetailClick(Player player, BountyGuiHolder holder, int slot) {
        UUID targetUuid = holder.getTargetUuid();
        if (targetUuid == null) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        double currentBounty = BountyManager.getTotalBounty(targetUuid);

        if (slot == 26) { // Close button
            player.closeInventory();
            return;
        }

        if (slot == 18) { // Back button
            BountyGui.open(player);
            return;
        }

        if (slot == 11) { // Add bounty button
            if (!BountyManager.hasEconomy()) {
                player.sendMessage(Chat.prefixed("Economy system is not available. Bounties require an economy plugin.", NamedTextColor.RED));
                return;
            }
            BountyGui.openAddBounty(player, targetUuid);
            return;
        }

        if (slot == 15 && currentBounty > 0) { // Remove bounty button
            // Remove all of this player's bounties on target
            boolean success = BountyManager.removeBounty(player, target, currentBounty);
            if (success) {
                player.sendMessage(Chat.prefixed("Removed your bounty on " + target.getName() + "!", NamedTextColor.GREEN));
                BountyGui.openPlayerDetail(player, targetUuid);
            } else {
                player.sendMessage(Chat.prefixed("You don't have an active bounty on " + target.getName() + ".", NamedTextColor.RED));
            }
            return;
        }
    }

    private void handleAddBountyClick(Player player, BountyGuiHolder holder, int slot) {
        UUID targetUuid = holder.getTargetUuid();
        if (targetUuid == null) return;
        if (slot == 26) {
            player.closeInventory();
            return;
        }
        if (slot == 18) {
            BountyGui.openPlayerDetail(player, targetUuid);
            return;
        }

        int[] slots = {10, 12, 14, 16};
        java.util.List<Double> presets = BountyConfig.presetAmounts();
        for (int i = 0; i < slots.length && i < presets.size(); i++) {
            if (slot == slots[i]) {
                BountyGui.openConfirmAdd(player, targetUuid, presets.get(i));
                return;
            }
        }
    }

    private void handleConfirmAddClick(Player player, BountyGuiHolder holder, int slot) {
        UUID targetUuid = holder.getTargetUuid();
        if (targetUuid == null) return;
        if (slot == 15) {
            BountyGui.openAddBounty(player, targetUuid);
            return;
        }
        if (slot != 11) return;

        if (!BountyManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available. Bounties require an economy plugin.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        // Check cooldown using BountyManager
        long remainingCooldown = BountyManager.getBountyCooldownRemaining(player.getUniqueId());
        if (remainingCooldown > 0) {
            player.sendMessage(Chat.prefixed(BountyConfig.message("cooldown", "Please wait before placing another bounty."), NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        double amount = holder.getPendingAmount();
        double total = BountyManager.addBounty(player, target, amount);
        if (total > 0) {
            player.sendMessage(Chat.prefixed(BountyConfig.message("created", "Bounty placed successfully."), NamedTextColor.GREEN));
            BountyGui.openPlayerDetail(player, targetUuid);
        }
    }
}

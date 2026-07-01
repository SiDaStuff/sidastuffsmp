package org.atrimilan.sidastuffsmp.sus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class SusGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SusDetailGuiHolder detailHolder) {
            handleDetailClick(event, detailHolder);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof SusGuiHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        int page = holder.getPage();
        int itemsPerPage = 45;

        if (slot == 45 && page > 0) {
            SusCommand.openSusGui(viewer, page - 1);
            return;
        }

        if (slot == 53) {
            SusCommand.openSusGui(viewer, page + 1);
            return;
        }

        if (slot == 49) {
            viewer.closeInventory();
            return;
        }

        if (slot < itemsPerPage) {
            var item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            if (item.getType() != Material.PLAYER_HEAD && item.getType() != Material.PAPER) return;

            int offset = page * itemsPerPage + slot;
            List<SusManager.SusEntry> entries = SusManager.getAllEntries();
            if (offset >= entries.size()) return;

            SusManager.SusEntry entry = entries.get(offset);
            SusDetailGui.open(viewer, entry.playerName(), 0);
        }
    }

    private void handleDetailClick(InventoryClickEvent event, SusDetailGuiHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        String playerName = holder.getPlayerName();
        int page = holder.getPage();

        if (slot == SusDetailGui.SLOT_BACK) {
            SusCommand.openSusGui(viewer, 0);
            return;
        }

        if (slot == SusDetailGui.SLOT_PREV && page > 0) {
            SusDetailGui.open(viewer, playerName, page - 1);
            return;
        }

        if (slot == SusDetailGui.SLOT_NEXT) {
            SusDetailGui.open(viewer, playerName, page + 1);
            return;
        }

        if (slot == SusDetailGui.SLOT_BROADCAST) {
            SusDetailGui.broadcastFlags(viewer, playerName);
            return;
        }
    }
}

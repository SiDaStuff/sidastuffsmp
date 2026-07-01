package org.atrimilan.sidastuffsmp.rtp;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class RtpGuiListener implements Listener {

    private static final int SLOT_OVERWORLD = 11;
    private static final int SLOT_NETHER = 13;
    private static final int SLOT_END = 15;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RtpGui.RtpGuiHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        RtpConfig.RegionConfig region = null;
        String dimensionName = null;

        if (slot == SLOT_OVERWORLD) {
            region = RtpConfig.getRegion("overworld-region");
            dimensionName = "Overworld";
        } else if (slot == SLOT_NETHER) {
            region = RtpConfig.getRegion("nether-region");
            dimensionName = "Nether";
        } else if (slot == SLOT_END) {
            region = RtpConfig.getRegion("end-region");
            dimensionName = "End";
        }

        if (region != null && dimensionName != null) {
            player.closeInventory();
            player.sendMessage(net.kyori.adventure.text.Component.text("Teleporting to " + dimensionName + "...", net.kyori.adventure.text.format.NamedTextColor.GRAY));
            RtpManager.startCountdown(player, region, false);
        }
    }
}
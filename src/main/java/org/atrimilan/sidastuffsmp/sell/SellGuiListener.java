package org.atrimilan.sidastuffsmp.sell;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SellGuiListener implements Listener {

    /** Slots in the bottom row that are reserved for control buttons. */
    private static final int SLOT_CLOSE = 45;
    private static final int SLOT_SELL = 53;

    private static final Map<UUID, Location> playerSignLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerSearchTerms = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGuiHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        boolean inTop = slot >= 0 && slot < event.getInventory().getSize();

        // Block any interaction with the bottom row control slots.
        if (inTop && (slot == SLOT_CLOSE || slot == SLOT_SELL)) {
            event.setCancelled(true);
            if (slot == SLOT_SELL) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                SellCommand.confirmSale(player, event.getInventory());
            } else if (slot == SLOT_CLOSE) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                player.closeInventory();
            }
            return;
        }

        // Allow freely taking and moving items in the top 5 rows (slots 0-44)
        // Only block interactions with the bottom row control slots
        if (inTop && slot >= 0 && slot < 45) {
            // Allow all interactions in the top 5 rows
            // Players can freely move items around and take them back
            return;
        }

        // Items in the bottom row (outside the control slots) cannot be placed there.
        if (inTop && slot >= 45) {
            event.setCancelled(true);
            return;
        }

        // Click in the player inventory: allow normal interaction.
        event.setCancelled(false);
    }

    /** Prevent dragging items into the bottom-row control slots. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGuiHolder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot == SLOT_CLOSE || slot == SLOT_SELL) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGuiHolder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        playerSearchTerms.remove(uuid);
        Location signLoc = playerSignLocations.remove(uuid);
        if (signLoc != null) {
            Bukkit.getScheduler().runTask(SiDaStuffSmp.getInstance(), () -> {
                if (signLoc.getBlock().getType() == Material.OAK_SIGN) {
                    signLoc.getBlock().setType(Material.AIR);
                }
            });
        }

        // Sell all items in the GUI when closed
        SellCommand.sellAllOnClose(player, event.getInventory());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        SellCommand.playerSelectedItems.remove(uuid);
        SellCommand.playerPendingTotal.remove(uuid);
        SellCommand.playerPricePerUnit.remove(uuid);
        SellCommand.playerEmptyShulkers.remove(uuid);
        playerSearchTerms.remove(uuid);
        Location signLoc = playerSignLocations.remove(uuid);
        if (signLoc != null) {
            Bukkit.getScheduler().runTask(SiDaStuffSmp.getInstance(), () -> {
                if (signLoc.getBlock().getType() == Material.OAK_SIGN) {
                    signLoc.getBlock().setType(Material.AIR);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!playerSearchTerms.containsKey(player.getUniqueId())) return;

        String line0 = PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim();
        String searchTerm;
        if (line0.isEmpty() || line0.equals("Type item name")) {
            searchTerm = null;
            playerSearchTerms.remove(player.getUniqueId());
        } else {
            searchTerm = line0;
            playerSearchTerms.put(player.getUniqueId(), searchTerm);
        }

        Location signLoc = playerSignLocations.remove(player.getUniqueId());
        if (signLoc != null) {
            Bukkit.getScheduler().runTask(SiDaStuffSmp.getInstance(), () -> {
                if (signLoc.getBlock().getType() == Material.OAK_SIGN) {
                    signLoc.getBlock().setType(Material.AIR);
                }
            });
        }

        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
            SellCommand.openSellGui(player);
        }, 2L);
    }
}
package org.atrimilan.sidastuffsmp.sync;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Coordinates a fresh "pull from Firebase + reload local database" cycle before any action
 * that depends on up-to-date listing data (buying an auction listing, fulfilling an order,
 * opening details for a listing/order). This is what makes the auction house and order
 * marketplace actually reflect the latest state of the world across server instances.
 */
public final class DataSync {

    private static final java.util.Map<java.util.UUID, Long> LAST_REFRESH = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long REFRESH_COOLDOWN_MS = 750;

    private DataSync() {}

    /**
     * Run a forced pull from Firebase (if enabled) and then re-open the target GUI.
     * The action is performed asynchronously and the GUI is rebuilt on the main thread once
     * the local SQLite database has caught up.
     */
    public static void refreshThenRun(Player player, String actionName, Runnable after) {
        SiDaStuffSmp plugin = SiDaStuffSmp.getInstance();
        if (plugin == null) {
            if (after != null) after.run();
            return;
        }

        long now = System.currentTimeMillis();
        Long last = LAST_REFRESH.get(player.getUniqueId());
        if (last != null && (now - last) < REFRESH_COOLDOWN_MS) {
            if (after != null) after.run();
            return;
        }
        LAST_REFRESH.put(player.getUniqueId(), now);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (AuctionFirebaseSync.isEnabled()) {
                    try {
                        AuctionFirebaseSync.forceSync();
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.WARNING,
                                "Auction sync pull during refresh failed: " + t.getMessage(), t);
                    }
                }
                if (OrderFirebaseSync.isEnabled()) {
                    try {
                        OrderFirebaseSync.forceSync();
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.WARNING,
                                "Order sync pull during refresh failed: " + t.getMessage(), t);
                    }
                }
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(Chat.prefixed("Refreshed " + actionName + ".", net.kyori.adventure.text.format.NamedTextColor.GRAY));
                    }
                    if (after != null) {
                        try {
                            after.run();
                        } catch (Throwable t) {
                            plugin.getLogger().log(Level.WARNING,
                                    "DataSync post-refresh action failed: " + t.getMessage(), t);
                        }
                    }
                });
            }
        });
    }
}

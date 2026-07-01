package org.atrimilan.sidastuffsmp.stats;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsListener implements Listener {

    private static final Map<UUID, Long> SESSION_START = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerStatsManager.ensurePlayer(uuid.toString(), player.getName());
        SESSION_START.put(uuid, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Long start = SESSION_START.remove(uuid);
        if (start != null) {
            int seconds = (int) ((System.currentTimeMillis() - start) / 1000);
            if (seconds > 0) {
                PlayerStatsManager.updatePlaytime(uuid, seconds);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        PlayerStatsManager.incrementDeaths(victim.getUniqueId());

        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            PlayerStatsManager.incrementKills(killer.getUniqueId());
        }
    }

    public static void saveAllPlaytime() {
        for (Map.Entry<UUID, Long> entry : SESSION_START.entrySet()) {
            UUID uuid = entry.getKey();
            long start = entry.getValue();
            int seconds = (int) ((System.currentTimeMillis() - start) / 1000);
            if (seconds > 0) {
                PlayerStatsManager.updatePlaytime(uuid, seconds);
            }
        }
        SESSION_START.clear();
    }

    public static void updateSessions() {
        long now = System.currentTimeMillis();
        java.util.List<UUID> toUpdate = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Long> entry : SESSION_START.entrySet()) {
            long start = entry.getValue();
            int seconds = (int) ((now - start) / 1000);
            if (seconds >= 60) {
                toUpdate.add(entry.getKey());
            }
        }
        for (UUID uuid : toUpdate) {
            PlayerStatsManager.updatePlaytime(uuid, 60);
            SESSION_START.put(uuid, now);
        }
    }
}
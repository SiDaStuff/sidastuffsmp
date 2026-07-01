package org.atrimilan.sidastuffsmp.sus;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.UUID;

public class SusPluginCommandListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase(java.util.Locale.ROOT);
        checkAndIntercept(message, event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String message = event.getCommand().toLowerCase(java.util.Locale.ROOT);
        if (message.startsWith("/")) message = message.substring(1);
        checkAndIntercept(message, "Console");
    }

    private void checkAndIntercept(String message, String sourceName) {
        if (message.startsWith("/sus add ") || message.startsWith("sus add ")) {
            String[] parts = message.split("\\s+", 4);
            if (parts.length < 4) return;
            String playerName = parts[2];
            String reason = parts[3];
            addPlayerToSus(playerName, reason, sourceName);
            return;
        }

        if (message.startsWith("/flaggeradd ") || message.startsWith("flaggeradd ")) {
            String[] parts = message.split("\\s+", 4);
            if (parts.length < 4) return;
            String playerName = parts[2];
            String reason = parts[3];
            addPlayerToSus(playerName, reason, sourceName);
        }
    }

    private void addPlayerToSus(String playerName, String reason, String source) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            SusManager.addPlayer(online.getUniqueId(), online.getName(), reason, source);
            return;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        SusManager.addPlayer(offline.getUniqueId(), playerName, reason, source);
    }
}

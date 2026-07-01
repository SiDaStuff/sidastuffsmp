package org.atrimilan.sidastuffsmp.listeners;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.commands.WarnCommand;
import org.atrimilan.sidastuffsmp.utils.WarnManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class WarningJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> warnings = WarnManager.getWarnings(player.getUniqueId());
        if (warnings.isEmpty()) {
            return;
        }

        for (int i = 0; i < warnings.size(); i++) {
            String warning = warnings.get(i);
            long delayTicks = i * 80L;
            Bukkit.getScheduler().runTaskLater(
                    SiDaStuffSmp.getInstance(),
                    () -> WarnCommand.showWarningPopup(player, warning),
                    delayTicks
            );
        }

        WarnManager.clearWarnings(player.getUniqueId());
    }
}

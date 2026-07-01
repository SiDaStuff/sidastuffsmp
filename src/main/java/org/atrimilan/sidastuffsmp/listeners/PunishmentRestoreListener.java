package org.atrimilan.sidastuffsmp.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.commands.PunishmentCommands;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.PunishmentManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PunishmentRestoreListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!PunishmentManager.isRestorePending(event.getPlayer().getUniqueId())) {
            return;
        }

        PunishmentCommands.RestoreResult result = PunishmentCommands.restoreSnapshotToOnlinePlayer(event.getPlayer());
        if (!result.restored()) {
            return;
        }

        event.getPlayer().sendMessage(Chat.prefixed("Your staff restore has been applied.", NamedTextColor.GREEN));
        if (!result.balanceRestored()) {
            event.getPlayer().sendMessage(Chat.prefixed("Your inventory and ender chest were restored, but your balance could not be restored.", NamedTextColor.YELLOW));
        } else {
            event.getPlayer().sendMessage(Chat.prefixed("Your balance has also been restored.", NamedTextColor.GREEN));
        }
        if (result.statsRestored()) {
            event.getPlayer().sendMessage(Chat.prefixed("Your stats have also been restored.", NamedTextColor.GREEN));
        }
    }
}

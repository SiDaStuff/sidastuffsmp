package org.atrimilan.sidastuffsmp.listeners;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinMessageListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ConfigManager.isJoinMessageEnabled()) {
            return;
        }

        String message = ConfigManager.getJoinMessageText();
        if (message == null || message.isBlank()) {
            return;
        }

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
            Book book = Book.book(
                    Component.text("SiDaStuff SMP", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true),
                    Component.text("SiDaStuff SMP"),
                    Component.text(message, NamedTextColor.BLACK)
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("Click Done to close.", NamedTextColor.DARK_GRAY))
            );
            player.openBook(book);
        }, 20L);
    }
}

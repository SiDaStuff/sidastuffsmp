package org.atrimilan.sidastuffsmp.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tiny helper that lets SignInput capture a single chat message without
 * registering a global chat listener on every plugin. Only one player is ever
 * waiting at a time and the wait is automatically cleared on quit.
 *
 * This is intentionally NOT exposed as a public API – all chat-driven flows
 * in the plugin go through {@link SignInput} which presents the captured text
 * inside its virtual sign GUI.
 */
final class ChatInput implements Listener {

    private static final Map<UUID, Consumer<String>> WAITING = new HashMap<>();
    private static ChatInput INSTANCE;

    private ChatInput() {}

    static void init() {
        if (INSTANCE == null) {
            INSTANCE = new ChatInput();
            Bukkit.getPluginManager().registerEvents(INSTANCE,
                    org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance());
        }
    }

    static void await(Player player, Consumer<String> onMessage, Runnable onCancel) {
        init();
        UUID uuid = player.getUniqueId();
        cancel(uuid);
        WAITING.put(uuid, msg -> {
            try {
                onMessage.accept(msg);
            } finally {
                WAITING.remove(uuid);
            }
        });
    }

    static boolean isWaiting(UUID uuid) {
        return WAITING.containsKey(uuid);
    }

    static void cancel(UUID uuid) {
        WAITING.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Consumer<String> consumer = WAITING.remove(uuid);
        if (consumer == null) return;

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText()
                .serialize(event.originalMessage()).trim();
        // Schedule the consumer on the main thread so we can safely touch the
        // player / inventory API.
        Bukkit.getScheduler().runTask(org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance(),
                () -> consumer.accept(text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }
}
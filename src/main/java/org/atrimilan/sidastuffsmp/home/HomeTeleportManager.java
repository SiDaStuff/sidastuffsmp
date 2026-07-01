package org.atrimilan.sidastuffsmp.home;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HomeTeleportManager implements Listener {

    private static final Map<UUID, HomeTeleportTask> activeTeleports = new HashMap<>();
    private static int seconds;

    static {
        seconds = HomeConfig.teleportDelaySeconds();
    }

    public HomeTeleportManager() {
        // public no-arg constructor for listener registration
    }

    public static void reload() {
        seconds = HomeConfig.teleportDelaySeconds();
    }

    public static void teleportToHome(Player player, Home home) {
        if (seconds <= 0) {
            doTeleport(player, home);
            return;
        }
        UUID uuid = player.getUniqueId();
        if (activeTeleports.containsKey(uuid)) {
            player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("You already have a pending teleport!", NamedTextColor.RED));
            return;
        }

        HomeTeleportTask task = new HomeTeleportTask(player, home, seconds);
        activeTeleports.put(uuid, task);
        task.start();
    }

    public static void cancelTeleport(UUID uuid) {
        HomeTeleportTask task = activeTeleports.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static boolean hasActiveTeleport(UUID uuid) {
        return activeTeleports.containsKey(uuid);
    }

    private static void doTeleport(Player player, Home home) {
        Location loc = home.toLocation(null);
        if (loc == null) {
            player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Home world is not loaded!", NamedTextColor.RED));
            return;
        }
        player.teleport(loc);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Teleported to home '" + home.getName() + "'!", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!HomeConfig.cancelOnMove()) return;
        Player player = event.getPlayer();
        if (!activeTeleports.containsKey(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
            cancelTeleport(player.getUniqueId());
            player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Teleport cancelled - you moved!", NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendActionBar(Component.text("Teleport cancelled", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!HomeConfig.cancelOnDamage()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!activeTeleports.containsKey(player.getUniqueId())) return;
        cancelTeleport(player.getUniqueId());
        player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Teleport cancelled - you took damage!", NamedTextColor.RED));
        player.sendActionBar(Component.text("Teleport cancelled", NamedTextColor.RED));
    }

    private static class HomeTeleportTask {
        private final Player player;
        private final Home home;
        private final int delaySeconds;
        private BukkitRunnable runnable;
        private boolean cancelled = false;

        HomeTeleportTask(Player player, Home home, int delaySeconds) {
            this.player = player;
            this.home = home;
            this.delaySeconds = delaySeconds;
        }

        void start() {
            player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Teleporting to '" + home.getName() + "' in " + delaySeconds + " seconds... Don't move!", NamedTextColor.GRAY));
            runnable = new BukkitRunnable() {
                int remaining = delaySeconds;
                @Override
                public void run() {
                    if (cancelled || !player.isOnline()) {
                        cancel();
                        activeTeleports.remove(player.getUniqueId());
                        return;
                    }
                    if (remaining <= 0) {
                        doTeleport(player, home);
                        activeTeleports.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }
                    player.sendActionBar(Component.text("Teleporting in " + remaining + "...", NamedTextColor.GRAY));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                    remaining--;
                }
            };
            runnable.runTaskTimer(SiDaStuffSmp.getInstance(), 0L, 20L);
        }

        void cancel() {
            cancelled = true;
            if (runnable != null) runnable.cancel();
            activeTeleports.remove(player.getUniqueId());
        }
    }
}

package org.atrimilan.sidastuffsmp.teleport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private static final long REQUEST_TIMEOUT_MS = 30_000;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final double MOVE_THRESHOLD = 0.5;

    private static final Map<UUID, List<TeleportRequest>> pendingRequests = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> countdownStartLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> countdownOtherLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> countdownOtherUuids = new ConcurrentHashMap<>();

    private TeleportManager() {}

    public static TeleportResult sendTpa(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            return new TeleportResult(false, "You cannot teleport to yourself!");
        }

        PlayerSettings settings = PlayerSettings.get(target.getUniqueId());
        if (!settings.isTpaEnabled()) {
            return new TeleportResult(false, target.getName() + " has disabled TPA requests.");
        }

        if (countdownTasks.containsKey(sender.getUniqueId())) {
            return new TeleportResult(false, "You already have a teleport in progress.");
        }

        List<TeleportRequest> existing = pendingRequests.getOrDefault(target.getUniqueId(), new ArrayList<>());
        for (TeleportRequest req : existing) {
            if (req.senderUuid().equals(sender.getUniqueId())) {
                return new TeleportResult(false, "You already have a pending request to " + target.getName() + ".");
            }
        }

        TeleportRequest request = new TeleportRequest(
                sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), target.getName(),
                TeleportRequest.Type.TPA,
                System.currentTimeMillis()
        );

        pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(request);
        scheduleRequestExpiry(target.getUniqueId(), request);

        return new TeleportResult(true, "Teleport request sent to " + target.getName() + ".", request);
    }

    public static TeleportResult sendTpaHere(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            return new TeleportResult(false, "You cannot teleport to yourself!");
        }

        PlayerSettings settings = PlayerSettings.get(target.getUniqueId());
        if (!settings.isTpaHereEnabled()) {
            return new TeleportResult(false, target.getName() + " has disabled TPAhere requests.");
        }

        if (countdownTasks.containsKey(sender.getUniqueId())) {
            return new TeleportResult(false, "You already have a teleport in progress.");
        }

        List<TeleportRequest> existing = pendingRequests.getOrDefault(target.getUniqueId(), new ArrayList<>());
        for (TeleportRequest req : existing) {
            if (req.senderUuid().equals(sender.getUniqueId())) {
                return new TeleportResult(false, "You already have a pending request to " + target.getName() + ".");
            }
        }

        TeleportRequest request = new TeleportRequest(
                sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), target.getName(),
                TeleportRequest.Type.TPAHERE,
                System.currentTimeMillis()
        );

        pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(request);
        scheduleRequestExpiry(target.getUniqueId(), request);

        return new TeleportResult(true, "Teleport-here request sent to " + target.getName() + ".", request);
    }

    public static TeleportResult acceptRequest(Player target) {
        TeleportRequest request = removeLatestPendingRequest(target.getUniqueId());
        if (request == null) {
            return new TeleportResult(false, "You have no pending teleport requests.");
        }

        Player sender = Bukkit.getPlayer(request.senderUuid());
        if (sender == null || !sender.isOnline()) {
            return new TeleportResult(false, "The player who sent the request is no longer online.");
        }

        if (countdownTasks.containsKey(sender.getUniqueId())) {
            return new TeleportResult(false, "Sender already has a teleport in progress.");
        }

        if (request.type() == TeleportRequest.Type.TPA) {
            startCountdown(sender, sender.getLocation(), target.getLocation(), target, "Teleporting to " + target.getName() + "...");
        } else {
            startCountdown(target, target.getLocation(), sender.getLocation(), sender, target.getName() + " is teleporting to you...");
        }

        return new TeleportResult(true, "Teleport request accepted!");
    }

    public static TeleportResult denyRequest(Player target) {
        TeleportRequest request = removeLatestPendingRequest(target.getUniqueId());
        if (request == null) {
            return new TeleportResult(false, "You have no pending teleport requests.");
        }

        Player sender = Bukkit.getPlayer(request.senderUuid());
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(Chat.prefixed(target.getName() + " denied your teleport request.", NamedTextColor.RED));
        }

        return new TeleportResult(true, "Teleport request denied.");
    }

    public static TeleportRequest getLatestPendingRequest(UUID targetUuid) {
        List<TeleportRequest> requests = getValidPendingRequests(targetUuid);
        if (requests.isEmpty()) return null;
        return requests.get(requests.size() - 1);
    }

    public static List<TeleportRequest> getPendingRequestsForTarget(UUID targetUuid) {
        return getValidPendingRequests(targetUuid);
    }

    public static TeleportRequest getPendingRequestFromSender(UUID targetUuid, String senderName) {
        List<TeleportRequest> requests = getValidPendingRequests(targetUuid);
        for (TeleportRequest req : requests) {
            if (req.senderName().equalsIgnoreCase(senderName)) {
                return req;
            }
        }
        return null;
    }

    private static List<TeleportRequest> getValidPendingRequests(UUID targetUuid) {
        List<TeleportRequest> requests = pendingRequests.get(targetUuid);
        if (requests == null) return new ArrayList<>();

        long now = System.currentTimeMillis();
        requests.removeIf(req -> now - req.createdAt() > REQUEST_TIMEOUT_MS);
        if (requests.isEmpty()) {
            pendingRequests.remove(targetUuid);
        }
        return new ArrayList<>(requests);
    }

    private static TeleportRequest removeLatestPendingRequest(UUID targetUuid) {
        List<TeleportRequest> requests = getValidPendingRequests(targetUuid);
        if (requests.isEmpty()) return null;

        TeleportRequest latest = requests.get(requests.size() - 1);

        List<TeleportRequest> current = pendingRequests.get(targetUuid);
        if (current != null) {
            current.remove(latest);
            if (current.isEmpty()) {
                pendingRequests.remove(targetUuid);
            }
        }

        return latest;
    }

    public static void cancelCountdown(UUID playerUuid) {
        BukkitTask task = countdownTasks.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
        countdownStartLocations.remove(playerUuid);
        countdownOtherLocations.remove(playerUuid);
        countdownOtherUuids.remove(playerUuid);
    }

    public static boolean hasActiveCountdown(UUID playerUuid) {
        return countdownTasks.containsKey(playerUuid);
    }

    private static void startCountdown(Player player, Location from, Location to, Player otherPlayer, String message) {
        countdownStartLocations.put(player.getUniqueId(), from.clone());
        if (otherPlayer != null) {
            countdownOtherLocations.put(player.getUniqueId(), otherPlayer.getLocation().clone());
            countdownOtherUuids.put(player.getUniqueId(), otherPlayer.getUniqueId());
        }

        final UUID playerUuid = player.getUniqueId();

        BukkitRunnable runnable = new BukkitRunnable() {
            int secondsLeft = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCountdown(playerUuid);
                    cancel();
                    return;
                }

                Location startLoc = countdownStartLocations.get(playerUuid);
                if (startLoc != null && player.getLocation().distanceSquared(startLoc) > MOVE_THRESHOLD * MOVE_THRESHOLD) {
                    player.sendMessage(Chat.prefixed("Teleport cancelled \u2014 you moved!", NamedTextColor.RED));
                    player.sendActionBar(Component.text("Teleport cancelled!", NamedTextColor.RED));
                    cancelCountdown(playerUuid);
                    cancel();
                    return;
                }

                Location otherStartLoc = countdownOtherLocations.get(playerUuid);
                UUID otherUuid = countdownOtherUuids.get(playerUuid);
                if (otherStartLoc != null && otherUuid != null) {
                    Player other = Bukkit.getPlayer(otherUuid);
                    if (other == null || !other.isOnline()) {
                        player.sendMessage(Chat.prefixed("Teleport cancelled \u2014 other player disconnected!", NamedTextColor.RED));
                        cancelCountdown(playerUuid);
                        cancel();
                        return;
                    }
                    if (other.getLocation().distanceSquared(otherStartLoc) > MOVE_THRESHOLD * MOVE_THRESHOLD) {
                        player.sendMessage(Chat.prefixed("Teleport cancelled \u2014 other player moved!", NamedTextColor.RED));
                        player.sendActionBar(Component.text("Teleport cancelled!", NamedTextColor.RED));
                        cancelCountdown(playerUuid);
                        cancel();
                        return;
                    }
                }

                if (secondsLeft <= 0) {
                    player.teleportAsync(to).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Chat.prefixed("Teleported!", NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        }
                    });
                    countdownTasks.remove(playerUuid);
                    countdownStartLocations.remove(playerUuid);
                    countdownOtherLocations.remove(playerUuid);
                    countdownOtherUuids.remove(playerUuid);
                    cancel();
                    return;
                }

                player.sendActionBar(Component.text("Teleporting in " + secondsLeft + "...", NamedTextColor.GRAY));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);

                secondsLeft--;
            }
        };

        BukkitTask task = runnable.runTaskTimer(SiDaStuffSmp.getInstance(), 0L, 20L);
        countdownTasks.put(player.getUniqueId(), task);
    }

    private static void scheduleRequestExpiry(UUID targetUuid, TeleportRequest request) {
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
            List<TeleportRequest> requests = pendingRequests.get(targetUuid);
            if (requests != null) {
                requests.remove(request);
                if (requests.isEmpty()) {
                    pendingRequests.remove(targetUuid);
                }
            }

            Player sender = Bukkit.getPlayer(request.senderUuid());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(Chat.prefixed("Your teleport request to " + request.targetName() + " has expired.", NamedTextColor.GRAY));
            }
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null && target.isOnline()) {
                target.sendMessage(Chat.prefixed("Teleport request from " + request.senderName() + " has expired.", NamedTextColor.GRAY));
            }
        }, REQUEST_TIMEOUT_MS / 50L);
    }

    public static void cleanup() {
        for (BukkitTask task : countdownTasks.values()) {
            if (task != null) task.cancel();
        }
        countdownTasks.clear();
        countdownStartLocations.clear();
        countdownOtherLocations.clear();
        countdownOtherUuids.clear();
        pendingRequests.clear();
    }

    public static void handleAutoAccept(Player target, TeleportRequest request) {
        List<TeleportRequest> requests = pendingRequests.get(target.getUniqueId());
        if (requests != null) {
            requests.remove(request);
            if (requests.isEmpty()) {
                pendingRequests.remove(target.getUniqueId());
            }
        }

        Player sender = Bukkit.getPlayer(request.senderUuid());
        if (sender == null || !sender.isOnline()) return;

        if (request.type() == TeleportRequest.Type.TPA) {
            startCountdown(sender, sender.getLocation(), target.getLocation(), target, "Teleporting to " + target.getName() + "...");
        } else {
            startCountdown(target, target.getLocation(), sender.getLocation(), sender, target.getName() + " is teleporting to you...");
        }

        target.sendMessage(Chat.prefixed("Auto-accepted teleport request from " + request.senderName() + ".", NamedTextColor.GREEN));
        sender.sendMessage(Chat.prefixed(target.getName() + " auto-accepted your teleport request!", NamedTextColor.GREEN));
    }
}

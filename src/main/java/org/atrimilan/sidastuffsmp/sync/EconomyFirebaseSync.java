package org.atrimilan.sidastuffsmp.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Syncs player balance & basic stats to Realtime Database at frequent intervals
 * so the website can show near-real-time data. Writes to {@code players/<username_safe>}.
 */
public class EconomyFirebaseSync {

    private static final long SYNC_INTERVAL_TICKS = 200L; // 10 seconds
    private static boolean initialized = false;
    private static int taskId = -1;

    private EconomyFirebaseSync() {}

    public static void init(SiDaStuffSmp plugin) {
        if (!FirebaseConfig.enabled()) {
            plugin.getLogger().info("EconomyFirebaseSync disabled (firebase.enabled=false).");
            return;
        }

        initialized = true;
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                EconomyFirebaseSync::performSync,
                SYNC_INTERVAL_TICKS, SYNC_INTERVAL_TICKS
        ).getTaskId();

        plugin.getLogger().info("EconomyFirebaseSync started (interval=" + SYNC_INTERVAL_TICKS + "t).");
    }

    public static void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        initialized = false;
    }

    public static boolean isEnabled() {
        return initialized;
    }

    private static void performSync() {
        SiDaStuffSmp plugin = SiDaStuffSmp.getInstance();
        if (plugin == null) return;

        Map<String, Object> syncMap = new HashMap<>();
        long now = Instant.now().getEpochSecond();

        // EconomyManager expects main-thread access; collect offline data first
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String name = player.getName();
            double balance = org.atrimilan.sidastuffsmp.economy.EconomyManager.getBalance(uuid);

            String safeKey = name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
            Map<String, Object> data = new HashMap<>();
            data.put("username", name);
            data.put("balance", (long) balance);
            data.put("uuid", uuid.toString());
            data.put("online", true);
            data.put("lastSeen", now);

            syncMap.put(safeKey, data);
        }

        if (syncMap.isEmpty()) return;

        String json = new Gson().toJson(syncMap);
        String databaseUrl = FirebaseConfig.getDatabaseUrl();
        databaseUrl = databaseUrl.endsWith("/") ? databaseUrl.substring(0, databaseUrl.length() - 1) : databaseUrl;
        String url = databaseUrl + "/players.json";

        String accessToken;
        try {
            accessToken = getAccessToken(plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("EconomyFirebaseSync: failed to get access token: " + e.getMessage());
            return;
        }

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(json))
                .build();

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        plugin.getLogger().warning("EconomyFirebaseSync: RTDB returned HTTP " + response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("EconomyFirebaseSync: failed to sync: " + ex.getMessage());
                    return null;
                });
    }

    private static String getAccessToken(SiDaStuffSmp plugin) throws Exception {
        java.io.File serviceAccountFile = new java.io.File(plugin.getDataFolder(), FirebaseConfig.getServiceAccountPath());
        if (!serviceAccountFile.exists()) {
            java.io.File absolute = new java.io.File(FirebaseConfig.getServiceAccountPath());
            if (absolute.isAbsolute() && absolute.exists()) {
                serviceAccountFile = absolute;
            } else {
                throw new IllegalStateException("Firebase service account file not found.");
            }
        }

        try (java.io.FileInputStream fis = new java.io.FileInputStream(serviceAccountFile)) {
            com.google.auth.oauth2.GoogleCredentials credentials = com.google.auth.oauth2.ServiceAccountCredentials.fromStream(fis)
                    .createScoped(java.util.List.of("https://www.googleapis.com/auth/firebase.database",
                            "https://www.googleapis.com/auth/userinfo.email"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }
}
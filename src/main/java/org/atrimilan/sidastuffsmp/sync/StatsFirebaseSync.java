package org.atrimilan.sidastuffsmp.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StatsFirebaseSync {

    private static boolean initialized = false;
    private static FirebaseRestClient restClient;
    private static String syncPath;
    private static long lastSyncTime = 0;
    private static int lastPushCount = 0;
    private static String lastError = null;

    private StatsFirebaseSync() {}

    public static void init(SiDaStuffSmp plugin) {
        if (!FirebaseConfig.enabled()) return;
        String path = FirebaseConfig.getStatsSyncPath();
        if (path == null || path.isEmpty()) return;

        try {
            restClient = new FirebaseRestClient();
            if (!restClient.isInitialized()) {
                restClient.init(plugin);
            }
            if (!restClient.isInitialized()) {
                plugin.getLogger().info("Firebase REST client not available. Stats sync is disabled.");
                return;
            }

            syncPath = path;
            initialized = true;
            plugin.getLogger().info("Stats Firebase sync initialized. Path: " + syncPath);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize stats Firebase sync: " + e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return initialized && restClient != null && restClient.isInitialized();
    }

    public static void performSync() {
        if (!initialized) return;
        syncAsync();
    }

    private static void syncAsync() {
        try {
            List<org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.PlayerStats> allStats =
                    org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.getAllStats();

            Map<String, Object> data = new HashMap<>();
            for (var stats : allStats) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("player_name", stats.playerName());
                playerData.put("kills", stats.kills());
                playerData.put("deaths", stats.deaths());
                playerData.put("rtp_count", stats.rtpCount());
                playerData.put("playtime_seconds", stats.playtimeSeconds());
                playerData.put("kdr", stats.kdr());
                String safeKey = stats.playerName().replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
                data.put(safeKey, playerData);
            }

            String json = new Gson().toJson(data);
        CompletableFuture<HttpResponse<String>> future = restClient.patch(syncPath, json);
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    lastError = throwable.getMessage();
                    return;
                }
                if (response.statusCode() == 200) {
                    lastPushCount = allStats.size();
                    lastSyncTime = System.currentTimeMillis();
                    lastError = null;
                } else {
                    lastError = "HTTP " + response.statusCode() + ": " + response.body();
                }
            });
        } catch (Exception e) {
            lastError = e.getMessage();
        }
    }

    public static String getLastSyncTime() {
        return lastSyncTime > 0 ? new java.util.Date(lastSyncTime).toString() : "Never";
    }

    public static int getLastPushCount() {
        return lastPushCount;
    }

    public static String getLastError() {
        return lastError;
    }

    public static void shutdown() {
        initialized = false;
        restClient = null;
        lastSyncTime = 0;
        lastPushCount = 0;
        lastError = null;
    }
}

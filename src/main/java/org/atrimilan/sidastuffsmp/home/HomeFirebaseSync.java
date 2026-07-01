package org.atrimilan.sidastuffsmp.home;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.sync.FirebaseConfig;
import org.atrimilan.sidastuffsmp.sync.FirebaseRestClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HomeFirebaseSync {

    private static boolean enabled = false;
    private static FirebaseRestClient restClient;
    private static String lastSyncTime = "Never";
    private static int lastPushCount = 0;
    private static int lastPullCount = 0;
    private static String lastError = null;

    private HomeFirebaseSync() {}

    public static void init(SiDaStuffSmp plugin) {
        if (!FirebaseConfig.enabled()) {
            return;
        }

        try {
            if (restClient == null) {
                restClient = new FirebaseRestClient();
            }
            if (!restClient.isInitialized()) {
                restClient.init(plugin);
            }
            enabled = restClient.isInitialized();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            plugin.getLogger().warning("Failed to initialize homes Firebase sync: " + cause.getMessage());
            enabled = false;
        }
    }

    public static void reload() {
        init(SiDaStuffSmp.getInstance());
    }

    public static boolean isEnabled() {
        return enabled && restClient != null && restClient.isInitialized();
    }

    public static FirebaseRestClient getClient() {
        return restClient;
    }

    public static String getHomesSyncPath() {
        return FirebaseConfig.getHomesSyncPath();
    }

    /**
     * Push all local homes to Firebase.
     */
    public static void performSync() {
        if (restClient == null || !restClient.isInitialized()) {
            lastError = "Firebase client not initialized";
            return;
        }

        try {
            List<Home> homes = HomeManager.getAllHomes();
            Map<String, Object> data = new HashMap<>();

            for (Home home : homes) {
                String key = home.getOwnerUuid().toString() + "_" + home.getSlot();
                data.put(key, home.toMap());
            }

            String json = new Gson().toJson(data);
            String syncPath = getHomesSyncPath();

            CompletableFuture<HttpResponse<String>> future = restClient.patch(syncPath, json);
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    lastError = throwable.getMessage();
                    SiDaStuffSmp.getInstance().getLogger().log(Level.WARNING, "Home sync failed: " + throwable.getMessage());
                    return;
                }
                if (response != null && response.statusCode() == 200) {
                    lastPushCount = homes.size();
                    lastSyncTime = new java.util.Date().toString();
                    lastError = null;
                    SiDaStuffSmp.getInstance().getLogger().info("Home sync completed: " + homes.size() + " homes pushed");
                } else if (response != null) {
                    lastError = "HTTP " + response.statusCode() + ": " + response.body();
                    SiDaStuffSmp.getInstance().getLogger().warning("Home sync failed: " + lastError);
                }
            });
        } catch (Exception e) {
            lastError = e.getMessage();
            SiDaStuffSmp.getInstance().getLogger().log(Level.SEVERE, "Home sync error", e);
        }
    }

    /**
     * Pull homes from Firebase and merge with local storage.
     * Remote homes take precedence for slots that exist locally.
     */
    public static void performPull() {
        if (restClient == null || !restClient.isInitialized()) {
            lastError = "Firebase client not initialized";
            return;
        }

        String syncPath = getHomesSyncPath();
        restClient.get(syncPath).whenComplete((response, throwable) -> {
            if (throwable != null) {
                lastError = throwable.getMessage();
                SiDaStuffSmp.getInstance().getLogger().log(Level.WARNING, "Home pull failed: " + throwable.getMessage());
                return;
            }

            if (response == null || response.statusCode() != 200) {
                if (response != null) {
                    lastError = "HTTP " + response.statusCode();
                }
                return;
            }

            try {
                String body = response.body();
                if (body == null || body.equals("null") || body.isEmpty()) {
                    lastPullCount = 0;
                    return;
                }

                Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
                Map<String, Map<String, Object>> remoteData = new Gson().fromJson(body, type);

                if (remoteData == null || remoteData.isEmpty()) {
                    lastPullCount = 0;
                    return;
                }

                int imported = 0;
                for (Map.Entry<String, Map<String, Object>> entry : remoteData.entrySet()) {
                    Home home = Home.fromMap(entry.getValue());
                    if (home != null) {
                        // Check if we should import this home
                        Home localHome = HomeManager.getHome(home.getOwnerUuid(), home.getSlot());
                        if (localHome == null || home.getUpdatedAt() > localHome.getUpdatedAt()) {
                            // Import/overwrite local home with remote
                            HomeManager.importHome(home);
                            imported++;
                        }
                    }
                }

                lastPullCount = imported;
                SiDaStuffSmp.getInstance().getLogger().info("Home pull completed: " + imported + " homes imported");
            } catch (Exception e) {
                lastError = e.getMessage();
                SiDaStuffSmp.getInstance().getLogger().log(Level.SEVERE, "Home pull parse error", e);
            }
        });
    }

    /**
     * Force both push and pull.
     */
    public static void forceSync() {
        performSync();
        // Give push a moment to complete before pull
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> performPull(), 20L);
    }

    public static String getLastSyncTime() {
        return lastSyncTime;
    }

    public static int getLastPushCount() {
        return lastPushCount;
    }

    public static int getLastPullCount() {
        return lastPullCount;
    }

    public static String getLastError() {
        return lastError;
    }
}
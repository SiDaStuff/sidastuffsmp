package org.atrimilan.sidastuffsmp.sync;

import com.google.gson.Gson;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.bounty.BountyManager;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BountyFirebaseSync {
    private static boolean enabled = false;
    private static FirebaseRestClient restClient;
    private static String lastSyncTime = "Never";
    private static int lastPushCount = 0;
    private static String lastError = null;

    private BountyFirebaseSync() {}

    public static void init(SiDaStuffSmp plugin) {
        if (!FirebaseConfig.enabled()) return;
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
            plugin.getLogger().warning("Failed to initialize bounties Firebase sync: " + cause.getMessage());
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled && restClient != null && restClient.isInitialized();
    }

    public static void performSync() {
        if (!isEnabled()) return;
        try {
            Map<String, Object> data = new HashMap<>();
            for (BountyManager.BountyEntry bounty : BountyManager.getTopBounties(100)) {
                Map<String, Object> bountyData = new HashMap<>();
                bountyData.put("targetName", bounty.targetName());
                bountyData.put("amount", bounty.amount());
                data.put(bounty.targetUuid().toString(), bountyData);
            }

            String json = new Gson().toJson(data);
            CompletableFuture<HttpResponse<String>> future = restClient.patch(FirebaseConfig.getBountiesSyncPath(), json);
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    lastError = throwable.getMessage();
                    return;
                }
                if (response.statusCode() == 200) {
                    lastPushCount = data.size();
                    lastSyncTime = new java.util.Date().toString();
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
        return lastSyncTime;
    }

    public static int getLastPushCount() {
        return lastPushCount;
    }

    public static String getLastError() {
        return lastError;
    }

    public static void shutdown() {
        enabled = false;
        lastSyncTime = "Never";
        lastPushCount = 0;
        lastError = null;
    }
}

package org.atrimilan.sidastuffsmp.sync;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpResponse;

public class AuctionFirebaseSync {

    private static boolean enabled = false;
    private static FirebaseRestClient restClient;
    private static String lastSyncTime = "Never";
    private static int lastPushCount = 0;
    private static int lastPullCount = 0;
    private static String lastError = null;

    private AuctionFirebaseSync() {}

    public static void init(SiDaStuffSmp plugin) {
        FirebaseConfig.init(plugin);

        if (!FirebaseConfig.enabled()) {
            plugin.getLogger().info("Firebase sync is disabled.");
            return;
        }

        try {
            restClient = new FirebaseRestClient();
            boolean result = restClient.init(plugin);
            enabled = result;
            if (!result) {
                restClient = null;
            }
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            plugin.getLogger().warning("Failed to initialize Firebase sync: " + cause.getMessage());
            enabled = false;
            restClient = null;
        }
    }

    public static void reload() {
        if (restClient != null) {
            restClient.shutdown();
        }
        init(SiDaStuffSmp.getInstance());
    }

    public static boolean isEnabled() {
        return enabled && restClient != null && restClient.isInitialized();
    }

    public static FirebaseRestClient getClient() {
        return restClient;
    }

    public static void performSync() {
        if (restClient != null && restClient.isInitialized()) {
            try {
                List<AuctionListing> listings = AuctionManager.getAllSyncableListings();
                Map<String, Object> data = new HashMap<>();
                for (AuctionListing listing : listings) {
                    Map<String, Object> listingData = new HashMap<>();
                    listingData.put("item", listing.itemBase64());
                    listingData.put("price", listing.price());
                    listingData.put("seller", listing.sellerName());
                    listingData.put("category", listing.category().name());
                    listingData.put("created", listing.createdAt());
                    listingData.put("status", listing.status());
                    data.put(String.valueOf(listing.id()), listingData);
                }

                String json = new Gson().toJson(data);
                String syncPath = FirebaseConfig.getSyncPath();
                CompletableFuture<HttpResponse<String>> future = restClient.patch(syncPath, json);
                future.whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        lastError = throwable.getMessage();
                        return;
                    }
                    if (response.statusCode() == 200) {
                        lastPushCount = listings.size();
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
    }

    public static void forceSync() {
        performSync();
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

    public static void shutdown() {
        if (restClient != null) {
            restClient.shutdown();
        }
        enabled = false;
        restClient = null;
        lastSyncTime = "Never";
        lastPushCount = 0;
        lastPullCount = 0;
        lastError = null;
    }
}

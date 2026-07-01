package org.atrimilan.sidastuffsmp.sync;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.order.OrderListing;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpResponse;

public class OrderFirebaseSync {

    private static boolean enabled = false;
    private static FirebaseRestClient restClient;
    private static String lastSyncTime = "Never";
    private static int lastPushCount = 0;
    private static int lastPullCount = 0;
    private static String lastError = null;

    private OrderFirebaseSync() {}

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
            plugin.getLogger().warning("Failed to initialize orders Firebase sync: " + cause.getMessage());
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

    public static void performSync() {
        if (restClient != null && restClient.isInitialized()) {
            try {
                List<OrderListing> orders = OrderManager.getAllSyncableOrders();
                Map<String, Object> data = new HashMap<>();
                for (OrderListing order : orders) {
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("item", order.materialName());
                    orderData.put("price", order.pricePerUnit());
                    orderData.put("quantity", order.quantity());
                    orderData.put("buyer", order.buyerName());
                    orderData.put("created", order.createdAt());
                    orderData.put("status", order.status());
                    data.put(String.valueOf(order.id()), orderData);
                }

                String json = new Gson().toJson(data);
                String syncPath = FirebaseConfig.getOrdersSyncPath();
                CompletableFuture<HttpResponse<String>> future = restClient.patch(syncPath, json);
                future.whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        lastError = throwable.getMessage();
                        return;
                    }
                    if (response.statusCode() == 200) {
                        lastPushCount = orders.size();
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
        enabled = false;
        lastSyncTime = "Never";
        lastPushCount = 0;
        lastPullCount = 0;
        lastError = null;
    }
}

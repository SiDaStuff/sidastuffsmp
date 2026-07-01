package org.atrimilan.sidastuffsmp.sync;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class FirebaseRestClient {

    private final Logger logger;
    private String databaseUrl;
    private GoogleCredentials credentials;
    private AccessToken cachedToken;
    private final HttpClient httpClient;
    private boolean initialized = false;

    public FirebaseRestClient() {
        this.logger = SiDaStuffSmp.getInstance().getLogger();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.databaseUrl = "";
    }

    public boolean init(SiDaStuffSmp plugin) {
        if (!FirebaseConfig.enabled()) {
            logger.info("Firebase sync is disabled.");
            return false;
        }

        String url = FirebaseConfig.getDatabaseUrl();
        if (url == null || url.isEmpty()) {
            logger.warning("Firebase database URL is empty. Sync disabled.");
            return false;
        }

        this.databaseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        try {
            File serviceAccountFile = new File(plugin.getDataFolder(), FirebaseConfig.getServiceAccountPath());
            if (!serviceAccountFile.exists()) {
                File absolute = new File(FirebaseConfig.getServiceAccountPath());
                if (absolute.isAbsolute() && absolute.exists()) {
                    serviceAccountFile = absolute;
                } else {
                    logger.warning("Firebase service account file not found: " + serviceAccountFile.getPath());
                    return false;
                }
            }

            try (FileInputStream serviceAccountStream = new FileInputStream(serviceAccountFile)) {
                this.credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                        .createScoped(List.of("https://www.googleapis.com/auth/firebase.database",
                                "https://www.googleapis.com/auth/userinfo.email"));
            }

            this.initialized = true;
            logger.info("Firebase REST client initialized. Database: " + this.databaseUrl);
            return true;
        } catch (Exception e) {
            logger.warning("Failed to initialize Firebase REST client: " + e.getMessage());
            return false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private AccessToken getAccessToken() throws IOException {
        if (cachedToken != null && cachedToken.getExpirationTime().toInstant().isAfter(Instant.now().plusSeconds(60))) {
            return cachedToken;
        }

        credentials.refreshIfExpired();
        cachedToken = credentials.getAccessToken();
        return cachedToken;
    }

    public CompletableFuture<HttpResponse<String>> get(String path) {
        return request("GET", path, null);
    }

    public CompletableFuture<HttpResponse<String>> put(String path, String jsonBody) {
        return request("PUT", path, jsonBody);
    }

    public CompletableFuture<HttpResponse<String>> patch(String path, String jsonBody) {
        return request("PATCH", path, jsonBody);
    }

    public CompletableFuture<HttpResponse<String>> delete(String path) {
        return request("DELETE", path, null);
    }

    private CompletableFuture<HttpResponse<String>> request(String method, String path, String body) {
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("Firebase REST client not initialized"));
        }

        try {
            AccessToken token = getAccessToken();
            String url = databaseUrl + "/" + (path.startsWith("/") ? path.substring(1) : path) + ".json";

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + token.getTokenValue())
                    .header("Content-Type", "application/json");

            switch (method) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public void shutdown() {
        initialized = false;
        credentials = null;
        cachedToken = null;
    }
}

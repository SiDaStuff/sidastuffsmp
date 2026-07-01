package org.atrimilan.sidastuffsmp.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.stats.PlayerStatsManager;
import org.atrimilan.sidastuffsmp.utils.PunishmentManager;
import org.atrimilan.sidastuffsmp.utils.WarnManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SiDaStuffSmpExpansion extends PlaceholderExpansion {

    private static final DecimalFormat DF = new DecimalFormat("#.##");
    private static final Map<String, String> LIVE_CACHE = new ConcurrentHashMap<>();
    private static int cacheTaskId = -1;

    private static volatile String globalAuctionActive = "0";
    private static volatile String globalAuctionTotal = "0";
    private static volatile String globalAuctionSold = "0";
    private static volatile String globalAuctionExpired = "0";
    private static volatile String globalOrdersActive = "0";
    private static volatile String globalOrdersTotal = "0";

    public static void startCacheRefresh() {
        stopCacheRefresh();
        refreshCache();
        cacheTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                SiDaStuffSmp.getInstance(),
                SiDaStuffSmpExpansion::refreshCache,
                20L, 20L).getTaskId();
    }

    public static void stopCacheRefresh() {
        if (cacheTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cacheTaskId);
            cacheTaskId = -1;
        }
    }

    private static void refreshCache() {
        ConcurrentHashMap<String, String> newCache = new ConcurrentHashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerStatsManager.PlayerStats stats = PlayerStatsManager.getStats(uuid);
            newCache.put(uuid + ":kills", String.valueOf(stats.kills()));
            newCache.put(uuid + ":deaths", String.valueOf(stats.deaths()));
            newCache.put(uuid + ":kdr", DF.format(stats.kdr()));
            newCache.put(uuid + ":playtime", String.valueOf(stats.playtimeSeconds()));
            newCache.put(uuid + ":playtime_formatted", formatPlaytime(stats.playtimeSeconds()));
            newCache.put(uuid + ":rtp_count", String.valueOf(stats.rtpCount()));
            newCache.put(uuid + ":auction_mine", String.valueOf(AuctionManager.getActiveListingCount(uuid)));
            newCache.put(uuid + ":auction_mine_sold", String.valueOf(AuctionManager.getUncollectedSoldCount(uuid)));
            newCache.put(uuid + ":auction_mine_expired", String.valueOf(AuctionManager.getUncollectedExpiredCount(uuid)));
            newCache.put(uuid + ":orders_active", String.valueOf(OrderManager.getActiveOrderCount(uuid)));
            newCache.put(uuid + ":orders_stash", String.valueOf(OrderManager.getUncollectedStashItemCount(uuid)));
            newCache.put(uuid + ":warn_count", String.valueOf(WarnManager.getWarnings(uuid).size()));
            newCache.put(uuid + ":punish_count", String.valueOf(PunishmentManager.getPunishCount(uuid)));
        }

        AuctionManager.StatsResult astats = AuctionManager.getStats();
        globalAuctionActive = String.valueOf(astats.active());
        globalAuctionTotal = String.valueOf(astats.total());
        globalAuctionSold = String.valueOf(astats.sold());
        globalAuctionExpired = String.valueOf(astats.expired());

        OrderManager.StatsResult ostats = OrderManager.getStats();
        globalOrdersActive = String.valueOf(ostats.active());
        globalOrdersTotal = String.valueOf(ostats.total());

        LIVE_CACHE.clear();
        LIVE_CACHE.putAll(newCache);
    }

    public static void invalidatePlayer(UUID uuid) {
        String prefix = uuid.toString() + ":";
        LIVE_CACHE.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sidastuffsmp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Atrimilan";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        String lowerParams = params.toLowerCase();

        switch (lowerParams) {
            case "auction_active" -> { return globalAuctionActive; }
            case "auction_total" -> { return globalAuctionTotal; }
            case "auction_sold" -> { return globalAuctionSold; }
            case "auction_expired" -> { return globalAuctionExpired; }
            case "orders_market_active" -> { return globalOrdersActive; }
            case "orders_market_total" -> { return globalOrdersTotal; }
        }

        UUID uuid = player.getUniqueId();
        String cacheKey = uuid + ":" + lowerParams;
        String cached = LIVE_CACHE.get(cacheKey);
        if (cached != null) return cached;

        return "";
    }

    private static String formatPlaytime(int seconds) {
        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString().trim();
    }
}

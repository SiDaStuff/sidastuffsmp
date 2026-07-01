package org.atrimilan.sidastuffsmp.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.economy.EconomyConfig;
import org.atrimilan.sidastuffsmp.economy.EconomyDatabase;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyPlaceholderExpansion extends PlaceholderExpansion {

    private static final Map<String, String> BALANCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> TOP_CACHE = new ConcurrentHashMap<>();
    private static int cacheTaskId = -1;

    @Override
    public @NotNull String getIdentifier() {
        return "economy";
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

    public static void startCacheRefresh() {
        stopCacheRefresh();
        refreshCache();
        cacheTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                SiDaStuffSmp.getInstance(),
                EconomyPlaceholderExpansion::refreshCache,
                20L, 20L).getTaskId();
    }

    public static void stopCacheRefresh() {
        if (cacheTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cacheTaskId);
            cacheTaskId = -1;
        }
    }

    public static void invalidatePlayer(UUID uuid) {
        BALANCE_CACHE.remove(uuid.toString() + ":balance");
        BALANCE_CACHE.remove(uuid.toString() + ":balance_formatted");
        BALANCE_CACHE.remove(uuid.toString() + ":balance_formatted_symbol");
        BALANCE_CACHE.remove(uuid.toString() + ":rank");
        BALANCE_CACHE.remove(uuid.toString() + ":has_account");
    }

    private static void refreshCache() {
        ConcurrentHashMap<String, String> newBalances = new ConcurrentHashMap<>();

        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            double balance = EconomyManager.getBalance(uuid);
            newBalances.put(uuid + ":balance", String.valueOf(balance));
            newBalances.put(uuid + ":balance_formatted", EconomyManager.formatBalance(balance));
            newBalances.put(uuid + ":balance_formatted_symbol", EconomyManager.formatBalanceWithSymbol(balance));
            int rank = EconomyManager.getPlayerRank(uuid);
            newBalances.put(uuid + ":rank", rank > 0 ? String.valueOf(rank) : "");
            newBalances.put(uuid + ":has_account", String.valueOf(EconomyManager.hasAccount(uuid)));
        }

        BALANCE_CACHE.clear();
        BALANCE_CACHE.putAll(newBalances);

        ConcurrentHashMap<String, String> newTops = new ConcurrentHashMap<>();
        List<EconomyDatabase.TopEntry> top10 = EconomyManager.getTopPlayers(10);
        for (int i = 0; i < top10.size(); i++) {
            int rank = i + 1;
            EconomyDatabase.TopEntry entry = top10.get(i);
            newTops.put("top_" + rank, entry.name());
            newTops.put("top_" + rank + "_balance", String.valueOf(entry.balance()));
            newTops.put("top_" + rank + "_balance_formatted", EconomyManager.formatBalanceWithSymbol(entry.balance()));
        }
        TOP_CACHE.clear();
        TOP_CACHE.putAll(newTops);
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        String lowerParams = params.toLowerCase();

        switch (lowerParams) {
            case "currency_name" -> { return EconomyConfig.currencyName(); }
            case "currency_name_plural" -> { return EconomyConfig.currencyNamePlural(); }
            case "currency_symbol" -> { return EconomyConfig.currencySymbol(); }
        }

        if (lowerParams.startsWith("top_")) {
            String cached = TOP_CACHE.get(lowerParams);
            return cached != null ? cached : "";
        }

        UUID uuid = player.getUniqueId();
        String cacheKey = uuid + ":" + lowerParams;
        String cached = BALANCE_CACHE.get(cacheKey);
        if (cached != null) return cached;

        return "";
    }
}

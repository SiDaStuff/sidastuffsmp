package org.atrimilan.sidastuffsmp.economy;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    private static boolean initialized = false;
    private static List<EconomyDatabase.TopEntry> cachedTopList = new ArrayList<>();
    private static long lastTopUpdate = 0;
    private static int topUpdateTaskId = -1;
    private static int vaultSyncTaskId = -1;
    private static final ConcurrentHashMap<UUID, Double> balanceCache = new ConcurrentHashMap<>();

    private EconomyManager() {}

    public static void init(SiDaStuffSmp plugin) {
        EconomyConfig.init(plugin);
        EconomyDatabase.init(plugin);
        VaultEconomyProvider.register(plugin);
        initialized = true;

        refreshTopList();
        long intervalTicks = EconomyConfig.topUpdateSeconds() * 20L;
        topUpdateTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, EconomyManager::refreshTopList, intervalTicks, intervalTicks).getTaskId();

        startVaultSyncTask(plugin);

        plugin.getLogger().info("Economy system initialized. Default starting balance is " + EconomyConfig.startingBalance() + ".");
    }

    public static void shutdown() {
        if (topUpdateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(topUpdateTaskId);
        }
        if (vaultSyncTaskId != -1) {
            Bukkit.getScheduler().cancelTask(vaultSyncTaskId);
        }
        EconomyDatabase.shutdown();
        balanceCache.clear();
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean createAccount(UUID uuid, String name) {
        boolean created = EconomyDatabase.createAccount(uuid, name, EconomyConfig.startingBalance());
        if (created) {
            balanceCache.put(uuid, EconomyConfig.startingBalance());
        }
        return created;
    }

    public static void updatePlayerName(UUID uuid, String name) {
        EconomyDatabase.updatePlayerName(uuid, name);
    }

    public static boolean hasAccount(UUID uuid) {
        if (balanceCache.containsKey(uuid)) return true;
        return EconomyDatabase.hasAccount(uuid);
    }

    public static double getBalance(UUID uuid) {
        Double cached = balanceCache.get(uuid);
        if (cached != null) return cached;
        if (!hasAccount(uuid)) return 0.0;
        double balance = EconomyDatabase.getBalance(uuid);
        balanceCache.put(uuid, balance);
        return balance;
    }

    public static boolean setBalance(UUID uuid, double amount) {
        double min = EconomyConfig.minBalance();
        double max = EconomyConfig.maxBalance();
        double clamped = Math.max(min, Math.min(max, amount));
        boolean result = EconomyDatabase.setBalance(uuid, clamped);
        if (result) {
            balanceCache.put(uuid, clamped);
            EconomyDatabase.addTransaction(uuid, "SET", clamped, null, null, "Balance set to " + clamped);
        }
        return result;
    }

    public static EconomyResult deposit(UUID uuid, double amount, String type, UUID targetUuid, String targetName, String description) {
        if (!hasAccount(uuid)) return new EconomyResult(false, "Account not found", 0);
        if (amount < 0) return new EconomyResult(false, "Negative amount", 0);

        double balance = getBalance(uuid);
        double max = EconomyConfig.maxBalance();
        if (balance + amount > max) return new EconomyResult(false, "Would exceed max balance", balance);

        if (!EconomyDatabase.addToBalance(uuid, amount)) return new EconomyResult(false, "Database error", balance);

        EconomyDatabase.addTransaction(uuid, type, amount, targetUuid, targetName, description);
        double newBalance = balance + amount;
        balanceCache.put(uuid, newBalance);
        return new EconomyResult(true, null, newBalance);
    }

    public static EconomyResult withdraw(UUID uuid, double amount, String type, UUID targetUuid, String targetName, String description) {
        if (!hasAccount(uuid)) return new EconomyResult(false, "Account not found", 0);
        if (amount < 0) return new EconomyResult(false, "Negative amount", 0);

        double balance = getBalance(uuid);
        if (balance < amount) return new EconomyResult(false, "Insufficient funds", balance);

        if (!EconomyDatabase.subtractFromBalance(uuid, amount)) return new EconomyResult(false, "Database error", balance);

        EconomyDatabase.addTransaction(uuid, type, amount, targetUuid, targetName, description);
        double newBalance = balance - amount;
        balanceCache.put(uuid, newBalance);
        return new EconomyResult(true, null, newBalance);
    }

    public static void invalidateCache(UUID uuid) {
        balanceCache.remove(uuid);
    }

    public static void updateCachedBalance(UUID uuid, double balance) {
        balanceCache.put(uuid, balance);
    }

    public static List<TransactionRecord> getTransactions(UUID uuid, int limit) {
        int maxLimit = EconomyConfig.historyLimit();
        return EconomyDatabase.getTransactions(uuid, Math.min(limit, maxLimit));
    }

    public static List<EconomyDatabase.TopEntry> getTopPlayers(int count) {
        if (cachedTopList.isEmpty()) refreshTopList();
        if (count >= cachedTopList.size()) return new ArrayList<>(cachedTopList);
        return new ArrayList<>(cachedTopList.subList(0, count));
    }

    public static int getPlayerRank(UUID uuid) {
        return EconomyDatabase.getPlayerRank(uuid);
    }

    public static String getPlayerName(UUID uuid) {
        return EconomyDatabase.getPlayerName(uuid);
    }

    public static UUID resolveUuid(String name) {
        UUID uuid = EconomyDatabase.getUuidByName(name);
        if (uuid != null) return uuid;

        @SuppressWarnings("deprecation")
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        return player.getUniqueId();
    }

    public static String formatBalance(double amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }

    public static String formatBalanceWithSymbol(double amount) {
        return EconomyConfig.currencySymbol() + formatBalance(amount);
    }

    private static void refreshTopList() {
        cachedTopList = EconomyDatabase.getTopPlayers(1000);
        lastTopUpdate = System.currentTimeMillis();
    }

    private static void startVaultSyncTask(SiDaStuffSmp plugin) {
        if (!EconomyConfig.provideVaultEconomy()) return;

        long syncInterval = EconomyConfig.vaultSyncSeconds() * 20L;
        vaultSyncTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                net.milkbowl.vault.economy.Economy vaultEconomy = null;
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp == null) return;
                vaultEconomy = rsp.getProvider();
                if (vaultEconomy == null) return;
                if (vaultEconomy.getName().equals("SiDaStuffEconomy")) return;

                for (UUID uuid : EconomyDatabase.getAllAccountUuids()) {
                    try {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        double vaultBalance = vaultEconomy.getBalance(player);
                        double ourBalance = EconomyDatabase.getBalance(uuid);
                        if (Double.compare(vaultBalance, ourBalance) != 0) {
                            EconomyDatabase.setBalance(uuid, vaultBalance);
                            balanceCache.put(uuid, vaultBalance);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }, syncInterval, syncInterval).getTaskId();
    }

    public record EconomyResult(boolean success, String errorMessage, double newBalance) {}
}

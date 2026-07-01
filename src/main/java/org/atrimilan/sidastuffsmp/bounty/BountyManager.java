package org.atrimilan.sidastuffsmp.bounty;

import net.milkbowl.vault.economy.Economy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BountyManager {

    private static Connection connection;
    private static Economy economy;
    private static File dbFile;

    // Cache of player bounties: targetUuid -> total amount
    private static final ConcurrentHashMap<UUID, Double> bountyCache = new ConcurrentHashMap<>();
    // Cache of top bounties sorted by amount
    private static final List<BountyEntry> topBounties = new ArrayList<>();
    // Cooldown tracking for bounty placement
    private static final ConcurrentHashMap<UUID, Long> bountyCooldowns = new ConcurrentHashMap<>();
    // Kill tracking for anti-farm: victimUuid -> (killerUuid -> killCount)
    private static final ConcurrentHashMap<UUID, Map<UUID, Integer>> killTracking = new ConcurrentHashMap<>();
    // Kill streak tracking: playerUuid -> streak count
    private static final ConcurrentHashMap<UUID, Integer> killStreaks = new ConcurrentHashMap<>();
    // Last kill time for anti-farm: playerUuid -> timestamp
    private static final ConcurrentHashMap<UUID, Long> lastKillTime = new ConcurrentHashMap<>();

    private static final long KILL_STREAK_WINDOW_MS = 60000; // 1 minute window for streak
    private static final int MAX_KILLS_PER_VICTIM = 3; // Max kills per victim in streak window
    private static final int MAX_STREAK_BEFORE_FLAG = 5; // Flag as potential farmer

    private BountyManager() {}

    public static void init(SiDaStuffSmp plugin) {
        setupEconomy();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dbFile = new File(plugin.getDataFolder(), "bounties.db");
        try {
            connect();
            createTables();
            loadBountyCache();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize bounties database: " + e.getMessage());
        }
    }

    public static void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
        connection = null;
    }

    public static boolean hasEconomy() {
        return economy != null;
    }

    private static void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    private static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=-2000");
            stmt.execute("PRAGMA temp_store=MEMORY");
        }
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bounties (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    setter_uuid TEXT NOT NULL,
                    setter_name TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    target_name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bounties_target ON bounties(target_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bounties_active ON bounties(active)");
        }
    }

    private static void loadBountyCache() {
        bountyCache.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT target_uuid, SUM(amount) as total FROM bounties WHERE active = 1 GROUP BY target_uuid")) {
            while (rs.next()) {
                UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                double total = rs.getDouble("total");
                bountyCache.put(targetUuid, total);
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().severe("Failed to load bounty cache: " + e.getMessage());
        }
        updateTopBounties();
    }

    public record BountyEntry(UUID targetUuid, String targetName, double amount) {}

    private static void updateTopBounties() {
        topBounties.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT target_uuid, target_name, SUM(amount) as total 
                 FROM bounties 
                 WHERE active = 1 
                 GROUP BY target_uuid 
                 ORDER BY total DESC 
                 LIMIT 100
             """)) {
            while (rs.next()) {
                topBounties.add(new BountyEntry(
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getString("target_name"),
                        rs.getDouble("total")
                ));
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().severe("Failed to update top bounties: " + e.getMessage());
        }
    }

    /**
     * Add a bounty on a player. Takes money from the setter.
     * @return The total bounty on the target after adding, or -1 if failed
     */
    public static double addBounty(Player setter, OfflinePlayer target, double amount) {
        if (!hasEconomy()) {
            setter.sendMessage(Chat.prefixed("Economy system not available!", NamedTextColor.RED));
            return -1;
        }
        if (target.getUniqueId().equals(setter.getUniqueId())) {
            setter.sendMessage(Chat.prefixed("You cannot place a bounty on yourself!", NamedTextColor.RED));
            return -1;
        }
        if (amount < BountyConfig.minBounty() || amount > BountyConfig.maxBounty()) {
            setter.sendMessage(Chat.prefixed("Amount must be between " + formatPrice(BountyConfig.minBounty()) + " and " + formatPrice(BountyConfig.maxBounty()) + "!", NamedTextColor.RED));
            return -1;
        }
        if (getActiveBountyCountBySetter(setter.getUniqueId()) >= BountyConfig.maxBountiesPerPlayer()) {
            setter.sendMessage(Chat.prefixed("You have reached the active bounty limit.", NamedTextColor.RED));
            return -1;
        }

        // Check cooldown
        long remainingCooldown = getBountyCooldownRemaining(setter.getUniqueId());
        if (remainingCooldown > 0) {
            setter.sendMessage(Chat.prefixed("Please wait " + remainingCooldown + " seconds before placing another bounty.", NamedTextColor.RED));
            return -1;
        }

        // Withdraw from setter
        if (!economy.withdrawPlayer(setter, amount).transactionSuccess()) {
            setter.sendMessage(Chat.prefixed("You don't have enough money for that bounty!", NamedTextColor.RED));
            return -1;
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + (BountyConfig.expireDays() * 24L * 60L * 60L * 1000L);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bounties (setter_uuid, setter_name, target_uuid, target_name, amount, created_at, expires_at, active) VALUES (?, ?, ?, ?, ?, ?, ?, 1)")) {
            ps.setString(1, setter.getUniqueId().toString());
            ps.setString(2, setter.getName());
            ps.setString(3, target.getUniqueId().toString());
            ps.setString(4, target.getName());
            ps.setDouble(5, amount);
            ps.setLong(6, now);
            ps.setLong(7, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Refund the player
            economy.depositPlayer(setter, amount);
            setter.sendMessage(Chat.prefixed("Failed to create bounty!", NamedTextColor.RED));
            return -1;
        }

        // Update cache
        double currentTotal = bountyCache.getOrDefault(target.getUniqueId(), 0.0);
        double newTotal = currentTotal + amount;
        bountyCache.put(target.getUniqueId(), newTotal);
        updateTopBounties();

        // Set cooldown
        setBountyCooldown(setter.getUniqueId());

        setter.sendMessage(Chat.prefixed("You placed a " + formatPrice(amount) + " bounty on " + target.getName() + "!", NamedTextColor.GREEN));
        return newTotal;
    }

    /**
     * Remove all bounties set by a player on a target. Refunds the total to the setter.
     * @return true if successful
     */
    public static boolean removeAllBountiesBySetter(Player setter, OfflinePlayer target) {
        if (!hasEconomy()) return false;

        double totalRefunded = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, amount FROM bounties WHERE setter_uuid = ? AND target_uuid = ? AND active = 1")) {
            ps.setString(1, setter.getUniqueId().toString());
            ps.setString(2, target.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<Integer> idsToDeactivate = new java.util.ArrayList<>();
                while (rs.next()) {
                    idsToDeactivate.add(rs.getInt("id"));
                    totalRefunded += rs.getDouble("amount");
                }
                if (idsToDeactivate.isEmpty()) {
                    setter.sendMessage(Chat.prefixed("You don't have any active bounties on " + target.getName() + ".", NamedTextColor.RED));
                    return false;
                }

                // Mark all as inactive
                StringBuilder placeholders = new StringBuilder();
                for (int i = 0; i < idsToDeactivate.size(); i++) {
                    if (i > 0) placeholders.append(",");
                    placeholders.append("?");
                }
                try (PreparedStatement updatePs = connection.prepareStatement(
                        "UPDATE bounties SET active = 0 WHERE id IN (" + placeholders + ")")) {
                    for (int i = 0; i < idsToDeactivate.size(); i++) {
                        updatePs.setInt(i + 1, idsToDeactivate.get(i));
                    }
                    updatePs.executeUpdate();
                }
            }
        } catch (SQLException e) {
            setter.sendMessage(Chat.prefixed("Failed to remove bounties!", NamedTextColor.RED));
            return false;
        }

        // Refund the total
        if (totalRefunded > 0 && hasEconomy()) {
            economy.depositPlayer(setter, totalRefunded);
        }

        // Update cache
        double currentTotal = bountyCache.getOrDefault(target.getUniqueId(), 0.0);
        bountyCache.put(target.getUniqueId(), Math.max(0, currentTotal - totalRefunded));
        updateTopBounties();

        setter.sendMessage(Chat.prefixed("You removed " + formatPrice(totalRefunded) + " from bounties on " + target.getName() + "!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Remove a bounty contribution. Refunds the setter.
     * @return true if successful
     */
    public static boolean removeBounty(Player setter, OfflinePlayer target, double amount) {
        if (!hasEconomy()) return false;

        // Find an active bounty from this setter on this target
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, amount FROM bounties WHERE setter_uuid = ? AND target_uuid = ? AND active = 1 ORDER BY created_at DESC LIMIT 1")) {
            ps.setString(1, setter.getUniqueId().toString());
            ps.setString(2, target.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double currentAmount = rs.getDouble("amount");
                    double refundAmount = Math.min(amount, currentAmount);

                    // Mark as inactive and refund
                    try (PreparedStatement updatePs = connection.prepareStatement(
                            "UPDATE bounties SET active = 0 WHERE id = ?")) {
                        updatePs.setInt(1, rs.getInt("id"));
                        updatePs.executeUpdate();
                    }

                    economy.depositPlayer(setter, refundAmount);

                    // Update cache
                    double currentTotal = bountyCache.getOrDefault(target.getUniqueId(), 0.0);
                    bountyCache.put(target.getUniqueId(), Math.max(0, currentTotal - refundAmount));
                    updateTopBounties();

                    setter.sendMessage(Chat.prefixed("You removed " + formatPrice(refundAmount) + " from the bounty on " + target.getName() + "!", NamedTextColor.GREEN));
                    return true;
                }
            }
        } catch (SQLException e) {
            setter.sendMessage(Chat.prefixed("Failed to remove bounty!", NamedTextColor.RED));
        }
        return false;
    }

    /**
     * Get the total active bounty on a player.
     */
    public static double getTotalBounty(UUID targetUuid) {
        return bountyCache.getOrDefault(targetUuid, 0.0);
    }

    /**
     * Get the total active bounty on a player by name.
     */
    public static double getTotalBounty(String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) return 0;
        return getTotalBounty(target.getUniqueId());
    }

    /**
     * Get top bounties for display.
     */
    public static List<BountyEntry> getTopBounties(int limit) {
        return topBounties.stream().limit(limit).toList();
    }

    public static int getActiveBountyCountBySetter(UUID setterUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM bounties WHERE setter_uuid = ? AND active = 1")) {
            ps.setString(1, setterUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Failed to count bounties: " + e.getMessage());
        }
        return 0;
    }

    public static List<BountyEntry> getBountiesSetBy(UUID setterUuid, int limit) {
        List<BountyEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT target_uuid, target_name, SUM(amount) as total
                FROM bounties
                WHERE setter_uuid = ? AND active = 1
                GROUP BY target_uuid, target_name
                ORDER BY total DESC
                LIMIT ?
                """)) {
            ps.setString(1, setterUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new BountyEntry(UUID.fromString(rs.getString("target_uuid")),
                            rs.getString("target_name"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Failed to load player bounties: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Get the bounty amount a specific player placed on a target.
     * Used for anti-abuse detection (checking if killer had bounty on victim).
     */
    public static double getBountyBySetterOnTarget(UUID setterUuid, UUID targetUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COALESCE(SUM(amount), 0) FROM bounties WHERE setter_uuid = ? AND target_uuid = ? AND active = 1")) {
            ps.setString(1, setterUuid.toString());
            ps.setString(2, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Failed to get bounty by setter: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Called when a player kills another player with a bounty.
     * Awards the bounty to the killer and deactivates all bounties on the target.
     * @return The total amount awarded to the killer
     */
    public static double claimBounty(Player killer, Player victim) {
        double totalBounty = getTotalBounty(victim.getUniqueId());
        if (totalBounty <= 0) return 0;

        // Anti-farm check: detect suspicious kill patterns
        if (isKillPatternSuspicious(killer, victim)) {
            SiDaStuffSmp.getInstance().getLogger().warning("Bounty claim flagged for suspicious pattern: " + killer.getName() + " killing " + victim.getName());
            // Still award the bounty but log it for review
        }

        // Deactivate all bounties on this target
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE bounties SET active = 0 WHERE target_uuid = ? AND active = 1")) {
            ps.setString(1, victim.getUniqueId().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().severe("Failed to deactivate bounties: " + e.getMessage());
        }

        // Award the bounty to the killer
        if (hasEconomy()) {
            economy.depositPlayer(killer, totalBounty);
        }

        // Clear cache
        bountyCache.remove(victim.getUniqueId());
        updateTopBounties();

        // Update kill tracking for anti-farm detection
        trackKill(killer.getUniqueId(), victim.getUniqueId());

        // Broadcast the reward
        Component message = Chat.prefix()
                .append(Component.text(killer.getName()))
                .append(Component.text(" claimed a ", NamedTextColor.WHITE))
                .append(Component.text(formatPrice(totalBounty), NamedTextColor.GOLD))
                .append(Component.text(" bounty by killing ", NamedTextColor.WHITE))
                .append(Component.text(victim.getName(), NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.YELLOW));
        Bukkit.broadcast(message);

        return totalBounty;
    }

    /**
     * Track a kill for anti-farm detection.
     */
    private static void trackKill(UUID killerUuid, UUID victimUuid) {
        long now = System.currentTimeMillis();

        // Update kill streak
        Integer currentStreak = killStreaks.getOrDefault(killerUuid, 0);
        Long lastKill = lastKillTime.get(killerUuid);

        if (lastKill != null && (now - lastKill) < KILL_STREAK_WINDOW_MS) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }

        killStreaks.put(killerUuid, currentStreak);
        lastKillTime.put(killerUuid, now);

        // Track kills per victim
        Map<UUID, Integer> victimKills = killTracking.computeIfAbsent(victimUuid, k -> new ConcurrentHashMap<>());
        int killsOnVictim = victimKills.getOrDefault(killerUuid, 0) + 1;
        victimKills.put(killerUuid, killsOnVictim);

        // Clean up old tracking data periodically
        if (killTracking.size() > 1000) {
            cleanupKillTracking();
        }
    }

    /**
     * Check if a kill pattern looks suspicious (potential farming).
     */
    public static boolean isKillPatternSuspicious(Player killer, Player victim) {
        UUID killerUuid = killer.getUniqueId();
        UUID victimUuid = victim.getUniqueId();

        // Check kill streak
        int streak = killStreaks.getOrDefault(killerUuid, 0);
        if (streak >= MAX_STREAK_BEFORE_FLAG) {
            return true;
        }

        // Check kills on this specific victim
        Map<UUID, Integer> victimKills = killTracking.get(victimUuid);
        if (victimKills != null) {
            int killsOnVictim = victimKills.getOrDefault(killerUuid, 0);
            if (killsOnVictim >= MAX_KILLS_PER_VICTIM) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get kill streak for a player.
     */
    public static int getKillStreak(UUID playerUuid) {
        return killStreaks.getOrDefault(playerUuid, 0);
    }

    /**
     * Clean up old kill tracking data.
     */
    private static void cleanupKillTracking() {
        long cutoff = System.currentTimeMillis() - (KILL_STREAK_WINDOW_MS * 2);
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Map<UUID, Integer>> entry : killTracking.entrySet()) {
            // Remove entries older than 2 streak windows
            Long lastKill = lastKillTime.get(entry.getKey());
            if (lastKill != null && lastKill < cutoff) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID uuid : toRemove) {
            killTracking.remove(uuid);
            killStreaks.remove(uuid);
            lastKillTime.remove(uuid);
        }
    }

    /**
     * Check if a player is on cooldown for claiming bounties.
     */
    public static boolean isOnClaimCooldown(UUID playerUuid) {
        // Could add claim cooldowns here if needed
        return false;
    }

    /**
     * Clean up expired bounties.
     */
    public static void cleanExpiredBounties() {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE bounties SET active = 0 WHERE expires_at < ? AND active = 1")) {
            ps.setLong(1, now);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                SiDaStuffSmp.getInstance().getLogger().info("Deactivated " + updated + " expired bounties");
                loadBountyCache();
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().severe("Failed to clean expired bounties: " + e.getMessage());
        }
    }

    public static String formatPrice(double amount) {
        if (economy == null) {
            // Fallback format when economy is not available
            if (amount >= 1000000) {
                return String.format("$%.1fM", amount / 1000000);
            } else if (amount >= 1000) {
                return String.format("$%.1fK", amount / 1000);
            } else {
                return String.format("$%.2f", amount);
            }
        }
        return economy.format(amount);
    }

    /**
     * Get remaining cooldown for a player in seconds.
     */
    public static long getBountyCooldownRemaining(UUID playerUuid) {
        Long expiry = bountyCooldowns.get(playerUuid);
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Set cooldown for a player after placing a bounty.
     */
    public static void setBountyCooldown(UUID playerUuid) {
        long seconds = BountyConfig.cooldownSeconds();
        bountyCooldowns.put(playerUuid, System.currentTimeMillis() + seconds * 1000L);
    }

    /**
     * Clear cooldown for a player.
     */
    public static void clearBountyCooldown(UUID playerUuid) {
        bountyCooldowns.remove(playerUuid);
    }
}

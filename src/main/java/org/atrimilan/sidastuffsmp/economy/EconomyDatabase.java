package org.atrimilan.sidastuffsmp.economy;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EconomyDatabase {

    private static Connection connection;
    private static File dbFile;

    private static final ThreadLocal<PreparedStatement> hasAccountPs = withSql("SELECT 1 FROM player_balances WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> getBalancePs = withSql("SELECT balance FROM player_balances WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> createAccountPs = withSql("INSERT OR IGNORE INTO player_balances (uuid, name, balance, last_updated) VALUES (?, ?, ?, ?)");
    private static final ThreadLocal<PreparedStatement> addToBalancePs = withSql("UPDATE player_balances SET balance = balance + ?, last_updated = ? WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> subtractFromBalancePs = withSql("UPDATE player_balances SET balance = balance - ?, last_updated = ? WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> setBalancePs = withSql("UPDATE player_balances SET balance = ?, last_updated = ? WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> updateNamePs = withSql("UPDATE player_balances SET name = ?, last_updated = ? WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> addTransactionPs = withSql("INSERT INTO economy_transactions (uuid, type, amount, target_uuid, target_name, description, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)");
    private static final ThreadLocal<PreparedStatement> getTransactionsPs = withSql("SELECT * FROM economy_transactions WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?");
    private static final ThreadLocal<PreparedStatement> getTopPlayersPs = withSql("SELECT uuid, name, balance FROM player_balances ORDER BY balance DESC LIMIT ?");
    private static final ThreadLocal<PreparedStatement> getPlayerRankPs = withSql("SELECT COUNT(*) + 1 AS rank FROM player_balances WHERE balance > (SELECT balance FROM player_balances WHERE uuid = ?)");
    private static final ThreadLocal<PreparedStatement> getPlayerNamePs = withSql("SELECT name FROM player_balances WHERE uuid = ?");
    private static final ThreadLocal<PreparedStatement> getUuidByNamePs = withSql("SELECT uuid FROM player_balances WHERE name = ? COLLATE NOCASE");

    private EconomyDatabase() {}

    private static ThreadLocal<PreparedStatement> withSql(String sql) {
        return ThreadLocal.withInitial(() -> {
            try {
                return connection.prepareStatement(sql);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to prepare statement: " + sql, e);
            }
        });
    }

    public static void init(SiDaStuffSmp plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dbFile = new File(plugin.getDataFolder(), "economy.db");
        try {
            connect();
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize economy database: " + e.getMessage());
        }
    }

    public static void shutdown() {
        for (ThreadLocal<?> tl : List.of(hasAccountPs, getBalancePs, createAccountPs, addToBalancePs,
                subtractFromBalancePs, setBalancePs, updateNamePs, addTransactionPs, getTransactionsPs,
                getTopPlayersPs, getPlayerRankPs, getPlayerNamePs, getUuidByNamePs)) {
            PreparedStatement ps = (PreparedStatement) tl.get();
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
            tl.remove();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
        connection = null;
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
                CREATE TABLE IF NOT EXISTS player_balances (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    balance REAL NOT NULL DEFAULT 0.0,
                    last_updated INTEGER NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS economy_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    target_uuid TEXT,
                    target_name TEXT,
                    description TEXT,
                    timestamp INTEGER NOT NULL
                )
                """);
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_eco_uuid ON economy_transactions(uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_eco_timestamp ON economy_transactions(timestamp)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_eco_balance ON player_balances(balance DESC)");
            } catch (SQLException ignored) {
            }
        }
    }

    public static boolean hasAccount(UUID uuid) {
        try {
            PreparedStatement ps = hasAccountPs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("hasAccount failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean createAccount(UUID uuid, String name, double startingBalance) {
        try {
            PreparedStatement ps = createAccountPs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, startingBalance);
            ps.setLong(4, System.currentTimeMillis());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("createAccount failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public static double getBalance(UUID uuid) {
        try {
            PreparedStatement ps = getBalancePs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getBalance failed for " + uuid + ": " + e.getMessage());
        }
        return 0.0;
    }

    public static boolean setBalance(UUID uuid, double amount) {
        try {
            PreparedStatement ps = setBalancePs.get();
            ps.clearParameters();
            ps.setDouble(1, amount);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("setBalance failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean addToBalance(UUID uuid, double amount) {
        try {
            PreparedStatement ps = addToBalancePs.get();
            ps.clearParameters();
            ps.setDouble(1, amount);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("addToBalance failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean subtractFromBalance(UUID uuid, double amount) {
        try {
            PreparedStatement ps = subtractFromBalancePs.get();
            ps.clearParameters();
            ps.setDouble(1, amount);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("subtractFromBalance failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public static void updatePlayerName(UUID uuid, String name) {
        try {
            PreparedStatement ps = updateNamePs.get();
            ps.clearParameters();
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("updatePlayerName failed for " + uuid + ": " + e.getMessage());
        }
    }

    public static String getPlayerName(UUID uuid) {
        try {
            PreparedStatement ps = getPlayerNamePs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getPlayerName failed for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    public static UUID getUuidByName(String name) {
        try {
            PreparedStatement ps = getUuidByNamePs.get();
            ps.clearParameters();
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getUuidByName failed for " + name + ": " + e.getMessage());
        }
        return null;
    }

    public static List<UUID> getAllAccountUuids() {
        List<UUID> uuids = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid FROM player_balances")) {
            while (rs.next()) {
                uuids.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getAllAccountUuids failed: " + e.getMessage());
        }
        return uuids;
    }

    public static void addTransaction(UUID uuid, String type, double amount, UUID targetUuid, String targetName, String description) {
        try {
            PreparedStatement ps = addTransactionPs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, targetUuid != null ? targetUuid.toString() : null);
            ps.setString(5, targetName);
            ps.setString(6, description);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("addTransaction failed for " + uuid + ": " + e.getMessage());
        }
    }

    public static List<TransactionRecord> getTransactions(UUID uuid, int limit) {
        List<TransactionRecord> records = new ArrayList<>();
        try {
            PreparedStatement ps = getTransactionsPs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String targetUuidStr = rs.getString("target_uuid");
                    records.add(new TransactionRecord(
                            rs.getInt("id"),
                            uuid,
                            rs.getString("type"),
                            rs.getDouble("amount"),
                            targetUuidStr != null ? UUID.fromString(targetUuidStr) : null,
                            rs.getString("target_name"),
                            rs.getString("description"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getTransactions failed for " + uuid + ": " + e.getMessage());
        }
        return records;
    }

    public static List<TopEntry> getTopPlayers(int count) {
        List<TopEntry> entries = new ArrayList<>();
        try {
            PreparedStatement ps = getTopPlayersPs.get();
            ps.clearParameters();
            ps.setInt(1, count);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new TopEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getDouble("balance")
                    ));
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getTopPlayers failed: " + e.getMessage());
        }
        return entries;
    }

    public static int getPlayerRank(UUID uuid) {
        try {
            PreparedStatement ps = getPlayerRankPs.get();
            ps.clearParameters();
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("rank");
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("getPlayerRank failed for " + uuid + ": " + e.getMessage());
        }
        return -1;
    }

    public static Connection getConnection() {
        return connection;
    }

    public record TopEntry(UUID uuid, String name, double balance) {}
}
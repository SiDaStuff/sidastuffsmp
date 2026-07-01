package org.atrimilan.sidastuffsmp.stats;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

public final class PlayerStatsManager {

    private static Connection connection;
    private static File dbFile;
    private static final Object DB_LOCK = new Object();

    private PlayerStatsManager() {}

    public static void init(SiDaStuffSmp plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dbFile = new File(plugin.getDataFolder(), "player_stats.db");
        try {
            connect();
            createTable();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize player stats database: " + e.getMessage());
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

    private static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
        }
    }

    private static void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    kills       INTEGER NOT NULL DEFAULT 0,
                    deaths      INTEGER NOT NULL DEFAULT 0,
                    rtp_count   INTEGER NOT NULL DEFAULT 0,
                    playtime_seconds INTEGER NOT NULL DEFAULT 0
                )
                """);
        }
    }

    public static void ensurePlayer(String uuid, String name) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_stats (player_uuid, player_name, kills, deaths, rtp_count, playtime_seconds) "
                            + "VALUES (?, ?, 0, 0, 0, 0) "
                            + "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name")) {
                ps.setString(1, uuid);
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void incrementKills(UUID playerUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(SiDaStuffSmp.getInstance(), () -> {
            synchronized (DB_LOCK) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE player_stats SET kills = kills + 1 WHERE player_uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                } catch (SQLException ignored) {
                }
            }
        });
    }

    public static void incrementDeaths(UUID playerUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(SiDaStuffSmp.getInstance(), () -> {
            synchronized (DB_LOCK) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE player_stats SET deaths = deaths + 1 WHERE player_uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                } catch (SQLException ignored) {
                }
            }
        });
    }

    public static void incrementRtpCount(UUID playerUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(SiDaStuffSmp.getInstance(), () -> {
            synchronized (DB_LOCK) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE player_stats SET rtp_count = rtp_count + 1 WHERE player_uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                } catch (SQLException ignored) {
                }
            }
        });
    }

    public static void updatePlaytime(UUID playerUuid, int seconds) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET playtime_seconds = playtime_seconds + ? WHERE player_uuid = ?")) {
                ps.setInt(1, seconds);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException ignored) {
            }
        }
    }

    public static PlayerStats getStats(UUID playerUuid) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_name, kills, deaths, rtp_count, playtime_seconds FROM player_stats WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return extractStats(rs);
                    }
                }
            } catch (SQLException ignored) {
            }
            return new PlayerStats("Unknown", 0, 0, 0, 0, 0);
        }
    }

    public static List<PlayerStats> getAllStats() {
        List<PlayerStats> results = new java.util.ArrayList<>();
        synchronized (DB_LOCK) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT player_name, kills, deaths, rtp_count, playtime_seconds FROM player_stats")) {
                while (rs.next()) {
                    results.add(extractStats(rs));
                }
            } catch (SQLException ignored) {
            }
        }
        return results;
    }

    private static PlayerStats extractStats(ResultSet rs) throws SQLException {
        String name = rs.getString("player_name");
        int kills = rs.getInt("kills");
        int deaths = rs.getInt("deaths");
        int rtp = rs.getInt("rtp_count");
        int playtime = rs.getInt("playtime_seconds");
        double kdr = deaths > 0 ? (double) kills / deaths : kills;
        return new PlayerStats(name, kills, deaths, rtp, playtime, kdr);
    }

    public static void wipeStats(UUID playerUuid) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_stats WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException ignored) { }
        }
    }

    public static void restoreStats(UUID playerUuid, String playerName, int kills, int deaths, int rtpCount, int playtimeSeconds) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_stats (player_uuid, player_name, kills, deaths, rtp_count, playtime_seconds) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, " +
                "kills = excluded.kills, deaths = excluded.deaths, rtp_count = excluded.rtp_count, " +
                "playtime_seconds = excluded.playtime_seconds")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerName);
                ps.setInt(3, kills);
                ps.setInt(4, deaths);
                ps.setInt(5, rtpCount);
                ps.setInt(6, playtimeSeconds);
                ps.executeUpdate();
            } catch (SQLException ignored) { }
        }
    }

    public record PlayerStats(String playerName, int kills, int deaths, int rtpCount, int playtimeSeconds, double kdr) {}

    public static UUID getUuidByName(String name) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_uuid FROM player_stats WHERE player_name = ? COLLATE NOCASE LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return UUID.fromString(rs.getString("player_uuid"));
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }

    public static String getPlayerName(UUID uuid) {
        synchronized (DB_LOCK) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_name FROM player_stats WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("player_name");
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }
}
package org.atrimilan.sidastuffsmp.home;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class HomeManager {

    private static java.sql.Connection conn;
    private static SiDaStuffSmp plugin;

    private HomeManager() {}

    public static void init(SiDaStuffSmp inst) {
        plugin = inst;
        try {
            connect();
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to init home database", e);
        }
    }

    public static void shutdown() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    private static void connect() throws SQLException {
        java.io.File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        String url = "jdbc:sqlite:" + new java.io.File(dataFolder, "homes.db").getAbsolutePath();
        conn = DriverManager.getConnection(url);
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS homes (
                    owner_uuid TEXT NOT NULL,
                    slot INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    icon TEXT,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (owner_uuid, slot)
                )
            """);
            // Migrate old homes without icon column
            try {
                stmt.executeUpdate("ALTER TABLE homes ADD COLUMN icon TEXT");
            } catch (SQLException ignored) {}
        }
    }

    public static java.sql.Connection getConnection() {
        return conn;
    }

    public static Home getHome(UUID ownerUuid, int slot) {
        String sql = "SELECT * FROM homes WHERE owner_uuid = ? AND slot = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setInt(2, slot);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get home", e);
        }
        return null;
    }

    public static java.util.List<Home> getHomes(UUID ownerUuid) {
        java.util.List<Home> homes = new java.util.ArrayList<>();
        String sql = "SELECT * FROM homes WHERE owner_uuid = ? ORDER BY slot ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                homes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get homes", e);
        }
        return homes;
    }

    /**
     * Insert (or replace) a home slot for the given owner. The default name is "home" if null.
     */
    public static void createHome(UUID ownerUuid, int slot, String name, String world,
                                  double x, double y, double z, float yaw, float pitch) {
        if (name == null || name.isBlank()) name = "home";
        else name = name.trim();
        deleteHome(ownerUuid, slot);
        String sql = "INSERT INTO homes (owner_uuid, slot, name, world, x, y, z, yaw, pitch, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, name);
            ps.setString(4, world);
            ps.setDouble(5, x);
            ps.setDouble(6, y);
            ps.setDouble(7, z);
            ps.setFloat(8, yaw);
            ps.setFloat(9, pitch);
            ps.setLong(10, System.currentTimeMillis());
            ps.setLong(11, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create home", e);
        }
    }

    /**
     * Alias of {@link #createHome(UUID, int, String, String, double, double, double, float, float)} used by
     * the GUI sign-input flow.
     */
    public static void createHomeFromLocation(UUID ownerUuid, int slot, String name, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        createHome(ownerUuid, slot, name, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    /**
     * Return all home rows for the given owner UUID (alias for {@link #getHomes(UUID)}).
     */
    public static java.util.List<Home> getHomesByOwnerUuid(UUID ownerUuid) {
        return getHomes(ownerUuid);
    }

    public static void setHome(UUID ownerUuid, int slot, String name, Location location) {
        deleteHome(ownerUuid, slot);
        String sql = "INSERT INTO homes (owner_uuid, slot, name, world, x, y, z, yaw, pitch, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, name);
            ps.setString(4, location.getWorld().getName());
            ps.setDouble(5, location.getX());
            ps.setDouble(6, location.getY());
            ps.setDouble(7, location.getZ());
            ps.setFloat(8, location.getYaw());
            ps.setFloat(9, location.getPitch());
            ps.setLong(10, System.currentTimeMillis());
            ps.setLong(11, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set home", e);
        }
    }

    public static boolean deleteHome(UUID ownerUuid, int slot) {
        String sql = "DELETE FROM homes WHERE owner_uuid = ? AND slot = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setInt(2, slot);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete home", e);
            return false;
        }
    }

    public static String getHomeName(UUID ownerUuid, int slot) {
        String sql = "SELECT name FROM homes WHERE owner_uuid = ? AND slot = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setInt(2, slot);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get home name", e);
        }
        return null;
    }

    public static void importFromCsv(java.io.File csvFile) throws Exception {
        importFromCsvDetailed(csvFile);
    }

    /**
     * Imports homes from the given CSV file. Returns counters describing the import summary.
     * Supports both formats:
     * <pre>
     *   owner_uuid,slot,world,x,y,z,yaw,pitch,created_at,updated_at
     * </pre>
     * and a "name" column at the end if provided.
     */
    public static HomeImportResult importFromCsvDetailed(java.io.File csvFile) throws Exception {
        int imported = 0;
        int skipped = 0;
        if (csvFile == null || !csvFile.exists()) {
            return new HomeImportResult(0, 0);
        }
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
            String header = br.readLine();
            if (header == null) return new HomeImportResult(0, 0);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 9) { skipped++; continue; }
                try {
                    UUID ownerUuid = UUID.fromString(parts[0].trim());
                    int slot = Integer.parseInt(parts[1].trim());
                    String world = parts[2].trim();
                    double x = Double.parseDouble(parts[3].trim());
                    double y = Double.parseDouble(parts[4].trim());
                    double z = Double.parseDouble(parts[5].trim());
                    float yaw = Float.parseFloat(parts[6].trim());
                    float pitch = Float.parseFloat(parts[7].trim());
                    long createdAt = Long.parseLong(parts[8].trim());
                    long updatedAt = parts.length > 9 ? Long.parseLong(parts[9].trim()) : createdAt;
                    String name = parts.length > 10 ? parts[10].trim() : "home";
                    if (name.isEmpty()) name = "home";

                    String sql = "INSERT OR REPLACE INTO homes (owner_uuid, slot, name, world, x, y, z, yaw, pitch, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, ownerUuid.toString());
                        ps.setInt(2, slot);
                        ps.setString(3, name);
                        ps.setString(4, world);
                        ps.setDouble(5, x);
                        ps.setDouble(6, y);
                        ps.setDouble(7, z);
                        ps.setFloat(8, yaw);
                        ps.setFloat(9, pitch);
                        ps.setLong(10, createdAt);
                        ps.setLong(11, updatedAt);
                        ps.executeUpdate();
                    }
                    imported++;
                } catch (Exception rowEx) {
                    skipped++;
                }
            }
        }
        return new HomeImportResult(imported, skipped);
    }

    private static Home mapRow(ResultSet rs) throws SQLException {
        return new Home(
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getInt("slot"),
                rs.getString("name"),
                rs.getString("world"),
                rs.getString("icon"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"),
                rs.getLong("created_at"),
                rs.getLong("updated_at")
        );
    }

    /**
     * Update the icon for a home slot.
     */
    public static void setHomeIcon(UUID ownerUuid, int slot, String icon) {
        String sql = "UPDATE homes SET icon = ?, updated_at = ? WHERE owner_uuid = ? AND slot = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, icon);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, ownerUuid.toString());
            ps.setInt(4, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set home icon", e);
        }
    }

    /**
     * Rename the home in the given slot. Falls back to "home" if the provided
     * name is null/blank. Returns true when a row was actually updated.
     * Prevents two homes from having the same name for the same player.
     */
    public static boolean renameHome(UUID ownerUuid, int slot, String newName) {
        if (newName == null || newName.isBlank()) newName = "home";
        else newName = newName.trim();
        // Check if another home of the same player already has this name (excluding the current slot)
        String checkSql = "SELECT COUNT(*) FROM homes WHERE owner_uuid = ? AND name = ? AND slot != ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, ownerUuid.toString());
            checkPs.setString(2, newName);
            checkPs.setInt(3, slot);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Another home with this name already exists
                    return false;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check for duplicate home name", e);
            return false;
        }
        String sql = "UPDATE homes SET name = ?, updated_at = ? WHERE owner_uuid = ? AND slot = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, ownerUuid.toString());
            ps.setInt(4, slot);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to rename home", e);
            return false;
        }
    }

    /**
     * Get all homes for all players (for Firebase sync).
     */
    public static List<Home> getAllHomes() {
        List<Home> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                homes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get all homes", e);
        }
        return homes;
    }

    /**
     * Get all homes for a specific owner (for Firebase sync).
     */
    public static List<Home> getAllHomesForOwner(UUID ownerUuid) {
        List<Home> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE owner_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                homes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get all homes for owner", e);
        }
        return homes;
    }

    public static int deleteAllHomes(UUID ownerUuid) {
        String sql = "DELETE FROM homes WHERE owner_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete all homes for " + ownerUuid, e);
            return 0;
        }
    }

    /**
     * Import a home from Firebase (upsert).
     */
    public static void importHome(Home home) {
        String sql = "INSERT OR REPLACE INTO homes (owner_uuid, slot, name, world, icon, x, y, z, yaw, pitch, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, home.getOwnerUuid().toString());
            ps.setInt(2, home.getSlot());
            ps.setString(3, home.getName());
            ps.setString(4, home.getWorld());
            ps.setString(5, home.getIcon());
            ps.setDouble(6, home.getX());
            ps.setDouble(7, home.getY());
            ps.setDouble(8, home.getZ());
            ps.setFloat(9, home.getYaw());
            ps.setFloat(10, home.getPitch());
            ps.setLong(11, home.getCreatedAt());
            ps.setLong(12, home.getUpdatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to import home", e);
        }
    }
}

package org.atrimilan.sidastuffsmp.home;

import org.bukkit.Location;

import java.util.UUID;

public class Home {
    private final UUID ownerUuid;
    private final int slot;
    private String name;
    private String world;
    private String icon; // Material name for the home icon, e.g., "RED_BED" or "PLAYER_HEAD"
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private long createdAt;
    private long updatedAt;

    public Home(UUID ownerUuid, int slot, String name, String world, double x, double y, double z, float yaw, float pitch, long createdAt, long updatedAt) {
        this(ownerUuid, slot, name, world, null, x, y, z, yaw, pitch, createdAt, updatedAt);
    }

    public Home(UUID ownerUuid, int slot, String name, String world, String icon, double x, double y, double z, float yaw, float pitch, long createdAt, long updatedAt) {
        this.ownerUuid = ownerUuid;
        this.slot = slot;
        this.name = name;
        this.world = world;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public int getSlot() { return slot; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Location toLocation(org.bukkit.World ignored) {
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    /**
     * Returns a human-friendly name for the world this home is stored in (e.g. "Overworld",
     * "Nether", "The End"). Falls back to the raw Bukkit world name when it can't classify.
     */
    public String getFriendlyWorldName() {
        if (world == null) return "Unknown";
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
        if (w != null) {
            org.bukkit.World.Environment env = w.getEnvironment();
            switch (env) {
                case NORMAL: return "Overworld";
                case NETHER: return "Nether";
                case THE_END: return "The End";
                default: return w.getName();
            }
        }
        String lower = world.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("nether")) return "Nether";
        if (lower.contains("end") || lower.contains("the_end")) return "The End";
        if (lower.contains("overworld") || lower.equals("world")) return "Overworld";
        return world;
    }

    public static Home fromLocation(UUID ownerUuid, int slot, String name, Location location) {
        return fromLocation(ownerUuid, slot, name, null, location);
    }

    public static Home fromLocation(UUID ownerUuid, int slot, String name, String icon, Location location) {
        return new Home(
                ownerUuid,
                slot,
                name,
                location.getWorld().getName(),
                icon,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );
    }

    /**
     * Serialize this home to a Map for Firebase sync.
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("ownerUuid", ownerUuid.toString());
        map.put("slot", slot);
        map.put("name", name);
        map.put("world", world);
        map.put("icon", icon);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    /**
     * Deserialize a home from a Firebase map.
     */
    public static Home fromMap(java.util.Map<String, Object> map) {
        if (map == null) return null;
        try {
            return new Home(
                    UUID.fromString((String) map.get("ownerUuid")),
                    ((Number) map.get("slot")).intValue(),
                    (String) map.get("name"),
                    (String) map.get("world"),
                    (String) map.get("icon"),
                    ((Number) map.get("x")).doubleValue(),
                    ((Number) map.get("y")).doubleValue(),
                    ((Number) map.get("z")).doubleValue(),
                    ((Number) map.get("yaw")).floatValue(),
                    ((Number) map.get("pitch")).floatValue(),
                    ((Number) map.get("createdAt")).longValue(),
                    ((Number) map.get("updatedAt")).longValue()
            );
        } catch (Exception e) {
            return null;
        }
    }
}

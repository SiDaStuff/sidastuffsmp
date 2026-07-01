package org.atrimilan.sidastuffsmp.home;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class HomeConfig {
    private static FileConfiguration config;
    private static File configFile;

    public static void init(SiDaStuffSmp plugin) {
        configFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!configFile.exists()) {
            saveDefaults(plugin);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private static void saveDefaults(SiDaStuffSmp plugin) {
        YamlConfiguration c = new YamlConfiguration();
        c.set("homes.enabled", true);
        c.set("homes.max-homes", 14);
        c.set("homes.teleport-delay-seconds", 5);
        c.set("homes.cancel-on-move", true);
        c.set("homes.cancel-on-damage", true);
        c.set("homes.blocked-worlds", Arrays.asList("world_nether", "world_the_end"));
        c.set("homes.cross-world-teleport", true);
        c.set("homes.backup-csv", "backup_homes.csv");
        try { c.save(new File(plugin.getDataFolder(), "homes.yml")); } catch (IOException ignored) {}
    }

    public static boolean enabled() {
        return config.getBoolean("homes.enabled", true);
    }

    public static int maxHomes() {
        return config.getInt("homes.max-homes", 14);
    }

    public static int teleportDelaySeconds() {
        return config.getInt("homes.teleport-delay-seconds", 5);
    }

    public static boolean cancelOnMove() {
        return config.getBoolean("homes.cancel-on-move", true);
    }

    public static boolean cancelOnDamage() {
        return config.getBoolean("homes.cancel-on-damage", true);
    }

    public static boolean crossWorldTeleport() {
        return config.getBoolean("homes.cross-world-teleport", true);
    }

    public static List<String> getBlockedWorlds() {
        return config.getStringList("homes.blocked-worlds");
    }

    public static String getBackupCsvName() {
        return config.getString("homes.backup-csv", "backup_homes.csv");
    }

    public static boolean isWorldBlocked(String world) {
        if (world == null) return false;
        return getBlockedWorlds().contains(world);
    }
}

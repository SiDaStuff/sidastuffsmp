package org.atrimilan.sidastuffsmp.rtp;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RtpConfig {

    private static final Set<Material> BLACKLISTED_BLOCKS = new HashSet<>();
    private static final Set<String> BLACKLISTED_BIOMES = new HashSet<>();
    private static final Map<String, RegionConfig> REGIONS = new HashMap<>();
    private static int allowedWalkRange = 3;
    private static int waitTimeSeconds = 5;
    private static int defaultCooldownSeconds = 30;
    private static int pregenerateLocationAmount = 10;

    private RtpConfig() {}

    public static void init(SiDaStuffSmp plugin) {
        FileConfiguration config = plugin.getConfig();

        config.addDefault("blacklisted-blocks", java.util.List.of("LAVA", "WATER", "BEDROCK", "AIR"));
        config.addDefault("blacklisted-biomes", java.util.List.of("BEACH"));
        config.addDefault("allowed-walk-range", 3);
        config.addDefault("wait-time-seconds", 5);
        config.addDefault("default-cooldown-seconds", 30);
        config.addDefault("pregenerate-location-amount", 10);
        config.addDefault("world-settings.overworld-region.world", "world");
        config.addDefault("world-settings.overworld-region.min-x", -10000);
        config.addDefault("world-settings.overworld-region.max-x", 10000);
        config.addDefault("world-settings.overworld-region.min-z", -10000);
        config.addDefault("world-settings.overworld-region.max-z", 10000);
        config.addDefault("world-settings.overworld-region.min-y", 0);
        config.addDefault("world-settings.overworld-region.max-y", 320);
        config.addDefault("world-settings.nether-region.world", "world_nether");
        config.addDefault("world-settings.nether-region.min-x", -5000);
        config.addDefault("world-settings.nether-region.max-x", 5000);
        config.addDefault("world-settings.nether-region.min-z", -5000);
        config.addDefault("world-settings.nether-region.max-z", 5000);
        config.addDefault("world-settings.nether-region.min-y", 0);
        config.addDefault("world-settings.nether-region.max-y", 120);
        config.addDefault("world-settings.end-region.world", "world_the_end");
        config.addDefault("world-settings.end-region.min-x", -7500);
        config.addDefault("world-settings.end-region.max-x", 7500);
        config.addDefault("world-settings.end-region.min-z", -7500);
        config.addDefault("world-settings.end-region.max-z", 7500);
        config.addDefault("world-settings.end-region.min-y", 0);
        config.addDefault("world-settings.end-region.max-y", 320);
        config.options().copyDefaults(true);
        plugin.saveConfig();

        BLACKLISTED_BLOCKS.clear();
        List<String> blocks = config.getStringList("blacklisted-blocks");
        for (String name : blocks) {
            try {
                Material mat = Material.matchMaterial(name);
                if (mat != null) BLACKLISTED_BLOCKS.add(mat);
            } catch (IllegalArgumentException ignored) {
            }
        }

        BLACKLISTED_BIOMES.clear();
        BLACKLISTED_BIOMES.addAll(config.getStringList("blacklisted-biomes"));

        allowedWalkRange = config.getInt("allowed-walk-range", 3);
        waitTimeSeconds = config.getInt("wait-time-seconds", 5);
        defaultCooldownSeconds = config.getInt("default-cooldown-seconds", 60);
        pregenerateLocationAmount = config.getInt("pregenerate-location-amount", 10);

        REGIONS.clear();
        ConfigurationSection worlds = config.getConfigurationSection("world-settings");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                ConfigurationSection section = worlds.getConfigurationSection(key);
                if (section == null) continue;
                String world = section.getString("world");
                int minX = section.getInt("min-x");
                int maxX = section.getInt("max-x");
                int minZ = section.getInt("min-z");
                int maxZ = section.getInt("max-z");
                int minY = section.getInt("min-y", 0);
                int maxY = section.getInt("max-y", 320);
                if (world != null) {
                    REGIONS.put(key, new RegionConfig(world, minX, maxX, minZ, maxZ, minY, maxY));
                }
            }
        }
    }

    public static Set<Material> getBlacklistedBlocks() {
        return BLACKLISTED_BLOCKS;
    }

    public static Set<String> getBlacklistedBiomes() {
        return BLACKLISTED_BIOMES;
    }

    public static int getAllowedWalkRange() {
        return allowedWalkRange;
    }

    public static int getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    public static int getDefaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    public static int getPregenerateLocationAmount() {
        return pregenerateLocationAmount;
    }

    public static RegionConfig getRegion(String key) {
        return REGIONS.get(key);
    }

    public static List<RegionConfig> getAllRegions() {
        return new ArrayList<>(REGIONS.values());
    }

    public record RegionConfig(String world, int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {}
}

package org.atrimilan.sidastuffsmp.bounty;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BountyConfig {

    private static FileConfiguration config;
    private static File configFile;

    private BountyConfig() {}

    public static void init(SiDaStuffSmp plugin) {
        configFile = new File(plugin.getDataFolder(), "bounties.yml");
        if (!configFile.exists()) {
            saveDefaults(plugin);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static boolean enabled() {
        return config.getBoolean("bounties.enabled", true);
    }

    public static double minBounty() {
        return config.getDouble("bounties.min-bounty", 10.0);
    }

    public static double maxBounty() {
        return config.getDouble("bounties.max-bounty", 1000000.0);
    }

    public static int maxBountiesPerPlayer() {
        return config.getInt("bounties.max-bounties-per-player", 10);
    }

    public static int guiItemsPerPage() {
        return config.getInt("bounties.gui-items-per-page", 45);
    }

    public static long expireDays() {
        return config.getLong("bounties.expire-days", 7L);
    }

    public static long cooldownSeconds() {
        return config.getLong("bounties.cooldown-seconds", 30L);
    }

    public static java.util.List<Double> presetAmounts() {
        java.util.List<Double> amounts = config.getDoubleList("bounties.gui-presets");
        if (amounts.isEmpty()) {
            amounts = java.util.List.of(minBounty(), 1000.0, 10000.0, 100000.0);
        }
        return amounts;
    }

    public static String message(String key, String fallback) {
        return config.getString("bounties.messages." + key, fallback);
    }

    private static void saveDefaults(SiDaStuffSmp plugin) {
        config = new YamlConfiguration();
        config.set("bounties.enabled", true);
        config.set("bounties.min-bounty", 10.0);
        config.set("bounties.max-bounty", 1000000.0);
        config.set("bounties.max-bounties-per-player", 10);
        config.set("bounties.gui-items-per-page", 45);
        config.set("bounties.expire-days", 7L);
        config.set("bounties.cooldown-seconds", 30L);
        config.set("bounties.gui-presets", java.util.List.of(100.0, 1000.0, 10000.0, 100000.0));
        config.set("bounties.messages.cooldown", "Please wait before placing another bounty.");
        config.set("bounties.messages.confirm-title", "Confirm Bounty");
        config.set("bounties.messages.created", "Bounty placed successfully.");
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save bounties.yml: " + e.getMessage());
        }
    }
}

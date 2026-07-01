package org.atrimilan.sidastuffsmp.auction;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AuctionConfig {

    private static FileConfiguration config;
    private static File configFile;

    private AuctionConfig() {}

    public static void init(SiDaStuffSmp plugin) {
        configFile = new File(plugin.getDataFolder(), "auction.yml");
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
        return config.getBoolean("auction.enabled", true);
    }

    public static long listingDurationHours() {
        return config.getLong("auction.listing-duration-hours", 168L);
    }

    public static int maxActiveListings() {
        return config.getInt("auction.max-active-listings-per-player", 10);
    }

    public static double maxPrice() {
        return config.getDouble("auction.max-price", 1000000.0);
    }

    public static double minPrice() {
        return config.getDouble("auction.min-price", 1.0);
    }

    public static double listingFeePercent() {
        return config.getDouble("auction.listing-fee-percent", 0.0);
    }

    public static long expireGraceHours() {
        return config.getLong("auction.expire-grace-hours", 48L);
    }

    public static long syncIntervalSeconds() {
        return config.getLong("auction.sync-interval-seconds", 60L);
    }

    private static void saveDefaults(SiDaStuffSmp plugin) {
        config = new YamlConfiguration();
        config.set("auction.enabled", true);
        config.set("auction.listing-duration-hours", 168L);
        config.set("auction.max-active-listings-per-player", 10);
        config.set("auction.max-price", 1000000.0);
        config.set("auction.min-price", 1.0);
        config.set("auction.listing-fee-percent", 0.0);
        config.set("auction.expire-grace-hours", 48L);
        config.set("auction.sync-interval-seconds", 60L);
        config.set("auction.categories.WEAPONS", "Swords, Axes, Bows, Tridents");
        config.set("auction.categories.ARMOR", "Helmets, Chestplates, Leggings, Boots");
        config.set("auction.categories.TOOLS", "Pickaxes, Shovels, Hoes, Shears");
        config.set("auction.categories.FOOD", "Food, Potions");
        config.set("auction.categories.BLOCKS", "Building Blocks, Ores");
        config.set("auction.categories.REDSTONE", "Redstone, Mechanisms");
        config.set("auction.categories.MISC", "Everything Else");
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save default auction.yml: " + e.getMessage());
        }
    }
}

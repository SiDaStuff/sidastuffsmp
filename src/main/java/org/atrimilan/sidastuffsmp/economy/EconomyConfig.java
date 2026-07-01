package org.atrimilan.sidastuffsmp.economy;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class EconomyConfig {

    private static FileConfiguration config;
    private static File configFile;

    private EconomyConfig() {}

    public static void init(SiDaStuffSmp plugin) {
        configFile = new File(plugin.getDataFolder(), "economy.yml");
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

    public static double startingBalance() {
        return config.getDouble("economy.starting-balance", 100.0);
    }

    public static double maxBalance() {
        return config.getDouble("economy.max-balance", 1000000000.0);
    }

    public static double minBalance() {
        return config.getDouble("economy.min-balance", 0.0);
    }

    public static String currencyName() {
        return config.getString("economy.currency.name", "Dollar");
    }

    public static String currencyNamePlural() {
        return config.getString("economy.currency.name-plural", "Dollars");
    }

    public static String currencySymbol() {
        return config.getString("economy.currency.symbol", "$");
    }

    public static boolean provideVaultEconomy() {
        return config.getBoolean("economy.provide-vault-economy", true);
    }

    public static long topUpdateSeconds() {
        return config.getLong("economy.top-update-seconds", 300L);
    }

    public static int historyLimit() {
        return config.getInt("economy.history-limit", 100);
    }

    public static long vaultSyncSeconds() {
        return config.getLong("economy.vault-sync-seconds", 30L);
    }

    private static void saveDefaults(SiDaStuffSmp plugin) {
        config = new YamlConfiguration();
        config.set("economy.starting-balance", 100.0);
        config.set("economy.max-balance", 1000000000.0);
        config.set("economy.min-balance", 0.0);
        config.set("economy.currency.name", "Dollar");
        config.set("economy.currency.name-plural", "Dollars");
        config.set("economy.currency.symbol", "$");
        config.set("economy.provide-vault-economy", true);
        config.set("economy.top-update-seconds", 300L);
        config.set("economy.history-limit", 100);
        config.set("economy.vault-sync-seconds", 30L);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save default economy.yml: " + e.getMessage());
        }
    }
}

package org.atrimilan.sidastuffsmp.sync;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FirebaseConfig {

    private static FileConfiguration config;
    private static File configFile;

    private FirebaseConfig() {}

    public static void init(SiDaStuffSmp plugin) {
        configFile = new File(plugin.getDataFolder(), "firebase.yml");
        if (!configFile.exists()) {
            saveDefaults(plugin);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static boolean enabled() {
        return config.getBoolean("firebase.enabled", false);
    }

    public static String getServiceAccountPath() {
        return config.getString("firebase.service-account-json", "firebase-service-account.json");
    }

    public static String getDatabaseUrl() {
        return config.getString("firebase.database-url", "");
    }

	public static String getSyncPath() {
		return config.getString("firebase.sync-path", "auction/listings");
	}

	public static String getOrdersSyncPath() {
		return config.getString("firebase.orders-sync-path", "orders/listings");
	}

	public static String getStatsSyncPath() {
		return config.getString("firebase.stats-sync-path", "player_stats");
	}

	public static String getBountiesSyncPath() {
		return config.getString("firebase.bounties-sync-path", "bounties/active");
	}

	public static String getHomesSyncPath() {
		return config.getString("firebase.homes-sync-path", "homes");
	}

    public static InputStream getServiceAccountStream(SiDaStuffSmp plugin) {
        String path = getServiceAccountPath();
        File absolute = new File(path);
        if (absolute.isAbsolute() && absolute.exists()) {
            try {
                return new FileInputStream(absolute);
            } catch (IOException e) {
                return null;
            }
        }

        File relative = new File(plugin.getDataFolder(), path);
        if (relative.exists()) {
            try {
                return new FileInputStream(relative);
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    private static void saveDefaults(SiDaStuffSmp plugin) {
        config = new YamlConfiguration();
        config.set("firebase.enabled", false);
        config.set("firebase.service-account-json", "firebase-service-account.json");
        config.set("firebase.database-url", "https://your-project.firebaseio.com");
		config.set("firebase.sync-path", "auction/listings");
		config.set("firebase.orders-sync-path", "orders/listings");
		config.set("firebase.stats-sync-path", "player_stats");
		config.set("firebase.bounties-sync-path", "bounties/active");
        config.set("firebase.homes-sync-path", "homes");
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save default firebase.yml: " + e.getMessage());
        }
    }
}

package org.atrimilan.sidastuffsmp.rtp;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.YamlConfiguration;

public final class RtpCooldown {

    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static File dataFile;

    private RtpCooldown() {}

    public static void init(SiDaStuffSmp plugin) {
        dataFile = new File(plugin.getDataFolder(), "rtp_cooldowns.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        loadAll();
    }

    public static long getRemainingCooldown(UUID uuid) {
        Long expiry = COOLDOWNS.get(uuid);
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public static void setCooldown(UUID uuid) {
        int seconds = RtpConfig.getDefaultCooldownSeconds();
        COOLDOWNS.put(uuid, System.currentTimeMillis() + seconds * 1000L);
        saveSingle(uuid);
    }

    public static void clearCooldown(UUID uuid) {
        COOLDOWNS.remove(uuid);
        saveSingle(uuid);
    }

    public static boolean isOnCooldown(UUID uuid) {
        return getRemainingCooldown(uuid) > 0;
    }

    private static void loadAll() {
        if (dataFile == null || !dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long expiry = config.getLong(key);
                if (expiry > System.currentTimeMillis()) {
                    COOLDOWNS.put(uuid, expiry);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void saveSingle(UUID uuid) {
        if (dataFile == null) return;
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : COOLDOWNS.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(dataFile);
        } catch (IOException ignored) {
        }
    }

    public static void cleanupExpired() {
        if (dataFile == null) return;
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= System.currentTimeMillis());
        saveAll();
    }

    public static void saveAll() {
        if (dataFile == null) return;
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : COOLDOWNS.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(dataFile);
        } catch (IOException ignored) {
        }
    }
}
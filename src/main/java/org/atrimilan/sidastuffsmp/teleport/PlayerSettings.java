package org.atrimilan.sidastuffsmp.teleport;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerSettings {

    private static final Map<UUID, PlayerSettings> CACHE = new ConcurrentHashMap<>();
    private static File dataFile;

    private final UUID playerUuid;
    private boolean tpaEnabled = true;
    private boolean tpaHereEnabled = true;
    private boolean tpaAutoEnabled = false;
    private boolean nightVisionEnabled = false;
    private boolean orderMessagesEnabled = true;
    private boolean auctionMessagesEnabled = true;
    private boolean confirmAuctionListing = true;
    private boolean confirmTpGui = true;

    private PlayerSettings(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public boolean isTpaEnabled() { return tpaEnabled; }
    public void setTpaEnabled(boolean enabled) { this.tpaEnabled = enabled; markDirty(); }
    public boolean isTpaHereEnabled() { return tpaHereEnabled; }
    public void setTpaHereEnabled(boolean enabled) { this.tpaHereEnabled = enabled; markDirty(); }
    public boolean isTpaAutoEnabled() { return tpaAutoEnabled; }
    public void setTpaAutoEnabled(boolean enabled) { this.tpaAutoEnabled = enabled; markDirty(); }
    public boolean isNightVisionEnabled() { return nightVisionEnabled; }
    public void setNightVisionEnabled(boolean enabled) { this.nightVisionEnabled = enabled; markDirty(); }
    public boolean isOrderMessagesEnabled() { return orderMessagesEnabled; }
    public void setOrderMessagesEnabled(boolean enabled) { this.orderMessagesEnabled = enabled; markDirty(); }
    public boolean isAuctionMessagesEnabled() { return auctionMessagesEnabled; }
    public void setAuctionMessagesEnabled(boolean enabled) { this.auctionMessagesEnabled = enabled; markDirty(); }
    public boolean isConfirmAuctionListing() { return confirmAuctionListing; }
    public void setConfirmAuctionListing(boolean enabled) { this.confirmAuctionListing = enabled; markDirty(); }
    public boolean isConfirmTpGui() { return confirmTpGui; }
    public void setConfirmTpGui(boolean enabled) { this.confirmTpGui = enabled; markDirty(); }

    public static PlayerSettings get(UUID playerUuid) {
        return CACHE.computeIfAbsent(playerUuid, k -> {
            PlayerSettings settings = new PlayerSettings(k);
            loadSingle(k, settings);
            return settings;
        });
    }

    public static void init(SiDaStuffSmp plugin) {
        dataFile = new File(plugin.getDataFolder(), "player_settings.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        loadAll();
    }

    private static void loadAll() {
        if (dataFile == null || !dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection playerSection = section.getConfigurationSection(key);
                if (playerSection == null) continue;

                PlayerSettings settings = new PlayerSettings(uuid);
                settings.tpaEnabled = playerSection.getBoolean("tpa_enabled", true);
                settings.tpaHereEnabled = playerSection.getBoolean("tpahere_enabled", true);
                settings.tpaAutoEnabled = playerSection.getBoolean("tpaauto_enabled", false);
                settings.nightVisionEnabled = playerSection.getBoolean("night_vision_enabled", false);
                settings.orderMessagesEnabled = playerSection.getBoolean("order_messages_enabled", true);
                settings.auctionMessagesEnabled = playerSection.getBoolean("auction_messages_enabled", true);
                settings.confirmAuctionListing = playerSection.getBoolean("confirm_auction_listing", true);
                settings.confirmTpGui = playerSection.getBoolean("confirm_tp_gui", true);
                CACHE.put(uuid, settings);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void loadSingle(UUID uuid, PlayerSettings settings) {
        if (dataFile == null || !dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String basePath = "players." + uuid.toString();
        if (!config.contains(basePath)) return;

        settings.tpaEnabled = config.getBoolean(basePath + ".tpa_enabled", true);
        settings.tpaHereEnabled = config.getBoolean(basePath + ".tpahere_enabled", true);
        settings.tpaAutoEnabled = config.getBoolean(basePath + ".tpaauto_enabled", false);
        settings.nightVisionEnabled = config.getBoolean(basePath + ".night_vision_enabled", false);
        settings.orderMessagesEnabled = config.getBoolean(basePath + ".order_messages_enabled", true);
        settings.auctionMessagesEnabled = config.getBoolean(basePath + ".auction_messages_enabled", true);
        settings.confirmAuctionListing = config.getBoolean(basePath + ".confirm_auction_listing", true);
        settings.confirmTpGui = config.getBoolean(basePath + ".confirm_tp_gui", true);
    }

    private static final Map<UUID, Boolean> dirtyFlags = new ConcurrentHashMap<>();

    private void markDirty() {
        dirtyFlags.put(playerUuid, true);
    }

    public static void saveAll() {
        if (dataFile == null) return;
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerSettings> entry : CACHE.entrySet()) {
            String basePath = "players." + entry.getKey().toString();
            PlayerSettings s = entry.getValue();
            config.set(basePath + ".tpa_enabled", s.tpaEnabled);
            config.set(basePath + ".tpahere_enabled", s.tpaHereEnabled);
            config.set(basePath + ".tpaauto_enabled", s.tpaAutoEnabled);
            config.set(basePath + ".night_vision_enabled", s.nightVisionEnabled);
            config.set(basePath + ".order_messages_enabled", s.orderMessagesEnabled);
            config.set(basePath + ".auction_messages_enabled", s.auctionMessagesEnabled);
            config.set(basePath + ".confirm_auction_listing", s.confirmAuctionListing);
            config.set(basePath + ".confirm_tp_gui", s.confirmTpGui);
        }

        try {
            config.save(dataFile);
        } catch (IOException ignored) {
        }
        dirtyFlags.clear();
    }

    public static void saveDirty() {
        if (dirtyFlags.isEmpty()) return;
        saveAll();
    }

    public static void unload(UUID playerUuid) {
        CACHE.remove(playerUuid);
        dirtyFlags.remove(playerUuid);
    }
}

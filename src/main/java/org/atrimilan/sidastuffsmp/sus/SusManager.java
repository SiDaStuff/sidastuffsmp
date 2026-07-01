package org.atrimilan.sidastuffsmp.sus;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SusManager {

    private static final ConcurrentHashMap<UUID, SusEntry> SUS_ENTRIES = new ConcurrentHashMap<>();
    private static org.bukkit.configuration.file.YamlConfiguration dataConfig;
    private static File dataFile;

    private SusManager() {}

    public static void init(SiDaStuffSmp plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataFile = new File(plugin.getDataFolder(), "sus_list.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create sus_list.yml");
            }
        }
        dataConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        loadAll();

        if (SusConfig.getAutoRemoveDays() > 0) {
            long cutoff = System.currentTimeMillis() - (SusConfig.getAutoRemoveDays() * 24L * 60L * 60L * 1000L);
            List<UUID> toRemove = new ArrayList<>();
            for (Map.Entry<UUID, SusEntry> entry : SUS_ENTRIES.entrySet()) {
                if (entry.getValue().lastFlagTime() < cutoff) {
                    toRemove.add(entry.getKey());
                }
            }
            for (UUID uuid : toRemove) {
                SUS_ENTRIES.remove(uuid);
            }
            if (!toRemove.isEmpty()) saveAll();
        }
    }

    public static void shutdown() {
        saveAll();
        SUS_ENTRIES.clear();
    }

    public static void addPlayer(UUID playerUuid, String playerName, String flag, String source) {
        SusEntry existing = SUS_ENTRIES.get(playerUuid);
        if (existing != null) {
            List<String> flags = new ArrayList<>(existing.flags());
            if (!flags.contains(flag)) {
                flags.add(flag);
            }
            List<String> sources = new ArrayList<>(existing.sources());
            if (!sources.contains(source)) {
                sources.add(source);
            }
            SUS_ENTRIES.put(playerUuid, new SusEntry(
                    playerUuid, playerName, flags, sources, System.currentTimeMillis()
            ));
        } else {
            List<String> flags = new ArrayList<>();
            flags.add(flag);
            List<String> sources = new ArrayList<>();
            sources.add(source);
            SUS_ENTRIES.put(playerUuid, new SusEntry(
                    playerUuid, playerName, flags, sources, System.currentTimeMillis()
            ));
        }
        saveEntry(playerUuid);

        if (SUS_ENTRIES.size() > SusConfig.getMaxEntries()) {
            long oldest = Long.MAX_VALUE;
            UUID oldestUuid = null;
            for (Map.Entry<UUID, SusEntry> e : SUS_ENTRIES.entrySet()) {
                if (e.getValue().lastFlagTime() < oldest) {
                    oldest = e.getValue().lastFlagTime();
                    oldestUuid = e.getKey();
                }
            }
            if (oldestUuid != null) {
                removeSus(oldestUuid);
            }
        }
    }

    public static void addFlagToPlayer(UUID playerUuid, String flag, String source) {
        SusEntry existing = SUS_ENTRIES.get(playerUuid);
        if (existing != null) {
            addPlayer(playerUuid, existing.playerName(), flag, source);
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerUuid);
            String name = offline.getName() != null ? offline.getName() : playerUuid.toString().substring(0, 8);
            addPlayer(playerUuid, name, flag, source);
        }
    }

    public static boolean removeSus(UUID playerUuid) {
        SusEntry removed = SUS_ENTRIES.remove(playerUuid);
        if (removed != null) {
            dataConfig.set("entries." + playerUuid.toString(), null);
            saveConfigFile();
            return true;
        }
        return false;
    }

    public static boolean removeSus(String playerName) {
        for (Map.Entry<UUID, SusEntry> entry : SUS_ENTRIES.entrySet()) {
            if (entry.getValue().playerName().equalsIgnoreCase(playerName)) {
                return removeSus(entry.getKey());
            }
        }
        return false;
    }

    public static boolean isOnSusList(UUID playerUuid) {
        return SUS_ENTRIES.containsKey(playerUuid);
    }

    public static SusEntry getEntry(UUID playerUuid) {
        return SUS_ENTRIES.get(playerUuid);
    }

    public static List<SusEntry> getAllEntries() {
        List<SusEntry> entries = new ArrayList<>(SUS_ENTRIES.values());
        entries.sort((a, b) -> Long.compare(b.lastFlagTime(), a.lastFlagTime()));
        return entries;
    }

    public static void clearAll() {
        SUS_ENTRIES.clear();
        dataConfig.set("entries", null);
        saveConfigFile();
    }

    private static void loadAll() {
        SUS_ENTRIES.clear();
        org.bukkit.configuration.ConfigurationSection section = dataConfig.getConfigurationSection("entries");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "entries." + key;
                String playerName = dataConfig.getString(path + ".player-name", "Unknown");
                List<String> flags = dataConfig.getStringList(path + ".flags");
                List<String> sources = dataConfig.getStringList(path + ".sources");
                long lastFlag = dataConfig.getLong(path + ".last-flag-time", 0);
                SUS_ENTRIES.put(uuid, new SusEntry(uuid, playerName, new ArrayList<>(flags), new ArrayList<>(sources), lastFlag));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static void saveEntry(UUID playerUuid) {
        SusEntry entry = SUS_ENTRIES.get(playerUuid);
        if (entry == null) return;
        String path = "entries." + playerUuid.toString();
        dataConfig.set(path + ".player-name", entry.playerName());
        dataConfig.set(path + ".flags", entry.flags());
        dataConfig.set(path + ".sources", entry.sources());
        dataConfig.set(path + ".last-flag-time", entry.lastFlagTime());
        saveConfigFile();
    }

    private static void saveAll() {
        dataConfig.set("entries", null);
        for (Map.Entry<UUID, SusEntry> mapEntry : SUS_ENTRIES.entrySet()) {
            SusEntry entry = mapEntry.getValue();
            String path = "entries." + mapEntry.getKey().toString();
            dataConfig.set(path + ".player-name", entry.playerName());
            dataConfig.set(path + ".flags", entry.flags());
            dataConfig.set(path + ".sources", entry.sources());
            dataConfig.set(path + ".last-flag-time", entry.lastFlagTime());
        }
        saveConfigFile();
    }

    private static void saveConfigFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException ignored) {}
    }

    public record SusEntry(UUID playerUuid, String playerName, List<String> flags, List<String> sources, long lastFlagTime) {}
}

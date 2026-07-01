package org.atrimilan.sidastuffsmp.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PunishmentManager {

    private static File dataFile;
    private static YamlConfiguration dataConfig;

    private PunishmentManager() {}

    public static void init(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dataFile = new File(plugin.getDataFolder(), "punishments.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create punishments.yml.");
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private static boolean dirty = false;
    private static int saveTaskId = -1;

    public static void save() {
        dirty = true;
        if (saveTaskId == -1) {
            saveTaskId = Bukkit.getScheduler().runTaskLaterAsynchronously(
                    org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance(),
                    PunishmentManager::flushSave, 20L
            ).getTaskId();
        }
    }

    public static void flushSave() {
        if (!dirty || dataConfig == null || dataFile == null) return;
        dirty = false;
        saveTaskId = -1;
        synchronized (dataFile) {
            try {
                dataConfig.save(dataFile);
            } catch (IOException ignored) {
            }
        }
    }

    public static int getPunishCount(UUID playerId) {
        return dataConfig.getInt(playerPath(playerId) + ".punishCount", 0);
    }

    public static int incrementPunishCount(UUID playerId, String lastKnownName) {
        String basePath = playerPath(playerId);
        int newCount = getPunishCount(playerId) + 1;
        dataConfig.set(basePath + ".punishCount", newCount);
        dataConfig.set(basePath + ".lastKnownName", lastKnownName);
        save();
        return newCount;
    }

    public static void resetAllPunishCounts() {
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return;
        for (String playerId : playersSection.getKeys(false)) {
            dataConfig.set("players." + playerId + ".punishCount", 0);
        }
        save();
    }

    public static void addHistoryEntry(
            UUID playerId,
            String lastKnownName,
            String action,
            String reason,
            String source,
            int durationDays,
            long expiresAtEpochMillis
    ) {
        String basePath = playerPath(playerId);
        long timestamp = System.currentTimeMillis();
        String historyPath = basePath + ".history." + timestamp;

        dataConfig.set(basePath + ".lastKnownName", lastKnownName);
        dataConfig.set(historyPath + ".action", action);
        dataConfig.set(historyPath + ".reason", reason);
        dataConfig.set(historyPath + ".source", source);
        dataConfig.set(historyPath + ".durationDays", durationDays);
        dataConfig.set(historyPath + ".expiresAtEpochMillis", expiresAtEpochMillis);
        save();
    }

    public static List<HistoryEntry> getHistory(UUID playerId, int maxEntries) {
        List<HistoryEntry> entries = new ArrayList<>();
        String historyPath = playerPath(playerId) + ".history";
        ConfigurationSection historySection = dataConfig.getConfigurationSection(historyPath);
        if (historySection == null) return entries;

        List<String> keys = new ArrayList<>(historySection.getKeys(false));
        keys.sort(Comparator.comparingLong(PunishmentManager::safeParseLong).reversed());

        int limit = Math.max(1, maxEntries);
        int count = 0;
        for (String key : keys) {
            if (count >= limit) break;
            String path = historyPath + "." + key;

            entries.add(new HistoryEntry(
                    safeParseLong(key),
                    dataConfig.getString(path + ".action", "UNKNOWN"),
                    dataConfig.getString(path + ".reason", "No reason provided."),
                    dataConfig.getString(path + ".source", "Unknown"),
                    dataConfig.getInt(path + ".durationDays", -1),
                    dataConfig.getLong(path + ".expiresAtEpochMillis", -1L)
            ));
            count++;
        }
        return entries;
    }

	public static void saveRestoreSnapshot(
		UUID playerId,
		String lastKnownName,
		ItemStack[] inventoryContents,
		ItemStack[] enderChestContents,
		double balance
	) {
		String basePath = playerPath(playerId);
		String snapshotPath = basePath + ".restoreSnapshot";

		dataConfig.set(basePath + ".lastKnownName", lastKnownName);
		dataConfig.set(snapshotPath + ".savedAtEpochMillis", System.currentTimeMillis());
		dataConfig.set(snapshotPath + ".pendingRestore", false);
		dataConfig.set(snapshotPath + ".inventoryBase64", serializeItems(inventoryContents));
		dataConfig.set(snapshotPath + ".enderChestBase64", serializeItems(enderChestContents));
		dataConfig.set(snapshotPath + ".balance", balance);
		save();
	}

	public static void saveRestoreSnapshot(
		UUID playerId,
		String lastKnownName,
		ItemStack[] inventoryContents,
		ItemStack[] enderChestContents,
		double balance,
		int kills,
		int deaths,
		int rtpCount,
		int playtimeSeconds
	) {
		String basePath = playerPath(playerId);
		String snapshotPath = basePath + ".restoreSnapshot";
		dataConfig.set(basePath + ".lastKnownName", lastKnownName);
		dataConfig.set(snapshotPath + ".savedAtEpochMillis", System.currentTimeMillis());
		dataConfig.set(snapshotPath + ".pendingRestore", false);
		dataConfig.set(snapshotPath + ".inventoryBase64", serializeItems(inventoryContents));
		dataConfig.set(snapshotPath + ".enderChestBase64", serializeItems(enderChestContents));
		dataConfig.set(snapshotPath + ".balance", balance);
		dataConfig.set(snapshotPath + ".kills", kills);
		dataConfig.set(snapshotPath + ".deaths", deaths);
		dataConfig.set(snapshotPath + ".rtpCount", rtpCount);
		dataConfig.set(snapshotPath + ".playtimeSeconds", playtimeSeconds);
		save();
	}

	public static boolean hasRestoreSnapshot(UUID playerId) {
        return dataConfig.getConfigurationSection(playerPath(playerId) + ".restoreSnapshot") != null;
    }

    public static void setRestorePending(UUID playerId, boolean pending) {
        dataConfig.set(playerPath(playerId) + ".restoreSnapshot.pendingRestore", pending);
        save();
    }

    public static boolean isRestorePending(UUID playerId) {
        return dataConfig.getBoolean(playerPath(playerId) + ".restoreSnapshot.pendingRestore", false);
    }

    public static void clearRestoreSnapshot(UUID playerId) {
        dataConfig.set(playerPath(playerId) + ".restoreSnapshot", null);
        save();
    }

    private static String serializeItems(ItemStack[] items) {
        if (items == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            return null;
        }
    }

    private static ItemStack[] deserializeItems(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (IOException | ClassNotFoundException exception) {
            return null;
        }
    }

    private static long safeParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private static String playerPath(UUID playerId) {
        return "players." + playerId;
    }

    public record HistoryEntry(
            long timestampEpochMillis,
            String action,
            String reason,
            String source,
            int durationDays,
            long expiresAtEpochMillis
    ) {}

	public record RestoreSnapshot(
		ItemStack[] inventoryContents,
		ItemStack[] enderChestContents,
		boolean pendingRestore,
		long savedAtEpochMillis,
		double balance,
		int kills,
		int deaths,
		int rtpCount,
		int playtimeSeconds
	) {}

	public static RestoreSnapshot getRestoreSnapshot(UUID playerId) {
		String snapshotPath = playerPath(playerId) + ".restoreSnapshot";
		ConfigurationSection section = dataConfig.getConfigurationSection(snapshotPath);
		if (section == null) {
			return null;
		}

		ItemStack[] inventory = deserializeItems(section.getString("inventoryBase64"));
		ItemStack[] enderChest = deserializeItems(section.getString("enderChestBase64"));
		boolean pendingRestore = section.getBoolean("pendingRestore", false);
		long savedAtEpochMillis = section.getLong("savedAtEpochMillis", -1L);
		double balance = section.getDouble("balance", -1.0);
		int kills = section.getInt("kills", -1);
		int deaths = section.getInt("deaths", -1);
		int rtpCount = section.getInt("rtpCount", -1);
		int playtimeSeconds = section.getInt("playtimeSeconds", -1);

		return new RestoreSnapshot(inventory, enderChest, pendingRestore, savedAtEpochMillis, balance, kills, deaths, rtpCount, playtimeSeconds);
	}
}

package org.atrimilan.sidastuffsmp.utils;

import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarnManager {

    private static final String WARNINGS_ROOT = "warnings.pending";

    private WarnManager() {}

    public static void addWarning(OfflinePlayer target, String warningText) {
        UUID uuid = target.getUniqueId();
        String path = warningPath(uuid);
        List<String> warnings = new ArrayList<>(ConfigManager.getConfig().getStringList(path + ".messages"));
        warnings.add(warningText.trim());
        ConfigManager.getConfig().set(path + ".name", target.getName());
        ConfigManager.getConfig().set(path + ".messages", warnings);
        ConfigManager.save();
    }

    public static List<String> getWarnings(UUID playerUuid) {
        return new ArrayList<>(ConfigManager.getConfig().getStringList(warningPath(playerUuid) + ".messages"));
    }

    public static void clearWarnings(UUID playerUuid) {
        ConfigManager.getConfig().set(warningPath(playerUuid), null);
        ConfigManager.save();
    }

    private static String warningPath(UUID playerUuid) {
        return WARNINGS_ROOT + "." + playerUuid;
    }
}

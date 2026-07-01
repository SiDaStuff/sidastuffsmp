package org.atrimilan.sidastuffsmp.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;

public class ConfigManager {

    private ConfigManager() {}

    private static FileConfiguration config;

    public static void init(JavaPlugin plugin) {
        config = plugin.getConfig();
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static boolean isJoinMessageEnabled() {
        return getConfig().getBoolean("join-message.enabled", false);
    }

    public static void setJoinMessageEnabled(boolean enabled) {
        getConfig().set("join-message.enabled", enabled);
        save();
    }

    public static String getJoinMessageText() {
        return getConfig().getString("join-message.text", "Welcome to SiDaStuff SMP.");
    }

    public static void setJoinMessageText(String text) {
        getConfig().set("join-message.text", text);
        save();
    }

    public static void save() {
        SiDaStuffSmp.getInstance().saveConfig();
    }

    public static void reload() {
        SiDaStuffSmp.getInstance().reloadConfig();
        config = SiDaStuffSmp.getInstance().getConfig();
    }

    // New setting: confirm listing via GUI
    public static boolean isListConfirmEnabled() {
        return getConfig().getBoolean("list-confirm.enabled", false);
    }

    public static void setListConfirmEnabled(boolean enabled) {
        getConfig().set("list-confirm.enabled", enabled);
        save();
    }
}

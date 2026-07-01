package org.atrimilan.sidastuffsmp.sus;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SusConfig {

    private static boolean enabled;
    private static boolean vulcanIntegration;
    private static int maxEntries;
    private static int autoRemoveDays;
    private static final Map<String, PunishPreset> PRESETS = new LinkedHashMap<>();

    private SusConfig() {}

    public static boolean isEnabled() { return enabled; }
    public static boolean isVulcanIntegration() { return vulcanIntegration; }
    public static int getMaxEntries() { return maxEntries; }
    public static int getAutoRemoveDays() { return autoRemoveDays; }
    public static Map<String, PunishPreset> getPresets() { return PRESETS; }

    public static PunishPreset getPreset(String key) {
        return PRESETS.get(key.toLowerCase());
    }

    public static void reload() {
        init(SiDaStuffSmp.getInstance());
    }

    public static void init(SiDaStuffSmp plugin) {
        FileConfiguration config = plugin.getConfig();

        config.addDefault("sus.enabled", true);
        config.addDefault("sus.vulcan-integration", true);
        config.addDefault("sus.max-entries", 100);
        config.addDefault("sus.auto-remove-days", 30);

        config.addDefault("punish-presets.esp.actions", List.of("ban"));
        config.addDefault("punish-presets.esp.duration-days", -1);
        config.addDefault("punish-presets.esp.reason", "ESP");

        config.addDefault("punish-presets.xray.actions", List.of("ban", "statswipe"));
        config.addDefault("punish-presets.xray.duration-days", 30);
        config.addDefault("punish-presets.xray.reason", "X-Ray");

        config.addDefault("punish-presets.speed.actions", List.of("tempban", "statswipe"));
        config.addDefault("punish-presets.speed.duration-days", 7);
        config.addDefault("punish-presets.speed.reason", "Speed Hacking");

        config.addDefault("punish-presets.fly.actions", List.of("tempban", "inventorywipe", "ecwipe"));
        config.addDefault("punish-presets.fly.duration-days", 14);
        config.addDefault("punish-presets.fly.reason", "Fly Hacking");

        config.addDefault("punish-presets.killaura.actions", List.of("ban", "statswipe", "inventorywipe", "ecwipe", "balancewipe"));
        config.addDefault("punish-presets.killaura.duration-days", -1);
        config.addDefault("punish-presets.killaura.reason", "KillAura");

        config.addDefault("punish-presets.duping.actions", List.of("ban", "statswipe", "inventorywipe", "ecwipe", "balancewipe", "auctionwipe", "homewipe", "orderwipe"));
        config.addDefault("punish-presets.duping.duration-days", -1);
        config.addDefault("punish-presets.duping.reason", "Duping");

        config.addDefault("punish-presets.griefing.actions", List.of("tempban", "balancewipe"));
        config.addDefault("punish-presets.griefing.duration-days", 14);
        config.addDefault("punish-presets.griefing.reason", "Griefing");

        config.addDefault("punish-presets.scamming.actions", List.of("tempban", "balancewipe", "auctionwipe", "orderwipe"));
        config.addDefault("punish-presets.scamming.duration-days", 30);
        config.addDefault("punish-presets.scamming.reason", "Scamming");

        config.addDefault("punish-presets.exploitation.actions", List.of("ban", "statswipe", "inventorywipe", "ecwipe", "balancewipe", "auctionwipe", "homewipe", "orderwipe"));
        config.addDefault("punish-presets.exploitation.duration-days", -1);
        config.addDefault("punish-presets.exploitation.reason", "Exploitation");

        config.addDefault("punish-presets.7days.actions", List.of("tempban"));
        config.addDefault("punish-presets.7days.duration-days", 7);
        config.addDefault("punish-presets.7days.reason", "7 Day Ban");

        config.addDefault("punish-presets.14days.actions", List.of("tempban"));
        config.addDefault("punish-presets.14days.duration-days", 14);
        config.addDefault("punish-presets.14days.reason", "14 Day Ban");

        config.addDefault("punish-presets.30days.actions", List.of("tempban"));
        config.addDefault("punish-presets.30days.duration-days", 30);
        config.addDefault("punish-presets.30days.reason", "30 Day Ban");

        config.addDefault("punish-presets.permban.actions", List.of("ban"));
        config.addDefault("punish-presets.permban.duration-days", -1);
        config.addDefault("punish-presets.permban.reason", "Permanent Ban");

        config.addDefault("punish-presets.statswipe.actions", List.of("statswipe"));
        config.addDefault("punish-presets.statswipe.duration-days", 0);
        config.addDefault("punish-presets.statswipe.reason", "Stats Wipe");

        config.addDefault("punish-presets.inventorywipe.actions", List.of("inventorywipe"));
        config.addDefault("punish-presets.inventorywipe.duration-days", 0);
        config.addDefault("punish-presets.inventorywipe.reason", "Inventory Wipe");

        config.addDefault("punish-presets.ecwipe.actions", List.of("ecwipe"));
        config.addDefault("punish-presets.ecwipe.duration-days", 0);
        config.addDefault("punish-presets.ecwipe.reason", "Ender Chest Wipe");

        config.addDefault("punish-presets.balancewipe.actions", List.of("balancewipe"));
        config.addDefault("punish-presets.balancewipe.duration-days", 0);
        config.addDefault("punish-presets.balancewipe.reason", "Balance Wipe");

        config.addDefault("punish-presets.auctionwipe.actions", List.of("auctionwipe"));
        config.addDefault("punish-presets.auctionwipe.duration-days", 0);
        config.addDefault("punish-presets.auctionwipe.reason", "Auction Wipe");

        config.addDefault("punish-presets.homewipe.actions", List.of("homewipe"));
        config.addDefault("punish-presets.homewipe.duration-days", 0);
        config.addDefault("punish-presets.homewipe.reason", "Home Wipe");

        config.addDefault("punish-presets.orderwipe.actions", List.of("orderwipe"));
        config.addDefault("punish-presets.orderwipe.duration-days", 0);
        config.addDefault("punish-presets.orderwipe.reason", "Order Wipe");

        config.options().copyDefaults(true);
        plugin.saveConfig();

        enabled = config.getBoolean("sus.enabled", true);
        vulcanIntegration = config.getBoolean("sus.vulcan-integration", true);
        maxEntries = config.getInt("sus.max-entries", 100);
        autoRemoveDays = config.getInt("sus.auto-remove-days", 30);

        PRESETS.clear();
        ConfigurationSection presetsSection = config.getConfigurationSection("punish-presets");
        if (presetsSection != null) {
            for (String key : presetsSection.getKeys(false)) {
                ConfigurationSection presetSec = presetsSection.getConfigurationSection(key);
                if (presetSec == null) continue;
                List<String> actions = presetSec.getStringList("actions");
                int durationDays = presetSec.getInt("duration-days", -1);
                String reason = presetSec.getString("reason", key);
                PRESETS.put(key.toLowerCase(), new PunishPreset(key.toLowerCase(), actions, durationDays, reason));
            }
        }
    }

    public record PunishPreset(String key, List<String> actions, int durationDays, String reason) {
        public boolean hasAction(String action) {
            for (String a : actions) {
                if (a.equalsIgnoreCase(action)) return true;
            }
            return false;
        }

        public boolean isPermanent() {
            return durationDays <= 0 && hasAction("ban");
        }
    }
}

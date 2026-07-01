package org.atrimilan.sidastuffsmp.order;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrderConfig {

    private static FileConfiguration config;
    private static File configFile;

    private OrderConfig() {}

    public static void init(SiDaStuffSmp plugin) {
        configFile = new File(plugin.getDataFolder(), "orders.yml");
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
        return config.getBoolean("orders.enabled", true);
    }

    public static long orderDurationDays() {
        return config.getLong("orders.duration-days", 7L);
    }

    public static int maxActiveOrdersPerPlayer() {
        return config.getInt("orders.max-active-per-player", 10);
    }

    public static double minPricePerUnit() {
        return config.getDouble("orders.min-price-per-unit", 1.0);
    }

    public static double maxPricePerUnit() {
        return config.getDouble("orders.max-price-per-unit", 1000000.0);
    }

    public static int maxQuantity() {
        return config.getInt("orders.max-quantity", 2304);
    }

    public static long expireGraceHours() {
        return config.getLong("orders.expire-grace-hours", 48L);
    }

    public static long syncIntervalSeconds() {
        return config.getLong("orders.sync-interval-seconds", 60L);
    }

    public static String getItemDownloadUrl() {
        return config.getString("orders.item-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/items.json");
    }

    public static String getBlockDownloadUrl() {
        return config.getString("orders.block-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/blocks.json");
    }

    public static String getEnchantmentsDownloadUrl() {
        return config.getString("orders.enchantments-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/enchantments.json");
    }

    public static String getEffectsDownloadUrl() {
        return config.getString("orders.effects-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/effects.json");
    }

    public static long deliveryDelayTicks() {
        return config.getLong("orders.delivery-delay-ticks", 100L);
    }

    public static boolean isEconomyShopGuiSellEnabled() {
        return config.getBoolean("orders.economyshopgui-sell-enabled", true);
    }

    public static boolean isConfirmListingGuiEnabled() {
        return config.getBoolean("orders.confirm-listing-gui", true);
    }

    public static boolean isSellPriorityEnabled() {
        return config.getBoolean("orders.sell-priority", true);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getAllowedMaterials() {
        List<String> materials = config.getStringList("orders.allowed-materials");
        if (materials.isEmpty()) {
            materials = new ArrayList<>();
            materials.add("DIAMOND");
            materials.add("IRON_INGOT");
            materials.add("GOLD_INGOT");
            materials.add("NETHERITE_INGOT");
            materials.add("EMERALD");
            materials.add("LAPIS_LAZULI");
            materials.add("REDSTONE");
            materials.add("COAL");
            materials.add("OAK_LOG");
            materials.add("SPRUCE_LOG");
            materials.add("BIRCH_LOG");
            materials.add("JUNGLE_LOG");
            materials.add("ACACIA_LOG");
            materials.add("DARK_OAK_LOG");
            materials.add("MANGROVE_LOG");
            materials.add("CHERRY_LOG");
            materials.add("CRIMSON_STEM");
            materials.add("WARPED_STEM");
            materials.add("COBBLESTONE");
            materials.add("STONE");
            materials.add("OBSIDIAN");
            materials.add("QUARTZ");
            materials.add("AMETHYST_SHARD");
            materials.add("COPPER_INGOT");
            materials.add("WHEAT");
            materials.add("CARROT");
            materials.add("POTATO");
            materials.add("BEETROOT");
            materials.add("PUMPKIN");
            materials.add("MELON_SLICE");
            materials.add("SUGAR_CANE");
            materials.add("NETHER_WART");
            materials.add("BLAZE_ROD");
            materials.add("ENDER_PEARL");
            materials.add("GHAST_TEAR");
            materials.add("BONE");
            materials.add("GUNPOWDER");
            materials.add("STRING");
            materials.add("SPIDER_EYE");
            materials.add("ROTTEN_FLESH");
            materials.add("ARROW");
            materials.add("EXPERIENCE_BOTTLE");
        }
        return materials;
    }

    private static void saveDefaults(SiDaStuffSmp plugin) {
        config = new YamlConfiguration();
        config.set("orders.enabled", true);
        config.set("orders.duration-days", 7L);
        config.set("orders.max-active-per-player", 10);
        config.set("orders.min-price-per-unit", 1.0);
        config.set("orders.max-price-per-unit", 1000000.0);
        config.set("orders.max-quantity", 2304);
        config.set("orders.expire-grace-hours", 48L);
        config.set("orders.sync-interval-seconds", 60L);
        config.set("orders.delivery-delay-ticks", 100L);
        config.set("orders.item-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/items.json");
        config.set("orders.block-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/blocks.json");
        config.set("orders.enchantments-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/enchantments.json");
        config.set("orders.effects-download-url",
                "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/1.21.11/effects.json");
        config.set("orders.economyshopgui-sell-enabled", true);
        config.set("orders.confirm-listing-gui", true);
        config.set("orders.sell-priority", true);
        config.set("orders.allowed-materials", getAllowedMaterials());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save default orders.yml: " + e.getMessage());
        }
    }
}

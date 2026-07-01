package org.atrimilan.sidastuffsmp.economy;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EconomyShopPriceLoader {

    private static final Map<Material, Double> SELL_PRICES = new HashMap<>();
    private static final Map<Material, Double> BUY_PRICES = new HashMap<>();
    private static boolean loaded = false;

    private EconomyShopPriceLoader() {}

    public static void loadPrices(SiDaStuffSmp plugin) {
        if (loaded) {
            plugin.getLogger().info("EconomyShopGUI prices already loaded, skipping...");
            return;
        }

        // Find EconomyShopGUI plugin directory
        Plugin esPlugin = plugin.getServer().getPluginManager().getPlugin("EconomyShopGUI");
        if (esPlugin == null) {
            plugin.getLogger().warning("EconomyShopGUI plugin not found! Cannot load shop prices.");
            return;
        }

        File shopsDir = new File(esPlugin.getDataFolder(), "shops");
        if (!shopsDir.exists() || !shopsDir.isDirectory()) {
            plugin.getLogger().warning("EconomyShopGUI shops directory not found at: " + shopsDir.getAbsolutePath());
            return;
        }

        File[] shopFiles = shopsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (shopFiles == null || shopFiles.length == 0) {
            plugin.getLogger().warning("No shop YAML files found in: " + shopsDir.getAbsolutePath());
            return;
        }

        int totalItems = 0;
        for (File shopFile : shopFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
                totalItems += parseShopFile(config, shopFile.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse shop file: " + shopFile.getName(), e);
            }
        }

        loaded = true;
        plugin.getLogger().info("Loaded " + totalItems + " item prices from " + shopFiles.length + " EconomyShopGUI shop files.");
    }

    private static int parseShopFile(YamlConfiguration config, String fileName) {
        int count = 0;
        
        // EconomyShopGUI structure: pages.page1.items.'1'.material, buy, sell
        if (!config.contains("pages")) {
            return 0;
        }

        for (String pageKey : config.getConfigurationSection("pages").getKeys(false)) {
            String itemsPath = "pages." + pageKey + ".items";
            if (!config.contains(itemsPath)) continue;

            for (String itemKey : config.getConfigurationSection(itemsPath).getKeys(false)) {
                String basePath = itemsPath + "." + itemKey;
                
                String materialStr = config.getString(basePath + ".material");
                Double buyPrice = config.getDouble(basePath + ".buy", -1);
                Double sellPrice = config.getDouble(basePath + ".sell", -1);

                if (materialStr == null) continue;

                try {
                    Material material = Material.valueOf(materialStr.toUpperCase());
                    
                    if (sellPrice >= 0) {
                        // Keep the highest sell price if duplicate
                        SELL_PRICES.merge(material, sellPrice, Math::max);
                    }
                    if (buyPrice >= 0) {
                        // Keep the lowest buy price if duplicate
                        BUY_PRICES.merge(material, buyPrice, Math::min);
                    }
                    count++;
                } catch (IllegalArgumentException e) {
                    // Invalid material name, skip
                }
            }
        }
        return count;
    }

    public static Double getSellPrice(Material material) {
        return SELL_PRICES.get(material);
    }

    public static Double getBuyPrice(Material material) {
        return BUY_PRICES.get(material);
    }

    public static boolean hasPrice(Material material) {
        return SELL_PRICES.containsKey(material) || BUY_PRICES.containsKey(material);
    }

    public static void reload(SiDaStuffSmp plugin) {
        SELL_PRICES.clear();
        BUY_PRICES.clear();
        loaded = false;
        loadPrices(plugin);
    }

    public static int getLoadedItemCount() {
        return SELL_PRICES.size();
    }
}
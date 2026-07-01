package org.atrimilan.sidastuffsmp.economy;

import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EconomyShopGuiPriceLoader {

    private static final Map<Material, Double> SELL_PRICES = new HashMap<>();
    private static final Map<Material, Double> BUY_PRICES = new HashMap<>();
    private static volatile boolean loaded = false;

    private EconomyShopGuiPriceLoader() {}

    /**
     * Loads all shop YAML files from the EconomyShopGUI shops folder.
     * First tries to read from the external EconomyShopGUI plugin folder,
     * then copies them to our data folder for backup/reference.
     * This runs on plugin startup and reads prices directly from the YAML files.
     */
    public static void loadPrices(SiDaStuffSmp plugin) {
        if (loaded) {
            return;
        }

        // Try to find the EconomyShopGUI shops folder
        // First check in the server's plugins directory (external EconomyShopGUI plugin)
        File externalShopFolder = new File("plugins/EconomyShopGUI/shops");
        
        // Also check in case getDataFolder().getParentFile() points to server root
        File pluginBasedShopFolder = new File(plugin.getDataFolder().getParentFile(), "EconomyShopGUI/shops");
        
        File shopsFolder = null;
        if (externalShopFolder.exists() && externalShopFolder.isDirectory()) {
            shopsFolder = externalShopFolder;
        } else if (pluginBasedShopFolder.exists() && pluginBasedShopFolder.isDirectory()) {
            shopsFolder = pluginBasedShopFolder;
        }
        
        if (shopsFolder == null || !shopsFolder.exists() || !shopsFolder.isDirectory()) {
            plugin.getLogger().warning("EconomyShopGUI shops folder not found at: " + externalShopFolder.getAbsolutePath() + " or " + pluginBasedShopFolder.getAbsolutePath());
            return;
        }

        File[] shopFiles = shopsFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (shopFiles == null || shopFiles.length == 0) {
            plugin.getLogger().warning("No shop YAML files found in: " + shopsFolder.getAbsolutePath());
            return;
        }

        // Copy shop files to our data folder for backup/reference
        copyShopFilesToDataFolder(plugin, shopsFolder);

        int totalItems = 0;
        for (File shopFile : shopFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
                int itemsLoaded = parseShopFile(config);
                totalItems += itemsLoaded;
                plugin.getLogger().info("Loaded " + itemsLoaded + " items from " + shopFile.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load shop file: " + shopFile.getName(), e);
            }
        }

        loaded = true;
        plugin.getLogger().info("EconomyShopGUI price loader: Loaded " + totalItems + " total items from " + shopFiles.length + " shop files");
    }

    /**
     * Copies shop files from the EconomyShopGUI plugin folder to our data folder
     * for backup and reference purposes.
     */
    private static void copyShopFilesToDataFolder(SiDaStuffSmp plugin, File sourceFolder) {
        File dataFolder = new File(plugin.getDataFolder(), "economyshopgui_shops");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File[] shopFiles = sourceFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (shopFiles == null || shopFiles.length == 0) {
            return;
        }

        for (File sourceFile : shopFiles) {
            File destFile = new File(dataFolder, sourceFile.getName());
            try {
                // Only copy if source is newer or dest doesn't exist
                if (!destFile.exists() || sourceFile.lastModified() > destFile.lastModified()) {
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Copied shop file: " + sourceFile.getName() + " to " + destFile.getAbsolutePath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy shop file: " + sourceFile.getName() + " - " + e.getMessage());
            }
        }
    }

    private static int parseShopFile(YamlConfiguration config) {
        int count = 0;
        
        // The YAML structure has pages with items
        // pages:
        //   page1:
        //     items:
        //       '1':
        //         material: ACACIA_BUTTON
        //         buy: 1.16
        //         sell: 0.29
        
        if (!config.isConfigurationSection("pages")) {
            return 0;
        }

        for (String pageKey : config.getConfigurationSection("pages").getKeys(false)) {
            String itemsPath = "pages." + pageKey + ".items";
            if (!config.isConfigurationSection(itemsPath)) {
                continue;
            }

            for (String itemKey : config.getConfigurationSection(itemsPath).getKeys(false)) {
                String basePath = itemsPath + "." + itemKey;
                
                String materialName = config.getString(basePath + ".material");
                if (materialName == null) {
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid material name, skip
                    continue;
                }

                Double sellPrice = config.getDouble(basePath + ".sell");
                Double buyPrice = config.getDouble(basePath + ".buy");

                if (sellPrice != null && sellPrice > 0) {
                    // Keep the highest sell price if duplicate materials exist across shops
                    SELL_PRICES.merge(material, sellPrice, Math::max);
                    count++;
                }
                if (buyPrice != null && buyPrice > 0) {
                    BUY_PRICES.merge(material, buyPrice, Math::max);
                }
            }
        }

        return count;
    }

    /**
     * Gets the sell price for a material.
     * @param material The material to get the price for
     * @return The sell price, or 0.0 if not found
     */
    public static double getSellPrice(Material material) {
        return SELL_PRICES.getOrDefault(material, 0.0);
    }

    /**
     * Gets the buy price for a material.
     * @param material The material to get the price for
     * @return The buy price, or 0.0 if not found
     */
    public static double getBuyPrice(Material material) {
        return BUY_PRICES.getOrDefault(material, 0.0);
    }

    /**
     * Gets all loaded sell prices.
     * @return Unmodifiable map of material to sell price
     */
    public static Map<Material, Double> getAllSellPrices() {
        return Map.copyOf(SELL_PRICES);
    }

    /**
     * Gets all loaded buy prices.
     * @return Unmodifiable map of material to buy price
     */
    public static Map<Material, Double> getAllBuyPrices() {
        return Map.copyOf(BUY_PRICES);
    }

    /**
     * Checks if prices have been loaded.
     * @return true if prices are loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Reloads prices from shop files.
     */
    public static void reload(SiDaStuffSmp plugin) {
        SELL_PRICES.clear();
        BUY_PRICES.clear();
        loaded = false;
        loadPrices(plugin);
    }
}
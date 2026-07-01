package org.atrimilan.sidastuffsmp.order;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.NamespacedKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MinecraftDataRegistry {

    private static final Gson GSON = new Gson();

    private static final List<MinecraftItem> ALL_ITEMS = new ArrayList<>();
    private static final List<MinecraftEnchantment> ALL_ENCHANTMENTS = new ArrayList<>();
    private static final List<MinecraftEffect> ALL_EFFECTS = new ArrayList<>();
    private static boolean loaded = false;

    private MinecraftDataRegistry() {}

    public static boolean isLoaded() {
        return loaded;
    }

    public static List<MinecraftItem> getAllItems() {
        return Collections.unmodifiableList(ALL_ITEMS);
    }

    public static List<MinecraftEnchantment> getAllEnchantments() {
        return Collections.unmodifiableList(ALL_ENCHANTMENTS);
    }

    public static List<MinecraftEffect> getAllEffects() {
        return Collections.unmodifiableList(ALL_EFFECTS);
    }

    public static List<MinecraftItem> searchItems(String query) {
        if (query == null || query.isBlank()) {
            return getAllItems();
        }
        String lower = query.trim().toLowerCase(Locale.ROOT);
        List<MinecraftItem> results = new ArrayList<>();
        for (MinecraftItem item : ALL_ITEMS) {
            if (item.name().toLowerCase(Locale.ROOT).contains(lower)
                    || item.displayName().toLowerCase(Locale.ROOT).contains(lower)
                    || item.name().replace('_', ' ').toLowerCase(Locale.ROOT).contains(lower)) {
                results.add(item);
            }
        }
        return results;
    }

    public static List<MinecraftEnchantment> searchEnchantments(String query) {
        if (query == null || query.isBlank()) {
            return getAllEnchantments();
        }
        String lower = query.trim().toLowerCase(Locale.ROOT);
        List<MinecraftEnchantment> results = new ArrayList<>();
        for (MinecraftEnchantment ench : ALL_ENCHANTMENTS) {
            if (ench.name().toLowerCase(Locale.ROOT).contains(lower)
                    || ench.displayName().toLowerCase(Locale.ROOT).contains(lower)
                    || ench.name().replace('_', ' ').toLowerCase(Locale.ROOT).contains(lower)) {
                results.add(ench);
            }
        }
        return results;
    }

    public static List<MinecraftEffect> searchEffects(String query) {
        if (query == null || query.isBlank()) {
            return getAllEffects();
        }
        String lower = query.trim().toLowerCase(Locale.ROOT);
        List<MinecraftEffect> results = new ArrayList<>();
        for (MinecraftEffect eff : ALL_EFFECTS) {
            if (eff.name().toLowerCase(Locale.ROOT).contains(lower)
                    || eff.displayName().toLowerCase(Locale.ROOT).contains(lower)
                    || eff.name().replace('_', ' ').toLowerCase(Locale.ROOT).contains(lower)) {
                results.add(eff);
            }
        }
        return results;
    }

    public static boolean isEnchantable(Material material) {
        if (material == null) return false;
        MinecraftItem mcItem = findItem(material);
        if (mcItem != null && !mcItem.enchantCategories().isEmpty()) {
            return true;
        }
        if (mcItem != null && mcItem.maxDurability() > 0) {
            return true;
        }
        String name = material.name().toLowerCase(Locale.ROOT);
        return name.contains("sword") || name.contains("pickaxe") || name.contains("axe")
            || name.contains("shovel") || name.contains("hoe") || name.contains("bow")
            || name.contains("crossbow") || name.contains("trident") || name.contains("mace")
            || name.contains("helmet") || name.contains("chestplate") || name.contains("leggings")
            || name.contains("boots") || name.contains("elytra") || name.contains("shield")
            || name.contains("fishing_rod") || name.contains("book") || name.contains("brush");
    }

    public static MinecraftItem findItem(Material material) {
        for (MinecraftItem item : ALL_ITEMS) {
            if (item.material() == material) return item;
        }
        return null;
    }

    public static MinecraftItem findItemByName(String name) {
        if (name == null) return null;
        for (MinecraftItem item : ALL_ITEMS) {
            if (item.name().equalsIgnoreCase(name)) return item;
        }
        return null;
    }

    public static List<MinecraftEnchantment> getApplicableEnchantments(Material material) {
        MinecraftItem mcItem = findItem(material);
        List<String> itemCategories = mcItem != null ? mcItem.enchantCategories() : Collections.emptyList();

        if (itemCategories.isEmpty()) {
            if (isEnchantable(material)) {
                return getAllEnchantments();
            }
            return Collections.emptyList();
        }

        List<MinecraftEnchantment> applicable = new ArrayList<>();
        for (MinecraftEnchantment ench : ALL_ENCHANTMENTS) {
            if (ench.category() != null && itemCategories.contains(ench.category())) {
                applicable.add(ench);
            }
        }

        if (itemCategories.contains("breakable") || itemCategories.contains("durability")) {
            for (MinecraftEnchantment ench : ALL_ENCHANTMENTS) {
                if (ench.category() != null && ench.category().equals("breakable")
                    && applicable.stream().noneMatch(e -> e.name().equals(ench.name()))) {
                    applicable.add(ench);
                }
            }
        }

        return applicable;
    }

    public static List<String> getConflictingEnchantments(String enchantmentName) {
        for (MinecraftEnchantment ench : ALL_ENCHANTMENTS) {
            if (ench.name().equals(enchantmentName)) {
                return ench.exclude();
            }
        }
        return Collections.emptyList();
    }

    public static boolean hasConflictingEnchantment(List<MinecraftEnchantment> selected, MinecraftEnchantment toAdd) {
        List<String> newExclude = toAdd.exclude();
        for (MinecraftEnchantment existing : selected) {
            if (newExclude.contains(existing.name())) return true;
            if (existing.exclude().contains(toAdd.name())) return true;
        }
        return false;
    }

    public static boolean isPotionItem(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.equals("POTION") || name.equals("SPLASH_POTION") || name.equals("LINGERING_POTION")
                || name.equals("TIPPED_ARROW");
    }

    public static Material resolveMaterial(String itemName) {
        if (itemName == null) return null;
        Material mat = Material.matchMaterial(itemName);
        if (mat != null) return mat;
        String upper = itemName.toUpperCase(Locale.ROOT);
        mat = Material.matchMaterial(upper);
        if (mat != null) return mat;
        try {
            return Material.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void init(SiDaStuffSmp plugin) {
        String itemsUrl = OrderConfig.getItemDownloadUrl();
        String blocksUrl = OrderConfig.getBlockDownloadUrl();
        String enchantmentsUrl = OrderConfig.getEnchantmentsDownloadUrl();
        String effectsUrl = OrderConfig.getEffectsDownloadUrl();

        if (itemsUrl == null || itemsUrl.isEmpty()) {
            loadFallback();
            return;
        }

        File dataDir = new File(plugin.getDataFolder(), "mcdata");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File itemsFile = new File(dataDir, "items.json");
        File blocksFile = new File(dataDir, "blocks.json");
        File enchantmentsFile = new File(dataDir, "enchantments.json");
        File effectsFile = new File(dataDir, "effects.json");

        boolean itemsDownloaded = ensureFile(plugin, itemsUrl, itemsFile, "items");
        boolean blocksDownloaded = blocksUrl != null && !blocksUrl.isEmpty()
                && ensureFile(plugin, blocksUrl, blocksFile, "blocks");
        boolean enchantmentsDownloaded = enchantmentsUrl != null && !enchantmentsUrl.isEmpty()
                && ensureFile(plugin, enchantmentsUrl, enchantmentsFile, "enchantments");
        boolean effectsDownloaded = effectsUrl != null && !effectsUrl.isEmpty()
                && ensureFile(plugin, effectsUrl, effectsFile, "effects");

        if (!itemsDownloaded) {
            plugin.getLogger().warning("Failed to download minecraft items data, using fallback material list.");
            loadFallback();
            return;
        }

        loadFromFiles(plugin, itemsFile, blocksDownloaded ? blocksFile : null,
                enchantmentsDownloaded ? enchantmentsFile : null,
                effectsDownloaded ? effectsFile : null);
    }

    private static boolean ensureFile(SiDaStuffSmp plugin, String urlStr, File target, String label) {
        if (target.exists() && target.length() > 0) {
            return true;
        }

        plugin.getLogger().info("Downloading minecraft " + label + " data from " + urlStr + "...");
        try {
            String rawUrl = convertGitHubUrl(urlStr);
            downloadFile(rawUrl, target);
            plugin.getLogger().info("Downloaded minecraft " + label + " data successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to download minecraft " + label + " data: " + e.getMessage());
            if (target.exists()) {
                target.delete();
            }
            return false;
        }
    }

    static String convertGitHubUrl(String url) {
        if (url == null) return url;
        if (url.contains("github.com/") && url.contains("/blob/")) {
            return url.replace("github.com/", "raw.githubusercontent.com/").replace("/blob/", "/");
        }
        return url;
    }

    private static void downloadFile(String urlStr, File destination) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "SiDaStuffSmp-MinecraftDataLoader/1.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + responseCode + " for " + urlStr);
        }

        File tempFile = new File(destination.getParent(), destination.getName() + ".tmp");
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            connection.disconnect();
        }

        if (destination.exists()) {
            destination.delete();
        }
        if (!tempFile.renameTo(destination)) {
            throw new Exception("Failed to rename temp file to " + destination.getName());
        }
    }

    private static void loadFromFiles(SiDaStuffSmp plugin, File itemsFile, File blocksFile, File enchantmentsFile, File effectsFile) {
        Map<String, MinecraftItem> itemMap = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(itemsFile);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            JsonArray itemsArray = GSON.fromJson(reader, JsonArray.class);
            if (itemsArray != null) {
                for (JsonElement element : itemsArray) {
                    JsonObject obj = element.getAsJsonObject();
                    String name = obj.has("name") ? obj.get("name").getAsString() : null;
                    String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : null;
                    int stackSize = obj.has("stackSize") ? obj.get("stackSize").getAsInt() : 64;
                    if (name != null && !name.equals("air")) {
                        Material mat = resolveMaterial(name);
                        if (mat != null && mat.isItem() && mat != Material.AIR) {
                            List<String> enchCats = new ArrayList<>();
                            if (obj.has("enchantCategories") && obj.get("enchantCategories").isJsonArray()) {
                                for (JsonElement catEl : obj.getAsJsonArray("enchantCategories")) {
                                    enchCats.add(catEl.getAsString());
                                }
                            }
                            int maxDur = obj.has("maxDurability") ? obj.get("maxDurability").getAsInt() : 0;
                            itemMap.put(name, new MinecraftItem(name, displayName != null ? displayName : name, stackSize, mat, Collections.unmodifiableList(enchCats), maxDur));
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse items.json: " + e.getMessage());
        }

        if (blocksFile != null && blocksFile.exists()) {
            try (FileInputStream fis = new FileInputStream(blocksFile);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                JsonArray blocksArray = GSON.fromJson(reader, JsonArray.class);
                if (blocksArray != null) {
                    for (JsonElement element : blocksArray) {
                        JsonObject obj = element.getAsJsonObject();
                        String name = obj.has("name") ? obj.get("name").getAsString() : null;
                        String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : null;
                        int stackSize = obj.has("stackSize") ? obj.get("stackSize").getAsInt() : 64;
                    if (name != null && !name.equals("air") && !itemMap.containsKey(name)) {
                        Material mat = resolveMaterial(name);
                        if (mat != null && mat.isItem() && mat != Material.AIR) {
                            List<String> enchCats = new ArrayList<>();
                            if (obj.has("enchantCategories") && obj.get("enchantCategories").isJsonArray()) {
                                for (JsonElement catEl : obj.getAsJsonArray("enchantCategories")) {
                                    enchCats.add(catEl.getAsString());
                                }
                            }
                            int maxDur = obj.has("maxDurability") ? obj.get("maxDurability").getAsInt() : 0;
                            itemMap.put(name, new MinecraftItem(name, displayName != null ? displayName : name, stackSize, mat, Collections.unmodifiableList(enchCats), maxDur));
                        }
                    }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse blocks.json: " + e.getMessage());
            }
        }

        ALL_ITEMS.clear();
        ALL_ITEMS.addAll(itemMap.values());

        ALL_ENCHANTMENTS.clear();
        if (enchantmentsFile != null && enchantmentsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(enchantmentsFile);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                JsonArray enchArray = GSON.fromJson(reader, JsonArray.class);
                if (enchArray != null) {
                    for (JsonElement element : enchArray) {
                        JsonObject obj = element.getAsJsonObject();
                        String name = obj.has("name") ? obj.get("name").getAsString() : null;
                        String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : null;
                        int maxLevel = obj.has("maxLevel") ? obj.get("maxLevel").getAsInt() : 1;
                        String category = obj.has("category") ? obj.get("category").getAsString() : null;
                    boolean treasureOnly = obj.has("treasureOnly") && obj.get("treasureOnly").getAsBoolean();
                    boolean curse = obj.has("curse") && obj.get("curse").getAsBoolean();
                    List<String> exclude = new ArrayList<>();
                    if (obj.has("exclude") && obj.get("exclude").isJsonArray()) {
                        for (JsonElement exEl : obj.getAsJsonArray("exclude")) {
                            exclude.add(exEl.getAsString());
                        }
                    }
                    if (name != null) {
                        ALL_ENCHANTMENTS.add(new MinecraftEnchantment(name, displayName != null ? displayName : name, maxLevel, category, treasureOnly, curse, Collections.unmodifiableList(exclude)));
                    }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse enchantments.json: " + e.getMessage());
            }
        }

        ALL_EFFECTS.clear();
        if (effectsFile != null && effectsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(effectsFile);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                JsonArray effectsArray = GSON.fromJson(reader, JsonArray.class);
                if (effectsArray != null) {
                    for (JsonElement element : effectsArray) {
                        JsonObject obj = element.getAsJsonObject();
                        String name = obj.has("name") ? obj.get("name").getAsString() : null;
                        String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : null;
                        String type = obj.has("type") ? obj.get("type").getAsString() : null;
                        if (name != null) {
                            ALL_EFFECTS.add(new MinecraftEffect(name, displayName != null ? displayName : name, type));
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse effects.json: " + e.getMessage());
            }
        }

        injectPotionItems();

        loaded = true;
        plugin.getLogger().info("Loaded " + ALL_ITEMS.size() + " items, " + ALL_ENCHANTMENTS.size() + " enchantments, " + ALL_EFFECTS.size() + " effects from minecraft-data.");
    }

    private static void loadFallback() {
        ALL_ITEMS.clear();
        ALL_ENCHANTMENTS.clear();
        ALL_EFFECTS.clear();
        for (Material mat : Material.values()) {
            if (mat.isItem() && mat != Material.AIR) {
                String name = mat.name().toLowerCase(Locale.ROOT);
                String displayName = OrderManager.formatMaterialName(mat);
                ALL_ITEMS.add(new MinecraftItem(name, displayName, mat.getMaxStackSize(), mat, Collections.emptyList(), 0));
            }
        }

        injectPotionItems();

        loaded = true;
    }

    private static final String[][] POTION_ENTRIES = {
            {"healing",          "Healing",          "HEALING"},
            {"fire_resistance",  "Fire Resistance",  "FIRE_RESISTANCE"},
            {"regeneration",     "Regeneration",     "REGENERATION"},
            {"strength",         "Strength",         "STRENGTH"},
            {"swiftness",        "Swiftness",        "SWIFTNESS"},
            {"night_vision",     "Night Vision",     "NIGHT_VISION"},
            {"invisibility",     "Invisibility",     "INVISIBILITY"},
            {"water_breathing",  "Water Breathing",  "WATER_BREATHING"},
            {"leaping",          "Leaping",          "LEAPING"},
            {"slow_falling",     "Slow Falling",     "SLOW_FALLING"},
            {"poison",           "Poison",           "POISON"},
            {"weakness",         "Weakness",         "WEAKNESS"},
            {"harming",          "Harming",          "HARMING"},
            {"slowness",         "Slowness",         "SLOWNESS"},
            {"turtle_master",    "Turtle Master",    "TURTLE_MASTER"},
            {"wind_charged",     "Wind Charged",     "WIND_CHARGED"},
            {"weaving",          "Weaving",          "WEAVING"},
            {"oozing",           "Oozing",           "OOZING"},
            {"infested",         "Infested",         "INFESTED"},
            {"luck",             "Luck",             "LUCK"},
    };

    private static final Object[][] BASE_POTION_DURATIONS = {
            // Vanilla Paper durations (drinkable potions) in ticks
            // Format: {effectKey, normal, extended, strong}
            // 20 ticks = 1 second
            new Object[]{"healing",          0},        // Instant
            new Object[]{"fire_resistance",  3600,      9600},     // 3:00, 8:00
            new Object[]{"regeneration",     900,       1800,      400},    // 0:45, 1:30, 0:20 (strong is 400, not 450)
            new Object[]{"strength",         3600,      9600,      1800},   // 3:00, 8:00, 1:30
            new Object[]{"swiftness",        3600,      9600,      1800},   // 3:00, 8:00, 1:30
            new Object[]{"night_vision",     3600,      9600},    // 3:00, 8:00
            new Object[]{"invisibility",    3600,      9600},    // 3:00, 8:00
            new Object[]{"water_breathing",  3600,      9600},    // 3:00, 8:00
            new Object[]{"leaping",          3600,      9600,      1800},   // 3:00, 8:00, 1:30
            new Object[]{"slow_falling",     1800,      4800},    // 1:30, 4:00
            new Object[]{"poison",           900,       1800,      400},    // 0:45, 1:30, 0:20 (strong is 400, not 432)
            new Object[]{"weakness",         1800,      4800},    // 1:30, 4:00
            new Object[]{"harming",          0},        // Instant
            new Object[]{"slowness",         1800,      4800,      400},    // 1:30, 4:00, 0:20
            new Object[]{"turtle_master",    400,       800,       400},    // 0:20, 0:40, 0:20
            new Object[]{"wind_charged",     3600},              // 3:00
            new Object[]{"weaving",          3600},              // 3:00
            new Object[]{"oozing",           3600},              // 3:00
            new Object[]{"infested",         3600},              // 3:00
            new Object[]{"luck",             6000},              // 5:00
    };

    private static final Set<String> HAS_STRONG = Set.of(
            "healing", "regeneration", "strength", "swiftness", "leaping",
            "poison", "harming", "turtle_master", "slowness"
    );

    private static final Set<String> HAS_LONG = Set.of(
            "fire_resistance", "regeneration", "strength", "swiftness",
            "night_vision", "invisibility", "water_breathing", "leaping",
            "slow_falling", "poison", "weakness", "slowness", "turtle_master"
    );

    private static PotionType resolvePotionType(String key) {
        try {
            return PotionType.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void injectPotionItems() {
        java.util.Set<String> existingNames = new java.util.HashSet<>();
        for (MinecraftItem item : ALL_ITEMS) {
            existingNames.add(item.name().toLowerCase(Locale.ROOT));
        }

        for (String[] entry : POTION_ENTRIES) {
            String effectKey = entry[0];
            String display = entry[1];
            PotionType potionType = resolvePotionType(entry[2]);

            for (Material mat : new Material[]{Material.POTION, Material.SPLASH_POTION,
                    Material.LINGERING_POTION, Material.TIPPED_ARROW}) {
                String prefix = switch (mat) {
                    case POTION -> "potion_of_";
                    case SPLASH_POTION -> "splash_potion_of_";
                    case LINGERING_POTION -> "lingering_potion_of_";
                    case TIPPED_ARROW -> "arrow_of_";
                    default -> "potion_of_";
                };
                String containerName = switch (mat) {
                    case POTION -> "Potion";
                    case SPLASH_POTION -> "Splash Potion";
                    case LINGERING_POTION -> "Lingering Potion";
                    case TIPPED_ARROW -> "Arrow";
                    default -> "Potion";
                };
                String itemKey = prefix + effectKey;
                String displayName = containerName + " of " + display;
                addPotionIfMissing(existingNames, itemKey, displayName, mat, effectKey, potionType, null);

                // Add strong variant if applicable
                if (HAS_STRONG.contains(effectKey)) {
                    String strongKey = prefix + "strong_" + effectKey;
                    String strongDisplayName = containerName + " of " + display + " II " + getPotionDuration("strong_" + effectKey, mat);
                    addPotionIfMissing(existingNames, strongKey, strongDisplayName, mat, "strong_" + effectKey, resolvePotionType("STRONG_" + entry[2]), null);
                }

                // Add long variant if applicable
                if (HAS_LONG.contains(effectKey)) {
                    String longKey = prefix + "long_" + effectKey;
                    String longDisplayName = containerName + " of " + display + " " + getPotionDuration("long_" + effectKey, mat);
                    addPotionIfMissing(existingNames, longKey, longDisplayName, mat, "long_" + effectKey, resolvePotionType("LONG_" + entry[2]), null);
                }
            }
        }

        addPotionIfMissing(existingNames, "awkward_potion", "Awkward Potion", Material.POTION, "awkward", PotionType.AWKWARD, null);
        addPotionIfMissing(existingNames, "mundane_potion", "Mundane Potion", Material.POTION, "mundane", PotionType.MUNDANE, null);
        addPotionIfMissing(existingNames, "thick_potion", "Thick Potion", Material.POTION, "thick", PotionType.THICK, null);
    }

    private static void addPotionIfMissing(java.util.Set<String> existingNames, String itemKey, String displayName, Material material, String potionEffectKey, PotionType potionType, String unused) {
        if (!existingNames.contains(itemKey.toLowerCase(Locale.ROOT))) {
            ALL_ITEMS.add(new MinecraftItem(itemKey.toLowerCase(Locale.ROOT), displayName, material.getMaxStackSize(), material, Collections.emptyList(), 0, potionEffectKey, potionType));
            existingNames.add(itemKey.toLowerCase(Locale.ROOT));
        }
    }

    public record MinecraftItem(String name, String displayName, int stackSize, Material material,
            List<String> enchantCategories, int maxDurability, String potionEffectKey,
            PotionType potionType) {

        public MinecraftItem(String name, String displayName, int stackSize, Material material,
                List<String> enchantCategories, int maxDurability) {
            this(name, displayName, stackSize, material, enchantCategories, maxDurability, null, null);
        }

        public MinecraftItem(String name, String displayName, int stackSize, Material material,
                List<String> enchantCategories, int maxDurability, String potionEffectKey) {
            this(name, displayName, stackSize, material, enchantCategories, maxDurability, potionEffectKey, null);
        }

        public boolean hasPotionEffect() {
            return potionEffectKey != null && !potionEffectKey.isEmpty();
        }

        public boolean hasVariants() {
            if (!hasPotionEffect()) return false;
            String base = getBaseEffectKey();
            return HAS_STRONG.contains(base) || HAS_LONG.contains(base);
        }

        public boolean hasStrongVariant() {
            if (!hasPotionEffect()) return false;
            return HAS_STRONG.contains(getBaseEffectKey());
        }

        public boolean hasLongVariant() {
            if (!hasPotionEffect()) return false;
            return HAS_LONG.contains(getBaseEffectKey());
        }

        public String getBaseEffectKey() {
            if (potionEffectKey == null) return null;
            String key = potionEffectKey;
            if (key.startsWith("strong_")) key = key.substring("strong_".length());
            if (key.startsWith("long_")) key = key.substring("long_".length());
            return key;
        }

        public String getBasePotionItemName() {
            if (potionEffectKey == null) return name;
            String base = potionEffectKey;
            if (base.startsWith("strong_")) base = base.substring("strong_".length());
            if (base.startsWith("long_")) base = base.substring("long_".length());
            String prefix = switch (material) {
                case POTION -> "potion_of_";
                case SPLASH_POTION -> "splash_potion_of_";
                case LINGERING_POTION -> "lingering_potion_of_";
                case TIPPED_ARROW -> "arrow_of_";
                default -> "potion_of_";
            };
            return prefix + base;
        }
    }

    public record MinecraftEnchantment(String name, String displayName, int maxLevel, String category,
                                       boolean treasureOnly, boolean curse, List<String> exclude) {}

    public record MinecraftEffect(String name, String displayName, String type) {}

    public static ItemStack createDisplayItemStack(MinecraftItem mcItem) {
        ItemStack item = new ItemStack(mcItem.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (mcItem.hasPotionEffect() && meta instanceof PotionMeta potionMeta) {
            if (mcItem.potionType() != null) {
                potionMeta.setBasePotionType(mcItem.potionType());
            } else if (mcItem.potionEffectKey() != null) {
                PotionEffectType effectType = PotionEffectType.getByKey(
                        NamespacedKey.minecraft(mcItem.potionEffectKey()));
                if (effectType != null) {
                    potionMeta.addCustomEffect(new PotionEffect(effectType, 600, 0), true);
                }
            }
            String duration = getPotionDuration(mcItem);
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.empty());
            if (duration != null) {
                lore.add(net.kyori.adventure.text.Component.text(duration, net.kyori.adventure.text.format.NamedTextColor.BLUE)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            potionMeta.lore(lore);
            potionMeta.displayName(net.kyori.adventure.text.Component.text(mcItem.displayName(), net.kyori.adventure.text.format.NamedTextColor.WHITE)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            item.setItemMeta(potionMeta);
        } else {
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String getPotionDuration(MinecraftItem mcItem) {
        if (mcItem.potionEffectKey() == null) return null;
        return getPotionDuration(mcItem.potionEffectKey(), mcItem.material());
    }

    public static String getPotionDuration(String potionEffectKey) {
        return getPotionDuration(potionEffectKey, Material.POTION);
    }

    public static String getPotionDuration(String potionEffectKey, Material potionMaterial) {
        if (potionEffectKey == null || potionEffectKey.isEmpty()) return null;
        String baseKey = potionEffectKey;
        boolean isStrong = baseKey.startsWith("strong_");
        boolean isLong = baseKey.startsWith("long_");
        if (isStrong) baseKey = baseKey.substring("strong_".length());
        if (isLong) baseKey = baseKey.substring("long_".length());

        for (Object[] durations : BASE_POTION_DURATIONS) {
            if (durations[0].toString().equals(baseKey)) {
                int ticks;
                if (((Number) durations[1]).intValue() == 0) return "Instant"; // Instant effects
                if (isStrong && durations.length > 3) {
                    ticks = ((Number) durations[3]).intValue();
                } else if (isLong && durations.length > 2) {
                    ticks = ((Number) durations[2]).intValue();
                } else {
                    ticks = ((Number) durations[1]).intValue();
                }

                // Lingering potions and tipped arrows have 25% duration of drinkable potions
                // Splash potions keep the same duration as drinkable potions
                if (potionMaterial == Material.LINGERING_POTION || potionMaterial == Material.TIPPED_ARROW) {
                    ticks = (int) Math.round(ticks * 0.25);
                }

                return ticksToDisplayString(ticks);
            }
        }
        return null;
    }

    /**
     * Convert tick duration to M:SS display format.
     */
    private static String ticksToDisplayString(int ticks) {
        if (ticks <= 0) return "Instant";
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + ":" + String.format("%02d", seconds);
        } else {
            return "0:" + String.format("%02d", seconds);
        }
    }

    /**
     * Convert a drinkable potion duration (in ticks) to lingering potion duration (25% of original).
     * Vanilla Paper uses 25% duration for lingering potions and tipped arrows.
     */
    private static int convertToLingeringTicks(int drinkableTicks) {
        if (drinkableTicks <= 0) return 0;
        return (int) Math.round(drinkableTicks * 0.25);
    }

    public static String getPotionEffectDisplay(String potionEffectKey) {
        if (potionEffectKey == null || potionEffectKey.isEmpty()) return "Potion Effect";
        boolean isStrong = potionEffectKey.startsWith("strong_");
        String baseKey = potionEffectKey;
        if (baseKey.startsWith("strong_")) baseKey = baseKey.substring("strong_".length());
        if (baseKey.startsWith("long_")) baseKey = baseKey.substring("long_".length());

        String name = formatPotionEffectName(baseKey);
        if (isStrong) {
            name += " II";
        }
        String duration = getPotionDuration(potionEffectKey);
        if (duration != null && !duration.isBlank()) {
            name += " - " + duration;
        }
        return name;
    }

    private static String formatPotionEffectName(String key) {
        for (String[] entry : POTION_ENTRIES) {
            if (entry[0].equals(key)) return entry[1];
        }
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    public static String getBasePotionEffectKey(String potionEffectKey) {
        if (potionEffectKey == null) return null;
        String base = potionEffectKey;
        if (base.startsWith("strong_")) base = base.substring("strong_".length());
        if (base.startsWith("long_")) base = base.substring("long_".length());
        return base;
    }

    public static int getPotionAmplifier(String potionEffectKey) {
        return potionEffectKey != null && potionEffectKey.startsWith("strong_") ? 1 : 0;
    }

    public static int getPotionDurationTicks(String potionEffectKey) {
        if (potionEffectKey == null || potionEffectKey.isEmpty()) return 3600;
        String baseKey = potionEffectKey;
        boolean isStrong = baseKey.startsWith("strong_");
        boolean isLong = baseKey.startsWith("long_");
        if (isStrong) baseKey = baseKey.substring("strong_".length());
        if (isLong) baseKey = baseKey.substring("long_".length());

        for (Object[] durations : BASE_POTION_DURATIONS) {
            if (durations[0].toString().equals(baseKey)) {
                if (((Number) durations[1]).intValue() == 0) return 1; // Instant effects
                int ticks;
                if (isStrong && durations.length > 3) {
                    ticks = ((Number) durations[3]).intValue();
                } else if (isLong && durations.length > 2) {
                    ticks = ((Number) durations[2]).intValue();
                } else {
                    ticks = ((Number) durations[1]).intValue();
                }
                // Lingering potions and tipped arrows have 25% duration
                if (potionEffectKey.contains("lingering") || potionEffectKey.contains("tipped")) {
                    ticks = convertToLingeringTicks(ticks);
                }
                return Math.max(1, ticks);
            }
        }
        return 3600;
    }

    public static PotionType getPotionType(String potionEffectKey) {
        if (potionEffectKey == null || potionEffectKey.isEmpty()) return null;
        String normalized = potionEffectKey.toUpperCase(Locale.ROOT);
        try {
            return PotionType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean hasStrong(MinecraftItem mcItem) {
        if (mcItem == null) return false;
        return mcItem.hasStrongVariant();
    }

    public static boolean hasLong(MinecraftItem mcItem) {
        if (mcItem == null) return false;
        return mcItem.hasLongVariant();
    }

    private static boolean isEffectNegative(String effectKey) {
        if (effectKey == null) return false;
        String lower = effectKey.toLowerCase(Locale.ROOT);
        return lower.contains("poison") || lower.contains("wither") || lower.contains("harm")
                || lower.contains("slowness") || lower.contains("weakness") || lower.contains("mining_fatigue")
                || lower.contains("blindness") || lower.contains("nausea") || lower.contains("hunger")
                || lower.contains("levitation") || lower.contains("unluck") || lower.contains("darkness")
                || lower.contains("bad_omen") || lower.contains("oozing")
                || lower.contains("weaving") || lower.contains("infestation") || lower.contains("wind_charging");
    }
}

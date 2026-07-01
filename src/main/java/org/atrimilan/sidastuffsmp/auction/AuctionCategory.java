package org.atrimilan.sidastuffsmp.auction;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum AuctionCategory {

    WEAPONS,
    ARMOR,
    TOOLS,
    FOOD,
    BLOCKS,
    REDSTONE,
    POTIONS,
    ENCHANTED,
    MISC;

    private static final Map<AuctionCategory, Set<Material>> CATEGORY_MAP = new EnumMap<>(AuctionCategory.class);
    private static volatile boolean initialized = false;

    public static void ensureInitialized() {
        if (initialized) return;
        synchronized (AuctionCategory.class) {
            if (initialized) return;
            doInitialize();
            initialized = true;
        }
    }

    private static void doInitialize() {
        Set<Material> weapons = EnumSet.noneOf(Material.class);
        weapons.add(Material.NETHERITE_SWORD);
        weapons.add(Material.DIAMOND_SWORD);
        weapons.add(Material.IRON_SWORD);
        weapons.add(Material.GOLDEN_SWORD);
        weapons.add(Material.STONE_SWORD);
        weapons.add(Material.WOODEN_SWORD);
        weapons.add(Material.BOW);
        weapons.add(Material.CROSSBOW);
        weapons.add(Material.TRIDENT);
        weapons.add(Material.MACE);
        if (Tag.ITEMS_AXES != null) {
            Tag.ITEMS_AXES.getValues().forEach(weapons::add);
        }
        CATEGORY_MAP.put(WEAPONS, weapons);

        Set<Material> armor = EnumSet.noneOf(Material.class);
        if (Tag.ITEMS_HEAD_ARMOR != null) Tag.ITEMS_HEAD_ARMOR.getValues().forEach(armor::add);
        if (Tag.ITEMS_CHEST_ARMOR != null) Tag.ITEMS_CHEST_ARMOR.getValues().forEach(armor::add);
        if (Tag.ITEMS_LEG_ARMOR != null) Tag.ITEMS_LEG_ARMOR.getValues().forEach(armor::add);
        if (Tag.ITEMS_FOOT_ARMOR != null) Tag.ITEMS_FOOT_ARMOR.getValues().forEach(armor::add);
        armor.add(Material.ELYTRA);
        armor.add(Material.SHIELD);
        armor.add(Material.TOTEM_OF_UNDYING);
        CATEGORY_MAP.put(ARMOR, armor);

        Set<Material> tools = EnumSet.noneOf(Material.class);
        if (Tag.ITEMS_PICKAXES != null) Tag.ITEMS_PICKAXES.getValues().forEach(tools::add);
        if (Tag.ITEMS_SHOVELS != null) Tag.ITEMS_SHOVELS.getValues().forEach(tools::add);
        if (Tag.ITEMS_HOES != null) Tag.ITEMS_HOES.getValues().forEach(tools::add);
        tools.add(Material.SHEARS);
        tools.add(Material.FLINT_AND_STEEL);
        tools.add(Material.FISHING_ROD);
        tools.add(Material.COMPASS);
        tools.add(Material.SPYGLASS);
        tools.add(Material.BRUSH);
        tools.add(Material.CLOCK);
        CATEGORY_MAP.put(TOOLS, tools);

        Set<Material> food = EnumSet.noneOf(Material.class);
        try {
            @SuppressWarnings("JavaReflectionMemberAccess")
            var field = Tag.class.getField("ITEMS_FOOD");
            @SuppressWarnings("unchecked")
            Tag<Material> foodTag = (Tag<Material>) field.get(null);
            foodTag.getValues().forEach(food::add);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        food.add(Material.HONEY_BOTTLE);
        food.add(Material.MILK_BUCKET);
        food.add(Material.RABBIT_STEW);
        food.add(Material.BEETROOT_SOUP);
        food.add(Material.MUSHROOM_STEW);
        food.add(Material.SUSPICIOUS_STEW);
        CATEGORY_MAP.put(FOOD, food);

        Set<Material> blocks = EnumSet.noneOf(Material.class);
        for (Material mat : Material.values()) {
            if (mat.isBlock() && !weapons.contains(mat) && !armor.contains(mat)) {
                blocks.add(mat);
            }
        }
        CATEGORY_MAP.put(BLOCKS, blocks);

        Set<Material> redstone = EnumSet.noneOf(Material.class);
        try {
            @SuppressWarnings("unchecked")
            var field = Tag.class.getField("ITEMS_REDSTONE");
            Tag<Material> redstoneTag = (Tag<Material>) field.get(null);
            redstoneTag.getValues().forEach(redstone::add);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        redstone.add(Material.REDSTONE);
        redstone.add(Material.REDSTONE_TORCH);
        redstone.add(Material.REDSTONE_BLOCK);
        redstone.add(Material.REPEATER);
        redstone.add(Material.COMPARATOR);
        redstone.add(Material.OBSERVER);
        redstone.add(Material.HOPPER);
        redstone.add(Material.DROPPER);
        redstone.add(Material.DISPENSER);
        redstone.add(Material.PISTON);
        redstone.add(Material.STICKY_PISTON);
        redstone.add(Material.LEVER);
        redstone.add(Material.TRIPWIRE_HOOK);
        redstone.add(Material.DAYLIGHT_DETECTOR);
        redstone.add(Material.TARGET);
        redstone.add(Material.CALIBRATED_SCULK_SENSOR);
        redstone.add(Material.SCULK_SENSOR);
        redstone.add(Material.SCULK_SHRIEKER);
        redstone.add(Material.NOTE_BLOCK);
        redstone.add(Material.JUKEBOX);
        redstone.add(Material.POWERED_RAIL);
        redstone.add(Material.DETECTOR_RAIL);
        redstone.add(Material.ACTIVATOR_RAIL);
        redstone.add(Material.RAIL);
        CATEGORY_MAP.put(REDSTONE, redstone);

        Set<Material> potions = EnumSet.noneOf(Material.class);
        potions.add(Material.POTION);
        potions.add(Material.SPLASH_POTION);
        potions.add(Material.LINGERING_POTION);
        potions.add(Material.EXPERIENCE_BOTTLE);
        potions.add(Material.GLASS_BOTTLE);
        potions.add(Material.TIPPED_ARROW);
        CATEGORY_MAP.put(POTIONS, potions);

        Set<Material> enchanted = EnumSet.noneOf(Material.class);
        if (org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.isLoaded()) {
            for (Material mat : Material.values()) {
                if (mat.isItem() && mat != Material.AIR
                    && org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.isEnchantable(mat)
                    && !weapons.contains(mat) && !armor.contains(mat) && !tools.contains(mat)) {
                    enchanted.add(mat);
                }
            }
        } else {
            for (Material mat : Material.values()) {
                if (mat.isItem() && mat != Material.AIR
                    && mat.getMaxStackSize() == 1
                    && !weapons.contains(mat) && !armor.contains(mat) && !tools.contains(mat)
                    && !food.contains(mat) && !blocks.contains(mat) && !redstone.contains(mat)
                    && !potions.contains(mat)) {
                    enchanted.add(mat);
                }
            }
        }
        enchanted.add(Material.ENCHANTED_BOOK);
        enchanted.add(Material.BOOK);
        CATEGORY_MAP.put(ENCHANTED, enchanted);

        Set<Material> misc = EnumSet.noneOf(Material.class);
        for (Material mat : Material.values()) {
            if (mat.isItem()
                    && !weapons.contains(mat)
                    && !armor.contains(mat)
                    && !tools.contains(mat)
                    && !food.contains(mat)
                    && !blocks.contains(mat)
                    && !redstone.contains(mat)
                    && !potions.contains(mat)
                    && !enchanted.contains(mat)) {
                misc.add(mat);
            }
        }
        CATEGORY_MAP.put(MISC, misc);
    }

    static {
        ensureInitialized();
    }

    public static AuctionCategory fromMaterial(Material material) {
        ensureInitialized();
        for (Map.Entry<AuctionCategory, Set<Material>> entry : CATEGORY_MAP.entrySet()) {
            if (entry.getValue().contains(material)) {
                return entry.getKey();
            }
        }
        return MISC;
    }

    public String displayName() {
        return switch (this) {
            case WEAPONS -> "Weapons";
            case ARMOR -> "Armor";
            case TOOLS -> "Tools";
            case FOOD -> "Food & Potions";
            case BLOCKS -> "Blocks";
            case REDSTONE -> "Redstone";
            case POTIONS -> "Potions";
            case ENCHANTED -> "Enchantable";
            case MISC -> "Misc";
        };
    }
}

package org.atrimilan.sidastuffsmp.sell;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.economy.EconomyShopGuiPriceLoader;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.ShulkerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SellCommand {

    public static final String DESCRIPTION = "Open sell GUI";
    public static final Set<String> ALIASES = Set.of("sell");

    static final Map<UUID, Map<Material, Integer>> playerSelectedItems = new HashMap<>();
    static final Map<UUID, Double> playerPendingTotal = new HashMap<>();
    static final Map<UUID, Map<Material, Double>> playerPricePerUnit = new HashMap<>();
    static final Map<UUID, List<ItemStack>> playerEmptyShulkers = new HashMap<>();

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("sell")
                .requires(source -> source.getSender().hasPermission("sidastuffsmp.sell") || source.getSender().hasPermission("sidastuffsmp.default"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage("Only players can use this command.");
                        return Command.SINGLE_SUCCESS;
                    }
                    openSellGui(player);
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    public static void openSellGui(Player player) {
        Inventory inv = Bukkit.createInventory(new SellGuiHolder(), 54, Component.text("Sell Items"));

        // Close button on the bottom row, far left
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            closeMeta.lore(List.of(Component.text("Returns items to your inventory.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            close.setItemMeta(closeMeta);
        }
        inv.setItem(45, close);

        // Sell button on the bottom row, far right (slot 53)
        ItemStack sell = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta sellMeta = sell.getItemMeta();
        if (sellMeta != null) {
            sellMeta.displayName(Component.text("Sell", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Sell every item in this GUI", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("If higher-paying orders are open,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("they will be used automatically.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            sellMeta.lore(lore);
            sell.setItemMeta(sellMeta);
        }
        inv.setItem(53, sell);

        // Initialize selected items map for player
        playerSelectedItems.put(player.getUniqueId(), new HashMap<>());
        playerPendingTotal.remove(player.getUniqueId());
        playerPricePerUnit.remove(player.getUniqueId());

        player.openInventory(inv);
    }

    public static void calculateTotal(Player player, Inventory inv) {
        Map<Material, Integer> selectedItems = playerSelectedItems.getOrDefault(player.getUniqueId(), new HashMap<>());
        selectedItems.clear();
        List<ItemStack> emptyShulkers = new ArrayList<>();

        // Collect items from input slots (0-44). Anything in the bottom row is ignored.
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir() && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                // Check if the item is a shulker box with items
                if (ShulkerUtil.isShulkerBox(item)) {
                    List<ItemStack> shulkerContents = ShulkerUtil.extractItemsFromShulker(item);
                    if (!shulkerContents.isEmpty()) {
                        // Extract items from shulker and count them
                        for (ItemStack content : shulkerContents) {
                            if (content != null && !content.getType().isAir()) {
                                selectedItems.merge(content.getType(), content.getAmount(), Integer::sum);
                            }
                        }
                        // Add empty shulkers to return to player
                        ItemStack emptyShulker = ShulkerUtil.getEmptyShulker(item);
                        if (emptyShulker != null) {
                            emptyShulker.setAmount(item.getAmount());
                            emptyShulkers.add(emptyShulker);
                        }
                    } else {
                        // Empty shulker, just count the shulker itself
                        selectedItems.merge(item.getType(), item.getAmount(), Integer::sum);
                    }
                } else {
                    // Regular item, count it
                    selectedItems.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            }
        }

        playerSelectedItems.put(player.getUniqueId(), selectedItems);
        playerEmptyShulkers.put(player.getUniqueId(), emptyShulkers);

        if (selectedItems.isEmpty()) {
            player.sendMessage(Chat.prefixed("No items to calculate. Put items in the top two rows.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        // When sell-priority is enabled, build a per-material price map that uses the best active
        // order price (if higher than the shop price) for each material before opening the
        // confirmation GUI. Items fulfilling those orders are physically delivered to the buyer
        // on confirm, not sold to the shop.
        java.util.Map<Material, Double> pricePerUnit = new java.util.HashMap<>();
        boolean useOrderPriority = org.atrimilan.sidastuffsmp.order.OrderConfig.isSellPriorityEnabled();
        double total = calculateSellValue(player, selectedItems, pricePerUnit, useOrderPriority);

        if (total <= 0) {
            player.sendMessage(Chat.prefixed("No sellable items found. Check if items have prices configured.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            openSellGui(player);
            return;
        }

        playerPendingTotal.put(player.getUniqueId(), total);
        playerPricePerUnit.put(player.getUniqueId(), pricePerUnit);
        openConfirmGui(player, selectedItems, total, pricePerUnit, useOrderPriority);
    }

    /**
     * Backwards-compatible call: calculates the shop sell value using only EconomyShopGUI prices
     * (or defaults if EconomyShopGUI is unavailable).
     */
    public static double calculateSellValue(Player player, Map<Material, Integer> items) {
        java.util.Map<Material, Double> ignored = new java.util.HashMap<>();
        return calculateSellValue(player, items, ignored, false);
    }

    /**
     * Calculates the sell value of the provided items. When {@code useOrderPriority} is true, the
     * per-unit price for each material is the larger of the EconomyShopGUI price and the best
     * active order's price. The resolved per-unit price is written to {@code pricePerUnit} for
     * use by {@link #confirmSale(Player, Inventory)}.
     */
    public static double calculateSellValue(Player player, Map<Material, Integer> items,
                                            java.util.Map<Material, Double> pricePerUnit,
                                            boolean useOrderPriority) {
        double total = 0.0;

        // Use the new EconomyShopGuiPriceLoader which reads YAML files directly
        java.util.Map<Material, Double> shopPrices = EconomyShopGuiPriceLoader.getAllSellPrices();

        java.util.Map<Material, Double> defaultPrices = shopPrices.isEmpty()
                ? getDefaultSellPriceMap()
                : java.util.Collections.emptyMap();

        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            Material mat = entry.getKey();
            int amount = entry.getValue();

            double shopPrice = shopPrices.getOrDefault(mat, defaultPrices.getOrDefault(mat, 0.0));
            double resolvedPrice = shopPrice;

            if (useOrderPriority) {
                double orderPrice = org.atrimilan.sidastuffsmp.order.OrderManager
                        .getBestOrderPriceForMaterial(mat.name(), player.getUniqueId());
                if (orderPrice > resolvedPrice) {
                    resolvedPrice = orderPrice;
                }
            }

            if (resolvedPrice > 0) {
                pricePerUnit.put(mat, resolvedPrice);
                total += resolvedPrice * amount;
            }
        }

        return total;
    }

    private static double getDefaultSellValue(Map<Material, Integer> items) {
        Map<Material, Double> defaults = getDefaultSellPriceMap();
        double total = 0.0;
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            Double price = defaults.get(entry.getKey());
            if (price != null) total += price * entry.getValue();
        }
        return total;
    }

    private static Map<Material, Double> getDefaultSellPriceMap() {
        return Map.ofEntries(
            Map.entry(Material.DIAMOND, 8.0),
            Map.entry(Material.DIAMOND_BLOCK, 72.0),
            Map.entry(Material.GOLD_INGOT, 4.0),
            Map.entry(Material.GOLD_BLOCK, 36.0),
            Map.entry(Material.IRON_INGOT, 1.0),
            Map.entry(Material.IRON_BLOCK, 9.0),
            Map.entry(Material.COAL, 1.0),
            Map.entry(Material.COAL_BLOCK, 9.0),
            Map.entry(Material.EMERALD, 4.0),
            Map.entry(Material.EMERALD_BLOCK, 36.0),
            Map.entry(Material.LAPIS_LAZULI, 0.5),
            Map.entry(Material.LAPIS_BLOCK, 4.5),
            Map.entry(Material.REDSTONE, 0.5),
            Map.entry(Material.REDSTONE_BLOCK, 4.5),
            Map.entry(Material.COPPER_INGOT, 0.5),
            Map.entry(Material.COPPER_BLOCK, 4.5),
            Map.entry(Material.NETHERITE_INGOT, 32.0),
            Map.entry(Material.NETHERITE_SCRAP, 8.0),
            Map.entry(Material.ANCIENT_DEBRIS, 16.0),
            Map.entry(Material.GOLDEN_APPLE, 4.0),
            Map.entry(Material.ENCHANTED_GOLDEN_APPLE, 128.0),
            Map.entry(Material.QUARTZ, 1.0),
            Map.entry(Material.AMETHYST_SHARD, 0.5),
            Map.entry(Material.COBWEB, 1.0),
            Map.entry(Material.STRING, 0.5),
            Map.entry(Material.ARROW, 0.1),
            Map.entry(Material.BONE, 0.5),
            Map.entry(Material.BONE_MEAL, 0.1),
            Map.entry(Material.SPIDER_EYE, 0.5),
            Map.entry(Material.FERMENTED_SPIDER_EYE, 1.0),
            Map.entry(Material.GHAST_TEAR, 4.0),
            Map.entry(Material.BLAZE_ROD, 2.0),
            Map.entry(Material.BLAZE_POWDER, 1.0),
            Map.entry(Material.ENDER_PEARL, 2.0),
            Map.entry(Material.ENDER_EYE, 4.0),
            Map.entry(Material.SHULKER_SHELL, 2.0),
            Map.entry(Material.PHANTOM_MEMBRANE, 1.0),
            Map.entry(Material.NAUTILUS_SHELL, 4.0),
            Map.entry(Material.HEART_OF_THE_SEA, 64.0),
            Map.entry(Material.GHAST_TEAR, 4.0),
            Map.entry(Material.RABBIT_HIDE, 0.25),
            Map.entry(Material.RABBIT_FOOT, 1.0),
            Map.entry(Material.SLIME_BALL, 1.0),
            Map.entry(Material.MAGMA_CREAM, 1.0),
            Map.entry(Material.LEATHER, 0.5),
            Map.entry(Material.GOAT_HORN, 1.0),
            Map.entry(Material.FEATHER, 0.25),
            Map.entry(Material.CHICKEN, 0.5),
            Map.entry(Material.PORKCHOP, 0.5),
            Map.entry(Material.BEEF, 0.5),
            Map.entry(Material.MUTTON, 0.5),
            Map.entry(Material.RABBIT, 0.5),
            Map.entry(Material.COD, 0.5),
            Map.entry(Material.SALMON, 0.5),
            Map.entry(Material.TROPICAL_FISH, 1.0),
            Map.entry(Material.PUFFERFISH, 1.0),
            Map.entry(Material.INK_SAC, 0.25),
            Map.entry(Material.GLOW_INK_SAC, 0.5),
            Map.entry(Material.SEAGRASS, 0.1),
            Map.entry(Material.KELP, 0.1),
            Map.entry(Material.SEA_PICKLE, 0.25),
            Map.entry(Material.BAMBOO, 0.1),
            Map.entry(Material.SUGAR_CANE, 0.25),
            Map.entry(Material.CACTUS, 0.25),
            Map.entry(Material.VINE, 0.1),
            Map.entry(Material.LILY_PAD, 0.1),
            Map.entry(Material.COCOA_BEANS, 0.5),
            Map.entry(Material.BEETROOT_SEEDS, 0.1),
            Map.entry(Material.MELON_SLICE, 0.1),
            Map.entry(Material.PUMPKIN, 0.5),
            Map.entry(Material.CARVED_PUMPKIN, 0.5),
            Map.entry(Material.HAY_BLOCK, 2.0),
            Map.entry(Material.WHEAT, 0.25),
            Map.entry(Material.BREAD, 0.5),
            Map.entry(Material.CAKE, 4.0),
            Map.entry(Material.COOKIE, 0.25),
            Map.entry(Material.APPLE, 0.5),
            Map.entry(Material.GOLDEN_CARROT, 2.0),
            Map.entry(Material.CARROT, 0.25),
            Map.entry(Material.POTATO, 0.25),
            Map.entry(Material.BEETROOT, 0.25),
            Map.entry(Material.BROWN_MUSHROOM, 0.25),
            Map.entry(Material.RED_MUSHROOM, 0.25),
            Map.entry(Material.MUSHROOM_STEM, 0.1),
            Map.entry(Material.FLOWER_POT, 0.5),
            Map.entry(Material.BRICK, 0.5),
            Map.entry(Material.NETHER_BRICK, 0.5),
            Map.entry(Material.NETHER_BRICKS, 2.0),
            Map.entry(Material.CRACKED_NETHER_BRICKS, 1.0),
            Map.entry(Material.CHISELED_NETHER_BRICKS, 2.0),
            Map.entry(Material.QUARTZ, 1.0),
            Map.entry(Material.QUARTZ_BLOCK, 4.0),
            Map.entry(Material.CHISELED_QUARTZ_BLOCK, 4.0),
            Map.entry(Material.QUARTZ_PILLAR, 4.0),
            Map.entry(Material.NETHER_WART, 0.5),
            Map.entry(Material.POPPED_CHORUS_FRUIT, 1.0),
            Map.entry(Material.CHORUS_FRUIT, 0.5),
            Map.entry(Material.SHULKER_BOX, 8.0),
            Map.entry(Material.DIAMOND_HELMET, 24.0),
            Map.entry(Material.DIAMOND_CHESTPLATE, 32.0),
            Map.entry(Material.DIAMOND_LEGGINGS, 28.0),
            Map.entry(Material.DIAMOND_BOOTS, 20.0),
            Map.entry(Material.DIAMOND_SWORD, 24.0),
            Map.entry(Material.DIAMOND_PICKAXE, 24.0),
            Map.entry(Material.DIAMOND_AXE, 24.0),
            Map.entry(Material.DIAMOND_SHOVEL, 24.0),
            Map.entry(Material.DIAMOND_HOE, 24.0),
            Map.entry(Material.GOLDEN_HELMET, 3.0),
            Map.entry(Material.GOLDEN_CHESTPLATE, 4.0),
            Map.entry(Material.GOLDEN_LEGGINGS, 3.5),
            Map.entry(Material.GOLDEN_BOOTS, 2.5),
            Map.entry(Material.GOLDEN_SWORD, 3.0),
            Map.entry(Material.GOLDEN_PICKAXE, 3.0),
            Map.entry(Material.GOLDEN_AXE, 3.0),
            Map.entry(Material.GOLDEN_SHOVEL, 3.0),
            Map.entry(Material.GOLDEN_HOE, 3.0),
            Map.entry(Material.IRON_HELMET, 3.0),
            Map.entry(Material.IRON_CHESTPLATE, 4.0),
            Map.entry(Material.IRON_LEGGINGS, 3.5),
            Map.entry(Material.IRON_BOOTS, 2.5),
            Map.entry(Material.IRON_SWORD, 3.0),
            Map.entry(Material.IRON_PICKAXE, 3.0),
            Map.entry(Material.IRON_AXE, 3.0),
            Map.entry(Material.IRON_SHOVEL, 3.0),
            Map.entry(Material.IRON_HOE, 3.0),
            Map.entry(Material.CHAINMAIL_HELMET, 1.5),
            Map.entry(Material.CHAINMAIL_CHESTPLATE, 2.0),
            Map.entry(Material.CHAINMAIL_LEGGINGS, 1.75),
            Map.entry(Material.CHAINMAIL_BOOTS, 1.25),
            Map.entry(Material.ELYTRA, 48.0),
            Map.entry(Material.TRIDENT, 32.0),
            Map.entry(Material.CROSSBOW, 6.0),
            Map.entry(Material.BOW, 3.0),
            Map.entry(Material.FISHING_ROD, 2.0),
            Map.entry(Material.CARROT_ON_A_STICK, 2.0),
            Map.entry(Material.WARPED_FUNGUS_ON_A_STICK, 4.0),
            Map.entry(Material.SHIELD, 4.0),
            Map.entry(Material.TURTLE_HELMET, 12.0),
            Map.entry(Material.CLOCK, 2.0),
            Map.entry(Material.COMPASS, 2.0),
            Map.entry(Material.RECOVERY_COMPASS, 32.0),
            Map.entry(Material.COMPASS, 16.0),
            Map.entry(Material.MAP, 2.0),
            Map.entry(Material.FILLED_MAP, 4.0),
            Map.entry(Material.ENDER_CHEST, 16.0),
            Map.entry(Material.ANVIL, 16.0),
            Map.entry(Material.CHIPPED_ANVIL, 12.0),
            Map.entry(Material.DAMAGED_ANVIL, 8.0),
            Map.entry(Material.BOOKSHELF,  4.0),
            Map.entry(Material.BOOK, 1.0),
            Map.entry(Material.ENCHANTED_BOOK, 2.0),
            Map.entry(Material.EXPERIENCE_BOTTLE, 1.0),
            Map.entry(Material.POTION, 1.0),
            Map.entry(Material.SPLASH_POTION, 1.0),
            Map.entry(Material.LINGERING_POTION, 2.0),
            Map.entry(Material.GLASS_BOTTLE, 0.25),
            Map.entry(Material.HONEYCOMB, 1.0),
            Map.entry(Material.HONEYCOMB_BLOCK, 8.0),
            Map.entry(Material.HONEY_BOTTLE, 2.0),
            Map.entry(Material.SLIME_BALL, 1.0),
            Map.entry(Material.HONEY_BLOCK, 4.0),
            Map.entry(Material.SLIME_BLOCK, 4.0),
            Map.entry(Material.IRON_NUGGET, 0.1),
            Map.entry(Material.GOLD_NUGGET, 0.4),
            Map.entry(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 16.0),
            Map.entry(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, 8.0),
            Map.entry(Material.MANGROVE_PROPAGULE, 0.25),
            Map.entry(Material.MANGROVE_ROOTS, 0.5),
            Map.entry(Material.MUDDY_MANGROVE_ROOTS, 0.5),
            Map.entry(Material.MUD, 0.1),
            Map.entry(Material.FROGSPAWN, 0.5),
            Map.entry(Material.ECHO_SHARD, 16.0)
        );
    }

    private static void openConfirmGui(Player player, Map<Material, Integer> selectedItems, double total,
                                       Map<Material, Double> pricePerUnit, boolean useOrderPriority) {
        Inventory inv = Bukkit.createInventory(new SellGuiHolder(), 36, Component.text("Confirm Sale"));

        // Header with total
        ItemStack header = new ItemStack(Material.GOLD_INGOT);
        ItemMeta headerMeta = header.getItemMeta();
        if (headerMeta != null) {
            headerMeta.displayName(Component.text("Total: " + formatPrice(total), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Items to sell:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            for (Map.Entry<Material, Integer> entry : selectedItems.entrySet()) {
                Double unit = pricePerUnit.get(entry.getKey());
                if (unit != null) {
                    lore.add(Component.text("  " + entry.getValue() + "x " + formatMaterialName(entry.getKey())
                                    + " @ " + formatPrice(unit) + " each",
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("  " + entry.getValue() + "x " + formatMaterialName(entry.getKey()),
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                }
            }
            if (useOrderPriority) {
                lore.add(Component.empty());
                lore.add(Component.text("Higher-paying orders used where available.", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click Confirm to complete the sale.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            headerMeta.lore(lore);
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text("Confirm Sale", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to sell items for", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(formatPrice(total), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            confirmMeta.lore(lore);
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Go back without selling.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            cancelMeta.lore(lore);
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(15, cancel);

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            closeMeta.lore(List.of(Component.text("Close and return items", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            close.setItemMeta(closeMeta);
        }
        inv.setItem(31, close);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Back to Sell GUI", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            backMeta.lore(List.of(Component.text("Go back and edit selection", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            back.setItemMeta(backMeta);
        }
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    /**
     * Backwards-compatible confirmation (no sell priority). Opens the confirm GUI with empty
     * per-unit price data.
     */
    private static void openConfirmGui(Player player, Map<Material, Integer> selectedItems, double total) {
        openConfirmGui(player, selectedItems, total, new HashMap<>(), false);
    }

    public static void sellAllOnClose(Player player, Inventory sourceInv) {
        UUID uuid = player.getUniqueId();

        // Build the per-material totals from the top 5 rows (slots 0-44).
        java.util.Map<Material, Integer> selectedItems = new java.util.HashMap<>();
        List<ItemStack> emptyShulkersToReturn = new ArrayList<>();

        for (int i = 0; i < 45; i++) {
            ItemStack item = sourceInv.getItem(i);
            if (item == null || item.getType().isAir() || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                continue;
            }

            // Check if the item is a shulker box with items
            if (ShulkerUtil.isShulkerBox(item)) {
                List<ItemStack> shulkerContents = ShulkerUtil.extractItemsFromShulker(item);
                if (!shulkerContents.isEmpty()) {
                    // Extract items from shulker and count them
                    for (ItemStack content : shulkerContents) {
                        if (content != null && !content.getType().isAir()) {
                            selectedItems.merge(content.getType(), content.getAmount(), Integer::sum);
                        }
                    }
                    // Add empty shulkers to return to player
                    ItemStack emptyShulker = ShulkerUtil.getEmptyShulker(item);
                    if (emptyShulker != null) {
                        emptyShulker.setAmount(item.getAmount());
                        emptyShulkersToReturn.add(emptyShulker);
                    }
                } else {
                    // Empty shulker, just count the shulker itself
                    selectedItems.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            } else {
                // Regular item, count it
                selectedItems.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        if (selectedItems.isEmpty()) {
            // Nothing to sell
            playerSelectedItems.remove(uuid);
            playerPendingTotal.remove(uuid);
            playerPricePerUnit.remove(uuid);
            playerEmptyShulkers.remove(uuid);
            return;
        }

        // Get shop prices for comparison
        java.util.Map<Material, Double> shopPrices = org.atrimilan.sidastuffsmp.economy.EconomyShopGuiPriceLoader.getAllSellPrices();
        boolean useOrderPriority = org.atrimilan.sidastuffsmp.order.OrderConfig.isSellPriorityEnabled();

        // Track what we deliver to orders vs sell to shop
        java.util.Map<Material, Integer> itemsToShop = new java.util.HashMap<>(selectedItems);
        double orderEarnings = 0;
        int itemsDeliveredToOrders = 0;

        if (useOrderPriority) {
            // Try to deliver each material to the best available order
            for (Material mat : new java.util.ArrayList<>(itemsToShop.keySet())) {
                int amount = itemsToShop.get(mat);
                double shopPrice = shopPrices.getOrDefault(mat, 0.0);

                // Find best order for this material
                org.atrimilan.sidastuffsmp.order.OrderListing bestOrder =
                        org.atrimilan.sidastuffsmp.order.OrderManager.getBestActiveOrderForMaterial(mat.name(), uuid);

                if (bestOrder != null && bestOrder.pricePerUnit() > shopPrice) {
                    // Deliver to order
                    ItemStack stack = new ItemStack(mat, amount);
                    org.atrimilan.sidastuffsmp.order.OrderManager.DeliverResult result =
                            org.atrimilan.sidastuffsmp.order.OrderManager.deliverItems(player, bestOrder.id(), new ItemStack[]{stack});

                    if (result.success()) {
                        orderEarnings += result.payment();
                        itemsDeliveredToOrders += result.deliveredCount();
                        itemsToShop.remove(mat);
                    }
                }
            }
        }

        // Calculate shop sell value for remaining items
        // Items with no price (price = 0) will be returned to the player
        double shopEarnings = 0;
        java.util.Map<Material, Integer> itemsWithNoPrice = new java.util.HashMap<>();
        for (java.util.Map.Entry<Material, Integer> entry : itemsToShop.entrySet()) {
            Material mat = entry.getKey();
            int amount = entry.getValue();
            double price = shopPrices.getOrDefault(mat, 0.0);
            if (price > 0) {
                shopEarnings += price * amount;
            } else {
                // Item has no price, will be returned to player
                itemsWithNoPrice.merge(mat, amount, Integer::sum);
            }
        }

        // Return items with no price to the player
        for (java.util.Map.Entry<Material, Integer> entry : itemsWithNoPrice.entrySet()) {
            ItemStack returnItem = new ItemStack(entry.getKey(), entry.getValue());
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(returnItem);
            for (java.util.Map.Entry<Integer, ItemStack> dropEntry : leftover.entrySet()) {
                player.getWorld().dropItemNaturally(player.getLocation(), dropEntry.getValue());
            }
        }

        // Remove items from the source inventory and handle shulkers.
        for (int i = 0; i < 45; i++) {
            ItemStack item = sourceInv.getItem(i);
            if (item == null || item.getType().isAir() || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                continue;
            }

            // For shulkers with items, we remove the entire shulker
            if (ShulkerUtil.isShulkerBox(item)) {
                sourceInv.setItem(i, null);
            } else {
                // For regular items, remove them
                Material mat = item.getType();
                int remaining = item.getAmount();
                for (int j = 0; j < 45 && remaining > 0; j++) {
                    ItemStack target = sourceInv.getItem(j);
                    if (target != null && target.getType() == mat) {
                        int take = Math.min(remaining, target.getAmount());
                        target.setAmount(target.getAmount() - take);
                        remaining -= take;
                        if (target.getAmount() <= 0) {
                            sourceInv.setItem(j, null);
                        }
                    }
                }
            }
        }

        // Return empty shulkers to the player
        for (ItemStack emptyShulker : emptyShulkersToReturn) {
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(emptyShulker);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // Credit the player for both order deliveries and shop sales
        double totalEarnings = orderEarnings + shopEarnings;
        if (totalEarnings > 0) {
            org.atrimilan.sidastuffsmp.economy.EconomyManager.deposit(uuid, totalEarnings, "sell",
                    null, null, "Sold via /sell GUI");
        }

        // Clean up
        playerSelectedItems.remove(uuid);
        playerPendingTotal.remove(uuid);
        playerPricePerUnit.remove(uuid);
        playerEmptyShulkers.remove(uuid);

        // Send appropriate message
        if (!itemsWithNoPrice.isEmpty()) {
            player.sendMessage(Chat.prefixed("Some items had no price and were returned: " + itemsWithNoPrice.size() + " material types", NamedTextColor.YELLOW));
        }
        if (orderEarnings > 0 && shopEarnings > 0) {
            player.sendMessage(Chat.prefixed("Sold items for " + formatPrice(totalEarnings)
                    + "! (" + formatPrice(orderEarnings) + " from orders, " + formatPrice(shopEarnings) + " from shop)", NamedTextColor.GREEN));
        } else if (orderEarnings > 0) {
            player.sendMessage(Chat.prefixed("Delivered " + itemsDeliveredToOrders + " items to orders for " + formatPrice(orderEarnings) + "!", NamedTextColor.GREEN));
        } else if (shopEarnings > 0) {
            player.sendMessage(Chat.prefixed("Sold items for " + formatPrice(shopEarnings) + "!", NamedTextColor.GREEN));
        } else if (itemsWithNoPrice.isEmpty()) {
            player.sendMessage(Chat.prefixed("No sellable items found. Check that items have prices configured.", NamedTextColor.RED));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static void confirmSale(Player player, Inventory sourceInv) {
        UUID uuid = player.getUniqueId();

        // Build the per-material totals from the top 5 rows (slots 0-44). The filler panes
        // are skipped automatically because they don't contribute any sellable value.
        java.util.Map<Material, Integer> selectedItems = new java.util.HashMap<>();
        List<ItemStack> emptyShulkersToReturn = new ArrayList<>();

        for (int i = 0; i < 45; i++) {
            ItemStack item = sourceInv.getItem(i);
            if (item == null || item.getType().isAir() || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                continue;
            }

            // Check if the item is a shulker box with items
            if (ShulkerUtil.isShulkerBox(item)) {
                List<ItemStack> shulkerContents = ShulkerUtil.extractItemsFromShulker(item);
                if (!shulkerContents.isEmpty()) {
                    // Extract items from shulker and count them
                    for (ItemStack content : shulkerContents) {
                        if (content != null && !content.getType().isAir()) {
                            selectedItems.merge(content.getType(), content.getAmount(), Integer::sum);
                        }
                    }
                    // Add empty shulkers to return to player
                    ItemStack emptyShulker = ShulkerUtil.getEmptyShulker(item);
                    if (emptyShulker != null) {
                        emptyShulker.setAmount(item.getAmount());
                        emptyShulkersToReturn.add(emptyShulker);
                    }
                } else {
                    // Empty shulker, just count the shulker itself
                    selectedItems.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            } else {
                // Regular item, count it
                selectedItems.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        if (selectedItems.isEmpty()) {
            player.sendMessage(Chat.prefixed("No items to sell. Place items in the GUI first.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        // Get shop prices for comparison
        java.util.Map<Material, Double> shopPrices = org.atrimilan.sidastuffsmp.economy.EconomyShopGuiPriceLoader.getAllSellPrices();
        boolean useOrderPriority = org.atrimilan.sidastuffsmp.order.OrderConfig.isSellPriorityEnabled();

        // Track what we deliver to orders vs sell to shop
        java.util.Map<Material, Integer> itemsToShop = new java.util.HashMap<>(selectedItems);
        double orderEarnings = 0;
        int itemsDeliveredToOrders = 0;

        if (useOrderPriority) {
            // Try to deliver each material to the best available order
            for (Material mat : new java.util.ArrayList<>(itemsToShop.keySet())) {
                int amount = itemsToShop.get(mat);
                double shopPrice = shopPrices.getOrDefault(mat, 0.0);

                // Find best order for this material
                org.atrimilan.sidastuffsmp.order.OrderListing bestOrder =
                        org.atrimilan.sidastuffsmp.order.OrderManager.getBestActiveOrderForMaterial(mat.name(), uuid);

                if (bestOrder != null && bestOrder.pricePerUnit() > shopPrice) {
                    // Deliver to order
                    ItemStack stack = new ItemStack(mat, amount);
                    org.atrimilan.sidastuffsmp.order.OrderManager.DeliverResult result =
                            org.atrimilan.sidastuffsmp.order.OrderManager.deliverItems(player, bestOrder.id(), new ItemStack[]{stack});

                    if (result.success()) {
                        orderEarnings += result.payment();
                        itemsDeliveredToOrders += result.deliveredCount();
                        itemsToShop.remove(mat);
                    }
                }
            }
        }

        // Calculate shop sell value for remaining items
        // Items with no price (price = 0) will be returned to the player
        double shopEarnings = 0;
        java.util.Map<Material, Integer> itemsWithNoPrice = new java.util.HashMap<>();
        for (java.util.Map.Entry<Material, Integer> entry : itemsToShop.entrySet()) {
            Material mat = entry.getKey();
            int amount = entry.getValue();
            double price = shopPrices.getOrDefault(mat, 0.0);
            if (price > 0) {
                shopEarnings += price * amount;
            } else {
                // Item has no price, will be returned to player
                itemsWithNoPrice.merge(mat, amount, Integer::sum);
            }
        }

        // Return items with no price to the player
        for (java.util.Map.Entry<Material, Integer> entry : itemsWithNoPrice.entrySet()) {
            ItemStack returnItem = new ItemStack(entry.getKey(), entry.getValue());
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(returnItem);
            for (java.util.Map.Entry<Integer, ItemStack> dropEntry : leftover.entrySet()) {
                player.getWorld().dropItemNaturally(player.getLocation(), dropEntry.getValue());
            }
        }

        // Remove items from the source inventory (top 45 slots only) and handle shulkers.
        for (int i = 0; i < 45; i++) {
            ItemStack item = sourceInv.getItem(i);
            if (item == null || item.getType().isAir() || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                continue;
            }

            // For shulkers with items, we remove the entire shulker
            if (ShulkerUtil.isShulkerBox(item)) {
                sourceInv.setItem(i, null);
            } else {
                // For regular items, remove them
                Material mat = item.getType();
                int remaining = item.getAmount();
                for (int j = 0; j < 45 && remaining > 0; j++) {
                    ItemStack target = sourceInv.getItem(j);
                    if (target != null && target.getType() == mat) {
                        int take = Math.min(remaining, target.getAmount());
                        target.setAmount(target.getAmount() - take);
                        remaining -= take;
                        if (target.getAmount() <= 0) {
                            sourceInv.setItem(j, null);
                        }
                    }
                }
            }
        }

        // Return empty shulkers to the player
        for (ItemStack emptyShulker : emptyShulkersToReturn) {
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(emptyShulker);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        // Also clear the stored empty shulkers from the pending map
        playerEmptyShulkers.remove(uuid);

        // Credit the player for both order deliveries and shop sales
        double totalEarnings = orderEarnings + shopEarnings;
        if (totalEarnings > 0) {
            org.atrimilan.sidastuffsmp.economy.EconomyManager.deposit(uuid, totalEarnings, "sell",
                    null, null, "Sold via /sell GUI");
        }

        // Send appropriate message
        if (!itemsWithNoPrice.isEmpty()) {
            player.sendMessage(Chat.prefixed("Some items had no price and were returned: " + itemsWithNoPrice.size() + " material types", NamedTextColor.YELLOW));
        }
        if (orderEarnings > 0 && shopEarnings > 0) {
            player.sendMessage(Chat.prefixed("Sold items for " + formatPrice(totalEarnings)
                    + "! (" + formatPrice(orderEarnings) + " from orders, " + formatPrice(shopEarnings) + " from shop)", NamedTextColor.GREEN));
        } else if (orderEarnings > 0) {
            player.sendMessage(Chat.prefixed("Delivered " + itemsDeliveredToOrders + " items to orders for " + formatPrice(orderEarnings) + "!", NamedTextColor.GREEN));
        } else if (shopEarnings > 0) {
            player.sendMessage(Chat.prefixed("Sold items for " + formatPrice(shopEarnings) + "!", NamedTextColor.GREEN));
        } else if (itemsWithNoPrice.isEmpty()) {
            player.sendMessage(Chat.prefixed("No sellable items found. Check that items have prices configured.", NamedTextColor.RED));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();
    }

    public static void cancelSale(Player player) {
        UUID uuid = player.getUniqueId();
        playerPendingTotal.remove(uuid);
        playerSelectedItems.remove(uuid);
        playerPricePerUnit.remove(uuid);
        playerEmptyShulkers.remove(uuid);
        openSellGui(player);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }

    public static void returnItems(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        playerSelectedItems.remove(uuid);
        playerPendingTotal.remove(uuid);
        playerPricePerUnit.remove(uuid);
        
        // Return any empty shulkers that were extracted during calculation
        List<ItemStack> emptyShulkers = playerEmptyShulkers.get(uuid);
        if (emptyShulkers != null) {
            for (ItemStack emptyShulker : emptyShulkers) {
                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(emptyShulker);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            playerEmptyShulkers.remove(uuid);
        }

        // Return items to player (top 5 rows only). Skip the filler panes so they don't
        // appear as stray gray glass in the player's inventory.
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir() && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                // If this is a shulker with items, extract the items and return them instead
                if (ShulkerUtil.isShulkerBox(item)) {
                    List<ItemStack> shulkerContents = ShulkerUtil.extractItemsFromShulker(item);
                    if (!shulkerContents.isEmpty()) {
                        // Return the extracted items
                        for (ItemStack content : shulkerContents) {
                            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(content);
                            for (ItemStack drop : leftover.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                        }
                        // Return the empty shulker
                        ItemStack emptyShulker = ShulkerUtil.getEmptyShulker(item);
                        if (emptyShulker != null) {
                            emptyShulker.setAmount(item.getAmount());
                            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(emptyShulker);
                            for (ItemStack drop : leftover.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                        }
                    } else {
                        // Empty shulker, just return it
                        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                } else {
                    // Regular item
                    java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                inv.setItem(i, null);
            }
        }
    }

    private static String formatPrice(double amount) {
        if (amount >= 1000000) {
            return String.format("%.2fM", amount / 1000000);
        } else if (amount >= 1000) {
            return String.format("%.2fK", amount / 1000);
        } else {
            return String.format("%.2f", amount);
        }
    }

    private static String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
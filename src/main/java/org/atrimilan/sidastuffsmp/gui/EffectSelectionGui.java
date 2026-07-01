package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EffectSelectionGui {

    private static final int PAGE_SIZE = 36;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_CLOSE = 53;

    private EffectSelectionGui() {}

    public static Inventory open(Player player, int page) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());

        List<MinecraftDataRegistry.MinecraftEffect> allEffects = MinecraftDataRegistry.getAllEffects();

        int totalEntries = allEffects.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(page, 0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalEntries);

        OrderGuiHolder holder = new OrderGuiHolder(OrderGuiHolder.GuiType.ENCHANTMENT_SELECT);
        holder.setViewerUuid(player.getUniqueId());
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Select Potion Effect"));

        for (int i = start; i < end; i++) {
            MinecraftDataRegistry.MinecraftEffect effect = allEffects.get(i);
            int slot = i - start;
            boolean isSelected = effect.name().equalsIgnoreCase(state.getSelectedEffect());

            Material potionMat = isNegativeEffect(effect) ? Material.SPLASH_POTION : Material.POTION;
            if (isSelected) {
                potionMat = Material.LINGERING_POTION;
            }

            ItemStack item = new ItemStack(potionMat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                PotionEffectType effectType = PotionEffectType.getByKey(NamespacedKey.minecraft(effect.name().toLowerCase(Locale.ROOT)));
                if (effectType != null && meta instanceof PotionMeta potionMeta) {
                    potionMeta.addCustomEffect(new PotionEffect(effectType, 600, 0), true);
                    // Set color based on effect type to ensure proper display
                    org.bukkit.Color potionColor = effectType.getColor();
                    if (potionColor != null) {
                        potionMeta.setColor(potionColor);
                    }
                    item.setItemMeta(potionMeta);
                    meta = item.getItemMeta();
                }

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                if (effect.type() != null) {
                    NamedTextColor typeColor = effect.type().equals("good") ? NamedTextColor.GREEN
                            : effect.type().equals("bad") ? NamedTextColor.RED
                            : NamedTextColor.GRAY;
                    String typeLabel = effect.type().substring(0, 1).toUpperCase(Locale.ROOT) + effect.type().substring(1);
                    lore.add(Component.text(typeLabel, typeColor)
                            .decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                if (isSelected) {
                    lore.add(Component.text("Currently selected", NamedTextColor.GREEN)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Click to deselect", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("Click to select", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                NamedTextColor nameColor = isSelected ? NamedTextColor.GREEN
                        : isNegativeEffect(effect) ? NamedTextColor.RED
                        : NamedTextColor.LIGHT_PURPLE;
                meta.displayName(Component.text(effect.displayName(), nameColor)
                        .decoration(TextDecoration.BOLD, isSelected)
                        .decoration(TextDecoration.ITALIC, false));
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }

        

        inv.setItem(SLOT_PREV, OrderGuiUtil.prevPageItem(page > 0));
        inv.setItem(SLOT_BACK, OrderGuiUtil.createControlItem(Material.ARROW,
                "Back to Create Order", List.of(
                        Component.text("Return to order creation", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false))));
        inv.setItem(SLOT_NEXT, OrderGuiUtil.nextPageItem(page < totalPages - 1));
        inv.setItem(SLOT_CLOSE, OrderGuiUtil.closeItem());

        ItemStack effectFiller = OrderGuiUtil.fillerItem();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, effectFiller);
            }
        }

        player.openInventory(inv);
        return inv;
    }

    private static boolean isNegativeEffect(MinecraftDataRegistry.MinecraftEffect effect) {
        if (effect.type() != null && effect.type().equals("bad")) return true;
        String name = effect.name().toLowerCase(Locale.ROOT);
        return name.contains("poison") || name.contains("wither") || name.contains("harm")
                || name.contains("slowness") || name.contains("weakness") || name.contains("mining_fatigue")
                || name.contains("blindness") || name.contains("nausea") || name.contains("hunger")
                || name.contains("levitation") || name.contains("unluck") || name.contains("darkness")
                || name.contains("bad_omen") || name.contains("slow_falling");
    }
}

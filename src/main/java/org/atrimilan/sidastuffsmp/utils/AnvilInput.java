package org.atrimilan.sidastuffsmp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class AnvilInput implements Listener {

    public static final class PendingRequest {
        private final String title;
        private final String tooltip;
        private final String placeholder;
        private final Consumer<String> onResult;
        private final Runnable onCancel;
        private boolean resolving = false;

        public PendingRequest(String title, String tooltip, String placeholder,
                              Consumer<String> onResult, Runnable onCancel) {
            this.title = title;
            this.tooltip = tooltip;
            this.placeholder = placeholder;
            this.onResult = onResult;
            this.onCancel = onCancel;
        }

        public String getTitle() { return title; }
        public String getTooltip() { return tooltip; }
        public String getPlaceholder() { return placeholder; }
        public Consumer<String> getOnResult() { return onResult; }
        public Runnable getOnCancel() { return onCancel; }
    }

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_ADDITIONAL = 1;
    private static final int SLOT_RESULT = 2;

    private static final Material[] ANVIL_ITEM_TYPES = {
            Material.NAME_TAG,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE
    };

    private static final Map<UUID, PendingRequest> PENDING = new HashMap<>();
    private static final Map<UUID, Long> RESOLVE_SCHEDULED = new HashMap<>();
    private static AnvilInput INSTANCE;

    private AnvilInput() {}

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new AnvilInput();
            Bukkit.getPluginManager().registerEvents(INSTANCE, SiDaStuffSmp.getInstance());
        }
    }

    public static void request(Player player, String title, String tooltip,
                               String placeholder, Consumer<String> onResult, Runnable onCancel) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        PENDING.remove(uuid);
        RESOLVE_SCHEDULED.remove(uuid);

        player.closeInventory();

        PendingRequest req = new PendingRequest(title, tooltip, placeholder, onResult, onCancel);
        PENDING.put(uuid, req);

        openAnvilGui(player, req, placeholder != null ? placeholder : "");
    }

    public static void request(Player player, String title, String tooltip,
                               Consumer<String> onResult) {
        request(player, title, tooltip, null, onResult, () -> {});
    }

    public static void request(Player player, String title, String tooltip,
                               Consumer<String> onResult, Runnable onCancel) {
        request(player, title, tooltip, null, onResult, onCancel);
    }

    public static void cancel(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PendingRequest req = PENDING.remove(uuid);
        if (req != null && req.onCancel != null && !req.resolving) {
            req.resolving = true;
            try {
                req.onCancel.run();
            } catch (Throwable ignored) {}
        }
    }

    private static void cancelWithCleanup(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PendingRequest req = PENDING.remove(uuid);
        if (req == null) return;

        INSTANCE.clearAnvilItems(player);
        player.setItemOnCursor(null);
        player.closeInventory();
        INSTANCE.forceCleanup(player);

        if (req.onCancel != null && !req.resolving) {
            req.resolving = true;
            Runnable cancelAction = req.onCancel;
            Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                INSTANCE.forceCleanup(player);
                try {
                    cancelAction.run();
                } catch (Throwable ignored) {}
            }, 2L);
        }
    }

    public static boolean hasPending(UUID uuid) {
        return PENDING.containsKey(uuid);
    }

    private static void openAnvilGui(Player player, PendingRequest req, String currentText) {
        UUID uuid = player.getUniqueId();

        InventoryView view = player.openAnvil(null, true);
        if (view == null) {
            PENDING.remove(uuid);
            if (req.onCancel != null) {
                req.onCancel.run();
            }
            return;
        }

        AnvilView anvilView = (AnvilView) view;
        anvilView.setRepairCost(0);

        ItemStack inputItem = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = inputItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(currentText != null && !currentText.isEmpty() ? currentText : " "));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.empty());
            if (req.tooltip != null && !req.tooltip.isEmpty()) {
                lore.add(Component.text(req.tooltip, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Edit name above, then click result slot", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            inputItem.setItemMeta(meta);
        }
        view.setItem(SLOT_INPUT, inputItem);

        ItemStack backItem = buildBackItem();
        view.setItem(SLOT_ADDITIONAL, backItem);

        ItemStack continueItem = buildContinueItem(currentText);
        view.setItem(SLOT_RESULT, continueItem);
    }

    private static ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back / Close", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.List.of(
                    Component.text("Return without saving", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildContinueItem(String currentText) {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Continue", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            if (currentText != null && !currentText.isEmpty()) {
                lore.add(Component.text("Submit: \"" + currentText + "\"", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Click to confirm", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getRenameText(Player player) {
        InventoryView view = player.getOpenInventory();
        if (view instanceof AnvilView anvilView) {
            String renameText = anvilView.getRenameText();
            if (renameText != null && !renameText.isBlank()) {
                return renameText.trim();
            }
        }
        if (view.getTopInventory() instanceof AnvilInventory anvilInv) {
            ItemStack inputItem = anvilInv.getItem(SLOT_INPUT);
            if (inputItem != null && inputItem.hasItemMeta() && inputItem.getItemMeta().hasDisplayName()) {
                String display = PlainTextComponentSerializer.plainText()
                        .serialize(inputItem.getItemMeta().displayName()).trim();
                if (!display.isEmpty() && !display.equals(" ")) {
                    return display;
                }
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        PendingRequest req = PENDING.get(uuid);
        if (req == null) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv instanceof AnvilInventory)) return;

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();

        if (rawSlot < 0) return;

        int topSize = topInv.getSize();

        if (rawSlot < topSize) {
            if (rawSlot == SLOT_RESULT) {
                String text = getRenameText(player);
                if (text == null || text.isEmpty()) {
                    if (req.placeholder != null && !req.placeholder.isEmpty()) {
                        text = req.placeholder;
                    } else {
                        player.sendMessage(Component.text("Please enter some text first.", NamedTextColor.RED));
                        return;
                    }
                }
                final String finalText = text;
                resolve(player, req, () -> {
                    if (req.onResult != null) {
                        req.onResult.accept(finalText);
                    }
                });
                return;
            }

            if (rawSlot == SLOT_ADDITIONAL) {
                cancelWithCleanup(player);
                return;
            }

            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!PENDING.containsKey(uuid)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv instanceof AnvilInventory)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        PendingRequest req = PENDING.get(uuid);
        if (req == null) return;

        String renameText = null;
        InventoryView view = player.getOpenInventory();
        if (view instanceof AnvilView anvilView) {
            renameText = anvilView.getRenameText();
        }
        if (renameText == null || renameText.isBlank()) {
            renameText = req.placeholder != null ? req.placeholder : "";
        }

        event.setResult(buildContinueItem(renameText));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        PendingRequest req = PENDING.remove(uuid);
        if (req == null) return;

        if (!(event.getInventory() instanceof AnvilInventory)) return;

        clearAnvilItems(player);
        player.setItemOnCursor(null);
        forceCleanup(player);

        if (!req.resolving && req.onCancel != null) {
            req.resolving = true;
            Runnable cancelAction = req.onCancel;
            Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                scheduleCleanup(player);
                try {
                    cancelAction.run();
                } catch (Throwable ignored) {}
            }, 1L);
        } else {
            scheduleCleanup(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PENDING.remove(uuid);
        RESOLVE_SCHEDULED.remove(uuid);
    }

    private void resolve(Player player, PendingRequest req, Runnable action) {
        if (req.resolving) return;
        req.resolving = true;
        UUID uuid = player.getUniqueId();
        PENDING.remove(uuid);

        clearAnvilItems(player);
        player.closeInventory();
        player.setItemOnCursor(null);
        scheduleCleanup(player);

        RESOLVE_SCHEDULED.put(uuid, System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
            RESOLVE_SCHEDULED.remove(uuid);
            forceCleanup(player);
            if (action != null) {
                try {
                    action.run();
                } catch (Throwable ignored) {}
            }
        }, 2L);
    }

    private void clearAnvilItems(Player player) {
        InventoryView view = player.getOpenInventory();
        AnvilInventory anvil = null;
        if (view.getTopInventory() instanceof AnvilInventory ai) {
            anvil = ai;
        }
        if (anvil == null) return;
        anvil.setItem(SLOT_INPUT, null);
        anvil.setItem(SLOT_ADDITIONAL, null);
        anvil.setItem(SLOT_RESULT, null);
    }

    private void scheduleCleanup(Player player) {
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
            forceCleanup(player);
        }, 1L);
    }

    private void forceCleanup(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            player.setItemOnCursor(null);
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && isAnvilItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        ItemStack cursor2 = player.getItemOnCursor();
        if (cursor2 != null && !cursor2.getType().isAir() && isAnvilItem(cursor2)) {
            player.setItemOnCursor(null);
        }
    }

    private static boolean isAnvilItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material mat = item.getType();
        for (Material anvilType : ANVIL_ITEM_TYPES) {
            if (mat == anvilType) return true;
        }
        return false;
    }
}

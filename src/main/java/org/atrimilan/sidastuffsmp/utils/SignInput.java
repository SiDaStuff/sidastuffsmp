package org.atrimilan.sidastuffsmp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Global sign-input utility used by every feature (homes, orders, auction,
 * bounties, etc.). The sign is NEVER placed in the world – this opens a small
 * virtual sign GUI backed by an inventory. Players click the "input" slot,
 * type one line in chat, and the text is appended to the input field. The GUI
 * is reopened automatically so the player can confirm, clear or cancel.
 *
 * The exposed entry points are:
 * <ul>
 *   <li>{@link #request(Player, String, String, String, Consumer, Runnable)} –
 *       generic request with title, label, optional placeholder, success and
 *       cancel callbacks.</li>
 *   <li>{@link #request(Player, String, String, Consumer)} – convenience
 *       overload without placeholder/cancel callbacks.</li>
 *   <li>{@link #request(Player, String, String, Consumer, Runnable)} –
 *       convenience overload with cancel callback, no placeholder.</li>
 *   <li>{@link #cancel(Player)} – cancel any pending request.</li>
 * </ul>
 *
 * Backwards-compatible helpers that the rest of the plugin already calls are
 * also retained (e.g. {@code requestText}, {@code requestIconSearch}).
 */
public final class SignInput implements Listener {

    /**
     * Description of a pending sign-input request. Stored per-player so we know
     * which callback to fire when the player submits or cancels.
     */
    public static final class PendingInput {
        private final String title;
        private final String label;
        private final String placeholder;
        private final Consumer<String> onResult;
        private final Runnable onCancel;

        public PendingInput(String title, String label, String placeholder,
                            Consumer<String> onResult, Runnable onCancel) {
            this.title = title;
            this.label = label;
            this.placeholder = placeholder;
            this.onResult = onResult;
            this.onCancel = onCancel;
        }

        public String getTitle() { return title; }
        public String getLabel() { return label; }
        public String getPlaceholder() { return placeholder; }
        public Consumer<String> getOnResult() { return onResult; }
        public Runnable getOnCancel() { return onCancel; }
    }

    /** Custom inventory holder used to identify our sign-input GUIs. */
    public static final class SignHolder implements InventoryHolder {
        private final UUID ownerUuid;

        public SignHolder(UUID ownerUuid) {
            this.ownerUuid = ownerUuid;
        }

        public UUID getOwnerUuid() {
            return ownerUuid;
        }

        @Override
        public @NotNull Inventory getInventory() {
            // Never actually used – this holder only exists so we can detect
            // the inventory in click events.
            return Bukkit.createInventory(this, 9, Component.empty());
        }
    }

    /** Slot that contains the text input "paper" inside the sign GUI. */
    private static final int SLOT_INPUT = 0;
    /** Slot that contains the "Confirm" button. */
    private static final int SLOT_CONFIRM = 3;
    /** Slot that contains the "Cancel" button. */
    private static final int SLOT_CANCEL = 5;
    /** Slot that contains a "Clear" button (resets the input box). */
    private static final int SLOT_CLEAR = 8;

    private static final Map<UUID, PendingInput> PENDING = new HashMap<>();
    private static final Map<UUID, String> CURRENT_TEXT = new HashMap<>();
    private static SignInput INSTANCE;

    private SignInput() {}

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new SignInput();
            Bukkit.getPluginManager().registerEvents(INSTANCE,
                    org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance());
            ChatInput.init();
        }
    }

    /**
     * Open the global sign-input GUI for the given player.
     *
     * @param player      The player that should enter input.
     * @param title       Title of the GUI (used as inventory title).
     * @param label       Label printed on the "input paper" item so the player
     *                    knows what to type.
     * @param placeholder Optional placeholder used when the player submits an
     *                    empty input (or null/blank to disallow empty input).
     * @param onResult    Callback invoked with the trimmed input string when
     *                    the player presses the green "Confirm" button.
     * @param onCancel    Callback invoked when the player presses the red
     *                    "Cancel" button or closes the GUI without confirming.
     */
    public static void request(Player player, String title, String label, String placeholder,
                               Consumer<String> onResult, Runnable onCancel) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        // Cancel any previous pending input for this player.
        PENDING.remove(uuid);
        CURRENT_TEXT.remove(uuid);

        player.closeInventory();

        PendingInput input = new PendingInput(title, label, placeholder, onResult, onCancel);
        PENDING.put(uuid, input);
        CURRENT_TEXT.put(uuid, "");

        Inventory inv = Bukkit.createInventory(new SignHolder(uuid), 9,
                Component.text(title == null ? "Input" : title,
                        NamedTextColor.DARK_BLUE, TextDecoration.BOLD));
        inv.setItem(SLOT_INPUT, buildInputPaper(label, ""));
        inv.setItem(SLOT_CONFIRM, buildConfirm());
        inv.setItem(SLOT_CANCEL, buildCancel());
        inv.setItem(SLOT_CLEAR, buildClear());
        player.openInventory(inv);
    }

    /** Convenience overload – cancel callback defaults to a no-op. */
    public static void request(Player player, String title, String label,
                               Consumer<String> onResult) {
        request(player, title, label, null, onResult, () -> {});
    }

    /** Convenience overload – no placeholder. */
    public static void request(Player player, String title, String label,
                               Consumer<String> onResult, Runnable onCancel) {
        request(player, title, label, null, onResult, onCancel);
    }

    /** Cancel any pending sign-input request for the given player. */
    public static void cancel(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PendingInput input = PENDING.remove(uuid);
        CURRENT_TEXT.remove(uuid);
        if (input != null && input.getOnCancel() != null) {
            try {
                input.getOnCancel().run();
            } catch (Throwable ignored) {}
        }
    }

    /* -------------------------------------------------------------- */
    /*   Backwards-compatible helpers (legacy API used elsewhere)    */
    /* -------------------------------------------------------------- */

    /** Legacy entry-point – matches the old {@code requestText} signature. */
    public static void requestText(Player player, String title, String label, String placeholder,
                                   Consumer<String> onResult, Runnable onCancel) {
        request(player, title, label, placeholder, onResult, onCancel);
    }

    /** Legacy entry-point – icon search flow used by the HomeIconGui. */
    public static void requestIconSearch(Player player, String placeholder,
                                         Consumer<String> onResult) {
        request(player, "Search Icon", "Search icon name:",
                placeholder, onResult, () -> {});
    }

    /* -------------------------------------------------------------- */
    /*   GUI construction helpers                                     */
    /* -------------------------------------------------------------- */

    private static ItemStack buildInputPaper(String label, String current) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            boolean hasText = current != null && !current.isEmpty();
            meta.displayName(Component.text(
                    hasText ? current : (label == null ? "Type in chat" : label),
                    NamedTextColor.WHITE, TextDecoration.BOLD));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            if (label != null && !label.isEmpty() && !hasText) {
                lore.add(Component.text(label, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click then type in chat", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    private static ItemStack buildConfirm() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Confirm", NamedTextColor.GREEN, TextDecoration.BOLD));
            meta.lore(java.util.List.of(
                    Component.text("Submit the text above", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildCancel() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Cancel", NamedTextColor.RED, TextDecoration.BOLD));
            meta.lore(java.util.List.of(
                    Component.text("Close without saving", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildClear() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Clear", NamedTextColor.YELLOW, TextDecoration.BOLD));
            meta.lore(java.util.List.of(
                    Component.text("Reset the text field", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /* -------------------------------------------------------------- */
    /*   Event handlers                                               */
    /* -------------------------------------------------------------- */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SignHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.getOwnerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot();
        UUID uuid = player.getUniqueId();
        PendingInput input = PENDING.get(uuid);
        if (input == null) return;

        if (slot == SLOT_INPUT) {
            // Switch into "typing" mode: register the chat listener FIRST
            // (so it survives the inventory-close event that fires from
            // player.closeInventory()), then close the GUI.
            ChatInput.await(player, msg -> {
                String existing = CURRENT_TEXT.getOrDefault(uuid, "");
                String combined;
                if (existing == null || existing.isEmpty()) {
                    combined = msg;
                } else {
                    combined = existing + " " + msg;
                }
                CURRENT_TEXT.put(uuid, combined);
                Bukkit.getScheduler().runTask(
                        org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance(),
                        () -> reopenSignGui(player, input));
            }, () -> Bukkit.getScheduler().runTask(
                    org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance(),
                    () -> reopenSignGui(player, input)));
            player.closeInventory();
            return;
        }

        if (slot == SLOT_CONFIRM) {
            String text = CURRENT_TEXT.getOrDefault(uuid, "").trim();
            PENDING.remove(uuid);
            CURRENT_TEXT.remove(uuid);
            player.closeInventory();
            if (text.isEmpty()) {
                if (input.getPlaceholder() != null && !input.getPlaceholder().isEmpty()) {
                    text = input.getPlaceholder();
                } else {
                    if (input.getOnCancel() != null) input.getOnCancel().run();
                    return;
                }
            }
            if (input.getOnResult() != null) input.getOnResult().accept(text);
            return;
        }

        if (slot == SLOT_CANCEL) {
            PENDING.remove(uuid);
            CURRENT_TEXT.remove(uuid);
            player.closeInventory();
            if (input.getOnCancel() != null) input.getOnCancel().run();
            return;
        }

        if (slot == SLOT_CLEAR) {
            CURRENT_TEXT.put(uuid, "");
            reopenSignGui(player, input);
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SignHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!holder.getOwnerUuid().equals(player.getUniqueId())) return;

        // If we are waiting for chat input (ChatInput owns the slot), let it
        // reopen the GUI itself once the player types.
        if (ChatInput.isWaiting(holder.getOwnerUuid())) return;

        PendingInput input = PENDING.remove(holder.getOwnerUuid());
        CURRENT_TEXT.remove(holder.getOwnerUuid());
        if (input != null && input.getOnCancel() != null) {
            try {
                input.getOnCancel().run();
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PENDING.remove(uuid);
        CURRENT_TEXT.remove(uuid);
        ChatInput.cancel(uuid);
    }

    private static void reopenSignGui(Player player, PendingInput input) {
        UUID uuid = player.getUniqueId();
        String current = CURRENT_TEXT.getOrDefault(uuid, "");
        Inventory inv = Bukkit.createInventory(new SignHolder(uuid), 9,
                Component.text(input.getTitle() == null ? "Input" : input.getTitle(),
                        NamedTextColor.DARK_BLUE, TextDecoration.BOLD));
        inv.setItem(SLOT_INPUT, buildInputPaper(input.getLabel(), current));
        inv.setItem(SLOT_CONFIRM, buildConfirm());
        inv.setItem(SLOT_CANCEL, buildCancel());
        inv.setItem(SLOT_CLEAR, buildClear());
        player.openInventory(inv);
    }

    /** Helper used by GUI builders when they want to preview the current text. */
    public static String getCurrentText(UUID uuid) {
        return CURRENT_TEXT.getOrDefault(uuid, "");
    }

    /** True when this player currently has an open sign-input request. */
    public static boolean hasPending(UUID uuid) {
        return PENDING.containsKey(uuid);
    }
}
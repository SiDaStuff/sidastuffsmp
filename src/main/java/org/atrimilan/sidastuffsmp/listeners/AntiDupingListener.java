package org.atrimilan.sidastuffsmp.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.commands.PunishmentCommands;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiDupingListener implements Listener {

    private static final String BYPASS_PERMISSION = "sidastuffsmp.antidupe.bypass";
    private static final String ALERT_PERMISSION = "sidastuffsmp.antidupe.alerts";

    private static final List<InventoryType> EXCLUDED_INVENTORY_TYPES = List.of(
            InventoryType.CRAFTING,
            InventoryType.WORKBENCH,
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            
            InventoryType.STONECUTTER,
            InventoryType.LOOM,
            InventoryType.CARTOGRAPHY,
            InventoryType.GRINDSTONE,
            InventoryType.SMITHING,
            InventoryType.ANVIL,
            InventoryType.BEACON,
            InventoryType.ENCHANTING,
            InventoryType.BREWING,
            InventoryType.MERCHANT,
            InventoryType.LECTERN
    );

    private final SiDaStuffSmp plugin;
    private static AntiDupingListener instance;
    private final Map<UUID, InventorySnapshot> snapshots = new HashMap<>();
    private final Map<UUID, Deque<Long>> alertHistory = new HashMap<>();
    private final Map<UUID, Long> autoPunishCooldown = new HashMap<>();

    public AntiDupingListener(SiDaStuffSmp plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static ExternalTestResult runExternalTestDupe(Player player, String source) {
        if (instance == null) {
            return new ExternalTestResult(false, 0, "Anti-dupe listener is not initialized.");
        }
        return instance.performExternalTestDupe(player, source);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        snapshots.put(event.getPlayer().getUniqueId(), captureSnapshot(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        snapshots.remove(playerId);
        alertHistory.remove(playerId);
        autoPunishCooldown.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isExcludedInventory(event.getInventory().getType())) {
            snapshots.put(player.getUniqueId(), captureSnapshot(player));
            return;
        }
        monitorInventoryMutation(player, "click");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isExcludedInventory(event.getInventory().getType())) {
            snapshots.put(player.getUniqueId(), captureSnapshot(player));
            return;
        }
        monitorInventoryMutation(player, "drag");
    }

    private boolean isExcludedInventory(InventoryType type) {
        return EXCLUDED_INVENTORY_TYPES.contains(type);
    }

    private void monitorInventoryMutation(Player player, String source) {
        if (!isAntiDupeEnabled() || player.hasPermission(BYPASS_PERMISSION) || player.getGameMode() == GameMode.CREATIVE) {
            snapshots.put(player.getUniqueId(), captureSnapshot(player));
            return;
        }

        InventorySnapshot before = snapshots.getOrDefault(player.getUniqueId(), captureSnapshot(player));
        Bukkit.getScheduler().runTask(plugin, () -> {
            InventorySnapshot after = captureSnapshot(player);
            snapshots.put(player.getUniqueId(), after);
            evaluateForDupe(player, before, after, source);
        });
    }

    private ExternalTestResult performExternalTestDupe(Player player, String source) {
        if (!isAntiDupeEnabled()) {
            return new ExternalTestResult(false, 0, "Anti-dupe is disabled in config.");
        }
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return new ExternalTestResult(false, 0, "Target has anti-dupe bypass permission.");
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return new ExternalTestResult(false, 0, "Target is in creative mode.");
        }

        InventorySnapshot before = captureSnapshot(player);
        int duplicatedItems = duplicatePlayerStorage(player.getInventory());
        InventorySnapshot after = captureSnapshot(player);
        snapshots.put(player.getUniqueId(), after);
        evaluateForDupe(player, before, after, source);

        return new ExternalTestResult(true, duplicatedItems, "External dupe test executed.");
    }

    private int duplicatePlayerStorage(PlayerInventory inventory) {
        ItemStack[] storage = inventory.getStorageContents();
        List<ItemStack> clones = new ArrayList<>();

        for (ItemStack item : storage) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }
            clones.add(item.clone());
        }

        int addedAmount = 0;
        for (ItemStack clone : clones) {
            int originalAmount = clone.getAmount();
            Map<Integer, ItemStack> leftover = inventory.addItem(clone);
            int leftoverAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            addedAmount += Math.max(0, originalAmount - leftoverAmount);
        }

        if (addedAmount > 0) {
            return addedAmount;
        }

        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }

            int boost = Math.max(1, item.getAmount());
            item.setAmount(item.getAmount() + boost);
            storage[i] = item;
            inventory.setStorageContents(storage);
            return boost;
        }

        return 0;
    }

    private void evaluateForDupe(Player player, InventorySnapshot before, InventorySnapshot after, String source) {
        int maxIncrease = largestPositiveIncrease(before.materialCounts(), after.materialCounts());
        int increaseThreshold = Math.max(1, ConfigManager.getConfig().getInt("anti-dupe.max-increase-per-check", 128));

        String flagReason = null;
        if (isOverstackCheckEnabled() && after.hasOverstack()) {
            flagReason = "overstacked item stack detected";
        } else if (maxIncrease >= increaseThreshold) {
            flagReason = "suspicious growth of +" + maxIncrease + " items in one inventory update";
        }

        if (flagReason == null) {
            return;
        }

        int alertCount = registerAlert(player.getUniqueId());
        broadcastAlert(player.getName(), source, flagReason, alertCount);

        int punishAfter = Math.max(1, ConfigManager.getConfig().getInt("anti-dupe.auto-punish-after-alerts", 5));
        if (alertCount < punishAfter) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = Math.max(30L, ConfigManager.getConfig().getLong("anti-dupe.auto-punish-cooldown-seconds", 300L)) * 1000L;
        long nextAllowedAt = autoPunishCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowedAt) {
            return;
        }

        autoPunishCooldown.put(player.getUniqueId(), now + cooldownMillis);
        alertHistory.remove(player.getUniqueId());

        PunishmentCommands.AutoPunishResult result = PunishmentCommands.autoPunishForDuping(player, "AntiDupe");
        String duration = result.permanent() ? "permanent" : result.durationDays() + " day(s)";
        broadcastSystem("[AntiDupe] Auto-punished " + result.playerName() + " for Duping at escalation "
                + result.punishCount() + " (" + duration + ").");
    }

    private int registerAlert(UUID playerId) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(10L, ConfigManager.getConfig().getLong("anti-dupe.alert-window-seconds", 300L)) * 1000L;

        Deque<Long> timestamps = alertHistory.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        timestamps.addLast(now);
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMillis) {
            timestamps.removeFirst();
        }
        return timestamps.size();
    }

    private void broadcastAlert(String playerName, String source, String reason, int count) {
        String message = "[AntiDupe] " + playerName + " flagged via " + source + ": " + reason + " (alerts in window: " + count + ").";
        broadcastSystem(message);
    }

    private void broadcastSystem(String message) {
        Component component = Chat.prefixed(message, NamedTextColor.RED);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(ALERT_PERMISSION)
                    || online.hasPermission("sidastuffsmp.punish.*")
                    || online.hasPermission("sidastuffsmp.punish")
                    || online.isOp()) {
                online.sendMessage(component);
            }
        }

        plugin.getLogger().warning(message);
    }

    private InventorySnapshot captureSnapshot(Player player) {
        Map<Material, Integer> counts = new HashMap<>();
        boolean hasOverstack = false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }

            counts.merge(item.getType(), item.getAmount(), Integer::sum);
            int maxStackSize = item.getMaxStackSize();
            if (maxStackSize > 0 && item.getAmount() > maxStackSize) {
                hasOverstack = true;
            }
        }

        return new InventorySnapshot(counts, hasOverstack);
    }

    private int largestPositiveIncrease(Map<Material, Integer> before, Map<Material, Integer> after) {
        int largest = 0;
        for (Map.Entry<Material, Integer> entry : after.entrySet()) {
            int beforeAmount = before.getOrDefault(entry.getKey(), 0);
            int delta = entry.getValue() - beforeAmount;
            if (delta > largest) {
                largest = delta;
            }
        }
        return largest;
    }

    private boolean isAntiDupeEnabled() {
        return ConfigManager.getConfig().getBoolean("anti-dupe.enabled", true);
    }

    private boolean isOverstackCheckEnabled() {
        return ConfigManager.getConfig().getBoolean("anti-dupe.check-overstack", true);
    }

    private record InventorySnapshot(Map<Material, Integer> materialCounts, boolean hasOverstack) {}
    public record ExternalTestResult(boolean executed, int duplicatedItems, String message) {}
}

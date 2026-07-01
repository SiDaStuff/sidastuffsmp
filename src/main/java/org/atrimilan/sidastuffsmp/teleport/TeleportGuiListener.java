package org.atrimilan.sidastuffsmp.teleport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class TeleportGuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeleportGuiHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.getViewerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getGuiType()) {
            case SEND_CONFIRM -> handleSendConfirmClick(player, holder, slot);
            case RECEIVE_CONFIRM -> handleReceiveConfirmClick(player, holder, slot);
            case SETTINGS -> handleSettingsClick(player, holder, slot);
        }
    }

    private void handleSendConfirmClick(Player player, TeleportGuiHolder holder, int slot) {
        if (slot == TeleportConfirmGui.SLOT_CONFIRM) {
            UUID targetUuid = holder.getTargetUuid();
            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null || !target.isOnline()) {
                player.sendMessage(Chat.prefixed("Player is no longer online.", NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            TeleportRequest.Type type = holder.getRequestType();
            TeleportResult result;
            if (type == TeleportRequest.Type.TPA) {
                result = TeleportManager.sendTpa(player, target);
            } else {
                result = TeleportManager.sendTpaHere(player, target);
            }

            player.closeInventory();

            if (result.success()) {
                player.sendMessage(Chat.prefixed("Teleport request sent to " + target.getName() + ".", NamedTextColor.GREEN));
                if (result.request() != null) {
                    PlayerSettings targetSettings = PlayerSettings.get(targetUuid);
                    if (targetSettings.isTpaAutoEnabled()) {
                        TeleportManager.handleAutoAccept(target, result.request());
                    } else {
                        sendChatRequest(target, result.request());
                    }
                }
            } else {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
            }
        } else if (slot == TeleportConfirmGui.SLOT_CANCEL) {
            player.closeInventory();
        }
    }

    private void handleReceiveConfirmClick(Player player, TeleportGuiHolder holder, int slot) {
        if (slot == TeleportConfirmGui.SLOT_CONFIRM) {
            player.closeInventory();
            TeleportResult result = TeleportManager.acceptRequest(player);
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        } else if (slot == TeleportConfirmGui.SLOT_CANCEL) {
            player.closeInventory();
            TeleportResult result = TeleportManager.denyRequest(player);
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
    }

    private void handleSettingsClick(Player player, TeleportGuiHolder holder, int slot) {
        PlayerSettings settings = PlayerSettings.get(player.getUniqueId());

        if (slot == SettingsGui.SLOT_TPA) {
            settings.setTpaEnabled(!settings.isTpaEnabled());
            player.sendMessage(Chat.prefixed("TPA requests: " + (settings.isTpaEnabled() ? "Enabled" : "Disabled"),
                    settings.isTpaEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_TPA_HERE) {
            settings.setTpaHereEnabled(!settings.isTpaHereEnabled());
            player.sendMessage(Chat.prefixed("TPAhere requests: " + (settings.isTpaHereEnabled() ? "Enabled" : "Disabled"),
                    settings.isTpaHereEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_TPA_AUTO) {
            settings.setTpaAutoEnabled(!settings.isTpaAutoEnabled());
            player.sendMessage(Chat.prefixed("TPAuto: " + (settings.isTpaAutoEnabled() ? "Enabled" : "Disabled"),
                    settings.isTpaAutoEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_NIGHT_VISION) {
            settings.setNightVisionEnabled(!settings.isNightVisionEnabled());
            NightVisionListener.applyNightVision(player);
            player.sendMessage(Chat.prefixed("Night Vision: " + (settings.isNightVisionEnabled() ? "Enabled" : "Disabled"),
                    settings.isNightVisionEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_ORDER_MESSAGES) {
            settings.setOrderMessagesEnabled(!settings.isOrderMessagesEnabled());
            player.sendMessage(Chat.prefixed("Order messages: " + (settings.isOrderMessagesEnabled() ? "Enabled" : "Disabled"),
                    settings.isOrderMessagesEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_AUCTION_MESSAGES) {
            settings.setAuctionMessagesEnabled(!settings.isAuctionMessagesEnabled());
            player.sendMessage(Chat.prefixed("Auction messages: " + (settings.isAuctionMessagesEnabled() ? "Enabled" : "Disabled"),
                    settings.isAuctionMessagesEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_CONFIRM_AUCTION) {
            settings.setConfirmAuctionListing(!settings.isConfirmAuctionListing());
            player.sendMessage(Chat.prefixed("Confirm auction listing: " + (settings.isConfirmAuctionListing() ? "Enabled" : "Disabled"),
                    settings.isConfirmAuctionListing() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_CONFIRM_TP) {
            settings.setConfirmTpGui(!settings.isConfirmTpGui());
            player.sendMessage(Chat.prefixed("Confirm TP GUI: " + (settings.isConfirmTpGui() ? "Enabled" : "Disabled"),
                    settings.isConfirmTpGui() ? NamedTextColor.GREEN : NamedTextColor.RED));
            SettingsGui.open(player);
        } else if (slot == SettingsGui.SLOT_CLOSE) {
            player.closeInventory();
        }
    }

    static void sendChatRequest(Player target, TeleportRequest request) {
        String typeLabel = request.type() == TeleportRequest.Type.TPA
                ? "teleport to you"
                : "teleport you to them";

        Component message = Component.empty()
                .append(Chat.prefix())
                .append(Component.text(request.senderName() + " wants to " + typeLabel + ". ", NamedTextColor.WHITE))
                .append(Component.text("[Open]", NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/tpaccept " + request.senderName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open", NamedTextColor.GREEN))))
                .append(Component.text(" "))
                .append(Component.text("[Deny]", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/tpadeny"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to deny", NamedTextColor.RED))));

        target.sendMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TeleportManager.cancelCountdown(uuid);
        PlayerSettings.saveDirty();
    }
}

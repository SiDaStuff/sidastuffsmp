package org.atrimilan.sidastuffsmp.teleport;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TeleportGuiHolder implements InventoryHolder {

    public enum GuiType {
        SEND_CONFIRM,
        RECEIVE_CONFIRM,
        SETTINGS
    }

    private final GuiType guiType;
    private UUID viewerUuid;
    private UUID targetUuid;
    private UUID requestSenderUuid;
    private TeleportRequest.Type requestType;

    public TeleportGuiHolder(GuiType guiType) {
        this.guiType = guiType;
    }

    public GuiType getGuiType() { return guiType; }
    public UUID getViewerUuid() { return viewerUuid; }
    public void setViewerUuid(UUID viewerUuid) { this.viewerUuid = viewerUuid; }
    public UUID getTargetUuid() { return targetUuid; }
    public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
    public UUID getRequestSenderUuid() { return requestSenderUuid; }
    public void setRequestSenderUuid(UUID requestSenderUuid) { this.requestSenderUuid = requestSenderUuid; }
    public TeleportRequest.Type getRequestType() { return requestType; }
    public void setRequestType(TeleportRequest.Type requestType) { this.requestType = requestType; }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}

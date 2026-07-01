package org.atrimilan.sidastuffsmp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyGuiHolder implements InventoryHolder {
    public enum GuiType { BOUNTY_BROWSER, BOUNTY_DETAIL, MY_BOUNTIES, ADD_BOUNTY, CONFIRM_ADD, REMOVE_BOUNTY }

    private final GuiType guiType;
    private int page = 0;
    private UUID viewerUuid;
    private UUID targetUuid;
    private double pendingAmount;

    public BountyGuiHolder(GuiType guiType) {
        this.guiType = guiType;
    }

    public GuiType getGuiType() {
        return guiType;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public void setViewerUuid(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(double pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public static class BrowserState {
        private int page = 0;
        private String searchTerm = null;

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }
    }

    private static final Map<UUID, BrowserState> BROWSER_STATES = new HashMap<>();

    public static BrowserState getBrowserState(UUID playerUuid) {
        return BROWSER_STATES.computeIfAbsent(playerUuid, k -> new BrowserState());
    }

    public static void clearBrowserState(UUID playerUuid) {
        BROWSER_STATES.remove(playerUuid);
    }
}

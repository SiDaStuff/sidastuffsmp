package org.atrimilan.sidastuffsmp.gui;

import org.atrimilan.sidastuffsmp.order.OrderSortMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrderGuiHolder implements InventoryHolder {

    public enum GuiType {
        BROWSER,
        MY_ORDERS,
        ORDER_DETAIL,
        DELIVER_ITEMS,
        CONFIRM_DELIVERY,
        NEW_ORDER_ITEM_PICKER,
        CONFIRM_CANCEL,
        CREATE_ORDER,
        ORDER_STASH,
        ENCHANTMENT_SELECT,
        CONFIRM_LISTING
    }

    private final GuiType guiType;
    private int orderId = -1;
    private int page = 0;
    private UUID viewerUuid;

    public OrderGuiHolder(GuiType guiType) {
        this.guiType = guiType;
    }

    public GuiType getGuiType() {
        return guiType;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
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

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public static class BrowserState {
        private int page = 0;
        private OrderSortMode sortMode = OrderSortMode.NEWEST;
        private String searchTerm = null;
        private int newItemPage = 0;
        private String newItemSearch = null;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public OrderSortMode getSortMode() { return sortMode; }
        public void setSortMode(OrderSortMode sortMode) { this.sortMode = sortMode; }
        public String getSearchTerm() { return searchTerm; }
        public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
        public int getNewItemPage() { return newItemPage; }
        public void setNewItemPage(int newItemPage) { this.newItemPage = newItemPage; }
        public String getNewItemSearch() { return newItemSearch; }
        public void setNewItemSearch(String newItemSearch) { this.newItemSearch = newItemSearch; }
    }

    private static final Map<UUID, BrowserState> BROWSER_STATES = new HashMap<>();

    public static BrowserState getBrowserState(UUID playerUuid) {
        return BROWSER_STATES.computeIfAbsent(playerUuid, k -> new BrowserState());
    }

    public static void clearBrowserState(UUID playerUuid) {
        BROWSER_STATES.remove(playerUuid);
    }

    public static class NewOrderState {
        private org.bukkit.Material selectedMaterial;
        private String selectedItemName;
        private int quantity;
        private double pricePerUnit;
        private int step;
        private java.util.List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment> selectedEnchantments = new java.util.ArrayList<>();
        private java.util.Map<String, Integer> enchantmentLevels = new java.util.HashMap<>();
        private String selectedEffect = null;
        private int enchantPage = 0;

        public org.bukkit.Material getSelectedMaterial() { return selectedMaterial; }
        public void setSelectedMaterial(org.bukkit.Material material) { this.selectedMaterial = material; }
        public String getSelectedItemName() { return selectedItemName; }
        public void setSelectedItemName(String itemName) { this.selectedItemName = itemName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPricePerUnit() { return pricePerUnit; }
        public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }
        public int getStep() { return step; }
        public void setStep(int step) { this.step = step; }
        public java.util.List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment> getSelectedEnchantments() { return selectedEnchantments; }
        public void setSelectedEnchantments(java.util.List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment> enchants) { this.selectedEnchantments = enchants != null ? enchants : new java.util.ArrayList<>(); }
        public int getEnchantmentLevel(String enchantName) { return enchantmentLevels.getOrDefault(enchantName, 1); }
        public java.util.Map<String, Integer> getEnchantmentLevels() { return enchantmentLevels; }
        public void setEnchantmentLevel(String enchantName, int level) { enchantmentLevels.put(enchantName, level); }
        public void toggleEnchantment(org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment ench) {
            boolean found = false;
            java.util.List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment> newList = new java.util.ArrayList<>();
            for (var e : selectedEnchantments) {
                if (e.name().equals(ench.name())) {
                    found = true;
                } else {
                    newList.add(e);
                }
            }
            if (!found) {
                newList.add(ench);
                enchantmentLevels.putIfAbsent(ench.name(), 1);
            } else {
                enchantmentLevels.remove(ench.name());
            }
            selectedEnchantments = newList;
        }
        public void incrementEnchantmentLevel(String enchantName, int maxLevel) {
            int current = enchantmentLevels.getOrDefault(enchantName, 1);
            int next = current >= maxLevel ? 1 : current + 1;
            enchantmentLevels.put(enchantName, next);
        }
        public String getSelectedEffect() { return selectedEffect; }
        public void setSelectedEffect(String effect) { this.selectedEffect = effect; }
        public int getEnchantPage() { return enchantPage; }
        public void setEnchantPage(int page) { this.enchantPage = page; }
    }

    private static final Map<UUID, NewOrderState> NEW_ORDER_STATES = new HashMap<>();

    public static NewOrderState getNewOrderState(UUID playerUuid) {
        return NEW_ORDER_STATES.computeIfAbsent(playerUuid, k -> new NewOrderState());
    }

    public static void clearNewOrderState(UUID playerUuid) {
        NEW_ORDER_STATES.remove(playerUuid);
    }

    private static final Map<UUID, Integer> PENDING_QUANTITY = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_PRICE = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_SEARCH = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_ITEM_SEARCH = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_NEW_ORDER_SEARCH = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_DELIVERY_AMOUNT = new HashMap<>();

    public static void setPendingDeliveryAmount(UUID playerUuid, int orderId) {
        PENDING_DELIVERY_AMOUNT.put(playerUuid, orderId);
    }

    public static int consumePendingDeliveryAmount(UUID playerUuid) {
        Integer val = PENDING_DELIVERY_AMOUNT.remove(playerUuid);
        return val != null ? val : -1;
    }

    public static boolean hasPendingDeliveryAmount(UUID playerUuid) {
        return PENDING_DELIVERY_AMOUNT.containsKey(playerUuid);
    }

    public static void setPendingQuantity(UUID playerUuid, int orderId) {
        PENDING_QUANTITY.put(playerUuid, orderId);
    }

    public static int consumePendingQuantity(UUID playerUuid) {
        Integer val = PENDING_QUANTITY.remove(playerUuid);
        return val != null ? val : -1;
    }

    public static boolean hasPendingQuantity(UUID playerUuid) {
        return PENDING_QUANTITY.containsKey(playerUuid);
    }

    public static void setPendingPrice(UUID playerUuid, int orderId) {
        PENDING_PRICE.put(playerUuid, orderId);
    }

    public static int consumePendingPrice(UUID playerUuid) {
        Integer val = PENDING_PRICE.remove(playerUuid);
        return val != null ? val : -1;
    }

    public static boolean hasPendingPrice(UUID playerUuid) {
        return PENDING_PRICE.containsKey(playerUuid);
    }

    public static void setPendingSearch(UUID playerUuid, int page) {
        PENDING_SEARCH.put(playerUuid, page);
    }

    public static int consumePendingSearch(UUID playerUuid) {
        Integer val = PENDING_SEARCH.remove(playerUuid);
        return val != null ? val : 0;
    }

    public static boolean hasPendingSearch(UUID playerUuid) {
        return PENDING_SEARCH.containsKey(playerUuid);
    }

    public static void setPendingItemSearch(UUID playerUuid, int page) {
        PENDING_ITEM_SEARCH.put(playerUuid, page);
    }

    public static int consumePendingItemSearch(UUID playerUuid) {
        Integer val = PENDING_ITEM_SEARCH.remove(playerUuid);
        return val != null ? val : 0;
    }

    public static boolean hasPendingItemSearch(UUID playerUuid) {
        return PENDING_ITEM_SEARCH.containsKey(playerUuid);
    }

    public static void setPendingNewOrderSearch(UUID playerUuid, int page) {
        PENDING_NEW_ORDER_SEARCH.put(playerUuid, page);
    }

    public static int consumePendingNewOrderSearch(UUID playerUuid) {
        Integer val = PENDING_NEW_ORDER_SEARCH.remove(playerUuid);
        return val != null ? val : 0;
    }

    public static boolean hasPendingNewOrderSearch(UUID playerUuid) {
        return PENDING_NEW_ORDER_SEARCH.containsKey(playerUuid);
    }
}

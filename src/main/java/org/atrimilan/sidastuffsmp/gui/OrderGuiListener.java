package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry;
import org.atrimilan.sidastuffsmp.order.OrderConfig;
import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.order.OrderSortMode;
import org.atrimilan.sidastuffsmp.utils.AmountParser;
import org.atrimilan.sidastuffsmp.utils.AnvilInput;
import org.atrimilan.sidastuffsmp.utils.Chat;
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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderGuiListener implements Listener {

    private static final int SLOT_CANCEL = 9;
    private static final int SLOT_ITEM_SELECT = 11;
    private static final int SLOT_AMOUNT = 13;
    private static final int SLOT_PRICE = 15;
    private static final int SLOT_ENCHANT = 16;
    private static final int SLOT_PUBLISH = 17;

    private static final int STASH_SLOT_PREV = 45;
    private static final int STASH_SLOT_COLLECT = 47;
    private static final int STASH_SLOT_DROP_ALL = 48;
    private static final int STASH_SLOT_SELL_ALL = 49;
    private static final int STASH_SLOT_BACK = 50;
    private static final int STASH_SLOT_NEXT = 52;
    private static final int STASH_SLOT_CLOSE = 53;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OrderGuiHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.getViewerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (holder.getGuiType() == OrderGuiHolder.GuiType.DELIVER_ITEMS) {
            handleDeliverInventoryClick(player, holder, event);
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        switch (holder.getGuiType()) {
            case BROWSER -> handleBrowserClick(player, holder, slot);
            case MY_ORDERS -> handleMyOrdersClick(player, holder, slot, event);
            case ORDER_DETAIL -> handleOrderDetailClick(player, holder, slot);
            case CONFIRM_DELIVERY -> handleConfirmDeliveryClick(player, holder, slot);
            case CONFIRM_CANCEL -> handleConfirmCancelClick(player, holder, slot);
            case NEW_ORDER_ITEM_PICKER -> handleNewOrderItemClick(player, holder, slot);
            case CREATE_ORDER -> handleCreateOrderClick(player, holder, slot);
            case ORDER_STASH -> handleOrderStashClick(player, holder, slot);
            case ENCHANTMENT_SELECT -> handleEnchantmentSelectClick(player, holder, slot, event);
            case CONFIRM_LISTING -> handleConfirmListingClick(player, holder, slot);
        }
    }

    private void handleBrowserClick(Player player, OrderGuiHolder holder, int slot) {
        OrderGuiHolder.BrowserState state = OrderGuiHolder.getBrowserState(player.getUniqueId());

        if (slot == OrderGuiUtil.SLOT_PREV_PAGE) {
            if (state.getPage() > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
                OrderBrowserGui.open(player, state.getPage() - 1, state.getSortMode(), state.getSearchTerm());
            }
            return;
        }

        if (slot == OrderGuiUtil.SLOT_NEXT_PAGE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
            OrderBrowserGui.open(player, state.getPage() + 1, state.getSortMode(), state.getSearchTerm());
            return;
        }

        if (slot == OrderGuiUtil.SLOT_SORT) {
            OrderSortMode next = state.getSortMode().next();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            OrderBrowserGui.open(player, state.getPage(), next, state.getSearchTerm());
            return;
        }

        if (slot == OrderGuiUtil.SLOT_REFRESH) {
            OrderBrowserGui.refresh(player);
            return;
        }

        if (slot == OrderGuiUtil.SLOT_SEARCH) {
            OrderGuiHolder.setPendingSearch(player.getUniqueId(), state.getPage());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Search Orders", "Type item name or material:",
                    state.getSearchTerm(),
                    text -> {
                        OrderGuiHolder.consumePendingSearch(player.getUniqueId());
                        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(),
                                () -> OrderBrowserGui.open(player, 0, OrderSortMode.NEWEST, text), 2L);
                    },
                    () -> OrderBrowserGui.open(player));
            return;
        }

        if (slot == OrderGuiUtil.SLOT_MY_ORDERS) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            OrderMyOrdersGui.open(player, 0);
            return;
        }

        if (slot == OrderGuiUtil.SLOT_NEW_ORDER) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            OrderGuiHolder.NewOrderState orderState = OrderGuiHolder.getNewOrderState(player.getUniqueId());
            orderState.setStep(0);
            orderState.setSelectedMaterial(null);
            orderState.setQuantity(0);
            orderState.setPricePerUnit(0);
            CreateOrderGui.open(player);
            return;
        }

        if (slot == OrderGuiUtil.SLOT_CLOSE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < OrderGuiUtil.ITEMS_PER_PAGE) {
            int offset = state.getPage() * OrderGuiUtil.ITEMS_PER_PAGE;
            List<OrderListing> orders = OrderManager.getBrowserOrders(state.getSortMode(), state.getSearchTerm(), offset, OrderGuiUtil.ITEMS_PER_PAGE);
            if (slot < orders.size()) {
                OrderListing order = orders.get(slot);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                OrderDeliverGui.open(player, order.id());
            }
        }
    }

    private void handleMyOrdersClick(Player player, OrderGuiHolder holder, int slot, InventoryClickEvent event) {
        if (slot == 45) {
            if (holder.getPage() > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
                OrderMyOrdersGui.open(player, holder.getPage() - 1);
            }
            return;
        }
        if (slot == 49) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            OrderBrowserGui.refresh(player);
            return;
        }
        if (slot == 52) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
            OrderMyOrdersGui.open(player, holder.getPage() + 1);
            return;
        }
        if (slot == 53) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            UUID uuid = player.getUniqueId();
            List<OrderListing> allOrders = OrderManager.getMyOrders(uuid);

            List<OrderListing> activeOrders = new ArrayList<>();
            List<OrderListing> completedOrders = new ArrayList<>();
            List<OrderListing> expiredOrders = new ArrayList<>();
            List<OrderListing> cancelledOrders = new ArrayList<>();

            for (OrderListing o : allOrders) {
                switch (o.status()) {
                    case "ACTIVE" -> activeOrders.add(o);
                    case "COMPLETED" -> completedOrders.add(o);
                    case "EXPIRED" -> expiredOrders.add(o);
                    case "CANCELLED" -> cancelledOrders.add(o);
                }
            }

            List<Object> entries = new ArrayList<>();
            entries.add(new Object());
            entries.addAll(activeOrders);
            entries.add(new Object());
            entries.addAll(completedOrders);
            entries.add(new Object());
            entries.addAll(expiredOrders);
            entries.add(new Object());
            entries.addAll(cancelledOrders);

            int page = holder.getPage();
            int start = page * 45;
            int entryIndex = start + slot;

            if (entryIndex < entries.size()) {
                Object entry = entries.get(entryIndex);
                if (entry instanceof OrderListing order) {
                    OrderDetailGui.open(player, order.id());
                }
            }
        }
    }

    private void handleOrderDetailClick(Player player, OrderGuiHolder holder, int slot) {
        int orderId = holder.getOrderId();
        if (slot == 11) {
            OrderListing order = OrderManager.getOrderById(orderId);
            if (order != null && order.status().equals("ACTIVE") && order.buyerUuid().equals(player.getUniqueId())) {
                if (OrderManager.hasUncollectedStashForOrder(orderId)) {
                    player.sendMessage(Chat.prefixed("You must collect your stash items before cancelling!", NamedTextColor.RED));
                    return;
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
                OrderConfirmCancelGui.open(player, orderId);
            }
            return;
        }
        if (slot == 15) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
            OrderStashGui.open(player, orderId, 0);
            return;
        }
        if (slot == 19) {
            java.util.UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastRefresh = OrderBrowserGui.getRefreshCooldown(uuid);
            if (lastRefresh != null && (now - lastRefresh) < 2000) {
                player.sendActionBar(net.kyori.adventure.text.Component.text("Please wait before refreshing.", NamedTextColor.RED));
                return;
            }
            OrderBrowserGui.setRefreshCooldown(uuid, now);
            OrderDetailGui.open(player, orderId);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            return;
        }
        if (slot == 22) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            OrderMyOrdersGui.open(player, 0);
            return;
        }
    }

    private void handleCreateOrderClick(Player player, OrderGuiHolder holder, int slot) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());

        if (slot == SLOT_CANCEL) {
            OrderGuiHolder.clearNewOrderState(player.getUniqueId());
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            OrderBrowserGui.refresh(player);
            return;
        }

        if (slot == SLOT_ITEM_SELECT) {
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            OrderGuiHolder.BrowserState browserState = OrderGuiHolder.getBrowserState(player.getUniqueId());
            OrderNewOrderGui.open(player, browserState.getNewItemPage(), browserState.getNewItemSearch());
            return;
        }

        if (slot == SLOT_AMOUNT) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Order Amount", "Type quantity (e.g. 64 or 10k):",
                    state.getQuantity() > 0 ? String.valueOf(state.getQuantity()) : "1",
                    text -> applyAmount(player, text),
                    () -> CreateOrderGui.open(player));
            return;
        }

        if (slot == SLOT_PRICE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Price per Unit", "Type price (e.g. 10 or 5.5k):",
                    state.getPricePerUnit() > 0 ? String.valueOf(state.getPricePerUnit()) : "0",
                    text -> applyPrice(player, text),
                    () -> CreateOrderGui.open(player));
            return;
        }

        if (slot == SLOT_ENCHANT && state.getSelectedMaterial() != null
                && org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.isEnchantable(state.getSelectedMaterial())) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            EnchantmentSelectionGui.open(player, state.getEnchantPage());
            return;
        }

        if (slot == 19 && state.getSelectedMaterial() != null
                && MinecraftDataRegistry.isPotionItem(state.getSelectedMaterial())) {
            org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem mcItem =
                    state.getSelectedItemName() != null
                            ? org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.findItemByName(state.getSelectedItemName())
                            : null;
            if (mcItem == null) {
                for (org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem item :
                        org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.getAllItems()) {
                    if (item.material() == state.getSelectedMaterial() && item.hasPotionEffect()) {
                        mcItem = item;
                        break;
                    }
                }
            }
            if (mcItem != null && mcItem.hasPotionEffect()) {
                String current = mcItem.name();
                String baseKey = mcItem.getBaseEffectKey();
                if (baseKey == null) baseKey = current;
                String containerPrefix = switch (mcItem.material()) {
                    case POTION -> "potion_of_";
                    case SPLASH_POTION -> "splash_potion_of_";
                    case LINGERING_POTION -> "lingering_potion_of_";
                    case TIPPED_ARROW -> "arrow_of_";
                    default -> "potion_of_";
                };
                boolean isStrong = current.contains("strong_");
                boolean isLong = current.contains("long_");
                boolean canStrong = mcItem.hasStrongVariant();
                boolean canLong = mcItem.hasLongVariant();

                String newName = current;
                if (isStrong) {
                    newName = canLong ? containerPrefix + "long_" + baseKey : containerPrefix + baseKey;
                } else if (isLong) {
                    newName = containerPrefix + baseKey;
                } else {
                    if (canStrong) {
                        newName = containerPrefix + "strong_" + baseKey;
                    } else if (canLong) {
                        newName = containerPrefix + "long_" + baseKey;
                    } else {
                        newName = containerPrefix + baseKey;
                    }
                }

                org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem newItem =
                        org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.findItemByName(newName);
                if (newItem != null) {
                    state.setSelectedItemName(newItem.name());
                    state.setSelectedMaterial(newItem.material());
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
                String displayVariant = org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.getPotionEffectDisplay(newItem != null ? newItem.potionEffectKey() : null);
                player.sendActionBar(net.kyori.adventure.text.Component.text(
                        displayVariant, NamedTextColor.AQUA));
                CreateOrderGui.open(player);
            }
            return;
        }

        if (slot == SLOT_PUBLISH) {
            Material material = state.getSelectedMaterial();
            int quantity = state.getQuantity();
            double pricePerUnit = state.getPricePerUnit();

            if (material == null || quantity <= 0 || pricePerUnit <= 0) {
                player.sendMessage(Chat.prefixed("Please select an item, amount, and price first.", NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            if (org.atrimilan.sidastuffsmp.order.OrderConfig.isConfirmListingGuiEnabled()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                OrderConfirmListingGui.open(player);
                return;
            }

            player.closeInventory();
            doPublishOrder(player, state);
        }
    }

    private void doPublishOrder(Player player, OrderGuiHolder.NewOrderState state) {
        Material material = state.getSelectedMaterial();
        int quantity = state.getQuantity();
        double pricePerUnit = state.getPricePerUnit();

        String requiredNbt = null;
        java.util.List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment> selEnch = state.getSelectedEnchantments();
        String selEffect = state.getSelectedEffect();
        if ((selEnch != null && !selEnch.isEmpty()) || (selEffect != null && !selEffect.isEmpty())) {
            requiredNbt = OrderManager.buildRequiredNbt(material, selEnch, state.getEnchantmentLevels() != null ? new java.util.HashMap<>(state.getEnchantmentLevels()) : null, selEffect);
        }

        String itemNameOverride = state.getSelectedItemName();
        OrderManager.CreateResult result = OrderManager.createOrder(player, material, quantity, pricePerUnit, requiredNbt, itemNameOverride);
        if (result.success()) {
            player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.GREEN));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
        OrderGuiHolder.clearNewOrderState(player.getUniqueId());
        OrderBrowserGui.refresh(player);
    }

    private void handleConfirmListingClick(Player player, OrderGuiHolder holder, int slot) {
        if (slot == 11) {
            player.closeInventory();
            OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());
            doPublishOrder(player, state);
        } else if (slot == 15) {
            CreateOrderGui.open(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    private void handleOrderStashClick(Player player, OrderGuiHolder holder, int slot) {
        int orderId = holder.getOrderId();
        int page = holder.getPage();

        if (slot == STASH_SLOT_PREV) {
            if (page > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                OrderStashGui.open(player, orderId, page - 1);
            }
            return;
        }
        if (slot == STASH_SLOT_COLLECT) {
            OrderManager.CollectResult result = OrderManager.collectStashForOrder(player, orderId);
            if (result.success()) {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.GREEN));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            OrderStashGui.open(player, orderId, page);
            return;
        }
        if (slot == STASH_SLOT_DROP_ALL) {
            OrderManager.DropResult result = OrderManager.dropAllStashForOrder(player, orderId);
            if (result.success()) {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.GREEN));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            OrderStashGui.open(player, orderId, page);
            return;
        }
        if (slot == STASH_SLOT_SELL_ALL) {
            sellStash(player, orderId);
            OrderStashGui.open(player, orderId, page);
            return;
        }
        if (slot == STASH_SLOT_BACK) {
            OrderDetailGui.open(player, orderId);
            return;
        }
        if (slot == STASH_SLOT_NEXT) {
            OrderStashGui.open(player, orderId, page + 1);
            return;
        }
        if (slot == STASH_SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
    }

    private void sellStash(Player player, int orderId) {
        if (!OrderConfig.isEconomyShopGuiSellEnabled()) {
            player.sendMessage(Chat.prefixed("Sell to shop is disabled.", NamedTextColor.RED));
            return;
        }

        OrderManager.SellStashResult result = OrderManager.sellStashForOrder(player, orderId);
        if (!result.success()) {
            player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
            return;
        }

        player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.GREEN));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void handleEnchantmentSelectClick(Player player, OrderGuiHolder holder, int slot, InventoryClickEvent event) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());
        int page = holder.getPage();

        if (slot == 45) {
            if (page > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                EnchantmentSelectionGui.open(player, page - 1);
            }
            return;
        }
        if (slot == 49) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            CreateOrderGui.open(player);
            return;
        }
        if (slot == 52) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            EnchantmentSelectionGui.open(player, page + 1);
            return;
        }
        if (slot == 53) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < 36) {
            Material selectedMat = state.getSelectedMaterial();
            List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftEnchantment> enchants;
            if (selectedMat != null) {
                enchants = org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.getApplicableEnchantments(selectedMat);
            } else {
                enchants = org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.getAllEnchantments();
            }
            int start = page * 36;
            int index = start + slot;
            if (index < enchants.size()) {
                var ench = enchants.get(index);
                boolean isSelected = state.getSelectedEnchantments().stream().anyMatch(e -> e.name().equals(ench.name()));
                if (isSelected) {
                    if (event.isRightClick()) {
                        state.toggleEnchantment(ench);
                    } else {
                        state.incrementEnchantmentLevel(ench.name(), ench.maxLevel());
                    }
                } else {
                    if (org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.hasConflictingEnchantment(
                            state.getSelectedEnchantments(), ench)) {
                        player.sendMessage(Chat.prefixed(ench.displayName() + " conflicts with a selected enchantment!", NamedTextColor.RED));
                        return;
                    }
                    state.toggleEnchantment(ench);
                }
                state.setEnchantPage(page);
                EnchantmentSelectionGui.open(player, page);
            }
        }
    }

    private void handleDeliverInventoryClick(Player player, OrderGuiHolder holder, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int topSize = event.getInventory().getSize();

        if (slot >= 0 && slot < OrderGuiUtil.DELIVER_INPUT_SIZE) {
            OrderListing order = OrderManager.getOrderById(holder.getOrderId());
            if (order == null) {
                event.setCancelled(true);
                return;
            }

            Material requiredMaterial = OrderGuiUtil.resolveMaterialFromName(order.materialName());
            if (requiredMaterial == Material.BARRIER) {
                event.setCancelled(true);
                return;
            }

            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                if (event.getCursor().getType() != requiredMaterial) {
                    event.setCancelled(true);
                    player.sendMessage(Chat.prefixed("Only " + OrderManager.formatMaterialName(requiredMaterial) + " is accepted for this order!", NamedTextColor.RED));
                    return;
                }
                if (order.hasNbtRequirement() && !OrderManager.nbtMatchesPublic(order, event.getCursor())) {
                    event.setCancelled(true);
                    player.sendMessage(Chat.prefixed("Item does not match the required enchantments/effects for this order!", NamedTextColor.RED));
                    return;
                }
            }

            if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                if (event.getCurrentItem().getType() != requiredMaterial) {
                    event.setCancelled(true);
                    player.sendMessage(Chat.prefixed("Only " + OrderManager.formatMaterialName(requiredMaterial) + " is accepted for this order!", NamedTextColor.RED));
                    return;
                }
            }
            return;
        }

        if (slot >= topSize) {
            OrderListing order = OrderManager.getOrderById(holder.getOrderId());
            if (order == null) {
                event.setCancelled(true);
                return;
            }
            Material requiredMaterial = OrderGuiUtil.resolveMaterialFromName(order.materialName());
            if (requiredMaterial == Material.BARRIER) {
                event.setCancelled(true);
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) {
                return;
            }

            if (clickedItem.getType() != requiredMaterial) {
                event.setCancelled(true);
                return;
            }

            if (order.hasNbtRequirement() && !OrderManager.nbtMatchesPublic(order, clickedItem)) {
                event.setCancelled(true);
                player.sendMessage(Chat.prefixed("Item does not match the required enchantments/effects for this order!", NamedTextColor.RED));
                return;
            }

            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack moving = clickedItem.clone();
                int remaining = moving.getAmount();
                for (int i = 0; i < OrderGuiUtil.DELIVER_INPUT_SIZE && remaining > 0; i++) {
                    ItemStack slotItem = event.getInventory().getItem(i);
                    if (slotItem == null || slotItem.getType().isAir()) {
                        ItemStack placed = moving.clone();
                        int take = Math.min(remaining, requiredMaterial.getMaxStackSize());
                        placed.setAmount(take);
                        event.getInventory().setItem(i, placed);
                        remaining -= take;
                    } else if (slotItem.isSimilar(moving) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                        int take = Math.min(remaining, slotItem.getMaxStackSize() - slotItem.getAmount());
                        slotItem.setAmount(slotItem.getAmount() + take);
                        remaining -= take;
                    }
                }
                clickedItem.setAmount(remaining);
            }
            return;
        }

        if (slot < 0) {
            return;
        }

        event.setCancelled(true);

        if (slot == OrderGuiUtil.DELIVER_SLOT_CONFIRM) {
            Inventory inv = event.getInventory();
            List<ItemStack> validItems = new ArrayList<>();
            int validCount = 0;

            OrderListing order = OrderManager.getOrderById(holder.getOrderId());
            if (order == null) {
                player.closeInventory();
                player.sendMessage(Chat.prefixed("Order no longer exists.", NamedTextColor.RED));
                return;
            }

            Material requiredMaterial = OrderGuiUtil.resolveMaterialFromName(order.materialName());
            if (requiredMaterial == Material.BARRIER) {
                player.closeInventory();
                return;
            }

            for (int i = 0; i < OrderGuiUtil.DELIVER_INPUT_SIZE; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.getType().isAir() && item.getType() == requiredMaterial) {
                    if (order.hasNbtRequirement() && !OrderManager.nbtMatchesPublic(order, item)) {
                        continue;
                    }
                    validItems.add(item.clone());
                    validCount += item.getAmount();
                }
            }

            if (validCount == 0) {
                String msg = order.hasNbtRequirement()
                        ? "No matching items to deliver. Items must have the required enchantments/effects."
                        : "No valid items to deliver. Place " + OrderManager.formatMaterialName(requiredMaterial) + " in the grid above.";
                player.sendMessage(Chat.prefixed(msg, NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            OrderConfirmDeliveryGui.open(player, holder.getOrderId(), validCount);
            return;
        }

        if (slot == OrderGuiUtil.DELIVER_SLOT_SET_AMOUNT) {
            OrderGuiHolder.setPendingDeliveryAmount(player.getUniqueId(), holder.getOrderId());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Delivery Amount", "Type delivery amount:", "0",
                    text -> applyDeliveryAmount(player, text),
                    () -> {
                        int orderId = OrderGuiHolder.consumePendingDeliveryAmount(player.getUniqueId());
                        if (orderId >= 0) OrderDeliverGui.open(player, orderId);
                    });
            return;
        }

        if (slot == OrderGuiUtil.DELIVER_SLOT_INFO) {
            return;
        }

        if (slot == 52) {
            returnItemsToPlayer(player, event.getInventory());
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            OrderBrowserGui.refresh(player);
            return;
        }

        if (slot == 53) {
            returnItemsToPlayer(player, event.getInventory());
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }
    }

    private void handleConfirmDeliveryClick(Player player, OrderGuiHolder holder, int slot) {
        if (slot == OrderGuiUtil.CONFIRM_SLOT_CONFIRM) {
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(Chat.prefixed("Delivering items...", NamedTextColor.YELLOW));

            Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                OrderListing order = OrderManager.getOrderById(holder.getOrderId());
                if (order == null) {
                    player.sendMessage(Chat.prefixed("Order no longer exists.", NamedTextColor.RED));
                    OrderBrowserGui.refresh(player);
                    return;
                }

                Material requiredMaterial = OrderGuiUtil.resolveMaterialFromName(order.materialName());
                if (requiredMaterial == Material.BARRIER) {
                    player.sendMessage(Chat.prefixed("Order material is invalid.", NamedTextColor.RED));
                    OrderBrowserGui.refresh(player);
                    return;
                }

                List<ItemStack> matchingItems = new ArrayList<>();
                List<ItemStack> nonMatchingItems = new ArrayList<>();
                List<ItemStack> emptyShulkersToReturn = new ArrayList<>();
                List<ItemStack> shulkersToClear = new ArrayList<>();

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        if (org.atrimilan.sidastuffsmp.utils.ShulkerUtil.isShulkerBox(item)) {
                            List<ItemStack> shulkerContents = org.atrimilan.sidastuffsmp.utils.ShulkerUtil.extractItemsFromShulker(item);
                            if (!shulkerContents.isEmpty()) {
                                for (ItemStack content : shulkerContents) {
                                    if (content != null && !content.getType().isAir() && content.getType() == requiredMaterial) {
                                        if (!order.hasNbtRequirement() || OrderManager.nbtMatchesPublic(order, content)) {
                                            matchingItems.add(content.clone());
                                        } else {
                                            nonMatchingItems.add(content.clone());
                                        }
                                    }
                                }
                                ItemStack emptyShulker = org.atrimilan.sidastuffsmp.utils.ShulkerUtil.getEmptyShulker(item);
                                if (emptyShulker != null) {
                                    emptyShulker.setAmount(item.getAmount());
                                    emptyShulkersToReturn.add(emptyShulker);
                                }
                                shulkersToClear.add(item);
                            } else {
                                if (item.getType() == requiredMaterial) {
                                    if (!order.hasNbtRequirement() || OrderManager.nbtMatchesPublic(order, item)) {
                                        matchingItems.add(item.clone());
                                    } else {
                                        nonMatchingItems.add(item.clone());
                                    }
                                } else {
                                    nonMatchingItems.add(item.clone());
                                }
                            }
                        } else if (item.getType() == requiredMaterial) {
                            if (!order.hasNbtRequirement() || OrderManager.nbtMatchesPublic(order, item)) {
                                matchingItems.add(item.clone());
                            } else {
                                nonMatchingItems.add(item.clone());
                            }
                        }
                    }
                }

                if (matchingItems.isEmpty()) {
                    player.sendMessage(Chat.prefixed("You don't have any matching " + OrderManager.formatMaterialName(requiredMaterial) + " to deliver.", NamedTextColor.RED));
                    OrderBrowserGui.refresh(player);
                    return;
                }

                int remaining = order.getRemainingQuantity();
                int totalToDeliver = 0;
                for (ItemStack item : matchingItems) {
                    totalToDeliver += item.getAmount();
                }

                int actualDeliverable = Math.min(totalToDeliver, remaining);
                int excess = totalToDeliver - actualDeliverable;

                List<ItemStack> deliverList = new ArrayList<>();
                int leftToDeliver = actualDeliverable;
                for (ItemStack item : matchingItems) {
                    if (leftToDeliver <= 0) break;
                    int take = Math.min(item.getAmount(), leftToDeliver);
                    ItemStack deliverItem = item.clone();
                    deliverItem.setAmount(take);
                    deliverList.add(deliverItem);
                    leftToDeliver -= take;
                }

                OrderManager.DeliverResult result = OrderManager.deliverItems(player, holder.getOrderId(), deliverList.toArray(new ItemStack[0]));
                if (result.success()) {
                    for (ItemStack shulker : shulkersToClear) {
                        shulker.setAmount(0);
                    }
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && !item.getType().isAir() && item.getType() == requiredMaterial) {
                            if (!order.hasNbtRequirement() || OrderManager.nbtMatchesPublic(order, item)) {
                                item.setAmount(0);
                            }
                        }
                    }

                    if (excess > 0) {
                        int deliveredSoFar = 0;
                        for (ItemStack item : matchingItems) {
                            int itemTotal = item.getAmount();
                            int takeFromThis = Math.min(itemTotal, actualDeliverable - deliveredSoFar);
                            int excessFromThis = itemTotal - takeFromThis;
                            if (excessFromThis > 0) {
                                ItemStack excessItem = item.clone();
                                excessItem.setAmount(excessFromThis);
                                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(excessItem);
                                for (ItemStack drop : leftover.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                                }
                            }
                            deliveredSoFar += takeFromThis;
                            if (deliveredSoFar >= actualDeliverable) break;
                        }
                    }

                    for (ItemStack item : nonMatchingItems) {
                        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }

                    for (ItemStack emptyShulker : emptyShulkersToReturn) {
                        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(emptyShulker);
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }

                    player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.GREEN));
                    if (result.excessCount() > 0) {
                        player.sendMessage(Chat.prefixed(result.excessCount() + " excess items returned to your inventory.", NamedTextColor.YELLOW));
                    }

                    if (result.payment() > 0) {
                        long delayTicks = OrderConfig.deliveryDelayTicks();
                        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                            if (player.isOnline()) {
                                boolean deposited = OrderManager.depositBalance(player, result.payment());
                                if (deposited) {
                                    player.sendMessage(Chat.prefixed("Payment received: " + OrderManager.formatPrice(result.payment()) + "!", NamedTextColor.GREEN));
                                } else {
                                    player.sendMessage(Chat.prefixed("Payment of " + OrderManager.formatPrice(result.payment()) + " failed. Please contact an admin.", NamedTextColor.RED));
                                }
                            }
                        }, delayTicks);
                    }
                } else {
                    player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
                }

                OrderBrowserGui.refresh(player);
            }, 1L);
        } else if (slot == OrderGuiUtil.CONFIRM_SLOT_CANCEL) {
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            OrderDeliverGui.open(player, holder.getOrderId());
        }
    }

    private void handleConfirmCancelClick(Player player, OrderGuiHolder holder, int slot) {
        if (slot == OrderGuiUtil.CONFIRM_SLOT_CONFIRM) {
            OrderManager.CancelResult result = OrderManager.cancelOrder(player, holder.getOrderId());
            player.closeInventory();
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (result.success()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            OrderMyOrdersGui.open(player, 0);
        } else if (slot == OrderGuiUtil.CONFIRM_SLOT_CANCEL) {
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            OrderDetailGui.open(player, holder.getOrderId());
        }
    }

    private void handleNewOrderItemClick(Player player, OrderGuiHolder holder, int slot) {
        OrderGuiHolder.BrowserState state = OrderGuiHolder.getBrowserState(player.getUniqueId());

        if (slot == 45) {
            if (holder.getPage() > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                OrderNewOrderGui.open(player, holder.getPage() - 1, state.getNewItemSearch());
            }
            return;
        }
        if (slot == 47) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            OrderGuiHolder.BrowserState bState = OrderGuiHolder.getBrowserState(player.getUniqueId());
            AnvilInput.request(player, "Search Item", "Type item name:",
                    bState.getNewItemSearch(),
                    text -> {
                        OrderGuiHolder.BrowserState state2 = OrderGuiHolder.getBrowserState(player.getUniqueId());
                        state2.setNewItemSearch(text);
                        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(),
                                () -> OrderNewOrderGui.open(player, 0, text), 2L);
                    },
                    () -> OrderNewOrderGui.open(player, 0, null));
            return;
        }
        if (slot == 49) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            CreateOrderGui.open(player);
            return;
        }
        if (slot == 52) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            OrderNewOrderGui.open(player, holder.getPage() + 1, state.getNewItemSearch());
            return;
        }
        if (slot == 53) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
            if (clicked == null || clicked.getType().isAir()) return;

            Material selected = clicked.getType();
            OrderGuiHolder.NewOrderState orderState = OrderGuiHolder.getNewOrderState(player.getUniqueId());
            orderState.setSelectedMaterial(selected);
            orderState.setSelectedItemName(null);
            orderState.setQuantity(0);
            orderState.setPricePerUnit(0);
            orderState.setStep(1);

            OrderGuiHolder.BrowserState browserState = OrderGuiHolder.getBrowserState(player.getUniqueId());
            List<org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem> allItems =
                    org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.searchItems(browserState.getNewItemSearch());
            int page = holder.getPage();
            int index = page * 45 + slot;
            if (index < allItems.size()) {
                org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry.MinecraftItem mcItem = allItems.get(index);
                orderState.setSelectedItemName(mcItem.name());
                if (mcItem.hasPotionEffect() && mcItem.potionEffectKey() != null) {
                    orderState.setSelectedEffect(mcItem.potionEffectKey());
                } else {
                    orderState.setSelectedEffect(null);
                }
                player.sendMessage(Chat.prefixed("Selected " + mcItem.displayName() + " for your order.", NamedTextColor.GREEN));
            } else {
                orderState.setSelectedEffect(null);
                player.sendMessage(Chat.prefixed("Selected " + OrderManager.formatMaterialName(selected) + " for your order.", NamedTextColor.GREEN));
            }

            CreateOrderGui.open(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof OrderGuiHolder holder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (holder.getGuiType() == OrderGuiHolder.GuiType.DELIVER_ITEMS) {
            returnItemsToPlayer(player, event.getInventory());
        }
    }

    private void returnItemsToPlayer(Player player, Inventory inv) {
        for (int i = 0; i < OrderGuiUtil.DELIVER_INPUT_SIZE; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                inv.setItem(i, null);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        AnvilInput.cancel(event.getPlayer());
    }

    private void applyAmount(Player player, String raw) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());
        try {
            int quantity = (int) AmountParser.parse(raw);
            if (quantity <= 0) {
                player.sendMessage(Chat.prefixed("Quantity must be positive.", NamedTextColor.RED));
            } else if (quantity > OrderConfig.maxQuantity()) {
                player.sendMessage(Chat.prefixed("Maximum quantity is " + OrderConfig.maxQuantity() + ".", NamedTextColor.RED));
            } else {
                state.setQuantity(quantity);
                state.setStep(2);
                player.sendMessage(Chat.prefixed("Amount set to " + quantity + ".", NamedTextColor.GREEN));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Chat.prefixed("Invalid amount: " + e.getMessage(), NamedTextColor.RED));
        }
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(),
                () -> CreateOrderGui.open(player), 2L);
    }

    private void applyPrice(Player player, String raw) {
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());
        try {
            double pricePerUnit = AmountParser.parse(raw);
            if (pricePerUnit <= 0) {
                player.sendMessage(Chat.prefixed("Price must be positive.", NamedTextColor.RED));
            } else if (pricePerUnit < OrderConfig.minPricePerUnit() || pricePerUnit > OrderConfig.maxPricePerUnit()) {
                player.sendMessage(Chat.prefixed("Price must be between "
                        + OrderManager.formatPrice(OrderConfig.minPricePerUnit())
                        + " and " + OrderManager.formatPrice(OrderConfig.maxPricePerUnit()) + ".",
                        NamedTextColor.RED));
            } else {
                state.setPricePerUnit(pricePerUnit);
                state.setStep(3);
                player.sendMessage(Chat.prefixed("Price set to "
                        + OrderManager.formatPrice(pricePerUnit) + " each.", NamedTextColor.GREEN));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Chat.prefixed("Invalid price: " + e.getMessage(), NamedTextColor.RED));
        }
        Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(),
                () -> CreateOrderGui.open(player), 2L);
    }

    private void applyDeliveryAmount(Player player, String raw) {
        int orderId = OrderGuiHolder.consumePendingDeliveryAmount(player.getUniqueId());
        if (orderId < 0) return;
        try {
            int deliverAmount = (int) AmountParser.parse(raw);
            if (deliverAmount <= 0) {
                player.sendMessage(Chat.prefixed("Amount must be positive.", NamedTextColor.RED));
                OrderGuiHolder.setPendingDeliveryAmount(player.getUniqueId(), orderId);
                return;
            }
            OrderListing order = OrderManager.getOrderById(orderId);
            if (order != null && order.status().equals("ACTIVE")) {
                int remaining = order.getRemainingQuantity();
                int actual = Math.min(deliverAmount, remaining);
                OrderConfirmDeliveryGui.open(player, orderId, actual);
            } else {
                player.sendMessage(Chat.prefixed("Order no longer exists or is no longer active.", NamedTextColor.RED));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Chat.prefixed("Invalid amount: " + e.getMessage(), NamedTextColor.RED));
            OrderGuiHolder.setPendingDeliveryAmount(player.getUniqueId(), orderId);
        }
    }
}
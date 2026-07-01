package org.atrimilan.sidastuffsmp.order;

import net.milkbowl.vault.economy.Economy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.sell.SellCommand;
import org.atrimilan.sidastuffsmp.teleport.PlayerSettings;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderManager {

    private static Connection connection;
    private static Economy economy;
    private static File dbFile;

    private static final ConcurrentHashMap<Integer, Object> deliveryLocks = new ConcurrentHashMap<>();

    private OrderManager() {}

    public static void init(SiDaStuffSmp plugin) {
        setupEconomy();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dbFile = new File(plugin.getDataFolder(), "orders.db");
        try {
            connect();
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize orders database: " + e.getMessage());
        }
    }

    public static void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
        connection = null;
    }

    private static void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    private static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=-2000");
            stmt.execute("PRAGMA temp_store=MEMORY");
        }
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS buy_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    buyer_uuid TEXT NOT NULL,
                    buyer_name TEXT NOT NULL,
                    material_name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    filled_quantity INTEGER NOT NULL DEFAULT 0,
                    price_per_unit REAL NOT NULL,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    completed_at INTEGER,
                    required_nbt TEXT
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_stash (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER NOT NULL,
                    buyer_uuid TEXT NOT NULL,
                    material_name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    collected INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (order_id) REFERENCES buy_orders(id)
                )
                """);
        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_status ON buy_orders(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_buyer_uuid ON buy_orders(buyer_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_material ON buy_orders(material_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stash_buyer_uuid ON order_stash(buyer_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stash_order_id ON order_stash(order_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stash_collected ON order_stash(collected)");
        } catch (SQLException ignored) {
        }

        try {
            stmt.execute("ALTER TABLE buy_orders ADD COLUMN required_nbt TEXT");
        } catch (SQLException ignored) {
        }
        }
    }

    public static boolean hasEconomy() {
        return economy != null;
    }

    public static boolean hasBalance(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return false;
        return economy.has(player, amount);
    }

    public static boolean withdrawBalance(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static boolean depositBalance(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public static double getBalance(OfflinePlayer player) {
        if (!hasEconomy()) return 0;
        return economy.getBalance(player);
    }

    public static String formatPrice(double amount) {
        return AuctionManager.formatPrice(amount);
    }

    public static CreateResult createOrder(Player buyer, Material material, int quantity, double pricePerUnit, String requiredNbt) {
        return createOrder(buyer, material, quantity, pricePerUnit, requiredNbt, null);
    }

    public static CreateResult createOrder(Player buyer, Material material, int quantity, double pricePerUnit, String requiredNbt, String itemNameOverride) {
        if (!hasEconomy()) {
            return new CreateResult(false, "Economy system is not available.", -1);
        }

        if (material == null || material == Material.AIR) {
            return new CreateResult(false, "Invalid material.", -1);
        }

        if (quantity <= 0) {
            return new CreateResult(false, "Quantity must be positive.", -1);
        }

        if (quantity > OrderConfig.maxQuantity()) {
            return new CreateResult(false, "Maximum quantity is " + OrderConfig.maxQuantity() + ".", -1);
        }

        if (pricePerUnit < OrderConfig.minPricePerUnit() || pricePerUnit > OrderConfig.maxPricePerUnit()) {
            return new CreateResult(false, "Price per unit must be between " + formatPrice(OrderConfig.minPricePerUnit()) + " and " + formatPrice(OrderConfig.maxPricePerUnit()) + ".", -1);
        }

        int activeCount = getActiveOrderCount(buyer.getUniqueId());
        if (activeCount >= OrderConfig.maxActiveOrdersPerPlayer()) {
            return new CreateResult(false, "You have reached the maximum of " + OrderConfig.maxActiveOrdersPerPlayer() + " active orders.", -1);
        }

        double totalCost = quantity * pricePerUnit;
        if (!hasBalance(buyer, totalCost)) {
            return new CreateResult(false, "You need " + formatPrice(totalCost) + " for this order. Your balance: " + formatPrice(getBalance(buyer)) + ".", -1);
        }

        if (!withdrawBalance(buyer, totalCost)) {
            return new CreateResult(false, "Failed to escrow funds. Try again.", -1);
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + (OrderConfig.orderDurationDays() * 24L * 60L * 60L * 1000L);

        String sql = "INSERT INTO buy_orders (buyer_uuid, buyer_name, material_name, quantity, filled_quantity, price_per_unit, status, created_at, expires_at, required_nbt) VALUES (?, ?, ?, ?, 0, ?, 'ACTIVE', ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, buyer.getUniqueId().toString());
            ps.setString(2, buyer.getName());
            ps.setString(3, itemNameOverride != null ? itemNameOverride : material.name());
            ps.setInt(4, quantity);
            ps.setDouble(5, pricePerUnit);
            ps.setLong(6, now);
            ps.setLong(7, expiresAt);
            if (requiredNbt != null && !requiredNbt.isBlank()) {
                ps.setString(8, requiredNbt);
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new CreateResult(true, "Order created for " + quantity + "x " + formatMaterialName(material) + " at " + formatPrice(pricePerUnit) + " each. Total escrowed: " + formatPrice(totalCost) + ".", id);
                }
            }
        } catch (SQLException e) {
            depositBalance(buyer, totalCost);
            return new CreateResult(false, "Database error: " + e.getMessage(), -1);
        }

        depositBalance(buyer, totalCost);
        return new CreateResult(false, "Failed to create order.", -1);
    }

    public static DeliverResult deliverItems(Player seller, int orderId, ItemStack[] items) {
        if (!hasEconomy()) {
            return new DeliverResult(false, "Economy system is not available.", 0, 0);
        }

        Object lock = deliveryLocks.computeIfAbsent(orderId, k -> new Object());
        synchronized (lock) {
            try {
                OrderListing order = getOrderById(orderId);
                if (order == null || !order.status().equals("ACTIVE")) {
                    deliveryLocks.remove(orderId);
                    return new DeliverResult(false, "This order is no longer active.", 0, 0);
                }

                if (order.buyerUuid().equals(seller.getUniqueId())) {
                    deliveryLocks.remove(orderId);
                    return new DeliverResult(false, "You cannot fulfill your own order.", 0, 0);
                }

        Material requiredMaterial = org.atrimilan.sidastuffsmp.gui.OrderGuiUtil.resolveMaterialFromName(order.materialName());
        if (requiredMaterial == Material.BARRIER) {
                    deliveryLocks.remove(orderId);
                    return new DeliverResult(false, "Order material is invalid.", 0, 0);
                }

                int totalValidItems = 0;
                for (ItemStack item : items) {
                    if (item != null && !item.getType().isAir() && item.getType() == requiredMaterial) {
                        if (order.hasNbtRequirement() && !nbtMatches(order, item)) {
                            continue;
                        }
                        totalValidItems += item.getAmount();
                    }
                }

                if (totalValidItems == 0) {
                    deliveryLocks.remove(orderId);
                    return new DeliverResult(false, "No valid items found for this order.", 0, 0);
                }

                int remaining = order.getRemainingQuantity();
                int deliverableAmount = Math.min(totalValidItems, remaining);
                int excessAmount = totalValidItems - deliverableAmount;

                double payment = deliverableAmount * order.pricePerUnit();

                int newFilled = order.filledQuantity() + deliverableAmount;
                String newStatus = newFilled >= order.quantity() ? "COMPLETED" : "ACTIVE";
                Long completedAt = newFilled >= order.quantity() ? System.currentTimeMillis() : null;

                String updateSql = "UPDATE buy_orders SET filled_quantity = ?, status = ?, completed_at = ? WHERE id = ? AND status = 'ACTIVE'";
                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setInt(1, newFilled);
                    ps.setString(2, newStatus);
                    if (completedAt != null) {
                        ps.setLong(3, completedAt);
                    } else {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    }
                    ps.setInt(4, orderId);
                    int affected = ps.executeUpdate();
                    if (affected != 1) {
                        boolean stillActive = false;
                        OrderListing recheck = getOrderById(orderId);
                        if (recheck != null && recheck.status().equals("ACTIVE")) {
                            stillActive = true;
                        }
                        deliveryLocks.remove(orderId);
                        if (stillActive) {
                            return new DeliverResult(false, "Concurrent delivery detected. Please try again.", 0, 0);
                        }
                        return new DeliverResult(false, "This order is no longer active.", 0, 0);
                    }
                }

                String stashSql = "INSERT INTO order_stash (order_id, buyer_uuid, material_name, quantity, created_at, collected) VALUES (?, ?, ?, ?, ?, 0)";
                try (PreparedStatement ps = connection.prepareStatement(stashSql)) {
                    ps.setInt(1, orderId);
                    ps.setString(2, order.buyerUuid().toString());
                    ps.setString(3, order.materialName());
                    ps.setInt(4, deliverableAmount);
                    ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            }

            deliveryLocks.remove(orderId);

            Player buyerPlayer = Bukkit.getPlayer(order.buyerUuid());
            if (buyerPlayer != null && buyerPlayer.isOnline()) {
                PlayerSettings buyerSettings = PlayerSettings.get(order.buyerUuid());
                if (buyerSettings.isOrderMessagesEnabled()) {
                    String matName = formatMaterialName(requiredMaterial);
                    if ("COMPLETED".equals(newStatus)) {
                        buyerPlayer.sendMessage(Chat.prefixed("Your order for " + matName + " has been fully fulfilled!", NamedTextColor.GREEN));
                    } else {
                        buyerPlayer.sendMessage(Chat.prefixed("Your order for " + matName + " received " + deliverableAmount + " item(s). " + order.getRemainingQuantity() + " remaining.", NamedTextColor.GREEN));
                    }
                    buyerPlayer.sendMessage(Chat.prefixed("Items ready to collect! Use /orders my and click Order Stash.", NamedTextColor.YELLOW));
                }
            }

            return new DeliverResult(true,
                        "Delivered " + deliverableAmount + "x " + formatMaterialName(requiredMaterial) + ". Payment pending...",
                        deliverableAmount, excessAmount, payment);

            } catch (SQLException e) {
                deliveryLocks.remove(orderId);
                return new DeliverResult(false, "Database error: " + e.getMessage(), 0, 0);
            }
        }
    }

    public static boolean hasUncollectedStashForOrder(int orderId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM order_stash WHERE order_id = ? AND collected = 0")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public static CancelResult cancelOrder(Player buyer, int orderId) {
        try {
            OrderListing order = getOrderById(orderId);
            if (order == null) {
                return new CancelResult(false, "Order not found.", 0);
            }

            if (!order.buyerUuid().equals(buyer.getUniqueId())) {
                return new CancelResult(false, "This is not your order.", 0);
            }

            if (!order.status().equals("ACTIVE")) {
                return new CancelResult(false, "Only active orders can be cancelled.", 0);
            }

            if (hasUncollectedStashForOrder(orderId)) {
                return new CancelResult(false, "You must collect your delivered items from the order stash before cancelling. Use Order Stash to collect items first.", 0);
            }

            double refund = order.getRemainingEscrow();

            String updateSql = "UPDATE buy_orders SET status = 'CANCELLED', completed_at = ? WHERE id = ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setInt(2, orderId);
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    return new CancelResult(false, "Order could not be cancelled.", 0);
                }
            }

            if (refund > 0 && hasEconomy()) {
                depositBalance(buyer, refund);
            }

            return new CancelResult(true, "Order cancelled. Refunded " + formatPrice(refund) + ".", refund);

        } catch (SQLException e) {
            return new CancelResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static CancelResult adminCancelOrder(int orderId) {
        try {
            OrderListing order = getOrderById(orderId);
            if (order == null) {
                return new CancelResult(false, "Order not found with ID " + orderId + ".", 0);
            }

            if (!order.status().equals("ACTIVE")) {
                return new CancelResult(false, "Order is not active (status: " + order.status() + ").", 0);
            }

            double refund = order.getRemainingEscrow();

            String updateSql = "UPDATE buy_orders SET status = 'CANCELLED', completed_at = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }

            Player buyer = Bukkit.getPlayer(order.buyerUuid());
            if (refund > 0 && hasEconomy()) {
                if (buyer != null && buyer.isOnline()) {
                    depositBalance(buyer, refund);
                } else {
                    OfflinePlayer offlineBuyer = Bukkit.getOfflinePlayer(order.buyerUuid());
                    depositBalance(offlineBuyer, refund);
                }
            }

            return new CancelResult(true, "Force-cancelled order " + orderId + ". Refunded " + formatPrice(refund) + ".", refund);

        } catch (SQLException e) {
            return new CancelResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static boolean adminDeleteOrder(int orderId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM buy_orders WHERE id = ?")) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static int adminExpireAll() {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE buy_orders SET status = 'EXPIRED' WHERE status = 'ACTIVE'")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            return -1;
        }
    }

    public static CollectResult collectStashForOrder(Player buyer, int orderId) {
        try {
            String selectSql = "SELECT * FROM order_stash WHERE order_id = ? AND buyer_uuid = ? AND collected = 0 ORDER BY created_at ASC";
            List<StashEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, buyer.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(mapStashRow(rs));
                    }
                }
            }

            if (entries.isEmpty()) {
                return new CollectResult(false, "No items in stash for this order.", 0);
            }

            OrderListing order = getOrderById(orderId);
            if (order == null) {
                return new CollectResult(false, "Order not found.", 0);
            }

        Material material = org.atrimilan.sidastuffsmp.gui.OrderGuiUtil.resolveMaterialFromName(order.materialName());
        if (material == Material.BARRIER) {
            return new CollectResult(false, "Invalid material in stash.", 0);
        }

        ItemStack template = null;
        if (order.hasNbtRequirement()) {
            template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
        }

        int totalQuantity = 0;
        for (StashEntry entry : entries) {
            totalQuantity += entry.quantity();
        }

        int maxStackSize = material.getMaxStackSize();
        List<ItemStack> toGive = new ArrayList<>();
        int remaining = totalQuantity;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            if (template != null) {
                ItemStack stack = template.clone();
                stack.setAmount(stackSize);
                toGive.add(stack);
            } else {
                toGive.add(new ItemStack(material, stackSize));
            }
            remaining -= stackSize;
        }

            java.util.Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(toGive.toArray(new ItemStack[0]));
            int actuallyGiven = totalQuantity;
            for (ItemStack drop : leftover.values()) {
                actuallyGiven -= drop.getAmount();
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
            }

        int stillInStash = totalQuantity - actuallyGiven;
        for (StashEntry entry : entries) {
            if (stillInStash <= 0) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE order_stash SET collected = 1 WHERE id = ?")) {
                    ps.setInt(1, entry.id());
                    ps.executeUpdate();
                }
            } else if (stillInStash >= entry.quantity()) {
                stillInStash -= entry.quantity();
            } else {
                int partialCollect = entry.quantity() - stillInStash;
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE order_stash SET quantity = ? WHERE id = ?")) {
                    ps.setInt(1, stillInStash);
                    ps.setInt(2, entry.id());
                    ps.executeUpdate();
                }
                stillInStash = 0;
            }
        }

        return new CollectResult(true, "Collected " + actuallyGiven + "x " + formatMaterialName(material) + "!", actuallyGiven);

        } catch (SQLException e) {
            return new CollectResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static DropResult dropAllStashForOrder(Player buyer, int orderId) {
        try {
            String selectSql = "SELECT * FROM order_stash WHERE order_id = ? AND buyer_uuid = ? AND collected = 0 ORDER BY created_at ASC";
            List<StashEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, buyer.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(mapStashRow(rs));
                    }
                }
            }

            if (entries.isEmpty()) {
                return new DropResult(false, "No items in stash for this order.", 0);
            }

            OrderListing order = getOrderById(orderId);
            if (order == null) {
                return new DropResult(false, "Order not found.", 0);
            }

        Material material = org.atrimilan.sidastuffsmp.gui.OrderGuiUtil.resolveMaterialFromName(order.materialName());
        if (material == Material.BARRIER) {
            return new DropResult(false, "Invalid material in stash.", 0);
        }

        ItemStack template = null;
        if (order.hasNbtRequirement()) {
            template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
        }

        int totalQuantity = 0;
        for (StashEntry entry : entries) {
            totalQuantity += entry.quantity();
        }

        int maxStackSize = material.getMaxStackSize();
        int remaining = totalQuantity;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            if (template != null) {
                ItemStack stack = template.clone();
                stack.setAmount(stackSize);
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), stack);
            } else {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), new ItemStack(material, stackSize));
            }
            remaining -= stackSize;
        }

        for (StashEntry entry : entries) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE order_stash SET collected = 1 WHERE id = ?")) {
                ps.setInt(1, entry.id());
                ps.executeUpdate();
            }
        }

        return new DropResult(true, "Dropped " + totalQuantity + "x " + formatMaterialName(material) + " at your feet.", totalQuantity);

        } catch (SQLException e) {
            return new DropResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static SellStashResult sellStashForOrder(Player buyer, int orderId) {
        try {
            String selectSql = "SELECT * FROM order_stash WHERE order_id = ? AND buyer_uuid = ? AND collected = 0 ORDER BY created_at ASC";
            List<StashEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, buyer.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(mapStashRow(rs));
                    }
                }
            }

            if (entries.isEmpty()) {
                return new SellStashResult(false, "No items in stash for this order.", 0, 0);
            }

            OrderListing order = getOrderById(orderId);
            if (order == null) {
                return new SellStashResult(false, "Order not found.", 0, 0);
            }

            Material material = org.atrimilan.sidastuffsmp.gui.OrderGuiUtil.resolveMaterialFromName(order.materialName());
            if (material == Material.BARRIER) {
                return new SellStashResult(false, "Invalid material in stash.", 0, 0);
            }

            int totalQuantity = 0;
            for (StashEntry entry : entries) {
                totalQuantity += entry.quantity();
            }

            java.util.Map<Material, Integer> items = new java.util.HashMap<>();
            items.put(material, totalQuantity);
            java.util.Map<Material, Double> pricePerUnit = new java.util.HashMap<>();
            double totalValue = SellCommand.calculateSellValue(buyer, items, pricePerUnit, false);
            if (totalValue <= 0) {
                return new SellStashResult(false, "No sell price is configured for " + formatMaterialName(material) + ".", 0, totalQuantity);
            }

            if (!depositBalance(buyer, totalValue)) {
                return new SellStashResult(false, "Payment failed. Please contact an admin.", 0, totalQuantity);
            }

            for (StashEntry entry : entries) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE order_stash SET collected = 1 WHERE id = ?")) {
                    ps.setInt(1, entry.id());
                    ps.executeUpdate();
                }
            }

            return new SellStashResult(true, "Sold " + totalQuantity + "x " + formatMaterialName(material) + " for " + formatPrice(totalValue) + ".", totalValue, totalQuantity);
        } catch (SQLException e) {
            return new SellStashResult(false, "Database error: " + e.getMessage(), 0, 0);
        }
    }

    public static List<StashEntry> getOrderStash(int orderId) {
        String sql = "SELECT * FROM order_stash WHERE order_id = ? ORDER BY created_at ASC";
        List<StashEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapStashRow(rs));
                }
            }
        } catch (SQLException ignored) {
        }
        return entries;
    }

    public static CollectResult collectStash(Player buyer) {
        try {
            String selectSql = "SELECT * FROM order_stash WHERE buyer_uuid = ? AND collected = 0 ORDER BY created_at ASC";
            List<StashEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setString(1, buyer.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(mapStashRow(rs));
                    }
                }
            }

            if (entries.isEmpty()) {
                return new CollectResult(false, "No items in your stash.", 0);
            }

        int totalCollected = 0;
        for (StashEntry entry : entries) {
            Material material = org.atrimilan.sidastuffsmp.gui.OrderGuiUtil.resolveMaterialFromName(entry.materialName());
            if (material == null) continue;

            OrderListing order = getOrderById(entry.orderId());
            ItemStack template = null;
            if (order != null && order.hasNbtRequirement()) {
                template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
            }

            int qty = entry.quantity();
            int maxStackSize = material.getMaxStackSize();
            List<ItemStack> toGive = new ArrayList<>();
            int remaining = qty;
            while (remaining > 0) {
                int stackSize = Math.min(remaining, maxStackSize);
                if (template != null) {
                    ItemStack stack = template.clone();
                    stack.setAmount(stackSize);
                    toGive.add(stack);
                } else {
                    toGive.add(new ItemStack(material, stackSize));
                }
                remaining -= stackSize;
            }

                java.util.Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(toGive.toArray(new ItemStack[0]));
                int actuallyGiven = qty;
                for (ItemStack drop : leftover.values()) {
                    actuallyGiven -= drop.getAmount();
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
                }
                totalCollected += actuallyGiven;

                if (actuallyGiven >= qty) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE order_stash SET collected = 1 WHERE id = ?")) {
                        ps.setInt(1, entry.id());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE order_stash SET quantity = ? WHERE id = ?")) {
                        ps.setInt(1, qty - actuallyGiven);
                        ps.setInt(2, entry.id());
                        ps.executeUpdate();
                    }
                }
            }

            return new CollectResult(true, "Collected " + totalCollected + " items from stash!", totalCollected);

        } catch (SQLException e) {
            return new CollectResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static List<OrderListing> getBrowserOrders(OrderSortMode sort, String searchTerm, int offset, int limit) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            return getBrowserOrdersWithSearch(sort, searchTerm, offset, limit);
        }

        String sql = "SELECT * FROM buy_orders WHERE status = 'ACTIVE' ORDER BY " + sort.sqlOrderBy() + " LIMIT ? OFFSET ?";

        List<OrderListing> orders = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Orders browser query failed: " + e.getMessage());
        }
        return orders;
    }

    private static List<OrderListing> getBrowserOrdersWithSearch(OrderSortMode sort, String searchTerm, int offset, int limit) {
        String term = searchTerm.trim().toLowerCase(Locale.ROOT);

        String sql = "SELECT * FROM buy_orders WHERE status = 'ACTIVE' ORDER BY " + sort.sqlOrderBy();

        List<OrderListing> allMatching = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderListing order = mapRow(rs);
                    if (matchesSearch(order, term)) {
                        allMatching.add(order);
                    }
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Orders browser query failed: " + e.getMessage());
        }

        if (offset >= allMatching.size()) return new ArrayList<>();
        int end = Math.min(offset + limit, allMatching.size());
        return allMatching.subList(offset, end);
    }

    private static boolean matchesSearch(OrderListing order, String lowerTerm) {
        if (order.materialName().toLowerCase(Locale.ROOT).contains(lowerTerm)) return true;
        String display = formatMaterialName(order.materialName()).toLowerCase(Locale.ROOT);
        if (display.contains(lowerTerm)) return true;
        if (order.buyerName().toLowerCase(Locale.ROOT).contains(lowerTerm)) return true;
        String normalized = order.materialName().replace('_', ' ').toLowerCase(Locale.ROOT);
        if (normalized.contains(lowerTerm)) return true;
        String searchNormalized = lowerTerm.replace(' ', '_');
        if (order.materialName().toLowerCase(Locale.ROOT).contains(searchNormalized)) return true;
        return false;
    }

    public static int getBrowserOrderCount(String searchTerm) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            return getBrowserOrderCountWithSearch(searchTerm);
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM buy_orders WHERE status = 'ACTIVE'")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    private static int getBrowserOrderCountWithSearch(String searchTerm) {
        String term = searchTerm.trim().toLowerCase(Locale.ROOT);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM buy_orders WHERE status = 'ACTIVE'")) {
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    OrderListing order = mapRow(rs);
                    if (matchesSearch(order, term)) {
                        count++;
                    }
                }
                return count;
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static List<OrderListing> getBrowserOrders(int offset, int limit) {
        return getBrowserOrders(OrderSortMode.NEWEST, null, offset, limit);
    }

    public static int getBrowserOrderCount() {
        return getBrowserOrderCount(null);
    }

    /**
     * Find the best (highest price-per-unit) active order for a given material name, ignoring any orders
     * owned by the provided seller UUID. The returned order (if any) still has remaining capacity.
     */
    public static OrderListing getBestActiveOrderForMaterial(String materialName, UUID sellerUuid) {
        if (materialName == null || materialName.isBlank()) return null;
        String sql = "SELECT * FROM buy_orders WHERE status = 'ACTIVE' AND material_name = ? ORDER BY price_per_unit DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, materialName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OrderListing order = mapRow(rs);
                    if (order.getRemainingQuantity() <= 0) return null;
                    if (sellerUuid != null && order.buyerUuid().equals(sellerUuid)) return null;
                    return order;
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    /**
     * Calculate the maximum price-per-unit a seller could earn for a given material by
     * looking at all active buy orders (excluding their own).
     */
    public static double getBestOrderPriceForMaterial(String materialName, UUID sellerUuid) {
        OrderListing best = getBestActiveOrderForMaterial(materialName, sellerUuid);
        return best != null ? best.pricePerUnit() : 0.0;
    }

    /**
     * Attempt to deliver a stack of items to the best available active order for that material
     * if that order pays more per unit than the supplied shop price. Returns null if no qualifying
     * order was found; otherwise returns a result describing the delivery.
     */
    public static DeliverResult tryDeliverToBestOrder(Player seller, Material material, int amount, double shopPrice) {
        if (material == null || amount <= 0) return null;
        if (!OrderConfig.isSellPriorityEnabled()) return null;

        OrderListing best = getBestActiveOrderForMaterial(material.name(), seller.getUniqueId());
        if (best == null) return null;
        if (best.pricePerUnit() <= shopPrice) return null;

        ItemStack stack = new ItemStack(material, amount);
        return deliverItems(seller, best.id(), new ItemStack[]{stack});
    }

    public static List<OrderListing> getMyOrders(UUID buyerUuid) {
        String sql = "SELECT * FROM buy_orders WHERE buyer_uuid = ? AND status IN ('ACTIVE', 'COMPLETED', 'EXPIRED', 'CANCELLED') ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'COMPLETED' THEN 1 WHEN 'EXPIRED' THEN 2 WHEN 'CANCELLED' THEN 3 END, created_at DESC";
        List<OrderListing> orders = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, buyerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        } catch (SQLException ignored) {
        }
        return orders;
    }

    public static int getActiveOrderCount(UUID buyerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM buy_orders WHERE buyer_uuid = ? AND status = 'ACTIVE'")) {
            ps.setString(1, buyerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static int getUncollectedStashCount(UUID buyerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM order_stash WHERE buyer_uuid = ? AND collected = 0")) {
            ps.setString(1, buyerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static int getUncollectedStashItemCount(UUID buyerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COALESCE(SUM(quantity), 0) FROM order_stash WHERE buyer_uuid = ? AND collected = 0")) {
            ps.setString(1, buyerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static OrderListing getOrderById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM buy_orders WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public static List<OrderListing> getAdminOrders(String playerName, int limit) {
        String sql;
        if (playerName != null && !playerName.isBlank()) {
            sql = "SELECT * FROM buy_orders WHERE buyer_name LIKE ? ORDER BY created_at DESC LIMIT ?";
        } else {
            sql = "SELECT * FROM buy_orders ORDER BY created_at DESC LIMIT ?";
        }

        List<OrderListing> orders = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            if (playerName != null && !playerName.isBlank()) {
                ps.setString(idx++, "%" + playerName.trim() + "%");
            }
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        } catch (SQLException ignored) {
        }
        return orders;
    }

    public static StatsResult getStats() {
        int total = 0, active = 0, completed = 0, expired = 0, cancelled = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT status, COUNT(*) as cnt FROM buy_orders GROUP BY status")) {
            while (rs.next()) {
                int cnt = rs.getInt("cnt");
                total += cnt;
                switch (rs.getString("status")) {
                    case "ACTIVE" -> active = cnt;
                    case "COMPLETED" -> completed = cnt;
                    case "EXPIRED" -> expired = cnt;
                    case "CANCELLED" -> cancelled = cnt;
                }
            }
        } catch (SQLException ignored) {
        }
        return new StatsResult(total, active, completed, expired, cancelled);
    }

    public static void tickExpiry() {
        try {
            String selectSql = "SELECT * FROM buy_orders WHERE status = 'ACTIVE' AND expires_at < ?";
            List<OrderListing> toExpire = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        toExpire.add(mapRow(rs));
                    }
                }
            }

            for (OrderListing order : toExpire) {
                double refund = order.getRemainingEscrow();

                String updateSql = "UPDATE buy_orders SET status = 'EXPIRED', completed_at = ? WHERE id = ? AND status = 'ACTIVE'";
                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setLong(1, System.currentTimeMillis());
                    ps.setInt(2, order.id());
                    ps.executeUpdate();
                }

                if (refund > 0 && hasEconomy()) {
                    OfflinePlayer buyer = Bukkit.getOfflinePlayer(order.buyerUuid());
                    depositBalance(buyer, refund);
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Order expiry tick failed: " + e.getMessage());
        }
    }

    public static void tickCleanup() {
        long graceCutoff = System.currentTimeMillis() - (OrderConfig.expireGraceHours() * 3600L * 1000L);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM buy_orders WHERE status IN ('CANCELLED', 'EXPIRED', 'COMPLETED') AND expires_at < ?")) {
            ps.setLong(1, graceCutoff);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM order_stash WHERE collected = 1 AND created_at < ?")) {
            ps.setLong(1, graceCutoff);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static void cleanupOnStartup() {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM buy_orders WHERE status IN ('CANCELLED', 'EXPIRED', 'COMPLETED')")) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM order_stash WHERE collected = 1")) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static List<OrderListing> getAllSyncableOrders() {
        List<OrderListing> orders = new ArrayList<>();
        String sql = "SELECT * FROM buy_orders WHERE status IN ('ACTIVE', 'COMPLETED', 'EXPIRED', 'CANCELLED')";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException ignored) {
        }
        return orders;
    }

    public static List<Integer> getCleanedUpOrderIds() {
        List<Integer> ids = new ArrayList<>();
        long graceCutoff = System.currentTimeMillis() - (OrderConfig.expireGraceHours() * 3600L * 1000L);
        String sql = "SELECT id FROM buy_orders WHERE status IN ('CANCELLED', 'EXPIRED', 'COMPLETED') AND expires_at < ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, graceCutoff);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException ignored) {
        }
        return ids;
    }

    public static void upsertFromFirebase(OrderListing order) {
        String sql = "INSERT OR REPLACE INTO buy_orders (id, buyer_uuid, buyer_name, material_name, quantity, filled_quantity, price_per_unit, status, created_at, expires_at, completed_at, required_nbt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, order.id());
            ps.setString(2, order.buyerUuid().toString());
            ps.setString(3, order.buyerName());
            ps.setString(4, order.materialName());
            ps.setInt(5, order.quantity());
            ps.setInt(6, order.filledQuantity());
            ps.setDouble(7, order.pricePerUnit());
            ps.setString(8, order.status());
            ps.setLong(9, order.createdAt());
            ps.setLong(10, order.expiresAt());
            if (order.completedAt() != null) {
                ps.setLong(11, order.completedAt());
            } else {
                ps.setNull(11, java.sql.Types.INTEGER);
            }
            if (order.requiredNbt() != null && !order.requiredNbt().isBlank()) {
                ps.setString(12, order.requiredNbt());
            } else {
                ps.setNull(12, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Firebase order upsert failed for order " + order.id() + ": " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    private static OrderListing mapRow(ResultSet rs) throws SQLException {
        Long completedAt = rs.getObject("completed_at") != null ? rs.getLong("completed_at") : null;
        String requiredNbt = rs.getObject("required_nbt") != null ? rs.getString("required_nbt") : null;
        return new OrderListing(
            rs.getInt("id"),
            UUID.fromString(rs.getString("buyer_uuid")),
            rs.getString("buyer_name"),
            rs.getString("material_name"),
            rs.getInt("quantity"),
            rs.getInt("filled_quantity"),
            rs.getDouble("price_per_unit"),
            rs.getString("status"),
            rs.getLong("created_at"),
            rs.getLong("expires_at"),
            completedAt,
            requiredNbt
        );
    }

    private static StashEntry mapStashRow(ResultSet rs) throws SQLException {
        return new StashEntry(
                rs.getInt("id"),
                rs.getInt("order_id"),
                rs.getString("buyer_uuid"),
                rs.getString("material_name"),
                rs.getInt("quantity"),
                rs.getLong("created_at"),
                rs.getInt("collected") == 1
        );
    }

    public static String buildRequiredNbt(Material material, List<MinecraftDataRegistry.MinecraftEnchantment> enchantments, java.util.Map<String, Integer> enchantmentLevels, String potionEffect) {
        if ((enchantments == null || enchantments.isEmpty()) && (potionEffect == null || potionEffect.isEmpty())) {
            return null;
        }
        ItemStack template = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = template.getItemMeta();
        if (meta == null) return null;

        if (enchantments != null && !enchantments.isEmpty()) {
            for (MinecraftDataRegistry.MinecraftEnchantment ench : enchantments) {
                int level = enchantmentLevels != null ? enchantmentLevels.getOrDefault(ench.name(), 1) : 1;
                    org.bukkit.enchantments.Enchantment bukkitEnch = org.bukkit.enchantments.Enchantment.getByKey(
                            org.bukkit.NamespacedKey.minecraft(ench.name().toLowerCase(Locale.ROOT)));
                if (bukkitEnch != null) {
                    meta.addEnchant(bukkitEnch, level, true);
                }
            }
        }

        if (potionEffect != null && !potionEffect.isEmpty() && meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
            org.bukkit.potion.PotionType potionType = MinecraftDataRegistry.getPotionType(potionEffect);
            if (potionType != null) {
                potionMeta.setBasePotionType(potionType);
            } else {
                    String baseEffect = MinecraftDataRegistry.getBasePotionEffectKey(potionEffect);
                    org.bukkit.potion.PotionEffectType effectType = org.bukkit.potion.PotionEffectType.getByKey(
                            org.bukkit.NamespacedKey.minecraft(baseEffect.toLowerCase(Locale.ROOT)));
            if (effectType != null) {
                potionMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                        effectType,
                        MinecraftDataRegistry.getPotionDurationTicks(potionEffect),
                        MinecraftDataRegistry.getPotionAmplifier(potionEffect)), false);
                try {
                    java.lang.reflect.Method setBasePotionType = org.bukkit.inventory.meta.PotionMeta.class.getMethod("setBasePotionType", org.bukkit.potion.PotionType.class);
                    setBasePotionType.invoke(potionMeta, org.bukkit.potion.PotionType.WATER);
                } catch (Exception ignored) {}
            }
            }
        }

        template.setItemMeta(meta);
        return org.atrimilan.sidastuffsmp.auction.AuctionManager.serializeItem(template);
    }

    private static boolean nbtMatches(OrderListing order, ItemStack delivered) {
        return nbtMatchesPublic(order, delivered);
    }

    public static boolean nbtMatchesPublic(OrderListing order, ItemStack delivered) {
        if (!order.hasNbtRequirement()) return true;
        ItemStack template = org.atrimilan.sidastuffsmp.auction.AuctionManager.deserializeItem(order.requiredNbt());
        if (template == null) return true;
        if (delivered.getType() != template.getType()) return false;

        org.bukkit.inventory.meta.ItemMeta templateMeta = template.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta deliveredMeta = delivered.getItemMeta();
        if (templateMeta == null) return true;
        if (deliveredMeta == null) return false;

        if (templateMeta.hasEnchants()) {
            for (org.bukkit.enchantments.Enchantment ench : templateMeta.getEnchants().keySet()) {
                int templateLevel = templateMeta.getEnchantLevel(ench);
                int deliveredLevel = deliveredMeta.getEnchantLevel(ench);
                if (deliveredLevel < templateLevel) return false;
            }
        }

        if (templateMeta instanceof org.bukkit.inventory.meta.PotionMeta templatePotion) {
            if (!(deliveredMeta instanceof org.bukkit.inventory.meta.PotionMeta deliveredPotion)) {
                return false;
            }
            if (templatePotion.hasCustomEffects()) {
                for (org.bukkit.potion.PotionEffect te : templatePotion.getCustomEffects()) {
                    boolean found = false;
                    for (org.bukkit.potion.PotionEffect de : deliveredPotion.getCustomEffects()) {
                        if (te.getType() == de.getType() && de.getAmplifier() >= te.getAmplifier()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
            }
            if (templatePotion.getBasePotionType() != null
                    && templatePotion.getBasePotionType() != org.bukkit.potion.PotionType.WATER
                    && deliveredPotion.getBasePotionType() != templatePotion.getBasePotionType()) {
                return false;
            }
        }

        return true;
    }

    public static String formatMaterialName(Material material) { return formatMaterialName(material.name()); }

    public static String formatMaterialName(String materialName) {
        MinecraftDataRegistry.MinecraftItem mcItem = MinecraftDataRegistry.findItemByName(materialName);
        if (mcItem != null && !mcItem.displayName().equals(mcItem.name())) {
            return mcItem.displayName();
        }
        String name = materialName.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public record CreateResult(boolean success, String message, int orderId) {}
    public record DeliverResult(boolean success, String message, int deliveredCount, int excessCount, double payment) {
        public DeliverResult(boolean success, String message, int deliveredCount, int excessCount) {
            this(success, message, deliveredCount, excessCount, 0);
        }
    }
    public record CancelResult(boolean success, String message, double refundAmount) {}
    public record CollectResult(boolean success, String message, int collectedCount) {}
    public record DropResult(boolean success, String message, int droppedCount) {}
    public record SellStashResult(boolean success, String message, double payment, int soldCount) {}
    public record StatsResult(int total, int active, int completed, int expired, int cancelled) {}

    public record StashEntry(int id, int orderId, String buyerUuid, String materialName, int quantity, long createdAt, boolean collected) {}
}

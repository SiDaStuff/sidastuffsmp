package org.atrimilan.sidastuffsmp.auction;

import net.milkbowl.vault.economy.Economy;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.teleport.PlayerSettings;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AuctionManager {

    private static Connection connection;
    private static Economy economy;
    private static File dbFile;

    private AuctionManager() {}

    public static void init(SiDaStuffSmp plugin) {
        setupEconomy();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dbFile = new File(plugin.getDataFolder(), "auction.db");
        try {
            connect();
            createTable();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize auction database: " + e.getMessage());
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

    private static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
        }
    }

    private static void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auction_listings (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid     TEXT    NOT NULL,
                    seller_name     TEXT    NOT NULL,
                    item_base64     TEXT    NOT NULL,
                    category        TEXT    NOT NULL DEFAULT 'MISC',
                    price           REAL    NOT NULL,
                    status          TEXT    NOT NULL DEFAULT 'ACTIVE',
                    buyer_uuid      TEXT,
                    buyer_name      TEXT,
                    created_at      INTEGER NOT NULL,
                    expires_at      INTEGER NOT NULL,
                    sold_at         INTEGER,
                    collected       INTEGER NOT NULL DEFAULT 0
                )
                """);
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_status ON auction_listings(status)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_seller_uuid ON auction_listings(seller_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_category ON auction_listings(category)");
            } catch (SQLException ignored) {
            }
        }
    }

    private static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
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
        return String.format(Locale.US, "$%,.2f", amount);
    }

    public static String serializeItem(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            dataOutput.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public static ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public static ListResult createListing(Player seller, ItemStack item, double price) {
        if (!hasEconomy()) {
            return new ListResult(false, "Economy system is not available.", -1);
        }

        if (item.getType() == Material.AIR || item.getAmount() <= 0) {
            return new ListResult(false, "You cannot list air or empty items.", -1);
        }

        if (item.getAmount() > item.getMaxStackSize()) {
            return new ListResult(false, "Overstacked items cannot be listed.", -1);
        }

        double minPrice = AuctionConfig.minPrice();
        double maxPrice = AuctionConfig.maxPrice();
        if (price < minPrice || price > maxPrice) {
            return new ListResult(false, "Price must be between " + formatPrice(minPrice) + " and " + formatPrice(maxPrice) + ".", -1);
        }

        int activeCount = getActiveListingCount(seller.getUniqueId());
        int maxListings = AuctionConfig.maxActiveListings();
        if (activeCount >= maxListings) {
            return new ListResult(false, "You have reached the maximum of " + maxListings + " active listings.", -1);
        }

        double feePercent = AuctionConfig.listingFeePercent();
        double fee = price * (feePercent / 100.0);
        if (fee > 0 && !hasBalance(seller, fee)) {
            return new ListResult(false, "You need " + formatPrice(fee) + " for the listing fee.", -1);
        }

        if (fee > 0) {
            withdrawBalance(seller, fee);
        }

        String serialized = serializeItem(item);
        if (serialized == null) {
            if (fee > 0) depositBalance(seller, fee);
            return new ListResult(false, "Failed to serialize item.", -1);
        }

        AuctionCategory category = AuctionCategory.fromMaterial(item.getType());
        long now = System.currentTimeMillis();
        long expiresAt = now + (AuctionConfig.listingDurationHours() * 3600L * 1000L);

        String sql = "INSERT INTO auction_listings (seller_uuid, seller_name, item_base64, category, price, status, created_at, expires_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.getUniqueId().toString());
            ps.setString(2, seller.getName());
            ps.setString(3, serialized);
            ps.setString(4, category.name());
            ps.setDouble(5, price);
            ps.setLong(6, now);
            ps.setLong(7, expiresAt);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    seller.getInventory().setItemInMainHand(null);
                    return new ListResult(true, "Listed for " + formatPrice(price) + "!", id);
                }
            }
        } catch (SQLException e) {
            if (fee > 0) depositBalance(seller, fee);
            return new ListResult(false, "Database error: " + e.getMessage(), -1);
        }

        if (fee > 0) depositBalance(seller, fee);
        return new ListResult(false, "Failed to create listing.", -1);
    }

    public static PurchaseResult purchaseListing(Player buyer, int listingId) {
        if (!hasEconomy()) {
            return new PurchaseResult(false, "Economy system is not available.");
        }

        try {
            String selectSql = "SELECT * FROM auction_listings WHERE id = ? AND status = 'ACTIVE'";
            AuctionListing listing = null;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, listingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        listing = mapRow(rs);
                    }
                }
            }

            if (listing == null) {
                return new PurchaseResult(false, "This listing is no longer available.");
            }

            if (listing.sellerUuid().equals(buyer.getUniqueId())) {
                return new PurchaseResult(false, "You cannot buy your own listing.");
            }

            if (!hasBalance(buyer, listing.price())) {
                return new PurchaseResult(false, "You don't have enough money. Price: " + formatPrice(listing.price()));
            }

            String updateSql = "UPDATE auction_listings SET status = 'SOLD', buyer_uuid = ?, buyer_name = ?, sold_at = ? WHERE id = ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, buyer.getUniqueId().toString());
                ps.setString(2, buyer.getName());
                ps.setLong(3, System.currentTimeMillis());
                ps.setInt(4, listingId);
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    return new PurchaseResult(false, "This listing was just purchased by someone else.");
                }
            }

            if (!withdrawBalance(buyer, listing.price())) {
                String revertSql = "UPDATE auction_listings SET status = 'ACTIVE', buyer_uuid = NULL, buyer_name = NULL, sold_at = NULL WHERE id = ? AND status = 'SOLD'";
                try (PreparedStatement ps = connection.prepareStatement(revertSql)) {
                    ps.setInt(1, listingId);
                    ps.executeUpdate();
                }
                return new PurchaseResult(false, "Payment failed. Your balance may have changed.");
            }

            ItemStack item = deserializeItem(listing.itemBase64());
            if (item != null) {
                java.util.Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(l -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), l));

                    Player sellerPlayer = Bukkit.getPlayer(listing.sellerUuid());
                    if (sellerPlayer != null && sellerPlayer.isOnline()) {
                        PlayerSettings sellerSettings = PlayerSettings.get(listing.sellerUuid());
                        if (sellerSettings.isAuctionMessagesEnabled()) {
                            sellerPlayer.sendMessage(Chat.prefixed("Your auction listing was purchased by " + buyer.getName() + " for " + formatPrice(listing.price()) + "!", NamedTextColor.GREEN));
                            sellerPlayer.sendMessage(Chat.prefixed("Use /ah my to collect your money.", NamedTextColor.YELLOW));
                        }
                    }

                    return new PurchaseResult(true, "Purchased for " + formatPrice(listing.price()) + "! Item dropped at your feet (inventory full).");
                }
            }

            Player sellerPlayer = Bukkit.getPlayer(listing.sellerUuid());
            if (sellerPlayer != null && sellerPlayer.isOnline()) {
                PlayerSettings sellerSettings = PlayerSettings.get(listing.sellerUuid());
                if (sellerSettings.isAuctionMessagesEnabled()) {
                    sellerPlayer.sendMessage(Chat.prefixed("Your auction listing was purchased by " + buyer.getName() + " for " + formatPrice(listing.price()) + "!", NamedTextColor.GREEN));
                    sellerPlayer.sendMessage(Chat.prefixed("Use /ah my to collect your money.", NamedTextColor.YELLOW));
                }
            }

            return new PurchaseResult(true, "Purchased for " + formatPrice(listing.price()) + "!");

        } catch (SQLException e) {
            return new PurchaseResult(false, "Database error: " + e.getMessage());
        }
    }

    public static CancelResult cancelListing(Player seller, int listingId) {
        try {
            String selectSql = "SELECT * FROM auction_listings WHERE id = ? AND status = 'ACTIVE' AND seller_uuid = ?";
            AuctionListing listing = null;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, listingId);
                ps.setString(2, seller.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        listing = mapRow(rs);
                    }
                }
            }

            if (listing == null) {
                return new CancelResult(false, "Listing not found or not cancellable.");
            }

            String updateSql = "UPDATE auction_listings SET status = 'CANCELLED', collected = 1 WHERE id = ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, listingId);
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    return new CancelResult(false, "Listing could not be cancelled (it may have been purchased).");
                }
            }

        ItemStack item = deserializeItem(listing.itemBase64());
        if (item != null) {
            int firstEmpty = seller.getInventory().firstEmpty();
            if (firstEmpty == -1) {
                seller.getWorld().dropItemNaturally(seller.getLocation(), item);
                return new CancelResult(true, "Listing cancelled. Item dropped at your feet (inventory full).");
            }
            seller.getInventory().addItem(item);
        }

        return new CancelResult(true, "Listing cancelled and item returned.");

        } catch (SQLException e) {
            return new CancelResult(false, "Database error: " + e.getMessage());
        }
    }

    public static CancelResult adminCancelListing(int listingId) {
        try {
            String selectSql = "SELECT * FROM auction_listings WHERE id = ? AND status = 'ACTIVE'";
            AuctionListing listing = null;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, listingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        listing = mapRow(rs);
                    }
                }
            }

            if (listing == null) {
                return new CancelResult(false, "Active listing not found with ID " + listingId + ".");
            }

            String updateSql = "UPDATE auction_listings SET status = 'CANCELLED', collected = 1 WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, listingId);
                ps.executeUpdate();
            }

        Player seller = Bukkit.getPlayer(listing.sellerUuid());
        if (seller != null && seller.isOnline()) {
            ItemStack item = deserializeItem(listing.itemBase64());
            if (item != null) {
                int firstEmpty = seller.getInventory().firstEmpty();
                if (firstEmpty == -1) {
                    seller.getWorld().dropItemNaturally(seller.getLocation(), item);
                } else {
                    seller.getInventory().addItem(item);
                }
            }
        }

        return new CancelResult(true, "Force-cancelled listing " + listingId + ".");

        } catch (SQLException e) {
            return new CancelResult(false, "Database error: " + e.getMessage());
        }
    }

    public static boolean adminDeleteListing(int listingId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM auction_listings WHERE id = ?")) {
            ps.setInt(1, listingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static int adminExpireAll() {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE auction_listings SET status = 'EXPIRED' WHERE status = 'ACTIVE'")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            return -1;
        }
    }

    public static CollectResult collectSoldMoney(Player seller, int listingId) {
        try {
            String selectSql = "SELECT * FROM auction_listings WHERE id = ? AND status = 'SOLD' AND seller_uuid = ? AND collected = 0";
            AuctionListing listing = null;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, listingId);
                ps.setString(2, seller.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        listing = mapRow(rs);
                    }
                }
            }

            if (listing == null) {
                return new CollectResult(false, "No sold listing found to collect.", 0);
            }

            String updateSql = "UPDATE auction_listings SET collected = 1 WHERE id = ? AND collected = 0";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, listingId);
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    return new CollectResult(false, "Already collected.", 0);
                }
            }

            boolean deposited = depositBalance(seller, listing.price());
            if (!deposited) {
                String revertSql = "UPDATE auction_listings SET collected = 0 WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(revertSql)) {
                    ps.setInt(1, listingId);
                    ps.executeUpdate();
                }
                return new CollectResult(false, "Failed to deposit money. Try again later.", 0);
            }

            return new CollectResult(true, "Collected " + formatPrice(listing.price()) + "!", listing.price());

        } catch (SQLException e) {
            return new CollectResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static CollectResult collectExpiredItem(Player seller, int listingId) {
        try {
            String selectSql = "SELECT * FROM auction_listings WHERE id = ? AND status = 'EXPIRED' AND seller_uuid = ? AND collected = 0";
            AuctionListing listing = null;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, listingId);
                ps.setString(2, seller.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        listing = mapRow(rs);
                    }
                }
            }

            if (listing == null) {
                return new CollectResult(false, "No expired listing found to collect.", 0);
            }

            String updateSql = "UPDATE auction_listings SET collected = 1 WHERE id = ? AND collected = 0";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, listingId);
                int affected = ps.executeUpdate();
                if (affected != 1) {
                    return new CollectResult(false, "Already collected.", 0);
                }
            }

        ItemStack item = deserializeItem(listing.itemBase64());
        if (item != null) {
            int firstEmpty = seller.getInventory().firstEmpty();
            if (firstEmpty == -1) {
                String revertSql2 = "UPDATE auction_listings SET collected = 0 WHERE id = ?";
                try (PreparedStatement ps2 = connection.prepareStatement(revertSql2)) {
                    ps2.setInt(1, listingId);
                    ps2.executeUpdate();
                }
                return new CollectResult(false, "Your inventory is full! Make room and try again.", 0);
            }
            seller.getInventory().addItem(item);
        }

        return new CollectResult(true, "Item returned to your inventory.", 0);

        } catch (SQLException e) {
            return new CollectResult(false, "Database error: " + e.getMessage(), 0);
        }
    }

    public static List<AuctionListing> getBrowserListings(AuctionSortMode sort, AuctionCategory category, String searchTerm, int offset, int limit) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            return getBrowserListingsWithSearch(sort, searchTerm, offset, limit);
        }

        String sql = "SELECT * FROM auction_listings WHERE status = 'ACTIVE' ORDER BY " + sort.sqlOrderBy() + " LIMIT ? OFFSET ?";

        List<AuctionListing> listings = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listings.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Auction browser query failed: " + e.getMessage());
        }
        return listings;
    }

    private static List<AuctionListing> getBrowserListingsWithSearch(AuctionSortMode sort, String searchTerm, int offset, int limit) {
        String term = searchTerm.trim().toLowerCase(Locale.ROOT);

        String sql = "SELECT * FROM auction_listings WHERE status = 'ACTIVE' ORDER BY " + sort.sqlOrderBy();

        List<AuctionListing> allMatching = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuctionListing listing = mapRow(rs);
                    if (matchesSearch(listing, term)) {
                        allMatching.add(listing);
                    }
                }
            }
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Auction browser query failed: " + e.getMessage());
        }

        if (offset >= allMatching.size()) return new ArrayList<>();
        int end = Math.min(offset + limit, allMatching.size());
        return allMatching.subList(offset, end);
    }

    private static boolean matchesSearch(AuctionListing listing, String lowerTerm) {
        if (listing.sellerName().toLowerCase(Locale.ROOT).contains(lowerTerm)) return true;
        if (listing.category().name().toLowerCase(Locale.ROOT).contains(lowerTerm)) return true;
        if (listing.category().displayName().toLowerCase(Locale.ROOT).contains(lowerTerm)) return true;

        ItemStack item = deserializeItem(listing.itemBase64());
        if (item != null) {
            String matName = item.getType().name().toLowerCase(Locale.ROOT);
            String matUnderscore = matName.replace("_", " ");
            String matDisplay = item.getType().toString().toLowerCase(Locale.ROOT).replace("_", " ");

            if (matName.contains(lowerTerm)) return true;
            if (matUnderscore.contains(lowerTerm)) return true;
            if (matDisplay.contains(lowerTerm)) return true;

            String normalizedTerm = lowerTerm.replace(" ", "_");
            if (matName.contains(normalizedTerm)) return true;

            if (item.hasItemMeta() && item.getItemMeta() != null) {
                if (item.getItemMeta().hasDisplayName()) {
                    String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(item.getItemMeta().displayName()).toLowerCase(Locale.ROOT);
                    if (displayName.contains(lowerTerm)) return true;
                }
            }
        }

        return false;
    }

    public static int getBrowserListingCount(AuctionCategory category, String searchTerm) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            return getBrowserListingCountWithSearch(searchTerm);
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM auction_listings WHERE status = 'ACTIVE'")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    private static int getBrowserListingCountWithSearch(String searchTerm) {
        String term = searchTerm.trim().toLowerCase(Locale.ROOT);

        int count = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM auction_listings WHERE status = 'ACTIVE'")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuctionListing listing = mapRow(rs);
                    if (matchesSearch(listing, term)) {
                        count++;
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return count;
    }

    public static List<AuctionListing> getMyListings(UUID playerUuid) {
        String sql = "SELECT * FROM auction_listings WHERE seller_uuid = ? AND (status = 'ACTIVE' OR (status = 'SOLD' AND collected = 0) OR (status = 'EXPIRED' AND collected = 0)) ORDER BY status, created_at DESC";
        List<AuctionListing> listings = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listings.add(mapRow(rs));
                }
            }
        } catch (SQLException ignored) {
        }
        return listings;
    }

    public static int getActiveListingCount(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM auction_listings WHERE seller_uuid = ? AND status = 'ACTIVE'")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static int getUncollectedSoldCount(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM auction_listings WHERE seller_uuid = ? AND status = 'SOLD' AND collected = 0")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static int getUncollectedExpiredCount(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM auction_listings WHERE seller_uuid = ? AND status = 'EXPIRED' AND collected = 0")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static AuctionListing getListingById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM auction_listings WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public static List<AuctionListing> getAdminListings(String playerName, int limit) {
        String sql;
        if (playerName != null && !playerName.isBlank()) {
            sql = "SELECT * FROM auction_listings WHERE seller_name LIKE ? ORDER BY created_at DESC LIMIT ?";
        } else {
            sql = "SELECT * FROM auction_listings ORDER BY created_at DESC LIMIT ?";
        }

        List<AuctionListing> listings = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            if (playerName != null && !playerName.isBlank()) {
                ps.setString(idx++, "%" + playerName.trim() + "%");
            }
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listings.add(mapRow(rs));
                }
            }
        } catch (SQLException ignored) {
        }
        return listings;
    }

    public static StatsResult getStats() {
        int total = 0, active = 0, sold = 0, expired = 0, cancelled = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT status, COUNT(*) as cnt FROM auction_listings GROUP BY status")) {
            while (rs.next()) {
                int cnt = rs.getInt("cnt");
                total += cnt;
                switch (rs.getString("status")) {
                    case "ACTIVE" -> active = cnt;
                    case "SOLD" -> sold = cnt;
                    case "EXPIRED" -> expired = cnt;
                    case "CANCELLED" -> cancelled = cnt;
                }
            }
        } catch (SQLException ignored) {
        }
        return new StatsResult(total, active, sold, expired, cancelled);
    }

    public static void tickExpiry() {
        try {
            String selectSql = "SELECT * FROM auction_listings WHERE status = 'ACTIVE' AND expires_at < ?";
            List<AuctionListing> toExpire = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        toExpire.add(mapRow(rs));
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE auction_listings SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < ?")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    public static void tickCleanup() {
        long graceCutoff = System.currentTimeMillis() - (AuctionConfig.expireGraceHours() * 3600L * 1000L);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM auction_listings WHERE ((status = 'CANCELLED' AND collected = 1) OR (status = 'EXPIRED' AND collected = 1) OR (status = 'SOLD' AND collected = 1)) AND expires_at < ?")) {
            ps.setLong(1, graceCutoff);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static void cleanupOnStartup() {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM auction_listings WHERE ((status = 'CANCELLED' AND collected = 1) OR (status = 'SOLD' AND collected = 1) OR (status = 'EXPIRED' AND collected = 1)) AND expires_at < ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static List<AuctionListing> getAllSyncableListings() {
        List<AuctionListing> listings = new ArrayList<>();
        String sql = "SELECT * FROM auction_listings WHERE status IN ('ACTIVE', 'SOLD') OR (status IN ('EXPIRED','CANCELLED') AND collected = 0)";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                listings.add(mapRow(rs));
            }
        } catch (SQLException ignored) {
        }
        return listings;
    }

    public static List<Integer> getCollectedOrCancelledIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM auction_listings WHERE (status = 'CANCELLED' AND collected = 1) OR (status = 'SOLD' AND collected = 1) OR (status = 'EXPIRED' AND collected = 1)";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException ignored) {
        }
        return ids;
    }

    public static void upsertFromFirebase(AuctionListing listing) {
        String sql = "INSERT OR REPLACE INTO auction_listings (id, seller_uuid, seller_name, item_base64, category, price, status, buyer_uuid, buyer_name, created_at, expires_at, sold_at, collected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, listing.id());
            ps.setString(2, listing.sellerUuid().toString());
            ps.setString(3, listing.sellerName());
            ps.setString(4, listing.itemBase64());
            ps.setString(5, listing.category().name());
            ps.setDouble(6, listing.price());
            ps.setString(7, listing.status());
            ps.setString(8, listing.buyerUuid() != null ? listing.buyerUuid().toString() : null);
            ps.setString(9, listing.buyerName());
            ps.setLong(10, listing.createdAt());
            ps.setLong(11, listing.expiresAt());
            if (listing.soldAt() != null) {
                ps.setLong(12, listing.soldAt());
            } else {
                ps.setNull(12, java.sql.Types.INTEGER);
            }
            ps.setInt(13, listing.collected() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            SiDaStuffSmp.getInstance().getLogger().warning("Firebase upsert failed for listing " + listing.id() + ": " + e.getMessage());
        }
    }

    private static AuctionListing mapRow(ResultSet rs) throws SQLException {
        UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
        UUID buyerUuid = rs.getString("buyer_uuid") != null ? UUID.fromString(rs.getString("buyer_uuid")) : null;
        Long soldAt = rs.getObject("sold_at") != null ? rs.getLong("sold_at") : null;

        return new AuctionListing(
                rs.getInt("id"),
                sellerUuid,
                rs.getString("seller_name"),
                rs.getString("item_base64"),
                AuctionCategory.valueOf(rs.getString("category")),
                rs.getDouble("price"),
                rs.getString("status"),
                buyerUuid,
                rs.getString("buyer_name"),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                soldAt,
                rs.getInt("collected") == 1
        );
    }

    public static Connection getConnection() {
        return connection;
    }

    public record ListResult(boolean success, String message, int listingId) {}
    public record PurchaseResult(boolean success, String message) {}
    public record CancelResult(boolean success, String message) {}
    public record CollectResult(boolean success, String message, double amount) {}
    public record StatsResult(int total, int active, int sold, int expired, int cancelled) {}
}

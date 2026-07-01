package org.atrimilan.sidastuffsmp.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record AuctionListing(
        int id,
        UUID sellerUuid,
        String sellerName,
        String itemBase64,
        AuctionCategory category,
        double price,
        String status,
        UUID buyerUuid,
        String buyerName,
        long createdAt,
        long expiresAt,
        Long soldAt,
        boolean collected
) {

    public enum Status {
        ACTIVE,
        SOLD,
        EXPIRED,
        CANCELLED
    }
}

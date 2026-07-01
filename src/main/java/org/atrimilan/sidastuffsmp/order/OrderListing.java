package org.atrimilan.sidastuffsmp.order;

import java.util.UUID;

public record OrderListing(
    int id,
    UUID buyerUuid,
    String buyerName,
    String materialName,
    int quantity,
    int filledQuantity,
    double pricePerUnit,
    String status,
    long createdAt,
    long expiresAt,
    Long completedAt,
    String requiredNbt
) {

    public enum Status {
        ACTIVE, COMPLETED, EXPIRED, CANCELLED
    }

    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public double getTotalEscrow() {
        return quantity * pricePerUnit;
    }

    public double getRemainingEscrow() {
        return getRemainingQuantity() * pricePerUnit;
    }

    public boolean hasNbtRequirement() {
        return requiredNbt != null && !requiredNbt.isBlank();
    }
}

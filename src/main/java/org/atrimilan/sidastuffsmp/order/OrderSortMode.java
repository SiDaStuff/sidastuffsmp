package org.atrimilan.sidastuffsmp.order;

import org.bukkit.Material;

public enum OrderSortMode {

    PRICE_ASC,
    PRICE_DESC,
    NEWEST,
    EXPIRING;

    public OrderSortMode next() {
        return switch (this) {
            case PRICE_ASC -> PRICE_DESC;
            case PRICE_DESC -> NEWEST;
            case NEWEST -> EXPIRING;
            case EXPIRING -> PRICE_ASC;
        };
    }

    public String displayName() {
        return switch (this) {
            case PRICE_ASC -> "Price: Low to High";
            case PRICE_DESC -> "Price: High to Low";
            case NEWEST -> "Newest First";
            case EXPIRING -> "Expiring Soon";
        };
    }

    public String sqlOrderBy() {
        return switch (this) {
            case PRICE_ASC -> "price_per_unit ASC";
            case PRICE_DESC -> "price_per_unit DESC";
            case NEWEST -> "created_at DESC";
            case EXPIRING -> "expires_at ASC";
        };
    }
}

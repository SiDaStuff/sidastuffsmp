package org.atrimilan.sidastuffsmp.economy;

import java.util.UUID;

public record TransactionRecord(
    int id,
    UUID playerUuid,
    String type,
    double amount,
    UUID targetUuid,
    String targetName,
    String description,
    long timestamp
) {}

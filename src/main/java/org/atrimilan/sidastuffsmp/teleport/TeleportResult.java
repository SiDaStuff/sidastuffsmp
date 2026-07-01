package org.atrimilan.sidastuffsmp.teleport;

import java.util.UUID;

public record TeleportResult(boolean success, String message, TeleportRequest request) {
    public TeleportResult(boolean success, String message) {
        this(success, message, null);
    }
}

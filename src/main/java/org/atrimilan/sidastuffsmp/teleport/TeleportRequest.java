package org.atrimilan.sidastuffsmp.teleport;

import java.util.UUID;

public record TeleportRequest(
    UUID senderUuid,
    String senderName,
    UUID targetUuid,
    String targetName,
    Type type,
    long createdAt
) {

    public enum Type {
        TPA,
        TPAHERE
    }
}

package com.bzj.chainsentinel.vo.sync;

import java.time.LocalDateTime;

public record SyncCheckpointVO(
        String network,
        Long lastHeight,
        String lastBlockHash,
        String status,
        LocalDateTime lastSyncedAt,
        String errorMessage,
        LocalDateTime updatedAt
) {
}

package com.bzj.chainsentinel.vo.sync;

import java.time.LocalDateTime;

public record SyncRunVO(
        Long id,
        String network,
        String runType,
        long startHeight,
        long targetHeight,
        Long lastSuccessHeight,
        int blocksProcessed,
        String status,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}

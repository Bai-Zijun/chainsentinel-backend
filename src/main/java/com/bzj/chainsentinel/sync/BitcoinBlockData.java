package com.bzj.chainsentinel.sync;

import java.time.LocalDateTime;

public record BitcoinBlockData(
        long height,
        String blockHash,
        String previousBlockHash,
        String merkleRoot,
        LocalDateTime blockTime,
        int transactionCount,
        int size,
        int weight
) {
}

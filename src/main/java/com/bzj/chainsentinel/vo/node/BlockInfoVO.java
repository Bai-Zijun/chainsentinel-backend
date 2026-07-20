package com.bzj.chainsentinel.vo.node;

import java.math.BigDecimal;
import java.util.List;

public record BlockInfoVO(
        String hash,
        int confirmations,
        long height,
        int version,
        String merkleRoot,
        long time,
        long medianTime,
        long nonce,
        String bits,
        BigDecimal difficulty,
        String chainwork,
        int size,
        int strippedSize,
        int weight,
        String previousBlockHash,
        String nextBlockHash,
        int transactionCount,
        int transactionsReturned,
        boolean transactionsTruncated,
        List<BlockTransactionSummaryVO> transactions
) {
}

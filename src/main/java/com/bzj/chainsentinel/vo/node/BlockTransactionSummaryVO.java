package com.bzj.chainsentinel.vo.node;

public record BlockTransactionSummaryVO(
        String txid,
        String hash,
        int version,
        int size,
        int virtualSize,
        int weight,
        long lockTime,
        int inputCount,
        int outputCount,
        boolean isCoinbase
) {
}

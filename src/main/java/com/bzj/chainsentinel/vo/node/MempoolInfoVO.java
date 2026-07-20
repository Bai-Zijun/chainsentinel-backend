package com.bzj.chainsentinel.vo.node;

import java.math.BigDecimal;

public record MempoolInfoVO(
        boolean loaded,
        long size,
        long bytes,
        long usage,
        BigDecimal totalFee,
        long maxMempool,
        BigDecimal mempoolMinFee,
        BigDecimal minRelayTxFee,
        BigDecimal incrementalRelayFee,
        long unbroadcastCount,
        boolean fullRbf
) {
}

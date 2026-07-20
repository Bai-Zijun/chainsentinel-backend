package com.bzj.chainsentinel.vo.node;

import java.math.BigDecimal;
import java.util.List;

public record BlockchainInfoVO(
        String chain,
        long blocks,
        long headers,
        String bestBlockHash,
        BigDecimal difficulty,
        BigDecimal verificationProgress,
        boolean initialBlockDownload,
        long sizeOnDisk,
        boolean pruned,
        List<String> warnings
) {
}

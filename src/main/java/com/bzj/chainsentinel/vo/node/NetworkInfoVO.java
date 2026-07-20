package com.bzj.chainsentinel.vo.node;

import java.math.BigDecimal;
import java.util.List;

public record NetworkInfoVO(
        int version,
        String subversion,
        int protocolVersion,
        boolean networkActive,
        int connections,
        int connectionsIn,
        int connectionsOut,
        BigDecimal relayFee,
        BigDecimal incrementalFee,
        List<String> warnings
) {
}

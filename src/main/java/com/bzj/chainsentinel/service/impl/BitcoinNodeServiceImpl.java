package com.bzj.chainsentinel.service.impl;

import com.bzj.chainsentinel.client.BitcoinCoreRpcClient;
import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.node.MempoolInfoVO;
import com.bzj.chainsentinel.vo.node.NetworkInfoVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BitcoinNodeServiceImpl implements BitcoinNodeService {

    private final BitcoinCoreRpcClient rpcClient;
    private final BitcoinProperties properties;

    @Override
    public BlockchainInfoVO getBlockchainInfo() {
        JsonNode result = requireObject(rpcClient.call("getblockchaininfo"));
        String chain = requireText(result, "chain");
        if (!properties.getNetwork().equals(chain)) {
            throw new BitcoinRpcException(
                    HttpStatus.BAD_GATEWAY,
                    "bitcoin network mismatch: expected " + properties.getNetwork() + ", got " + chain
            );
        }

        return new BlockchainInfoVO(
                chain,
                requireLong(result, "blocks"),
                requireLong(result, "headers"),
                requireText(result, "bestblockhash"),
                requireDecimal(result, "difficulty"),
                requireDecimal(result, "verificationprogress"),
                requireBoolean(result, "initialblockdownload"),
                requireLong(result, "size_on_disk"),
                requireBoolean(result, "pruned"),
                readWarnings(result.get("warnings"))
        );
    }

    @Override
    public NetworkInfoVO getNetworkInfo() {
        JsonNode result = requireObject(rpcClient.call("getnetworkinfo"));
        return new NetworkInfoVO(
                requireInt(result, "version"),
                requireText(result, "subversion"),
                requireInt(result, "protocolversion"),
                requireBoolean(result, "networkactive"),
                requireInt(result, "connections"),
                requireInt(result, "connections_in"),
                requireInt(result, "connections_out"),
                requireDecimal(result, "relayfee"),
                requireDecimal(result, "incrementalfee"),
                readWarnings(result.get("warnings"))
        );
    }

    @Override
    public MempoolInfoVO getMempoolInfo() {
        JsonNode result = requireObject(rpcClient.call("getmempoolinfo"));
        return new MempoolInfoVO(
                requireBoolean(result, "loaded"),
                requireLong(result, "size"),
                requireLong(result, "bytes"),
                requireLong(result, "usage"),
                requireDecimal(result, "total_fee"),
                requireLong(result, "maxmempool"),
                requireDecimal(result, "mempoolminfee"),
                requireDecimal(result, "minrelaytxfee"),
                requireDecimal(result, "incrementalrelayfee"),
                requireLong(result, "unbroadcastcount"),
                requireBoolean(result, "fullrbf")
        );
    }

    private JsonNode requireObject(JsonNode value) {
        if (value == null || !value.isObject()) {
            throw invalidResult();
        }
        return value;
    }

    private JsonNode requireField(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            throw invalidResult();
        }
        return value;
    }

    private String requireText(JsonNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.isTextual()) {
            throw invalidResult();
        }
        return value.textValue();
    }

    private int requireInt(JsonNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.canConvertToInt()) {
            throw invalidResult();
        }
        return value.intValue();
    }

    private long requireLong(JsonNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.canConvertToLong()) {
            throw invalidResult();
        }
        return value.longValue();
    }

    private BigDecimal requireDecimal(JsonNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.isNumber()) {
            throw invalidResult();
        }
        return value.decimalValue();
    }

    private boolean requireBoolean(JsonNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.isBoolean()) {
            throw invalidResult();
        }
        return value.booleanValue();
    }

    private List<String> readWarnings(JsonNode warnings) {
        if (warnings == null || warnings.isNull()) {
            return List.of();
        }
        if (warnings.isTextual()) {
            return warnings.textValue().isBlank() ? List.of() : List.of(warnings.textValue());
        }
        if (!warnings.isArray()) {
            throw invalidResult();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode warning : warnings) {
            if (!warning.isTextual()) {
                throw invalidResult();
            }
            values.add(warning.textValue());
        }
        return List.copyOf(values);
    }

    private BitcoinRpcException invalidResult() {
        return new BitcoinRpcException(
                HttpStatus.BAD_GATEWAY,
                "bitcoin rpc returned an unexpected result"
        );
    }
}

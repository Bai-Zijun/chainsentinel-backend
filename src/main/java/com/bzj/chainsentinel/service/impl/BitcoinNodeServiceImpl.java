package com.bzj.chainsentinel.service.impl;

import com.bzj.chainsentinel.client.BitcoinCoreRpcClient;
import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.bzj.chainsentinel.exception.ResourceNotFoundException;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.vo.node.BlockInfoVO;
import com.bzj.chainsentinel.vo.node.BlockTransactionSummaryVO;
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

    @Override
    public BlockInfoVO getBlockByHeight(long height, int transactionLimit) {
        JsonNode blockHashResult;
        JsonNode blockResult;
        try {
            blockHashResult = rpcClient.call("getblockhash", List.of(height));
            if (blockHashResult == null || !blockHashResult.isTextual()) {
                throw invalidResult();
            }
            blockResult = rpcClient.call("getblock", List.of(blockHashResult.textValue(), 2));
        } catch (BitcoinRpcException exception) {
            if (exception.getRpcCode() != null
                    && (exception.getRpcCode() == -8 || exception.getRpcCode() == -5)) {
                throw new ResourceNotFoundException("block at height " + height + " not found");
            }
            throw exception;
        }

        JsonNode block = requireObject(blockResult);
        JsonNode transactions = requireField(block, "tx");
        if (!transactions.isArray()) {
            throw invalidResult();
        }

        int transactionCount = requireInt(block, "nTx");
        if (transactionCount != transactions.size()) {
            throw invalidResult();
        }
        int returned = Math.min(transactionLimit, transactions.size());
        List<BlockTransactionSummaryVO> summaries = new ArrayList<>(returned);
        for (int index = 0; index < returned; index++) {
            summaries.add(toTransactionSummary(transactions.get(index)));
        }

        return new BlockInfoVO(
                requireText(block, "hash"),
                requireInt(block, "confirmations"),
                requireLong(block, "height"),
                requireInt(block, "version"),
                requireText(block, "merkleroot"),
                requireLong(block, "time"),
                requireLong(block, "mediantime"),
                requireLong(block, "nonce"),
                requireText(block, "bits"),
                requireDecimal(block, "difficulty"),
                requireText(block, "chainwork"),
                requireInt(block, "size"),
                requireInt(block, "strippedsize"),
                requireInt(block, "weight"),
                optionalText(block, "previousblockhash"),
                optionalText(block, "nextblockhash"),
                transactionCount,
                summaries.size(),
                transactionCount > summaries.size(),
                List.copyOf(summaries)
        );
    }

    private BlockTransactionSummaryVO toTransactionSummary(JsonNode transaction) {
        JsonNode object = requireObject(transaction);
        JsonNode inputs = requireField(object, "vin");
        JsonNode outputs = requireField(object, "vout");
        if (!inputs.isArray() || !outputs.isArray()) {
            throw invalidResult();
        }

        boolean coinbase = !inputs.isEmpty()
                && inputs.get(0).isObject()
                && inputs.get(0).has("coinbase");
        return new BlockTransactionSummaryVO(
                requireText(object, "txid"),
                requireText(object, "hash"),
                requireInt(object, "version"),
                requireInt(object, "size"),
                requireInt(object, "vsize"),
                requireInt(object, "weight"),
                requireLong(object, "locktime"),
                inputs.size(),
                outputs.size(),
                coinbase
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

    private String optionalText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalidResult();
        }
        return value.textValue();
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

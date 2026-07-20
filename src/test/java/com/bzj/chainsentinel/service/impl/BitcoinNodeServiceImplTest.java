package com.bzj.chainsentinel.service.impl;

import com.bzj.chainsentinel.client.BitcoinCoreRpcClient;
import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.bzj.chainsentinel.exception.ResourceNotFoundException;
import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.node.BlockInfoVO;
import com.bzj.chainsentinel.vo.node.MempoolInfoVO;
import com.bzj.chainsentinel.vo.node.NetworkInfoVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitcoinNodeServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BitcoinCoreRpcClient rpcClient;

    private BitcoinNodeServiceImpl service;

    @BeforeEach
    void setUp() {
        BitcoinProperties properties = new BitcoinProperties();
        properties.setNetwork("testnet4");
        service = new BitcoinNodeServiceImpl(rpcClient, properties);
    }

    @Test
    void mapsBlockchainInfoAndChecksNetwork() throws JsonProcessingException {
        when(rpcClient.call("getblockchaininfo")).thenReturn(json("""
                {
                  "chain":"testnet4",
                  "blocks":143838,
                  "headers":143838,
                  "bestblockhash":"00000000007fa7a85dbda09f7e957cc5b0fa22a548917375119178e4da885048",
                  "difficulty":1,
                  "verificationprogress":1,
                  "initialblockdownload":false,
                  "size_on_disk":13166803162,
                  "pruned":false,
                  "warnings":[]
                }
                """));

        BlockchainInfoVO result = service.getBlockchainInfo();

        assertEquals("testnet4", result.chain());
        assertEquals(143838, result.blocks());
        assertFalse(result.initialBlockDownload());
        assertEquals(13166803162L, result.sizeOnDisk());
    }

    @Test
    void mapsNetworkAndMempoolInfo() throws JsonProcessingException {
        when(rpcClient.call("getnetworkinfo")).thenReturn(json("""
                {
                  "version":300200,
                  "subversion":"/Satoshi:30.2.0/",
                  "protocolversion":70016,
                  "networkactive":true,
                  "connections":10,
                  "connections_in":0,
                  "connections_out":10,
                  "relayfee":0.000001,
                  "incrementalfee":0.000001,
                  "warnings":[]
                }
                """));
        when(rpcClient.call("getmempoolinfo")).thenReturn(json("""
                {
                  "loaded":true,
                  "size":43,
                  "bytes":24371,
                  "usage":131504,
                  "total_fee":0.00033660,
                  "maxmempool":300000000,
                  "mempoolminfee":0.000001,
                  "minrelaytxfee":0.000001,
                  "incrementalrelayfee":0.000001,
                  "unbroadcastcount":0,
                  "fullrbf":true
                }
                """));

        NetworkInfoVO network = service.getNetworkInfo();
        MempoolInfoVO mempool = service.getMempoolInfo();

        assertEquals(70016, network.protocolVersion());
        assertTrue(network.networkActive());
        assertEquals(10, network.connectionsOut());
        assertEquals(43, mempool.size());
        assertEquals(0, new BigDecimal("0.000001").compareTo(mempool.mempoolMinFee()));
        assertTrue(mempool.fullRbf());
    }

    @Test
    void rejectsUnexpectedNetwork() throws JsonProcessingException {
        when(rpcClient.call("getblockchaininfo")).thenReturn(json("""
                {"chain":"main"}
                """));

        BitcoinRpcException exception = assertThrows(
                BitcoinRpcException.class,
                () -> service.getBlockchainInfo()
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertTrue(exception.getMessage().contains("network mismatch"));
    }

    @Test
    void returnsBlockWithLimitedTransactionSummaries() throws JsonProcessingException {
        String blockHash = "1".repeat(64);
        when(rpcClient.call("getblockhash", List.of(143838L)))
                .thenReturn(json('"' + blockHash + '"'));
        when(rpcClient.call("getblock", List.of(blockHash, 2))).thenReturn(json("""
                {
                  "hash":"%s",
                  "confirmations":3,
                  "height":143838,
                  "version":536870912,
                  "merkleroot":"%s",
                  "time":1783860041,
                  "mediantime":1783855236,
                  "nonce":42,
                  "bits":"1d00ffff",
                  "difficulty":1,
                  "chainwork":"%s",
                  "size":321,
                  "strippedsize":280,
                  "weight":1161,
                  "previousblockhash":"%s",
                  "nTx":2,
                  "tx":[
                    {
                      "txid":"%s","hash":"%s","version":2,"size":190,
                      "vsize":109,"weight":436,"locktime":0,
                      "vin":[{"coinbase":"00"}],"vout":[{"n":0},{"n":1}]
                    },
                    {
                      "txid":"%s","hash":"%s","version":2,"size":131,
                      "vsize":92,"weight":365,"locktime":143837,
                      "vin":[{"txid":"%s","vout":0}],"vout":[{"n":0}]
                    }
                  ]
                }
                """.formatted(
                blockHash,
                "2".repeat(64),
                "3".repeat(64),
                "4".repeat(64),
                "a".repeat(64),
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64),
                "d".repeat(64)
        )));

        BlockInfoVO result = service.getBlockByHeight(143838, 1);

        assertEquals(143838, result.height());
        assertEquals(2, result.transactionCount());
        assertEquals(1, result.transactionsReturned());
        assertTrue(result.transactionsTruncated());
        assertTrue(result.transactions().get(0).isCoinbase());
        assertEquals(2, result.transactions().get(0).outputCount());
    }

    @Test
    void mapsMissingBlockToNotFound() {
        when(rpcClient.call("getblockhash", List.of(999999999L))).thenThrow(
                new BitcoinRpcException(HttpStatus.BAD_GATEWAY, "Block height out of range", -8)
        );

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getBlockByHeight(999999999L, 20)
        );

        assertEquals("block at height 999999999 not found", exception.getMessage());
    }

    private JsonNode json(String value) throws JsonProcessingException {
        return objectMapper.readTree(value);
    }
}

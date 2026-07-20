package com.bzj.chainsentinel.client;

import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BitcoinCoreRpcClientTest {

    private MockRestServiceServer server;
    private BitcoinCoreRpcClient client;

    @BeforeEach
    void setUp() {
        BitcoinProperties properties = new BitcoinProperties();
        properties.setRpcUser("rpc-user");
        properties.setRpcPassword("rpc-password");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://127.0.0.1:18443")
                .defaultHeaders(headers -> headers.setBasicAuth("rpc-user", "rpc-password"));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BitcoinCoreRpcClient(builder.build(), properties);
    }

    @Test
    void sendsJsonRpcRequestAndReturnsResult() {
        server.expect(requestTo("http://127.0.0.1:18443/"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic cnBjLXVzZXI6cnBjLXBhc3N3b3Jk"))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("chainsentinel"))
                .andExpect(jsonPath("$.method").value("getblockchaininfo"))
                .andExpect(jsonPath("$.params").isArray())
                .andRespond(withSuccess(
                        """
                        {"jsonrpc":"2.0","result":{"chain":"testnet4","blocks":143838},"id":"chainsentinel"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        JsonNode result = client.call("getblockchaininfo");

        assertEquals("testnet4", result.get("chain").textValue());
        assertEquals(143838, result.get("blocks").intValue());
        server.verify();
    }

    @Test
    void mapsAuthenticationFailure() {
        server.expect(requestTo("http://127.0.0.1:18443/"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        BitcoinRpcException exception = assertThrows(
                BitcoinRpcException.class,
                () -> client.call("getblockchaininfo")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("bitcoin rpc authentication failed", exception.getMessage());
    }

    @Test
    void mapsJsonRpcError() {
        server.expect(requestTo("http://127.0.0.1:18443/"))
                .andRespond(withSuccess(
                        """
                        {"jsonrpc":"2.0","result":null,"error":{"code":-32601,"message":"Method not found"},"id":"chainsentinel"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        BitcoinRpcException exception = assertThrows(
                BitcoinRpcException.class,
                () -> client.call("missingmethod")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals(-32601, exception.getRpcCode());
    }
}

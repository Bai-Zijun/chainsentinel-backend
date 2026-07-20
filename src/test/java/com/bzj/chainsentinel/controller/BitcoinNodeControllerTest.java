package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.bzj.chainsentinel.exception.GlobalExceptionHandler;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.node.MempoolInfoVO;
import com.bzj.chainsentinel.vo.node.NetworkInfoVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BitcoinNodeController.class)
@Import(GlobalExceptionHandler.class)
class BitcoinNodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BitcoinNodeService bitcoinNodeService;

    @Test
    void returnsBlockchainInfo() throws Exception {
        BlockchainInfoVO info = new BlockchainInfoVO(
                "testnet4",
                143838,
                143838,
                "0".repeat(64),
                BigDecimal.ONE,
                BigDecimal.ONE,
                false,
                13166803162L,
                false,
                List.of()
        );
        when(bitcoinNodeService.getBlockchainInfo()).thenReturn(info);

        mockMvc.perform(get("/api/node/blockchain"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chain").value("testnet4"))
                .andExpect(jsonPath("$.data.blocks").value(143838))
                .andExpect(jsonPath("$.data.bestBlockHash").value("0".repeat(64)));
    }

    @Test
    void returnsNetworkAndMempoolInfo() throws Exception {
        when(bitcoinNodeService.getNetworkInfo()).thenReturn(new NetworkInfoVO(
                300200, "/Satoshi:30.2.0/", 70016, true,
                10, 0, 10, new BigDecimal("0.000001"),
                new BigDecimal("0.000001"), List.of()
        ));
        when(bitcoinNodeService.getMempoolInfo()).thenReturn(new MempoolInfoVO(
                true, 43, 24371, 131504, new BigDecimal("0.00033660"),
                300000000, new BigDecimal("0.000001"), new BigDecimal("0.000001"),
                new BigDecimal("0.000001"), 0, true
        ));

        mockMvc.perform(get("/api/node/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.protocolVersion").value(70016))
                .andExpect(jsonPath("$.data.networkActive").value(true));

        mockMvc.perform(get("/api/node/mempool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(43))
                .andExpect(jsonPath("$.data.fullRbf").value(true));
    }

    @Test
    void mapsRpcFailureToHttpStatus() throws Exception {
        when(bitcoinNodeService.getBlockchainInfo()).thenThrow(new BitcoinRpcException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "bitcoin rpc service unavailable"
        ));

        mockMvc.perform(get("/api/node/blockchain"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("bitcoin rpc service unavailable"));
    }
}

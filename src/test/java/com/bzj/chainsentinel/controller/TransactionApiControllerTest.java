package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.entity.Transaction;
import com.bzj.chainsentinel.exception.GlobalExceptionHandler;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.service.TransactionFeatureService;
import com.bzj.chainsentinel.service.TransactionService;
import com.bzj.chainsentinel.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        TransactionController.class,
        TransactionFeatureController.class,
        AnomalyResultController.class
})
@Import(GlobalExceptionHandler.class)
class TransactionApiControllerTest {

    private static final String TX_HASH = "ea5c23e6268e1eb09187f91e47106ca7a43e068452d3ee089b282b1d2fe12e67";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionFeatureService transactionFeatureService;

    @MockitoBean
    private AnomalyResultService anomalyResultService;

    @Test
    void getTransactionReturnsSuccess() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setTxHash(TX_HASH);
        when(transactionService.getByTxHash(TX_HASH)).thenReturn(transaction);

        mockMvc.perform(get("/api/transactions/{txHash}", TX_HASH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.txHash").value(TX_HASH));
    }

    @Test
    void getTransactionReturnsRealHttp404WhenMissing() throws Exception {
        when(transactionService.getByTxHash(TX_HASH)).thenReturn(null);

        mockMvc.perform(get("/api/transactions/{txHash}", TX_HASH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("transaction not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getTransactionRejectsInvalidHashBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/transactions/{txHash}", "not-a-transaction-hash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message")
                        .value("txHash must be a 64-character hexadecimal string"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void getRiskReturnsRealHttp404WhenMissing() throws Exception {
        when(anomalyResultService.getByTxHash(TX_HASH)).thenReturn(null);

        mockMvc.perform(get("/api/transactions/{txHash}/risk", TX_HASH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("anomaly result not found"));
    }

    @Test
    void pageHighRiskValidatesPageAndSize() throws Exception {
        mockMvc.perform(get("/api/transactions/risk/high")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(anomalyResultService);
    }

    @Test
    void pageHighRiskReturnsPage() throws Exception {
        AnomalyResult result = new AnomalyResult();
        result.setTxHash(TX_HASH);
        result.setRiskLevel("HIGH");
        PageResult<AnomalyResult> pageResult = new PageResult<>(List.of(result), 1L, 1L, 10L);
        when(anomalyResultService.pageHighRisk(1, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/api/transactions/risk/high")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].riskLevel").value("HIGH"));
    }
}

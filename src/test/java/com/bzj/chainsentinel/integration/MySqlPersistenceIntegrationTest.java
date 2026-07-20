package com.bzj.chainsentinel.integration;

import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.entity.Transaction;
import com.bzj.chainsentinel.mapper.AnomalyResultMapper;
import com.bzj.chainsentinel.mapper.TransactionMapper;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.service.TransactionService;
import com.bzj.chainsentinel.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class MySqlPersistenceIntegrationTest {

    private static final String TX_HASH = "ea5c23e6268e1eb09187f91e47106ca7a43e068452d3ee089b282b1d2fe12e67";

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private AnomalyResultMapper anomalyResultMapper;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnomalyResultService anomalyResultService;

    @Test
    void flywaySchemaSupportsQueriesAndRejectsDuplicateTransactions() {
        Transaction transaction = new Transaction();
        transaction.setTxHash(TX_HASH);
        transactionMapper.insert(transaction);

        AnomalyResult anomalyResult = new AnomalyResult();
        anomalyResult.setTxHash(TX_HASH);
        anomalyResult.setAnomalyScore(new BigDecimal("0.95000000"));
        anomalyResult.setRiskLevel("HIGH");
        anomalyResult.setModelName("IsolationForest");
        anomalyResult.setModelVersion("iforest_v1");
        anomalyResultMapper.insert(anomalyResult);

        Transaction loaded = transactionService.getByTxHash(TX_HASH);
        PageResult<AnomalyResult> highRisk = anomalyResultService.pageHighRisk(1, 10);

        assertNotNull(loaded);
        assertEquals(TX_HASH, loaded.getTxHash());
        assertEquals(1, highRisk.getTotal());
        assertEquals(TX_HASH, highRisk.getRecords().get(0).getTxHash());

        Transaction duplicate = new Transaction();
        duplicate.setTxHash(TX_HASH);
        assertThrows(RuntimeException.class, () -> transactionMapper.insert(duplicate));
    }
}

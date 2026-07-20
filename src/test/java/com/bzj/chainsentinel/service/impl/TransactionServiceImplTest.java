package com.bzj.chainsentinel.service.impl;

import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.entity.Transaction;
import com.bzj.chainsentinel.entity.TransactionFeature;
import com.bzj.chainsentinel.mapper.TransactionMapper;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.service.TransactionFeatureService;
import com.bzj.chainsentinel.vo.TransactionAnalysisVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final String TX_HASH = "ea5c23e6268e1eb09187f91e47106ca7a43e068452d3ee089b282b1d2fe12e67";

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionFeatureService transactionFeatureService;

    @Mock
    private AnomalyResultService anomalyResultService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void getAnalysisCombinesTransactionFeatureAndRisk() {
        Transaction transaction = new Transaction();
        transaction.setTxHash(TX_HASH);
        TransactionFeature feature = new TransactionFeature();
        feature.setTxHash(TX_HASH);
        AnomalyResult risk = new AnomalyResult();
        risk.setTxHash(TX_HASH);

        when(transactionMapper.selectOne(any())).thenReturn(transaction);
        when(transactionFeatureService.getByTxHash(TX_HASH)).thenReturn(feature);
        when(anomalyResultService.getByTxHash(TX_HASH)).thenReturn(risk);

        TransactionAnalysisVO analysis = transactionService.getAnalysisByTxHash(TX_HASH);

        assertSame(transaction, analysis.getTransaction());
        assertSame(feature, analysis.getFeatures());
        assertSame(risk, analysis.getRisk());
        verify(transactionFeatureService).getByTxHash(TX_HASH);
        verify(anomalyResultService).getByTxHash(TX_HASH);
    }

    @Test
    void getAnalysisStopsWhenTransactionDoesNotExist() {
        when(transactionMapper.selectOne(any())).thenReturn(null);

        TransactionAnalysisVO analysis = transactionService.getAnalysisByTxHash(TX_HASH);

        assertNull(analysis);
        verifyNoInteractions(transactionFeatureService, anomalyResultService);
    }
}

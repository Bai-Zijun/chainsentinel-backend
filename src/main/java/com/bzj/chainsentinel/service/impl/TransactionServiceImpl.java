package com.bzj.chainsentinel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.entity.Transaction;
import com.bzj.chainsentinel.entity.TransactionFeature;
import com.bzj.chainsentinel.mapper.TransactionMapper;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.service.TransactionFeatureService;
import com.bzj.chainsentinel.service.TransactionService;
import com.bzj.chainsentinel.vo.TransactionAnalysisVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final TransactionFeatureService transactionFeatureService;
    private final AnomalyResultService anomalyResultService;

    @Override
    public Transaction getByTxHash(String txHash) {
        LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Transaction::getTxHash, txHash);
        return transactionMapper.selectOne(wrapper);
    }

    @Override
    public TransactionAnalysisVO getAnalysisByTxHash(String txHash) {
        Transaction transaction = this.getByTxHash(txHash);

        if (transaction == null) {
            return null;
        }

        TransactionFeature features = transactionFeatureService.getByTxHash(txHash);
        AnomalyResult risk = anomalyResultService.getByTxHash(txHash);

        return new TransactionAnalysisVO(transaction, features, risk);
    }
}
package com.bzj.chainsentinel.service;

import com.bzj.chainsentinel.entity.Transaction;

import com.bzj.chainsentinel.vo.TransactionAnalysisVO;

public interface TransactionService {
    Transaction getByTxHash(String txHash);

    TransactionAnalysisVO getAnalysisByTxHash(String txHash);
}

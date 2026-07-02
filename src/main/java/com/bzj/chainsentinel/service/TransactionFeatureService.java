package com.bzj.chainsentinel.service;

import com.bzj.chainsentinel.entity.TransactionFeature;

public interface TransactionFeatureService {

    TransactionFeature getByTxHash(String txhash);
}

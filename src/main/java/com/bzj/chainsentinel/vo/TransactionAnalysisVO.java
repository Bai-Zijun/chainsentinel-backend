package com.bzj.chainsentinel.vo;

import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.entity.Transaction;
import com.bzj.chainsentinel.entity.TransactionFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionAnalysisVO {

    private Transaction transaction;

    private TransactionFeature features;

    private AnomalyResult risk;
}

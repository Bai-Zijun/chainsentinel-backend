package com.bzj.chainsentinel.service;

import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.node.BlockInfoVO;
import com.bzj.chainsentinel.vo.node.MempoolInfoVO;
import com.bzj.chainsentinel.vo.node.NetworkInfoVO;

public interface BitcoinNodeService {

    BlockchainInfoVO getBlockchainInfo();

    NetworkInfoVO getNetworkInfo();

    MempoolInfoVO getMempoolInfo();

    BlockInfoVO getBlockByHeight(long height, int transactionLimit);
}

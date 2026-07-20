package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.node.MempoolInfoVO;
import com.bzj.chainsentinel.vo.node.NetworkInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/node")
@RequiredArgsConstructor
public class BitcoinNodeController {

    private final BitcoinNodeService bitcoinNodeService;

    @GetMapping("/blockchain")
    public ApiResponse<BlockchainInfoVO> getBlockchainInfo() {
        return ApiResponse.success(bitcoinNodeService.getBlockchainInfo());
    }

    @GetMapping("/network")
    public ApiResponse<NetworkInfoVO> getNetworkInfo() {
        return ApiResponse.success(bitcoinNodeService.getNetworkInfo());
    }

    @GetMapping("/mempool")
    public ApiResponse<MempoolInfoVO> getMempoolInfo() {
        return ApiResponse.success(bitcoinNodeService.getMempoolInfo());
    }
}

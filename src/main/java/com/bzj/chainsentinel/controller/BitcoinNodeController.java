package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.vo.node.BlockInfoVO;
import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.node.MempoolInfoVO;
import com.bzj.chainsentinel.vo.node.NetworkInfoVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/node")
@RequiredArgsConstructor
@Validated
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

    @GetMapping("/blocks/{height}")
    public ApiResponse<BlockInfoVO> getBlockByHeight(
            @PathVariable @Min(value = 0, message = "height must be greater than or equal to 0") long height,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "txLimit must be between 1 and 100")
            @Max(value = 100, message = "txLimit must be between 1 and 100") int txLimit
    ) {
        return ApiResponse.success(bitcoinNodeService.getBlockByHeight(height, txLimit));
    }
}

package com.bzj.chainsentinel.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "bitcoin")
public class BitcoinProperties {

    @NotBlank
    private String rpcUrl = "http://127.0.0.1:18443";
    private String rpcUser = "";
    private String rpcPassword = "";

    @Min(1)
    @Max(120)
    private int rpcTimeout = 10;

    @NotBlank
    private String network = "testnet4";
}

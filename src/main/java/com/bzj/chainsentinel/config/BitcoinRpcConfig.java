package com.bzj.chainsentinel.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(BitcoinProperties.class)
public class BitcoinRpcConfig {

    @Bean
    @Qualifier("bitcoinRestClient")
    public RestClient bitcoinRestClient(RestClient.Builder builder, BitcoinProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getRpcTimeout());
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder clientBuilder = builder
                .baseUrl(properties.getRpcUrl())
                .requestFactory(requestFactory);

        if (StringUtils.hasText(properties.getRpcUser())
                && StringUtils.hasText(properties.getRpcPassword())) {
            clientBuilder.defaultHeaders(headers -> headers.setBasicAuth(
                    properties.getRpcUser(),
                    properties.getRpcPassword()
            ));
        }

        return clientBuilder.build();
    }
}

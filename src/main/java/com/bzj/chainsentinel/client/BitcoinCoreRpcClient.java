package com.bzj.chainsentinel.client;

import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpTimeoutException;
import java.util.List;

@Component
public class BitcoinCoreRpcClient {

    private final RestClient restClient;
    private final BitcoinProperties properties;

    public BitcoinCoreRpcClient(
            @Qualifier("bitcoinRestClient") RestClient restClient,
            BitcoinProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public JsonNode call(String method) {
        return call(method, List.of());
    }

    public JsonNode call(String method, List<?> params) {
        validateCredentials();
        RpcRequest request = new RpcRequest("2.0", "chainsentinel", method, params);

        try {
            JsonNode payload = restClient.post()
                    .uri("/")
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            return extractResult(payload);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                throw new BitcoinRpcException(
                        HttpStatus.BAD_GATEWAY,
                        "bitcoin rpc authentication failed",
                        exception
                );
            }
            throw new BitcoinRpcException(
                    HttpStatus.BAD_GATEWAY,
                    "bitcoin rpc returned HTTP " + exception.getStatusCode().value(),
                    exception
            );
        } catch (ResourceAccessException exception) {
            if (hasTimeoutCause(exception)) {
                throw new BitcoinRpcException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "bitcoin rpc request timed out",
                        exception
                );
            }
            throw new BitcoinRpcException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "bitcoin rpc service unavailable",
                    exception
            );
        } catch (RestClientException exception) {
            throw new BitcoinRpcException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "bitcoin rpc service unavailable",
                    exception
            );
        }
    }

    private JsonNode extractResult(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new BitcoinRpcException(HttpStatus.BAD_GATEWAY, "bitcoin rpc returned invalid JSON");
        }

        JsonNode error = payload.get("error");
        if (error != null && !error.isNull()) {
            int code = error.path("code").asInt();
            String message = error.path("message").asText("unknown rpc error");
            throw new BitcoinRpcException(
                    HttpStatus.BAD_GATEWAY,
                    "bitcoin rpc error " + code + ": " + message,
                    code
            );
        }

        if (!payload.has("result")) {
            throw new BitcoinRpcException(HttpStatus.BAD_GATEWAY, "bitcoin rpc response has no result");
        }
        return payload.get("result");
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(properties.getRpcUser())
                || !StringUtils.hasText(properties.getRpcPassword())) {
            throw new BitcoinRpcException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "bitcoin rpc credentials are not configured"
            );
        }
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException
                    || current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record RpcRequest(String jsonrpc, String id, String method, List<?> params) {
    }
}

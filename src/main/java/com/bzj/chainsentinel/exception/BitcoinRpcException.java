package com.bzj.chainsentinel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BitcoinRpcException extends RuntimeException {

    private final HttpStatus status;
    private final Integer rpcCode;

    public BitcoinRpcException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public BitcoinRpcException(HttpStatus status, String message, Integer rpcCode) {
        this(status, message, rpcCode, null);
    }

    public BitcoinRpcException(HttpStatus status, String message, Throwable cause) {
        this(status, message, null, cause);
    }

    private BitcoinRpcException(HttpStatus status, String message, Integer rpcCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.rpcCode = rpcCode;
    }
}

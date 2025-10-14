package com.hritik.Sharded_Saga_Wallet_System.exceptions;

public class SagaException extends RuntimeException {
    public SagaException(String message) {
        super(message);
    }

    public SagaException(String message, Throwable cause) {
        super(message, cause);
    }
}

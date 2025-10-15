package com.hritik.Sharded_Saga_Wallet_System.exceptions;

public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
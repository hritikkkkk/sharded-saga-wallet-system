package com.hritik.Sharded_Saga_Wallet_System.exceptions;

public class WalletException extends RuntimeException {
    public WalletException(String message) {
        super(message);
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }
}
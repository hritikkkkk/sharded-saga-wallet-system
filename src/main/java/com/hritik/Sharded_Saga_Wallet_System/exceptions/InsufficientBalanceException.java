package com.hritik.Sharded_Saga_Wallet_System.exceptions;


public class InsufficientBalanceException extends WalletException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
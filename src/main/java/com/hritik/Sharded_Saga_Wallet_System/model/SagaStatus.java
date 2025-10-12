package com.hritik.Sharded_Saga_Wallet_System.model;

public enum SagaStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
}

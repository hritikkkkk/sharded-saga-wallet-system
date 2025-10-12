package com.hritik.Sharded_Saga_Wallet_System.model;
public enum StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    SKIPPED,
}
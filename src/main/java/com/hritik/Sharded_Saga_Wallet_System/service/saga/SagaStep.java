package com.hritik.Sharded_Saga_Wallet_System.service.saga;

public interface SagaStep {
    boolean execute(SagaContext sagaContext);

    boolean compensate(SagaContext sagaContext);

    String getStepName();
}

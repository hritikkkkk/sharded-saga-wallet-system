package com.hritik.Sharded_Saga_Wallet_System.service.saga.steps;

import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaStepInterface;

import java.util.Map;

public class SagaStepFactory {
    private Map<String, SagaStepInterface> sagaStepMap;

    public SagaStepInterface getStepName(String stepName) {
        return sagaStepMap.get(stepName);
    }
}

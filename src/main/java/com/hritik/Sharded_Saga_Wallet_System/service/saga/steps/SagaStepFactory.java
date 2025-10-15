package com.hritik.Sharded_Saga_Wallet_System.service.saga.steps;

import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SagaStepFactory {
    private final Map<String, SagaStepInterface> sagaStepMap;

    public static final List<SagaStepType> TransferMoneySagaSteps = List.of(
            SagaStepType.DEBIT_SOURCE_WALLET_STEP,
            SagaStepType.CREDIT_DESTINATION_WALLET_STEP,
            SagaStepType.UPDATE_TRANSACTION_STATUS_STEP
    );


    public SagaStepInterface getStepName(String stepName) {
        return sagaStepMap.get(stepName);
    }
}

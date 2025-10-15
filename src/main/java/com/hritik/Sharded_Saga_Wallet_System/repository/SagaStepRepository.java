package com.hritik.Sharded_Saga_Wallet_System.repository;

import com.hritik.Sharded_Saga_Wallet_System.model.SagaStep;
import com.hritik.Sharded_Saga_Wallet_System.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {


    Optional<SagaStep> findBySagaInstanceIdAndStepNameAndStatus(Long sagaInstanceId, String stepName, StepStatus status);
}

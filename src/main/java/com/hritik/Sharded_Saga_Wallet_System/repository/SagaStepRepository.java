package com.hritik.Sharded_Saga_Wallet_System.repository;

import com.hritik.Sharded_Saga_Wallet_System.model.SagaStep;
import com.hritik.Sharded_Saga_Wallet_System.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {


    Optional<SagaStep> findBySagaInstanceIdAndStepNameAndStatus(Long sagaInstanceId, String stepName, StepStatus status);

    List<SagaStep> findCompletedStepsBySagaInstanceId(Long sagaInstanceId);
}

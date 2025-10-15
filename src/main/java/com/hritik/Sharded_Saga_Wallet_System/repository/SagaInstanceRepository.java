package com.hritik.Sharded_Saga_Wallet_System.repository;

import com.hritik.Sharded_Saga_Wallet_System.model.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, Long> {
}

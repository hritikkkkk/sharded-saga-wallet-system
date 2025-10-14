package com.hritik.Sharded_Saga_Wallet_System.service.saga.steps;

import com.hritik.Sharded_Saga_Wallet_System.exceptions.ResourceNotFoundException;
import com.hritik.Sharded_Saga_Wallet_System.model.Transaction;
import com.hritik.Sharded_Saga_Wallet_System.model.TransactionStatus;
import com.hritik.Sharded_Saga_Wallet_System.repository.TransactionRepository;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaContext;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaStepInterface;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTransactionStatus implements SagaStepInterface {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        try {
            Long transactionId = context.getLong("transactionId");

            if (transactionId == null) {
                log.error("Missing required context: transactionId");
                return false;
            }

            log.info("Updating transaction status to SUCCESS for transaction {}", transactionId);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Transaction not found with id: " + transactionId));

            // Store original status for compensation
            context.put("originalTransactionStatus", transaction.getStatus().name());

            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            log.info("Transaction {} status updated to SUCCESS", transactionId);
            context.put("transactionStatusAfterUpdate", transaction.getStatus().name());

            return true;

        } catch (Exception e) {
            log.error("Error updating transaction status", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        try {
            Long transactionId = context.getLong("transactionId");
            String originalStatusStr = context.getString("originalTransactionStatus");

            if (transactionId == null) {
                log.error("Missing required context for compensation: transactionId");
                return false;
            }

            log.info("Compensating transaction status for transaction {}", transactionId);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Transaction not found with id: " + transactionId));

            // Set to CANCELLED instead of original status for better tracking
            TransactionStatus compensationStatus = TransactionStatus.CANCELLED;

            if (originalStatusStr != null) {
                try {
                    TransactionStatus originalStatus = TransactionStatus.valueOf(originalStatusStr);
                    log.info("Original status was {}, setting to CANCELLED", originalStatus);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid original status: {}", originalStatusStr);
                }
            }

            transaction.setStatus(compensationStatus);
            transactionRepository.save(transaction);

            log.info("Transaction {} status compensated to {}", transactionId, compensationStatus);

            return true;

        } catch (Exception e) {
            log.error("Error compensating transaction status", e);
            return false;
        }
    }

    @Override
    public String getStepName() {
        return SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString();
    }

}

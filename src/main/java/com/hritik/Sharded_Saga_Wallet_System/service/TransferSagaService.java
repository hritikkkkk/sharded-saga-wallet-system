package com.hritik.Sharded_Saga_Wallet_System.service;


import com.hritik.Sharded_Saga_Wallet_System.exceptions.InsufficientBalanceException;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.InvalidTransactionException;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.SagaException;
import com.hritik.Sharded_Saga_Wallet_System.model.Transaction;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaContext;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaOrchestrator;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.steps.SagaStepFactory;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.steps.SagaStepType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferSagaService {

    private final TransactionService transactionService;
    private final SagaOrchestrator sagaOrchestrator;
    private final WalletService walletService;

    @Transactional
    public Long initiateTransfer(Long fromWalletId, Long toWalletId,
                                 BigDecimal amount, String description) {
        System.out.println("**********transferrring***********");
        log.info("Initiating transfer from wallet {} to wallet {} with amount {} and description '{}'",
                fromWalletId, toWalletId, amount, description);

        try {
            // Validate inputs
            validateTransferRequest(fromWalletId, toWalletId, amount);

            // Pre-validate wallets exist and are active
            validateWallets(fromWalletId, toWalletId);

            // Create transaction record
            Transaction transaction = transactionService.createTransaction(
                    fromWalletId, toWalletId, amount, description);

            // Create saga context with all necessary data
            SagaContext sagaContext = SagaContext.builder()
                    .data(Map.of(
                            "transactionId", transaction.getId(),
                            "fromWalletId", fromWalletId,
                            "toWalletId", toWalletId,
                            "amount", amount,
                            "description", description != null ? description : ""
                    ))
                    .build();

            log.debug("Saga context created for transaction {}", transaction.getId());

            // Start the saga
            Long sagaInstanceId = sagaOrchestrator.startSaga(sagaContext);
            log.info("Saga instance {} created for transaction {}", sagaInstanceId, transaction.getId());

            // Link transaction to saga
            transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

            // Execute the saga asynchronously (or synchronously based on requirements)
            executeTransferSaga(sagaInstanceId);

            return sagaInstanceId;

        } catch (InvalidTransactionException | IllegalArgumentException e) {
            log.error("Validation failed for transfer request", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error initiating transfer", e);
            throw new SagaException("Failed to initiate transfer", e);
        }
    }

    @Transactional
    public void executeTransferSaga(Long sagaInstanceId) {
        log.info("Executing transfer saga {}", sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        try {
            // Execute all steps in the transfer saga
            for (SagaStepType step : SagaStepFactory.TransferMoneySagaSteps) {
                log.info("Executing step {} for saga {}", step, sagaInstanceId);

                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step.toString());

                if (!success) {
                    log.error("Step {} failed for saga {}, initiating rollback", step, sagaInstanceId);
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    throw new SagaException("Transfer saga failed at step: " + step);
                }

                log.info("Step {} completed successfully for saga {}", step, sagaInstanceId);
            }

            // All steps completed successfully
            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga {} completed successfully", sagaInstanceId);

        } catch (InsufficientBalanceException e) {
            // Business exception - clean up and re-throw
            log.error("Insufficient balance in saga {} at step {}", sagaInstanceId, e);
            try {
                sagaOrchestrator.failSaga(sagaInstanceId);
            } catch (Exception failEx) {
                log.error("Failed to mark saga as failed", failEx);
            }
            throw e; // Re-throw for proper error response

        }

        catch (SagaException e) {
            log.error("Saga exception occurred during execution of saga {}", sagaInstanceId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error executing saga {}", sagaInstanceId, e);

            try {
                sagaOrchestrator.failSaga(sagaInstanceId);
            } catch (Exception failException) {
                log.error("Failed to properly fail saga {}", sagaInstanceId, failException);
            }

            throw new SagaException("Failed to execute transfer saga", e);
        }
    }

    private void validateTransferRequest(Long fromWalletId, Long toWalletId, BigDecimal amount) {
        if (fromWalletId == null) {
            throw new InvalidTransactionException("Source wallet ID cannot be null");
        }

        if (toWalletId == null) {
            throw new InvalidTransactionException("Destination wallet ID cannot be null");
        }

        if (fromWalletId.equals(toWalletId)) {
            throw new InvalidTransactionException(
                    "Cannot transfer to the same wallet. Source and destination must be different");
        }

        if (amount == null) {
            throw new InvalidTransactionException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }

        // Optional: Add maximum transaction limit
        BigDecimal maxTransactionAmount = new BigDecimal("1000000");
        if (amount.compareTo(maxTransactionAmount) > 0) {
            throw new InvalidTransactionException(
                    "Transaction amount exceeds maximum limit of " + maxTransactionAmount);
        }
    }

    private void validateWallets(Long fromWalletId, Long toWalletId) {
        // Validate source wallet exists and is active
        var sourceWallet = walletService.getWalletById(fromWalletId);
        if (!sourceWallet.getIsActive()) {
            throw new InvalidTransactionException(
                    "Source wallet " + fromWalletId + " is not active");
        }

        // Validate destination wallet exists and is active
        var destWallet = walletService.getWalletById(toWalletId);
        if (!destWallet.getIsActive()) {
            throw new InvalidTransactionException(
                    "Destination wallet " + toWalletId + " is not active");
        }
    }
}
package com.hritik.Sharded_Saga_Wallet_System.service;


import com.hritik.Sharded_Saga_Wallet_System.exceptions.ResourceNotFoundException;
import com.hritik.Sharded_Saga_Wallet_System.model.Transaction;
import com.hritik.Sharded_Saga_Wallet_System.model.TransactionStatus;
import com.hritik.Sharded_Saga_Wallet_System.model.TransactionType;
import com.hritik.Sharded_Saga_Wallet_System.repository.TransactionRepository;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.InvalidTransactionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    @Transactional
    public Transaction createTransaction(Long fromWalletId, Long toWalletId,
                                         BigDecimal amount, String description) {
        log.info("Creating transaction from wallet {} to wallet {} with amount {} and description '{}'",
                fromWalletId, toWalletId, amount, description);

        validateTransactionRequest(fromWalletId, toWalletId, amount);

        try {
            // Verify both wallets exist
            walletService.getWalletById(fromWalletId);
            walletService.getWalletById(toWalletId);

            Transaction transaction = Transaction.builder()
                    .fromWalletId(fromWalletId)
                    .toWalletId(toWalletId)
                    .amount(amount)
                    .description(description != null ? description : "")
                    .status(TransactionStatus.PENDING)
                    .type(TransactionType.TRANSFER)
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Transaction created successfully with id {}", savedTransaction.getId());
            return savedTransaction;

        } catch (DataAccessException e) {
            log.error("Database error while creating transaction", e);
            throw new InvalidTransactionException("Failed to create transaction due to database error");
        }
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        log.debug("Fetching transaction with id {}", id);

        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }

        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByWalletId(Long walletId) {
        log.debug("Fetching transactions for wallet {}", walletId);

        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        return transactionRepository.findByWalletId(walletId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByFromWalletId(Long fromWalletId) {
        log.debug("Fetching debit transactions from wallet {}", fromWalletId);

        if (fromWalletId == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        return transactionRepository.findByFromWalletId(fromWalletId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByToWalletId(Long toWalletId) {
        log.debug("Fetching credit transactions to wallet {}", toWalletId);

        if (toWalletId == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        return transactionRepository.findByToWalletId(toWalletId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsBySagaInstanceId(Long sagaInstanceId) {
        log.debug("Fetching transactions for saga instance {}", sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        return transactionRepository.findBySagaInstanceId(sagaInstanceId);
    }

    @Transactional
    public void updateTransactionWithSagaInstanceId(Long transactionId, Long sagaInstanceId) {
        log.info("Updating transaction {} with saga instance id {}", transactionId, sagaInstanceId);

        if (transactionId == null || sagaInstanceId == null) {
            throw new IllegalArgumentException("Transaction ID and Saga instance ID cannot be null");
        }

        try {
            Transaction transaction = getTransactionById(transactionId);
            transaction.setSagaInstanceId(sagaInstanceId);
            transactionRepository.save(transaction);

            log.info("Transaction {} updated successfully with saga instance id {}",
                    transactionId, sagaInstanceId);

        } catch (DataAccessException e) {
            log.error("Database error while updating transaction {} with saga instance",
                    transactionId, e);
            throw new InvalidTransactionException(
                    "Failed to update transaction with saga instance due to database error");
        }
    }

    @Transactional
    public void updateTransactionStatus(Long transactionId, TransactionStatus status) {
        log.info("Updating transaction {} status to {}", transactionId, status);

        if (transactionId == null || status == null) {
            throw new IllegalArgumentException("Transaction ID and status cannot be null");
        }

        try {
            Transaction transaction = getTransactionById(transactionId);
            transaction.setStatus(status);
            transactionRepository.save(transaction);

            log.info("Transaction {} status updated successfully to {}", transactionId, status);

        } catch (DataAccessException e) {
            log.error("Database error while updating transaction {} status", transactionId, e);
            throw new com.hritik.Sharded_Saga_Wallet_System.exceptions.InvalidTransactionException(
                    "Failed to update transaction status due to database error");
        }
    }

    private void validateTransactionRequest(Long fromWalletId, Long toWalletId, BigDecimal amount) {
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
}


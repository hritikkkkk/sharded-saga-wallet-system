package com.hritik.Sharded_Saga_Wallet_System.service.saga.steps;

import com.hritik.Sharded_Saga_Wallet_System.exceptions.InsufficientBalanceException;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.ResourceNotFoundException;
import com.hritik.Sharded_Saga_Wallet_System.model.Wallet;
import com.hritik.Sharded_Saga_Wallet_System.repository.WalletRepository;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaContext;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebitSourceWalletStep implements SagaStepInterface {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        try {
            Long fromWalletId = context.getLong("fromWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (fromWalletId == null || amount == null) {
                log.error("Missing required context: fromWalletId or amount");
                return false;
            }

            log.info("Debiting {} from wallet {}", amount, fromWalletId);

            Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Wallet not found with id: " + fromWalletId));

            if (!wallet.getIsActive()) {
                log.error("Wallet {} is not active", fromWalletId);
                return false;
            }

            BigDecimal currentBalance = wallet.getBalance();
            log.info("Current balance of wallet {}: {}", fromWalletId, currentBalance);

            // Store original balance for compensation
            context.put("originalSourceWalletBalance", currentBalance);

            if (currentBalance.compareTo(amount) < 0) {
                log.error("Insufficient balance in wallet {}. Available: {}, Required: {}",
                        fromWalletId, currentBalance, amount);
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance. Available: %s, Required: %s",
                                currentBalance, amount));
            }

            BigDecimal newBalance = currentBalance.subtract(amount);
            walletRepository.updateBalanceByUserId(fromWalletId, newBalance);

            log.info("Wallet {} debited successfully. New balance: {}", fromWalletId, newBalance);
            context.put("sourceWalletBalanceAfterDebit", newBalance);

            return true;

        } catch (InsufficientBalanceException e) {
            log.error("Insufficient balance for debit operation", e);
            return false;
        } catch (Exception e) {
            log.error("Error debiting source wallet", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        try {
            Long fromWalletId = context.getLong("fromWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (fromWalletId == null || amount == null) {
                log.error("Missing required context for compensation: fromWalletId or amount");
                return false;
            }

            log.info("Compensating debit: crediting {} back to wallet {}", amount, fromWalletId);

            Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Wallet not found with id: " + fromWalletId));

            BigDecimal currentBalance = wallet.getBalance();
            log.info("Current balance before compensation: {}", currentBalance);

            context.put("sourceWalletBalanceBeforeCreditCompensation", currentBalance);

            BigDecimal newBalance = currentBalance.add(amount);
            walletRepository.updateBalanceByUserId(fromWalletId, newBalance);

            log.info("Debit compensated successfully. Wallet {} new balance: {}",
                    fromWalletId, newBalance);
            context.put("sourceWalletBalanceAfterCreditCompensation", newBalance);

            return true;

        } catch (Exception e) {
            log.error("Error compensating source wallet debit", e);
            return false;
        }
    }

    @Override
    public String getStepName() {
        return SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString();
    }
}
package com.hritik.Sharded_Saga_Wallet_System.service.saga.steps;

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
public class CreditDestinationWalletStep implements SagaStepInterface {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        try {
            Long toWalletId = context.getLong("toWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (toWalletId == null || amount == null) {
                log.error("Missing required context: toWalletId or amount");
                return false;
            }

            log.info("Crediting {} to wallet {}", amount, toWalletId);

            Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Wallet not found with id: " + toWalletId));

            if (!wallet.getIsActive()) {
                log.error("Destination wallet {} is not active", toWalletId);
                return false;
            }

            BigDecimal currentBalance = wallet.getBalance();
            log.info("Current balance of wallet {}: {}", toWalletId, currentBalance);

            // Store original balance for compensation
            context.put("originalToWalletBalance", currentBalance);

            BigDecimal newBalance = currentBalance.add(amount);
            walletRepository.updateBalanceByUserId(toWalletId, newBalance);

            log.info("Wallet {} credited successfully. New balance: {}", toWalletId, newBalance);
            context.put("toWalletBalanceAfterCredit", newBalance);

            return true;

        } catch (Exception e) {
            log.error("Error crediting destination wallet", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        try {
            Long toWalletId = context.getLong("toWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (toWalletId == null || amount == null) {
                log.error("Missing required context for compensation: toWalletId or amount");
                return false;
            }

            log.info("Compensating credit: debiting {} from wallet {}", amount, toWalletId);

            Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Wallet not found with id: " + toWalletId));

            BigDecimal currentBalance = wallet.getBalance();
            log.info("Current balance before compensation: {}", currentBalance);

            if (currentBalance.compareTo(amount) < 0) {
                log.error("Cannot compensate: insufficient balance in destination wallet {}. " +
                        "Available: {}, Required: {}", toWalletId, currentBalance, amount);
                return false;
            }

            BigDecimal newBalance = currentBalance.subtract(amount);
            walletRepository.updateBalanceByUserId(toWalletId, newBalance);

            log.info("Credit compensated successfully. Wallet {} new balance: {}",
                    toWalletId, newBalance);
            context.put("toWalletBalanceAfterCreditCompensation", newBalance);

            return true;

        } catch (Exception e) {
            log.error("Error compensating destination wallet credit", e);
            return false;
        }
    }

    @Override
    public String getStepName() {
        return SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString();
    }
}
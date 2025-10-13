package com.hritik.Sharded_Saga_Wallet_System.service.saga.steps;

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
@Slf4j
@RequiredArgsConstructor
public class CreditDestinationWalletStepInterface implements SagaStepInterface {

    private final WalletRepository walletRepository;


    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Crediting destination wallet {} with amount {}", toWalletId, amount);


        Wallet wallet = walletRepository.findByIdWithLock(toWalletId).orElseThrow(() -> new RuntimeException("no wallet found"));
        log.info("Wallet fetched with balance {}", wallet.getBalance());
        context.put("toWalletBalanceBeforeCredit {} ", wallet.getBalance());
        wallet.credit(amount);

        walletRepository.save(wallet);
        log.info("wallet saved with balance {} ", wallet.getBalance());
        context.put("toWalletBalanceAfterCredit", wallet.getBalance());
        return true;

    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        // Step 1: Get the destination wallet id from the context
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Compensation credit of destination wallet {} with amount {}", toWalletId, amount);

        // Step 2: Fetch the destination wallet from the database with a lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        log.info("Wallet fetched with balance {}", wallet.getBalance());
        context.put("toWalletBalanceBeforeCreditCompensation",wallet.getBalance());
        // Step 3: Credit the destination wallet
        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("Wallet saved with balance {}", wallet.getBalance());
        context.put("toWalletBalanceAfterCreditCompensation", wallet.getBalance());

        log.info("Credit compensation of destination wallet step executed successfully");
        return true;
    }

    @Override
    public String getStepName() {
        return "CreditDestinationWalletStep";
    }
}

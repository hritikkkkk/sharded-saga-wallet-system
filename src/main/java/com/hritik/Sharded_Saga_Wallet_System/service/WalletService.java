package com.hritik.Sharded_Saga_Wallet_System.service;


import com.hritik.Sharded_Saga_Wallet_System.exceptions.InsufficientBalanceException;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.ResourceNotFoundException;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.WalletException;
import com.hritik.Sharded_Saga_Wallet_System.model.Wallet;
import com.hritik.Sharded_Saga_Wallet_System.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;

    @Transactional
    public Wallet createWallet(Long userId) {
        log.info("Creating wallet for user {}", userId);

        try {
            // Validate user exists
            userService.getUserById(userId);

            // Check if wallet already exists for user
            List<Wallet> existingWallets = walletRepository.findByUserId(userId);
            if (!existingWallets.isEmpty() && existingWallets.stream().anyMatch(Wallet::getIsActive)) {
                throw new WalletException("Active wallet already exists for user " + userId);
            }

            Wallet wallet = Wallet.builder()
                    .userId(userId)
                    .isActive(true)
                    .balance(BigDecimal.ZERO)
                    .build();

            wallet = walletRepository.save(wallet);
            log.info("Wallet created successfully with id {} for user {}", wallet.getId(), userId);
            return wallet;

        } catch (DataAccessException e) {
            log.error("Database error while creating wallet for user {}", userId, e);
            throw new WalletException("Failed to create wallet due to database error", e);
        }
    }

    @Transactional(readOnly = true)
    public Wallet getWalletById(Long id) {
        log.debug("Fetching wallet with id {}", id);

        if (id == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        return walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Wallet> getWalletsByUserId(Long userId) {
        log.debug("Fetching wallets for user {}", userId);

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return walletRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Wallet getActiveWalletByUserId(Long userId) {
        log.debug("Fetching active wallet for user {}", userId);

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        List<Wallet> wallets = walletRepository.findByUserId(userId);

        return wallets.stream()
                .filter(Wallet::getIsActive)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active wallet found for user: " + userId));
    }

    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        log.info("Debiting {} from user {}", amount, userId);

        validateAmount(amount);

        try {
            Wallet wallet = getActiveWalletByUserId(userId);

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance. Available: %s, Required: %s",
                                wallet.getBalance(), amount));
            }

            BigDecimal newBalance = wallet.getBalance().subtract(amount);
            walletRepository.updateBalanceByWalletId(wallet.getId(), newBalance);

            log.info("Successfully debited {} from user {}. New balance: {}",
                    amount, userId, newBalance);

        } catch (DataAccessException e) {
            log.error("Database error while debiting wallet for user {}", userId, e);
            throw new WalletException("Failed to debit wallet due to database error", e);
        }
    }

    @Transactional
    public void credit(Long userId, BigDecimal amount) {
        log.info("Crediting {} to user {}", amount, userId);

        validateAmount(amount);

        try {
            Wallet wallet = getActiveWalletByUserId(userId);

            BigDecimal newBalance = wallet.getBalance().add(amount);
            walletRepository.updateBalanceByWalletId(wallet.getId(), newBalance);

            log.info("Successfully credited {} to user {}. New balance: {}",
                    amount, userId, newBalance);

        } catch (DataAccessException e) {
            log.error("Database error while crediting wallet for user {}", userId, e);
            throw new WalletException("Failed to credit wallet due to database error", e);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(Long walletId) {
        log.debug("Fetching balance for wallet {}", walletId);

        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        Wallet wallet = getWalletById(walletId);
        return wallet.getBalance();
    }

    @Transactional
    public void deactivateWallet(Long walletId) {
        log.info("Deactivating wallet {}", walletId);

        try {
            Wallet wallet = getWalletById(walletId);
            wallet.setIsActive(false);
            walletRepository.save(wallet);

            log.info("Wallet {} deactivated successfully", walletId);

        } catch (DataAccessException e) {
            log.error("Database error while deactivating wallet {}", walletId, e);
            throw new WalletException("Failed to deactivate wallet due to database error", e);
        }
    }

    @Transactional
    public void activateWallet(Long walletId) {
        log.info("Activating wallet {}", walletId);

        try {
            Wallet wallet = getWalletById(walletId);
            wallet.setIsActive(true);
            walletRepository.save(wallet);

            log.info("Wallet {} activated successfully", walletId);

        } catch (DataAccessException e) {
            log.error("Database error while activating wallet {}", walletId, e);
            throw new WalletException("Failed to activating wallet due to database error", e);
        }
    }


    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
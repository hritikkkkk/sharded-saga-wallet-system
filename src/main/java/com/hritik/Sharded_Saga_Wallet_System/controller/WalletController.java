package com.hritik.Sharded_Saga_Wallet_System.controller;

import com.hritik.Sharded_Saga_Wallet_System.dto.CreateWalletRequestDTO;
import com.hritik.Sharded_Saga_Wallet_System.dto.CreditWalletRequestDTO;
import com.hritik.Sharded_Saga_Wallet_System.dto.DebitWalletRequestDTO;
import com.hritik.Sharded_Saga_Wallet_System.dto.WalletBalanceDTO;
import com.hritik.Sharded_Saga_Wallet_System.model.Wallet;
import com.hritik.Sharded_Saga_Wallet_System.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequestDTO request) {
        log.info("Creating wallet for user {}", request.getUserId());

        Wallet wallet = walletService.createWallet(request.getUserId());

        log.info("Wallet {} created successfully for user {}", wallet.getId(), request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long id) {
        log.info("Fetching wallet with id {}", id);

        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<WalletBalanceDTO> getWalletBalance(@PathVariable Long id) {
        log.info("Fetching balance for wallet {}", id);

        Wallet wallet = walletService.getWalletById(id);

        WalletBalanceDTO response = WalletBalanceDTO.builder()
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .isActive(wallet.getIsActive())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Wallet>> getWalletsByUserId(@PathVariable Long userId) {
        log.info("Fetching wallets for user {}", userId);

        List<Wallet> wallets = walletService.getWalletsByUserId(userId);
        return ResponseEntity.ok(wallets);
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<Wallet> debitWallet(
            @PathVariable Long userId,
            @Valid @RequestBody DebitWalletRequestDTO request) {

        log.info("Debiting {} from user {}", request.getAmount(), userId);

        walletService.debit(userId, request.getAmount());
        Wallet wallet = walletService.getActiveWalletByUserId(userId);

        log.info("Debit successful for user {}", userId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<Wallet> creditWallet(
            @PathVariable Long userId,
            @Valid @RequestBody CreditWalletRequestDTO request) {

        log.info("Crediting {} to user {}", request.getAmount(), userId);

        walletService.credit(userId, request.getAmount());
        Wallet wallet = walletService.getActiveWalletByUserId(userId);

        log.info("Credit successful for user {}", userId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("{userId}/activeWallet")
    public ResponseEntity<Wallet> getActiveWallet(@PathVariable Long userId) {
        Wallet wallet = walletService.getActiveWalletByUserId(userId);
        return ResponseEntity.ok(wallet);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateWallet(@PathVariable Long id) {
        log.info("Deactivating wallet {}", id);

        walletService.deactivateWallet(id);

        log.info("Wallet {} deactivated successfully", id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("{id}/activate")
    public ResponseEntity<Void> activateWallet(@PathVariable Long id) {
        log.info("Activating wallet {}", id);

        walletService.activateWallet(id);

        log.info("Wallet {} activated successfully");
        return ResponseEntity.noContent().build();
    }
}


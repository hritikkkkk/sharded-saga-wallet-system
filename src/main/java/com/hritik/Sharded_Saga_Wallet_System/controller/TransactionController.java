package com.hritik.Sharded_Saga_Wallet_System.controller;

import com.hritik.Sharded_Saga_Wallet_System.dto.TransferRequestDTO;
import com.hritik.Sharded_Saga_Wallet_System.dto.TransferResponseDTO;
import com.hritik.Sharded_Saga_Wallet_System.model.SagaInstance;
import com.hritik.Sharded_Saga_Wallet_System.service.TransferSagaService;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransferSagaService transferSagaService;
    private final SagaOrchestrator sagaOrchestrator;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDTO> createTransfer(
            @Valid @RequestBody TransferRequestDTO request) {

        log.info("Received transfer request from wallet {} to wallet {} for amount {}",
                request.getFromWalletId(), request.getToWalletId(), request.getAmount());

        Long sagaInstanceId = transferSagaService.initiateTransfer(
                request.getFromWalletId(),
                request.getToWalletId(),
                request.getAmount(),
                request.getDescription()
        );

        SagaInstance saga = sagaOrchestrator.getSagaInstance(sagaInstanceId);

        TransferResponseDTO response = TransferResponseDTO.builder()
                .sagaInstanceId(sagaInstanceId)
                .status(saga.getStatus().name())
                .message("Transfer initiated successfully")
                .build();

        log.info("Transfer initiated with saga instance {}", sagaInstanceId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/saga/{sagaInstanceId}")
    public ResponseEntity<SagaInstance> getSagaStatus(@PathVariable Long sagaInstanceId) {
        log.info("Fetching saga instance status for id {}", sagaInstanceId);

        SagaInstance saga = sagaOrchestrator.getSagaInstance(sagaInstanceId);
        return ResponseEntity.ok(saga);
    }
}
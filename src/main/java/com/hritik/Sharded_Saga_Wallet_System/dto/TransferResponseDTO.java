package com.hritik.Sharded_Saga_Wallet_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDTO {
    private Long sagaInstanceId;
    private Long transactionId;
    private String status;
    private String message;
}

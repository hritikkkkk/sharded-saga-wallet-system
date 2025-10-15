package com.hritik.Sharded_Saga_Wallet_System.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDTO {

    @NotNull(message = "Source wallet ID cannot be null")
    private Long fromWalletId;

    @NotNull(message = "Destination wallet ID cannot be null")
    private Long toWalletId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}

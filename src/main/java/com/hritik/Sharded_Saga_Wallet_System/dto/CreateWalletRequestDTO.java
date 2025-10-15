package com.hritik.Sharded_Saga_Wallet_System.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequestDTO {

    @NotNull(message = "User ID cannot be null")
    private Long userId;
}
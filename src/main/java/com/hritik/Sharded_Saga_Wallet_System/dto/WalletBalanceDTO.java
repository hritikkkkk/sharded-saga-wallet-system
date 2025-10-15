package com.hritik.Sharded_Saga_Wallet_System.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletBalanceDTO {
    private Long walletId;
    private BigDecimal balance;
    private Boolean isActive;
}

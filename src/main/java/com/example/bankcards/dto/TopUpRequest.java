package com.example.bankcards.dto;

import java.math.BigDecimal;

// запрос для пополнения баланса
public class TopUpRequest {
    private BigDecimal amount;
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}

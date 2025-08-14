package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.util.UUID;

// запрос для перевода средств
public class TransferRequest {
    private UUID ownerId;
    private UUID fromCardId;
    private UUID toCardId;
    private BigDecimal amount;

    public TransferRequest() {}

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public UUID getFromCardId() { return fromCardId; }
    public void setFromCardId(UUID fromCardId) { this.fromCardId = fromCardId; }
    public UUID getToCardId() { return toCardId; }
    public void setToCardId(UUID toCardId) { this.toCardId = toCardId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}

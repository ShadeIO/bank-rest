package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionDto {
    private UUID id;
    private UUID owner;
    private String fromMasked;
    private String toMasked;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private String message;

    public TransactionDto(UUID id, UUID owner, String fromMasked, String toMasked, BigDecimal amount,
                          TransactionStatus status, LocalDateTime createdAt, String message) {
        this.id = id;
        this.owner = owner;
        this.fromMasked = fromMasked;
        this.toMasked = toMasked;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.message = message;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public String getFromMasked() { return fromMasked; }
    public void setFromMasked(String fromMasked) { this.fromMasked = fromMasked; }

    public String getToMasked() { return toMasked; }
    public void setToMasked(String toMasked) { this.toMasked = toMasked; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

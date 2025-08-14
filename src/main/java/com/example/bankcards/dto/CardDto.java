package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CardDto {
    private UUID id;
    private UUID ownerId;
    private String maskedNumber;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;

    public CardDto(UUID id, UUID ownerId, String maskedNumber, LocalDate expiryDate,
                   CardStatus status, BigDecimal balance) {
        this.id = id;
        this.ownerId = ownerId;
        this.maskedNumber = maskedNumber;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwner() { return ownerId; }
    public void setOwner(UUID ownerId) { this.ownerId = ownerId; }

    public String getMaskedNumber() { return maskedNumber; }
    public void setMaskedNumber(String maskedNumber) { this.maskedNumber = maskedNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}

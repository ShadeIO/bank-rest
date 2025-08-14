package com.example.bankcards.entity;

import com.example.bankcards.config.CardNumberConverter;
import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, unique = true, length = 32)
    @Convert(converter = CardNumberConverter.class)
    private String encryptedCardNumber;

    @Column(name = "pan_hash", length = 88, unique = true)
    private String panHash;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "last4", length = 4)
    private String last4;

    public Card() {}

    public Card(User owner, String encryptedCardNumber, LocalDate expiryDate) {
        this.owner = owner;
        this.encryptedCardNumber = encryptedCardNumber;
        this.expiryDate = expiryDate;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getEncryptedCardNumber() { return encryptedCardNumber; }
    public void setEncryptedCardNumber(String encryptedCardNumber) {
        this.encryptedCardNumber = encryptedCardNumber;
    }

    public String getPanHash() { return panHash; }
    public void setPanHash(String panHash) { this.panHash = panHash; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }

    // маскировка номера карты
    @Transient
    public String getMaskedNumber() {
        if (encryptedCardNumber == null || encryptedCardNumber.length() < 4) return "****";
        String last4 = encryptedCardNumber.substring(encryptedCardNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}

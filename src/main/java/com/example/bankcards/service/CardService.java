package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.PanHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CardService(CardRepository cardRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    // Список всех карт (admin сценарий)
    @Transactional(readOnly = true)
    public List<CardDto> getAll() {
        return cardRepository.findAllWithOwner()
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    // Карты конкретного пользователя
    @Transactional(readOnly = true)
    public List<CardDto> getByUser(UUID userId) {
        return cardRepository.findByOwnerId(userId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    // Создать карту пользователю
    @Transactional
    public CardDto createForUser(UUID userId, CreateCardRequest req) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String normalized = PanHasher.normalize(req.getEncryptedCardNumber());
        String hash = PanHasher.hmacSha256Base64(normalized);
        LocalDate expire = req.getExpiryDate();

        if (req.getEncryptedCardNumber().replace(" ", "").length() != 16) throw new IllegalArgumentException("Invalid card number (length must be 16)");

        if (expire.isBefore(LocalDate.now())) throw new IllegalArgumentException("Card is expired");

        if (cardRepository.existsByPanHash(hash)) throw new IllegalArgumentException("PanHash already exists");

        if (cardRepository.existsByEncryptedCardNumber(req.getEncryptedCardNumber())) {
            throw new IllegalArgumentException("Card number already exists");
        }

        Card c = new Card();
        c.setOwner(owner);
        c.setEncryptedCardNumber(normalized);
        c.setLast4(normalized.substring(normalized.length()-4));
        c.setPanHash(hash);
        c.setExpiryDate(req.getExpiryDate());
        c.setStatus(CardStatus.ACTIVE);
        c.setBalance(BigDecimal.ZERO);

        cardRepository.save(c);
        return toDto(c);
    }

    // Пополнение баланса
    @Transactional
    public void topUp(UUID cardId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        var card = cardRepository.findByIdForUpdate(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalArgumentException("Card is not ACTIVE: " + card.getStatus());
        }
        card.setBalance(card.getBalance().add(amount));
    }

    // Удалить карту
    @Transactional
    public boolean delete(UUID cardId) {
        if (transactionRepository.existsByFromCard_Id(cardId)
                || transactionRepository.existsByToCard_Id(cardId)) {
            throw new IllegalStateException("Card has related transactions");
        }
        if (!cardRepository.existsById(cardId)) return false;
        cardRepository.deleteById(cardId);
        return true;
    }

    public Page<CardDto> getOwnPaged(UUID ownerId, CardStatus status, String last4, Pageable pageable) {
        Page<Card> page;
        boolean hasStatus = status != null;
        boolean hasLast4 = last4 != null && !last4.isBlank();

        if (hasStatus && hasLast4) {
            page = cardRepository.findByOwnerIdAndStatusAndLast4Containing(ownerId, status, last4, pageable);
        } else if (hasStatus) {
            page = cardRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        } else if (hasLast4) {
            page = cardRepository.findByOwnerIdAndLast4Containing(ownerId, last4, pageable);
        } else {
            page = cardRepository.findByOwnerId(ownerId, pageable);
        }
        return page.map(this::toDto);
    }

    // установка статуса
    @Transactional
    public void adminSetStatus(UUID cardId, CardStatus status) {
        Card c = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        c.setStatus(status);
    }

    // запрос блокировки
    @Transactional
    public void userRequestBlock(UUID cardId, UUID ownerId) {
        Card c = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (!c.getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException("Forbidden");
        }
        if (c.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalArgumentException("Card already blocked");
        }
        c.setStatus(CardStatus.BLOCK_REQUESTED);
    }

    public CardDto toDto(Card c) {
        String masked = c.getMaskedNumber();
        return new CardDto(
                c.getId(),
                c.getOwner().getId(),
                masked,
                c.getExpiryDate(),
                c.getStatus(),
                c.getBalance()
        );
    }
}

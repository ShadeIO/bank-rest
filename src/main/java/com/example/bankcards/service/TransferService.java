package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransferService(CardRepository cardRepository,
                           TransactionRepository transactionRepository,
                           UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // перевод средств
    @Transactional
    public TransactionDto transfer(TransferRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (req.getOwnerId() == null || req.getFromCardId() == null || req.getToCardId() == null) {
            throw new IllegalArgumentException("ownerId, fromCardId and toCardId are required");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Card from = cardRepository.findByIdForUpdate(req.getFromCardId())
                .orElseThrow(() -> new EntityNotFoundException("From-card not found"));
        Card to = cardRepository.findByIdForUpdate(req.getToCardId())
                .orElseThrow(() -> new EntityNotFoundException("To-card not found"));

        if (!from.getOwner().getId().equals(owner.getId()) || !to.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Cards must belong to the same user");
        }
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("From and to cards must be different");
        }
        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        requireActive(from, "From");
        requireActive(to,   "To");

        BigDecimal amount = req.getAmount();

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        Transaction t = new Transaction();
        t.setOwner(owner);
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(amount);
        t.setStatus(TransactionStatus.SUCCESS);
        t.setMessage("OK");

        transactionRepository.save(t);
        return new TransactionDto(
                t.getId(), owner.getId(), from.getMaskedNumber(), to.getMaskedNumber(),
                t.getAmount(), t.getStatus(), t.getCreatedAt(), t.getMessage()
        );
    }

    public List<TransactionDto> getByCard(UUID cardId) {
        return transactionRepository.findByCardId(cardId).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    private TransactionDto map(Transaction t) {
        return new TransactionDto(
                t.getId(),
                t.getOwner().getId(),
                t.getFromCard().getMaskedNumber(),
                t.getToCard().getMaskedNumber(),
                t.getAmount(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getMessage()
        );
    }

    public List<TransactionDto> getByUser(UUID userId) {
        return transactionRepository.findByUserId(userId).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    // активна ли карта
    private void requireActive(Card c, String side) {
        if (c.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    (side + " card is not ACTIVE: " + c.getStatus()).trim()
            );
        }
    }

}

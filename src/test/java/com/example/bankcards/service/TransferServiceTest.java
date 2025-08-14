package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock UserRepository userRepository;
    @Mock CardRepository cardRepository;
    @Mock TransactionRepository transactionRepository;

    TransferService service;

    UUID ownerId = UUID.randomUUID();
    UUID fromId  = UUID.randomUUID();
    UUID toId    = UUID.randomUUID();

    User owner;
    Card from;
    Card to;

    @BeforeEach
    void setUp() {
        service = new TransferService(cardRepository, transactionRepository, userRepository);

        owner = new User();
        owner.setId(ownerId);
        owner.setUsername("testuser");

        from = new Card();
        from.setId(fromId);
        from.setOwner(owner);
        from.setStatus(CardStatus.ACTIVE);
        from.setBalance(new BigDecimal("1000"));

        to = new Card();
        to.setId(toId);
        to.setOwner(owner);
        to.setStatus(CardStatus.ACTIVE);
        to.setBalance(new BigDecimal("10"));
    }

    private TransferRequest req(BigDecimal amount) {
        TransferRequest r = new TransferRequest();
        r.setOwnerId(ownerId);
        r.setFromCardId(fromId);
        r.setToCardId(toId);
        r.setAmount(amount);
        return r;
    }

    @Test
    void transfer_success() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        // сервис может вызывать findByIdForUpdate или findById — подстрахуемся обоими
        when(cardRepository.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(toId)).thenReturn(Optional.of(to));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.transfer(req(new BigDecimal("250")));

        assertEquals(new BigDecimal("750"), from.getBalance());
        assertEquals(new BigDecimal("260"), to.getBalance());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());

        // убеждаемся, что транзакция сохранена
        ArgumentCaptor<com.example.bankcards.entity.Transaction> cap = ArgumentCaptor.forClass(com.example.bankcards.entity.Transaction.class);
        verify(transactionRepository).save(cap.capture());
        assertEquals(new BigDecimal("250"), cap.getValue().getAmount());
        assertEquals(ownerId, cap.getValue().getOwner().getId());
    }

    @Test
    void transfer_insufficientFunds_throws400() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(toId)).thenReturn(Optional.of(to));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(req(new BigDecimal("5000"))));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_nonActiveFrom_throws400() {
        from.setStatus(CardStatus.BLOCKED);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(toId)).thenReturn(Optional.of(to));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(req(new BigDecimal("10"))));
        assertTrue(ex.getMessage().contains("From") && ex.getMessage().contains("not ACTIVE"));
    }

    @Test
    void transfer_nonActiveTo_throws400() {
        to.setStatus(CardStatus.BLOCK_REQUESTED);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(toId)).thenReturn(Optional.of(to));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(req(new BigDecimal("10"))));
        assertTrue(ex.getMessage().contains("To") && ex.getMessage().contains("not ACTIVE"));
    }

    @Test
    void transfer_differentOwner_throws400() {
        User other = new User();
        other.setId(UUID.randomUUID());
        to.setOwner(other);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(toId)).thenReturn(Optional.of(to));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(req(new BigDecimal("10"))));
        assertTrue(ex.getMessage().toLowerCase().contains("same user"));
    }
}

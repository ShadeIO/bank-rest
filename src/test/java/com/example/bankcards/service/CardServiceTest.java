package com.example.bankcards.service;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.PanHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock CardRepository cardRepository;
    @Mock UserRepository userRepository;
    @Mock TransactionRepository transactionRepository;

    CardService service;

    UUID userId = UUID.randomUUID();
    User user;

    @BeforeEach
    void setUp() {
        service = new CardService(cardRepository, userRepository, transactionRepository);
        user = new User();
        user.setId(userId);
        user.setUsername("user1");
    }

    @Test
    void create_duplicatePan_throws400() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CreateCardRequest req = new CreateCardRequest();
        req.setEncryptedCardNumber("4111 1111 1111 1111");
        req.setExpiryDate(LocalDate.of(2030,12,1));

        when(cardRepository.existsByPanHash(anyString())).thenReturn(true);
        assertThatThrownBy(() -> service.createForUser(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(cardRepository, never()).save(any());
    }

    @Test
    void topUp_notActive_throws400() {
        UUID cardId = UUID.randomUUID();
        Card c = new Card();
        c.setId(cardId);
        c.setOwner(user);
        c.setStatus(CardStatus.BLOCKED);
        c.setBalance(BigDecimal.ZERO);
        when(cardRepository.findByIdForUpdate(cardId)).thenReturn(Optional.of(c));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.topUp(cardId, new BigDecimal("100")));
        assertTrue(ex.getMessage().contains("not ACTIVE"));
    }
}

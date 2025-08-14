package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.RoleType;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.SecurityConfig;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransferController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class // <-- твой SecurityConfig
        )
)
@Import(TestSecurityConfig.class)
class TransferControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean com.example.bankcards.service.TransferService transferService;
    @MockBean com.example.bankcards.repository.UserRepository userRepository;
    @MockBean com.example.bankcards.repository.CardRepository cardRepository;

    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean com.example.bankcards.security.JwtUtil jwtUtil;

    @Test
//    @WithMockUser(username = "testuser")
    void transfer_forbiddenIfOwnerMismatch() throws Exception {
        UUID me = UUID.randomUUID();
        var current = new User();
        current.setId(me);
        current.setUsername("testuser");
        current.setRole(RoleType.USER);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(current));

        // Запрос с чужим ownerId
        var req = new TransferRequest();
        req.setOwnerId(UUID.randomUUID());
        req.setFromCardId(UUID.randomUUID());
        req.setToCardId(UUID.randomUUID());
        req.setAmount(new BigDecimal("10"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_okWhenOwnerMatchesAndCardsBelongToUser() throws Exception {
        UUID me = UUID.randomUUID();
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(new User() {{ setId(me); setUsername("testuser"); }}));

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        // обе карты принадлежат текущему пользователю
        var owner = new User(); owner.setId(me);
        var from = new Card(); from.setId(fromId); from.setOwner(owner); from.setStatus(CardStatus.ACTIVE);
        var to   = new Card(); to.setId(toId);     to.setOwner(owner);   to.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(to));

        // мокаем успешный ответ сервиса
        when(transferService.transfer(any())).thenReturn(new TransactionDto(
                UUID.randomUUID(), me, fromId.toString(), toId.toString(), new BigDecimal("10"),
                TransactionStatus.SUCCESS, LocalDateTime.now(), "OK"
        ));

        var req = new TransferRequest();
        req.setOwnerId(me);
        req.setFromCardId(fromId);
        req.setToCardId(toId);
        req.setAmount(new BigDecimal("10"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.owner").value(me.toString()));
    }
}

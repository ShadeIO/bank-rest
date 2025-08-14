package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.SecurityConfig;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class // <-- твой SecurityConfig
        )
)
@Import(TestSecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Autowired MockMvc mvc;

    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean com.example.bankcards.security.JwtUtil jwtUtil;

    // зависимости контроллера
    @MockBean com.example.bankcards.service.CardService cardService;
    @MockBean com.example.bankcards.service.TransferService transferService;
    @MockBean com.example.bankcards.repository.UserRepository userRepository;
    @MockBean com.example.bankcards.repository.CardRepository cardRepository;

    @Test
    @WithMockUser(username = "testuser")
    void myCards_returnsOnlyOwn_withPagingAndFilter() throws Exception {
        var me = new User(); me.setId(UUID.randomUUID()); me.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(me));

        var dto = new CardDto(
                UUID.randomUUID(),
                me.getId(),
                "**** **** **** 1234",
                LocalDate.of(2030,12,1),
                CardStatus.ACTIVE,
                new BigDecimal("100")
        );
        when(cardService.getOwnPaged(eq(me.getId()), eq(CardStatus.ACTIVE), eq("1234"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mvc.perform(get("/api/cards/my")
                        .param("status", "ACTIVE")
                        .param("last4", "1234")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].maskedNumber", is("**** **** **** 1234")));
    }
}

package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleType;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.SecurityConfig;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class // <-- твой SecurityConfig
        )
)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mvc;

    @MockBean com.example.bankcards.service.UserService userService;
    @MockBean com.example.bankcards.repository.UserRepository userRepository;

    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean com.example.bankcards.security.JwtUtil jwtUtil;

    @Test
//    @WithMockUser(username = "testuser")
    void listUsers_asUser_returnsOnlySelf() throws Exception {
        var u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("testuser");
        u.setRole(RoleType.USER);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(u));

        mvc.perform(get("/api/users").with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("testuser")));
    }

    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void listUsers_asAdmin_returnsAll() throws Exception {
        var u1 = new UserDto(); u1.setId(UUID.randomUUID()); u1.setUsername("testuser"); u1.setRole("USER");
        var u2 = new UserDto(); u2.setId(UUID.randomUUID()); u2.setUsername("bob");   u2.setRole("USER");
        when(userService.all()).thenReturn(List.of(u1, u2));

        mvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("testuser")))
                .andExpect(jsonPath("$[1].username", is("bob")));
    }

    @Test
//    @WithMockUser(username = "testuser")
    void getById_user_canSeeOnlySelf() throws Exception {
        var idTestUser = UUID.randomUUID();
        var idBob = UUID.randomUUID();

        var userTestUser = new User(); userTestUser.setId(idTestUser); userTestUser.setUsername("testuser"); userTestUser.setRole(RoleType.USER);
        var userBob = new User();   userBob.setId(idBob);   userBob.setUsername("bob"); userBob.setRole(RoleType.USER);

        when(userRepository.findById(idTestUser)).thenReturn(Optional.of(userTestUser));
        when(userRepository.findById(idBob)).thenReturn(Optional.of(userBob));

        // свой id -> 200
        mvc.perform(get("/api/users/{id}", idTestUser).with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")));

        // чужой id -> 403
        mvc.perform(get("/api/users/{id}", idBob).with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }
}

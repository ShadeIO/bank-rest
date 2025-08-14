package com.example.bankcards.controller;

import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // GET /api/users
    // user: возвращает данные о текущем пользователе
    // admin: возвращает данные о всех пользователях
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<UserDto>> all(Authentication auth) {
        if (isAdmin(auth)) {
            return ResponseEntity.ok(userService.all());
        }
        var meOpt = userRepository.findByUsername(auth.getName());
        return meOpt.map(user -> ResponseEntity.ok(List.of(toDto(user)))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /api/users/{userId} - получить по ID
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID userId, Authentication auth) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) return ResponseEntity.notFound().build();

        User u = user.get();
        if (!isAdmin(auth) && !u.getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(toDto(u));
    }

    // GET /api/users/by-username/{username} - получить по username
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> getByUsername(@PathVariable String username, Authentication auth) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return ResponseEntity.notFound().build();

        User u = user.get();
        if (!isAdmin(auth) && !u.getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(toDto(u));
    }

    // POST /api/users/register - создать (регистрация) с правами user
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest req) {
        UserDto dto = userService.register(req, false);
        return ResponseEntity.created(URI.create("/api/users/" + dto.getId())).body(dto);
    }

    // POST /api/users/register-admin - создать (регистрация) с правами admin
    @PostMapping("/register-admin")
    public ResponseEntity<UserDto> registerAdmin(@RequestBody RegisterRequest req) {
        UserDto dto = userService.register(req, true);
        return ResponseEntity.created(URI.create("/api/users/" + dto.getId())).body(dto);
    }

    // DELETE /api/users/{userId} удалить по ID (admin)
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID userId) {
        if (!userRepository.existsById(userId)) return ResponseEntity.notFound().build();
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setRole(u.getRole().name());
        return dto;
    }
}

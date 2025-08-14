package com.example.bankcards.controller;

import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        String token = jwtUtil.generate(auth.getName());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @GetMapping("/me")
    public java.util.Map<String, Object> me(org.springframework.security.core.Authentication auth) {
        if (auth == null) return java.util.Map.of("authenticated", false);
        return java.util.Map.of(
                "authenticated", true,
                "name", auth.getName(),
                "authorities", auth.getAuthorities().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList())
        );
    }

}

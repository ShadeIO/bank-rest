package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public TransferController(TransferService transferService,
                              UserRepository userRepository,
                              CardRepository cardRepository) {
        this.transferService = transferService;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    // POST /api/transfers - Перевод между своими картами
    @PostMapping
    public ResponseEntity<TransactionDto> transfer(@RequestBody TransferRequest req, Authentication auth) {
        UUID me = currentUserId(auth);
        if (req.getOwnerId() == null || !me.equals(req.getOwnerId())) {
            throw new AccessDeniedException("Forbidden");
        }
        // обе карты должны быть моими
        Card from = cardRepository.findById(req.getFromCardId())
                .orElseThrow(() -> new IllegalArgumentException("From card not found"));
        Card to = cardRepository.findById(req.getToCardId())
                .orElseThrow(() -> new IllegalArgumentException("To card not found"));
        if (!from.getOwner().getId().equals(me) || !to.getOwner().getId().equals(me)) {
            throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(transferService.transfer(req));
    }

    // GET /api/transfers/card/{cardId} - История по карте
    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<TransactionDto>> byCard(@PathVariable UUID cardId, Authentication auth) {
        if (isAdmin(auth)) {
            UUID me = currentUserId(auth);
            Card c = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            if (!c.getOwner().getId().equals(me)) throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(transferService.getByCard(cardId));
    }

    // GET /api/transfers/user/{userId} - История по пользователю
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDto>> byUser(@PathVariable UUID userId, Authentication auth) {
        if (isAdmin(auth) && !currentUserId(auth).equals(userId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(transferService.getByUser(userId));
    }

    private static boolean isAdmin(Authentication auth) {
        return auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private UUID currentUserId(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    }
}

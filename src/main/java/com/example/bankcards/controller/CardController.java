package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TopUpRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public CardController(CardService cardService,
                          UserRepository userRepository,
                          CardRepository cardRepository) {
        this.cardService = cardService;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    // GET /api/cards
    // user: запросить свои карты (списком)
    // admin: запросить все карты (списком)
    @GetMapping
    public ResponseEntity<List<CardDto>> all(Authentication auth) {
        if (isAdmin(auth)) {
            return ResponseEntity.ok(cardService.getAll());
        }
        UUID me = currentUserId(auth);
        return ResponseEntity.ok(cardService.getByUser(me));
    }

    // GET /api/cards/my - запросить свои карты (пагинация)
    @GetMapping("/my")
    public ResponseEntity<org.springframework.data.domain.Page<CardDto>> myCards(
            Authentication auth,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String last4,
            @ParameterObject @PageableDefault(sort = "expiryDate") Pageable pageable) {

        UUID me = currentUserId(auth);
        return ResponseEntity.ok(cardService.getOwnPaged(me, status, last4, pageable));
    }

    // POST /api/cards/{cardId}/request-block - запросить блокировку карты
    @PostMapping("/{cardId}/request-block")
    public ResponseEntity<Void> requestBlock(@PathVariable UUID cardId, Authentication auth) {
        UUID me = currentUserId(auth);
        cardService.userRequestBlock(cardId, me);
        return ResponseEntity.noContent().build();
    }

    // POST /api/cards/{cardId}/block - заблокировать карту (admin)
    @PostMapping("/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminBlock(@PathVariable UUID cardId) {
        cardService.adminSetStatus(cardId, CardStatus.BLOCKED);
        return ResponseEntity.noContent().build();
    }

    // POST /api/cards/{cardId}/activate - активировать карту (admin)
    @PostMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminActivate(@PathVariable UUID cardId) {
        cardService.adminSetStatus(cardId, CardStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }

    // GET /api/cards/{cardId} - запросить карту по id
    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> get(@PathVariable UUID cardId, Authentication auth) {
        Card c = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (!isAdmin(auth) && !c.getOwner().getId().equals(currentUserId(auth))) {
            throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(cardService.toDto(c));
    }

    // GET /api/cards/user/{userId} — карты пользователя
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CardDto>> getCardsByUser(@PathVariable UUID userId, Authentication auth) {
        if (!isAdmin(auth) && !currentUserId(auth).equals(userId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return ResponseEntity.ok(cardService.getByUser(userId));
    }

    // POST /api/cards/user/{userId} — создать карту пользователю
    @PostMapping("/user/{userId}")
    public ResponseEntity<CardDto> createCardForUser(@PathVariable UUID userId,
                                                     @RequestBody CreateCardRequest request,
                                                     Authentication auth) {
        if (!isAdmin(auth) && !currentUserId(auth).equals(userId)) {
            throw new AccessDeniedException("Forbidden");
        }
        CardDto created = cardService.createForUser(userId, request);
        return ResponseEntity.created(URI.create("/api/cards/" + created.getId())).body(created);
    }

    // POST /api/cards/{cardId}/top-up — пополнить баланс карты
    @PostMapping("/{cardId}/top-up")
    public ResponseEntity<Void> topUp(@PathVariable UUID cardId,
                                      @RequestBody TopUpRequest req,
                                      Authentication auth) {
        if (!isAdmin(auth)) {
            UUID me = currentUserId(auth);
            Card c = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            if (!c.getOwner().getId().equals(me)) throw new AccessDeniedException("Forbidden");
        }
        cardService.topUp(cardId, req.getAmount());
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/cards/{cardId} - удалить карту (admin)
    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID cardId) {
        boolean removed = cardService.delete(cardId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private UUID currentUserId(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    }
}

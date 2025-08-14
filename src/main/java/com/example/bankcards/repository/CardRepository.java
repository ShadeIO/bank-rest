package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import java.util.*;

public interface CardRepository extends JpaRepository<Card, UUID> {

    // для обновления данных карты
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id")
    Optional<Card> findByIdForUpdate(@Param("id") UUID id);

    List<Card> findByOwnerId(UUID ownerId);

    boolean existsByEncryptedCardNumber(String encryptedCardNumber);

    @Query("select c from Card c join fetch c.owner")
    List<Card> findAllWithOwner();

    boolean existsByPanHash(String panHash);

    //для пагинации
    Page<Card> findByOwnerId(UUID ownerId, Pageable pageable);
    Page<Card> findByOwnerIdAndStatus(UUID ownerId, CardStatus status, Pageable pageable);

    Page<Card> findByOwnerIdAndLast4Containing(UUID ownerId, String last4, Pageable pageable);
    Page<Card> findByOwnerIdAndStatusAndLast4Containing(UUID ownerId, CardStatus status, String last4, Pageable pageable);
}

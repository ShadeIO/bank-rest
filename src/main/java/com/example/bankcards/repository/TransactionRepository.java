package com.example.bankcards.repository;

import com.example.bankcards.entity.Transaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
           select t from Transaction t
           where t.fromCard.id = :cardId or t.toCard.id = :cardId
           order by t.createdAt desc
           """)
    List<Transaction> findByCardId(@Param("cardId") UUID cardId);

    @Query("""
           select t from Transaction t
           where t.fromCard.owner.id = :userId or t.toCard.owner.id = :userId
           order by t.createdAt desc
           """)
    List<Transaction> findByUserId(@Param("userId") UUID userId);

    boolean existsByFromCard_Id(UUID cardId);
    boolean existsByToCard_Id(UUID cardId);
}

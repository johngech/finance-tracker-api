package com.marakicode.financetracker.transactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategoryEntity, Long> {

    Optional<TransactionCategoryEntity> findByName(String name);

    @Query(value = "INSERT INTO transaction_categories (name) VALUES (:name) " +
            "ON CONFLICT (name) DO NOTHING",
            nativeQuery = true)
    void insertIfAbsent(@Param("name") String name);
}

package com.marakicode.financetracker.transactions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionTypeRepository extends JpaRepository<TransactionTypeEntity, Long> {

    Optional<TransactionTypeEntity> findByName(String name);
}

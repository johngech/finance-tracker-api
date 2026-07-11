package com.marakicode.financetracker.transactions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("SELECT t.type.name, COUNT(t) FROM Transaction t GROUP BY t.type.name")
    List<Object[]> countByTypeName();

    @Query("SELECT t.type.name, COALESCE(SUM(t.amount), 0) FROM Transaction t GROUP BY t.type.name")
    List<Object[]> sumAmountByTypeName();

    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.account.user.id = :userId")
    @EntityGraph("Transaction.withAccount")
    Optional<Transaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Override
    @NonNull
    @EntityGraph("Transaction.withAccount")
    Optional<Transaction> findById(@NonNull Long id);

    @Override
    @NonNull
    @EntityGraph("Transaction.withAccount")
    Page<Transaction> findAll(@Nullable Specification<Transaction> spec, @NonNull Pageable pageable);
}

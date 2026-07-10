package com.marakicode.financetracker.accounts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    @EntityGraph("Account.withType")
    Page<Account> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId")
    @EntityGraph("Account.withType")
    Optional<Account> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.user.id = :userId AND a.name = :name")
    boolean existsByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    @Override
    @EntityGraph("Account.withType")
    Page<Account> findAll(@Nullable Specification<Account> spec, Pageable pageable);
}

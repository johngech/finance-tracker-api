package com.marakicode.financetracker.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTypeRepository extends JpaRepository<AccountTypeEntity, Long> {

    Optional<AccountTypeEntity> findByName(String name);
}

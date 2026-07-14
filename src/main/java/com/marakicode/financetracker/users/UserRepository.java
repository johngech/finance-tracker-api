package com.marakicode.financetracker.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    long countByRoleAndActiveTrue(Role role);

    @Query("SELECT " +
        "COUNT(u), " +
        "SUM(CASE WHEN u.active = true THEN 1 ELSE 0 END), " +
        "SUM(CASE WHEN u.active = false THEN 1 ELSE 0 END), " +
        "SUM(CASE WHEN u.role = com.marakicode.financetracker.users.Role.ADMIN THEN 1 ELSE 0 END), " +
        "SUM(CASE WHEN u.role = com.marakicode.financetracker.users.Role.USER THEN 1 ELSE 0 END) " +
        "FROM User u")
    Object[] getUserStatistics();
}

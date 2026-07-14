package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.transactions.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only aggregation repository for Transaction data.
 * Does NOT extend JpaRepository to prevent accidental writes.
 */
@Transactional(readOnly = true)
public interface ReportsRepository extends JpaRepository<Transaction, Long> {

    /**
     * Summary: total income, total expense, and transaction count for a user within a date range.
     * Uses CASE inside SUM for conditional aggregation.
     */
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN t.type.name = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type.name = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
            COUNT(t)
        FROM Transaction t
        WHERE t.account.user.id = :userId
            AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
            AND (:toDate IS NULL OR t.transactionDate <= :toDate)
        """)
    Object[] getSummaryByUserId(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Category breakdown: sum of amounts grouped by category name.
     * Null categories are mapped to 'Uncategorized' via COALESCE.
     * Optional type filter (INCOME/EXPENSE).
     */
    @Query("""
        SELECT COALESCE(t.category.name, 'Uncategorized'), SUM(t.amount)
        FROM Transaction t
        WHERE t.account.user.id = :userId
            AND (:type IS NULL OR t.type.name = :type)
            AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
            AND (:toDate IS NULL OR t.transactionDate <= :toDate)
        GROUP BY t.category.name
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> getCategoryBreakdown(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Monthly breakdown: income and expense sums grouped by month for a given year.
     * Uses MONTH() and YEAR() JPQL date functions.
     */
    @Query("""
        SELECT MONTH(t.transactionDate),
            COALESCE(SUM(CASE WHEN t.type.name = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type.name = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM Transaction t
        WHERE t.account.user.id = :userId
            AND YEAR(t.transactionDate) = :year
        GROUP BY MONTH(t.transactionDate)
        ORDER BY MONTH(t.transactionDate)
        """)
    List<Object[]> getMonthlyBreakdown(
            @Param("userId") Long userId,
            @Param("year") int year);

    /**
     * Account breakdown: income and expense sums grouped by account.
     */
    @Query("""
        SELECT t.account.id, t.account.name,
            COALESCE(SUM(CASE WHEN t.type.name = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type.name = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM Transaction t
        WHERE t.account.user.id = :userId
            AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
            AND (:toDate IS NULL OR t.transactionDate <= :toDate)
        GROUP BY t.account.id, t.account.name
        ORDER BY t.account.name
        """)
    List<Object[]> getAccountBreakdown(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * System-wide summary: total income, total expense, and transaction count within a date range.
     */
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN t.type.name = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type.name = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
            COUNT(t)
        FROM Transaction t
        WHERE (:fromDate IS NULL OR t.transactionDate >= :fromDate)
            AND (:toDate IS NULL OR t.transactionDate <= :toDate)
        """)
    Object[] getSystemSummary(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * System-wide category breakdown.
     */
    @Query("""
        SELECT COALESCE(t.category.name, 'Uncategorized'), SUM(t.amount)
        FROM Transaction t
        WHERE (:type IS NULL OR t.type.name = :type)
            AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
            AND (:toDate IS NULL OR t.transactionDate <= :toDate)
        GROUP BY t.category.name
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> getSystemCategoryBreakdown(
            @Param("type") String type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * System-wide monthly breakdown for a given year.
     */
    @Query("""
        SELECT MONTH(t.transactionDate),
            COALESCE(SUM(CASE WHEN t.type.name = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type.name = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM Transaction t
        WHERE YEAR(t.transactionDate) = :year
        GROUP BY MONTH(t.transactionDate)
        ORDER BY MONTH(t.transactionDate)
        """)
    List<Object[]> getSystemMonthlyBreakdown(@Param("year") int year);

    /**
     * System-wide account breakdown.
     */
    @Query("""
        SELECT t.account.id, t.account.name,
            COALESCE(SUM(CASE WHEN t.type.name = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type.name = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM Transaction t
        WHERE (:fromDate IS NULL OR t.transactionDate >= :fromDate)
            AND (:toDate IS NULL OR t.transactionDate <= :toDate)
        GROUP BY t.account.id, t.account.name
        ORDER BY t.account.name
        """)
    List<Object[]> getSystemAccountBreakdown(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}

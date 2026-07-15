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
 *
 * <p>Uses native SQL for queries with optional (nullable) parameters because
 * PostgreSQL's JDBC driver cannot infer parameter types from JPQL's
 * {@code (:param IS NULL OR ...)} pattern — a null parameter has no type context.
 * Native SQL with {@code COALESCE} forces a type by binding the parameter
 * against a typed literal (e.g. {@code DATE '1900-01-01'}).
 */
@Transactional(readOnly = true)
public interface ReportsRepository extends JpaRepository<Transaction, Long> {

    /**
     * Summary: total income, total expense, and transaction count for a user within a date range.
     * Returns a list for consistency with native SQL result mapping.
     */
    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
            COUNT(t.id)
        FROM transactions t
        JOIN accounts a ON a.id = t.account_id
        WHERE a.user_id = :userId
            AND (COALESCE(CAST(:fromDate AS date), DATE '1900-01-01') = DATE '1900-01-01' OR t.transaction_date >= :fromDate)
            AND (COALESCE(CAST(:toDate AS date), DATE '2100-12-31') = DATE '2100-12-31' OR t.transaction_date <= :toDate)
        """, nativeQuery = true)
    List<Object[]> getSummaryByUserId(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Category breakdown: sum of amounts grouped by category name.
     * Null categories are mapped to 'Uncategorized' via COALESCE.
     * Optional type filter (INCOME/EXPENSE).
     */
    @Query(value = """
        SELECT COALESCE(t.category, 'Uncategorized'), SUM(t.amount)
        FROM transactions t
        JOIN accounts a ON a.id = t.account_id
        WHERE a.user_id = :userId
            AND (COALESCE(:type, '%%') = '%%' OR t.type = :type)
            AND (COALESCE(CAST(:fromDate AS date), DATE '1900-01-01') = DATE '1900-01-01' OR t.transaction_date >= :fromDate)
            AND (COALESCE(CAST(:toDate AS date), DATE '2100-12-31') = DATE '2100-12-31' OR t.transaction_date <= :toDate)
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
        """, nativeQuery = true)
    List<Object[]> getCategoryBreakdown(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Monthly breakdown: income and expense sums grouped by month for a given year.
     * Uses EXTRACT(MONTH FROM ...) and EXTRACT(YEAR FROM ...) in native SQL.
     */
    @Query(value = """
        SELECT EXTRACT(MONTH FROM t.transaction_date),
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM transactions t
        JOIN accounts a ON a.id = t.account_id
        WHERE a.user_id = :userId
            AND EXTRACT(YEAR FROM t.transaction_date) = :year
        GROUP BY EXTRACT(MONTH FROM t.transaction_date)
        ORDER BY EXTRACT(MONTH FROM t.transaction_date)
        """, nativeQuery = true)
    List<Object[]> getMonthlyBreakdown(
            @Param("userId") Long userId,
            @Param("year") int year);

    /**
     * Account breakdown: income and expense sums grouped by account.
     */
    @Query(value = """
        SELECT t.account_id, a.name,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM transactions t
        JOIN accounts a ON a.id = t.account_id
        WHERE a.user_id = :userId
            AND (COALESCE(CAST(:fromDate AS date), DATE '1900-01-01') = DATE '1900-01-01' OR t.transaction_date >= :fromDate)
            AND (COALESCE(CAST(:toDate AS date), DATE '2100-12-31') = DATE '2100-12-31' OR t.transaction_date <= :toDate)
        GROUP BY t.account_id, a.name
        ORDER BY a.name
        """, nativeQuery = true)
    List<Object[]> getAccountBreakdown(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * System-wide summary: total income, total expense, and transaction count within a date range.
     */
    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
            COUNT(t.id)
        FROM transactions t
        WHERE (COALESCE(CAST(:fromDate AS date), DATE '1900-01-01') = DATE '1900-01-01' OR t.transaction_date >= :fromDate)
            AND (COALESCE(CAST(:toDate AS date), DATE '2100-12-31') = DATE '2100-12-31' OR t.transaction_date <= :toDate)
        """, nativeQuery = true)
    List<Object[]> getSystemSummary(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * System-wide category breakdown.
     */
    @Query(value = """
        SELECT COALESCE(t.category, 'Uncategorized'), SUM(t.amount)
        FROM transactions t
        WHERE (COALESCE(:type, '%%') = '%%' OR t.type = :type)
            AND (COALESCE(CAST(:fromDate AS date), DATE '1900-01-01') = DATE '1900-01-01' OR t.transaction_date >= :fromDate)
            AND (COALESCE(CAST(:toDate AS date), DATE '2100-12-31') = DATE '2100-12-31' OR t.transaction_date <= :toDate)
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
        """, nativeQuery = true)
    List<Object[]> getSystemCategoryBreakdown(
            @Param("type") String type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * System-wide monthly breakdown for a given year.
     */
    @Query(value = """
        SELECT EXTRACT(MONTH FROM t.transaction_date),
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM transactions t
        WHERE EXTRACT(YEAR FROM t.transaction_date) = :year
        GROUP BY EXTRACT(MONTH FROM t.transaction_date)
        ORDER BY EXTRACT(MONTH FROM t.transaction_date)
        """, nativeQuery = true)
    List<Object[]> getSystemMonthlyBreakdown(@Param("year") int year);

    /**
     * System-wide account breakdown.
     */
    @Query(value = """
        SELECT t.account_id, a.name,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM transactions t
        JOIN accounts a ON a.id = t.account_id
        WHERE (COALESCE(CAST(:fromDate AS date), DATE '1900-01-01') = DATE '1900-01-01' OR t.transaction_date >= :fromDate)
            AND (COALESCE(CAST(:toDate AS date), DATE '2100-12-31') = DATE '2100-12-31' OR t.transaction_date <= :toDate)
        GROUP BY t.account_id, a.name
        ORDER BY a.name
        """, nativeQuery = true)
    List<Object[]> getSystemAccountBreakdown(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}

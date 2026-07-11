package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.common.SearchUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> accountIdEquals(Long accountId) {
        return (accountId == null)
                ? null
                : (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> typeEquals(TransactionType type) {
        return (type == null)
                ? null
                : (root, query, cb) -> {
                    Join<Object, Object> typeJoin = root.join("type");
                    return cb.equal(typeJoin.get("name"), type.name());
                };
    }

    public static Specification<Transaction> dateBetween(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<LocalDate> date = root.get("transactionDate");
            if (from == null) {
                return cb.lessThanOrEqualTo(date, to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(date, from);
            }
            return cb.between(date, from, to);
        };
    }

    public static Specification<Transaction> categoryEquals(String category) {
        return (category == null)
                ? null
                : (root, query, cb) -> {
                    Join<Object, Object> categoryJoin = root.join("category");
                    return cb.equal(categoryJoin.get("name"), category);
                };
    }

    public static Specification<Transaction> descriptionContains(String search) {
        return (search == null || search.isBlank())
                ? null
                : (root, query, cb) -> cb.like(cb.lower(root.get("description")),
                        SearchUtils.toLikeContainsPattern(search.toLowerCase()));
    }
}

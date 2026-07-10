package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.common.SearchUtils;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class AccountSpecification {

    public static Specification<Account> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Account> nameContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")),
                    SearchUtils.toLikeContainsPattern(search.toLowerCase()));
        };
    }

    public static Specification<Account> typeEquals(AccountType type) {
        return (root, query, cb) -> {
            Join<Object, Object> typeJoin = root.join("type");
            return cb.equal(typeJoin.get("name"), type.name());
        };
    }

    public static Specification<Account> currencyEquals(String currency) {
        return (root, query, cb) -> cb.equal(root.get("currency"), currency);
    }
}

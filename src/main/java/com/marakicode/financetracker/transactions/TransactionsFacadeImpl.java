package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.transactions.dto.TransactionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionsFacadeImpl implements TransactionsFacade {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionSummary getTransactionById(Long transactionId) {
        var transaction = findById(transactionId);
        return toSummary(transaction);
    }

    @Override
    public PagedResponse<TransactionSummary> listTransactions(String search, Pageable pageable) {
        var spec = Specification.<Transaction>unrestricted().and(TransactionSpecification.descriptionContains(search));
        var page = transactionRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, this::toSummary);
    }

    @Override
    public PagedResponse<TransactionSummary> listUserTransactions(Long userId, Pageable pageable) {
        var spec = Specification.<Transaction>unrestricted().and(userIdEquals(userId));
        var page = transactionRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, this::toSummary);
    }

    @Override
    public TransactionStatistics getStatistics() {
        var counts = resolveCounts(transactionRepository.countByTypeName());
        var amounts = resolveAmounts(transactionRepository.sumAmountByTypeName());
        return new TransactionStatistics(
            counts[0] + counts[1], counts[0], counts[1],
            amounts[0], amounts[1], amounts[0].subtract(amounts[1]));
    }

    private Transaction findById(Long id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Specification<Transaction> userIdEquals(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) return null;
            return cb.equal(root.get("account").get("user").get("id"), userId);
        };
    }

    private long[] resolveCounts(List<Object[]> rows) {
        long income = 0, expense = 0;
        for (Object[] row : rows) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if ("INCOME".equals(name)) income = count;
            else if ("EXPENSE".equals(name)) expense = count;
        }
        return new long[]{income, expense};
    }

    private BigDecimal[] resolveAmounts(List<Object[]> rows) {
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (Object[] row : rows) {
            String name = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if ("INCOME".equals(name)) income = amount;
            else if ("EXPENSE".equals(name)) expense = amount;
        }
        return new BigDecimal[]{income, expense};
    }

    private TransactionSummary toSummary(Transaction t) {
        var account = t.getAccount();
        var user = account.getUser();
        return new TransactionSummary(
            t.getId(), account.getId(), account.getName(),
            user.getId(), user.getFirstName() + " " + user.getLastName(),
            resolveType(t), t.getAmount(), t.getDescription(),
            t.getTransactionDate(), resolveCategory(t),
            t.getCreatedAt());
    }

    private TransactionType resolveType(Transaction t) {
        if (t.getType() == null) return null;
        return TransactionType.valueOf(t.getType().getName());
    }

    private String resolveCategory(Transaction t) {
        return t.getCategory() != null ? t.getCategory().getName() : null;
    }
}

package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.transactions.dto.TransactionSummary;
import org.springframework.data.domain.Pageable;

public interface TransactionsFacade {
    TransactionSummary getTransactionById(Long transactionId);
    PagedResponse<TransactionSummary> listTransactions(String search, Pageable pageable);
    PagedResponse<TransactionSummary> listUserTransactions(Long userId, Pageable pageable);
    TransactionStatistics getStatistics();
}

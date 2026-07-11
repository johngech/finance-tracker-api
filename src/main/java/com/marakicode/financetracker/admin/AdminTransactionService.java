package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.transactions.TransactionsFacade;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.transactions.dto.TransactionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private final TransactionsFacade transactionsFacade;

    public PagedResponse<TransactionSummary> listTransactions(
            String search, Pageable pageable) {
        return transactionsFacade.listTransactions(search, pageable);
    }

    public TransactionSummary getTransaction(Long transactionId) {
        return transactionsFacade.getTransactionById(transactionId);
    }

    public TransactionStatistics getStatistics() {
        return transactionsFacade.getStatistics();
    }
}

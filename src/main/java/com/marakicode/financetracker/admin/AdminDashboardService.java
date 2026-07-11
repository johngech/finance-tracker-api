package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.admin.dto.DashboardResponse;
import com.marakicode.financetracker.accounts.AccountsFacade;
import com.marakicode.financetracker.transactions.TransactionsFacade;
import com.marakicode.financetracker.users.UsersFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UsersFacade usersFacade;
    private final AccountsFacade accountsFacade;
    private final TransactionsFacade transactionsFacade;

    public DashboardResponse getDashboard() {
        return new DashboardResponse(
            usersFacade.getStatistics(),
            accountsFacade.getStatistics(),
            transactionsFacade.getStatistics()
        );
    }
}

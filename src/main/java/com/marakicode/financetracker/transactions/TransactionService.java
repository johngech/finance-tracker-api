package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.accounts.Account;
import com.marakicode.financetracker.accounts.AccountRepository;
import com.marakicode.financetracker.common.CurrentUserProvider;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.dto.TransactionCreateRequest;
import com.marakicode.financetracker.transactions.dto.TransactionResponse;
import com.marakicode.financetracker.transactions.dto.TransactionUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        var userId = currentUserProvider.getCurrentUserId();
        var account = findOwnedAccount(request.accountId(), userId);
        checkAccountFrozen(account);
        var transaction = buildTransaction(request, account);
        checkInsufficientFunds(transaction, account);
        transaction = transactionRepository.save(transaction);
        applyBalanceEffect(account, request.type(), request.amount());
        accountRepository.save(account);
        log.info("event=transaction.created transactionId={} type={} amount={} accountId={}",
                transaction.getId(), request.type(), request.amount(), request.accountId());
        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        var userId = currentUserProvider.getCurrentUserId();
        Transaction transaction = findOwnedTransaction(id, userId);
        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(
            Long accountId,
            TransactionType type,
            String category,
            String search,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        var userId = currentUserProvider.getCurrentUserId();
        Specification<Transaction> spec = buildSpec(accountId, type, category, search, from, to);
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("account").get("user").get("id"), userId));
        var page = transactionRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, transactionMapper::toResponse);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        var userId = currentUserProvider.getCurrentUserId();
        Transaction existing = findOwnedTransaction(id, userId);
        if (isAllFieldsNull(request)) return transactionMapper.toResponse(existing);

        TransactionType originalType = toTransactionType(existing.getType());
        BigDecimal originalAmount = existing.getAmount();

        applyUpdates(existing, request);

        reconcileBalanceIfNeeded(existing, originalType, originalAmount);
        transactionRepository.save(existing);
        log.info("event=transaction.updated transactionId={}", id);
        return transactionMapper.toResponse(existing);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        var userId = currentUserProvider.getCurrentUserId();
        Transaction transaction = findOwnedTransaction(id, userId);
        Account account = transaction.getAccount();
        reverseBalanceEffect(account, toTransactionType(transaction.getType()), transaction.getAmount());
        accountRepository.save(account);
        transactionRepository.delete(transaction);
        log.info("event=transaction.deleted transactionId={}", id);
    }

    private Transaction buildTransaction(TransactionCreateRequest request, Account account) {
        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setAccount(account);
        transaction.setType(resolveType(request.type()));
        if (request.category() != null) {
            transaction.setCategory(resolveCategory(request.category()));
        }
        return transaction;
    }

    private Account findOwnedAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private Transaction findOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    private TransactionTypeEntity resolveType(TransactionType type) {
        return transactionTypeRepository.findByName(type.name())
                .orElseThrow(() -> new IllegalArgumentException("Unknown transaction type: " + type));
    }

    private TransactionCategoryEntity resolveCategory(String name) {
        return transactionCategoryRepository.findByName(name)
                .orElseGet(() -> createCategory(name));
    }

    private TransactionCategoryEntity createCategory(String name) {
        transactionCategoryRepository.insertIfAbsent(name);
        return transactionCategoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Failed to create category: " + name));
    }

    private TransactionType toTransactionType(TransactionTypeEntity entity) {
        return TransactionType.valueOf(entity.getName());
    }

    private void checkAccountFrozen(Account account) {
        if (account.isFrozen()) {
            log.warn("event=transaction.account_frozen accountId={}", account.getId());
            throw new AccountFrozenException(
                    "Cannot create transaction: account is frozen");
        }
    }

    private void checkInsufficientFunds(Transaction transaction, Account account) {
        if (toTransactionType(transaction.getType()) == TransactionType.EXPENSE
                && account.getBalance().compareTo(transaction.getAmount()) < 0) {
            log.warn("event=transaction.insufficient_funds accountId={} balance={} requested={}",
                    account.getId(), account.getBalance(), transaction.getAmount());
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + account.getBalance());
        }
    }

    private void applyBalanceEffect(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
    }

    private void reverseBalanceEffect(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
    }

    private boolean isAllFieldsNull(TransactionUpdateRequest request) {
        return request.type() == null
                && request.amount() == null
                && request.description() == null
                && request.transactionDate() == null
                && request.category() == null;
    }

    private void applyUpdates(Transaction existing, TransactionUpdateRequest request) {
        if (request.type() != null) existing.setType(resolveType(request.type()));
        if (request.amount() != null) existing.setAmount(request.amount());
        if (request.description() != null) existing.setDescription(request.description());
        if (request.transactionDate() != null) existing.setTransactionDate(request.transactionDate());
        if (request.category() != null) existing.setCategory(resolveCategory(request.category()));
    }

    private void reconcileBalanceIfNeeded(Transaction existing, TransactionType originalType, BigDecimal originalAmount) {
        TransactionType newType = toTransactionType(existing.getType());
        BigDecimal newAmount = existing.getAmount();
        if (originalType.equals(newType) && originalAmount.compareTo(newAmount) == 0) return;

        Account account = existing.getAccount();
        reverseBalanceEffect(account, originalType, originalAmount);
        checkInsufficientFunds(existing, account);
        applyBalanceEffect(account, newType, newAmount);
        accountRepository.save(account);
    }

    private Specification<Transaction> buildSpec(
            Long accountId,
            TransactionType type,
            String category,
            String search,
            LocalDate from,
            LocalDate to
    ) {
        return Specification.<Transaction>unrestricted()
                .and(TransactionSpecification.accountIdEquals(accountId))
                .and(TransactionSpecification.typeEquals(type))
                .and(TransactionSpecification.categoryEquals(category))
                .and(TransactionSpecification.descriptionContains(search))
                .and(TransactionSpecification.dateBetween(from, to));
    }
}

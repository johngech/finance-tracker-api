package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.accounts.Account;
import com.marakicode.financetracker.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@NamedEntityGraph(name = "Transaction.withAccount", attributeNodes = {
    @NamedAttributeNode("account"),
    @NamedAttributeNode("type"),
    @NamedAttributeNode("category")
})
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "name", nullable = false)
    private TransactionTypeEntity type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category", referencedColumnName = "name")
    private TransactionCategoryEntity category;
}

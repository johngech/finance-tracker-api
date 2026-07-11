package com.marakicode.financetracker.transactions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Transaction Category Repository Tests")
class TransactionCategoryRepositoryTest {

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    private TransactionCategoryEntity saveCategory(String name) {
        var entity = new TransactionCategoryEntity();
        entity.setName(name);
        return transactionCategoryRepository.save(entity);
    }

    @Test
    @DisplayName("findByName_returnsCategory - existing category is found by name")
    void findByName_returnsCategory() {
        // Arrange
        saveCategory("TRANSPORT");

        // Act
        var result = transactionCategoryRepository.findByName("TRANSPORT");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("TRANSPORT");
    }

    @Test
    @DisplayName("findByName_returnsEmpty_whenNotExists - non-existent name returns empty Optional")
    void findByName_returnsEmpty_whenNotExists() {
        // Act
        var result = transactionCategoryRepository.findByName("NONEXISTENT");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByName_caseSensitive - FOOD and food are distinct categories")
    void findByName_caseSensitive() {
        // Arrange
        var upper = saveCategory("FOOD");
        var lower = saveCategory("food");

        // Act
        var foundUpper = transactionCategoryRepository.findByName("FOOD");
        var foundLower = transactionCategoryRepository.findByName("food");

        // Assert
        assertThat(foundUpper).isPresent();
        assertThat(foundLower).isPresent();
        assertThat(foundUpper.get().getId()).isNotEqualTo(foundLower.get().getId());
    }

    @Test
    @Disabled("Native INSERT...ON CONFLICT requires PostgreSQL — H2 does not support this syntax")
    @DisplayName("insertIfAbsent_insertsNewCategory - new category is persisted when it does not exist")
    void insertIfAbsent_insertsNewCategory() {
        // This test validates the native ON CONFLICT query against PostgreSQL
        transactionCategoryRepository.insertIfAbsent("FOOD");

        var result = transactionCategoryRepository.findByName("FOOD");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("FOOD");
    }

    @Test
    @Disabled("Native INSERT...ON CONFLICT requires PostgreSQL — H2 does not support this syntax")
    @DisplayName("insertIfAbsent_noOpsOnDuplicate - duplicate insert does not throw or create second row")
    void insertIfAbsent_noOpsOnDuplicate() {
        // This test validates idempotent insert against PostgreSQL
        transactionCategoryRepository.insertIfAbsent("HOUSING");
        var before = transactionCategoryRepository.findByName("HOUSING");

        transactionCategoryRepository.insertIfAbsent("HOUSING");

        var after = transactionCategoryRepository.findByName("HOUSING");
        assertThat(after).isPresent();
        assertThat(after.get().getId()).isEqualTo(before.get().getId());
    }
}

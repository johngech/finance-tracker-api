package com.marakicode.financetracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FinancetrackerApplicationTests {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
    }

}

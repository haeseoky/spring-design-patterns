package com.ocean.pattern.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 거래 ID 생성기 테스트
 */
@DisplayName("거래 ID 생성기 테스트")
class TransactionIdGeneratorTest {
    
    @Test
    @DisplayName("일반 거래 ID 생성 테스트")
    void testGenerate() {
        // When
        String transactionId = TransactionIdGenerator.generate("TEST");
        
        // Then
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("TEST-"));
        assertEquals(13, transactionId.length()); // "TEST-" (5) + UUID 8자리 (8) = 13
        
        // 두 번 생성했을 때 다른 ID가 나와야 함
        String anotherTransactionId = TransactionIdGenerator.generate("TEST");
        assertNotEquals(transactionId, anotherTransactionId);
    }
    
    @Test
    @DisplayName("신용카드 거래 ID 생성 테스트")
    void testGenerateCreditCard() {
        // When
        String transactionId = TransactionIdGenerator.generateCreditCard();
        
        // Then
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("CC-"));
        assertEquals(11, transactionId.length()); // "CC-" (3) + UUID 8자리 (8) = 11
    }
    
    @Test
    @DisplayName("현금 거래 ID 생성 테스트")
    void testGenerateCash() {
        // When
        String transactionId = TransactionIdGenerator.generateCash();
        
        // Then
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("CASH-"));
        assertEquals(13, transactionId.length()); // "CASH-" (5) + UUID 8자리 (8) = 13
    }
    
    @Test
    @DisplayName("PayPal 거래 ID 생성 테스트")
    void testGeneratePayPal() {
        // When
        String transactionId = TransactionIdGenerator.generatePayPal();
        
        // Then
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("PP-"));
        assertEquals(11, transactionId.length()); // "PP-" (3) + UUID 8자리 (8) = 11
    }
    
    @Test
    @DisplayName("계좌이체 거래 ID 생성 테스트")
    void testGenerateBankTransfer() {
        // When
        String transactionId = TransactionIdGenerator.generateBankTransfer();
        
        // Then
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("BT-"));
        assertEquals(11, transactionId.length()); // "BT-" (3) + UUID 8자리 (8) = 11
    }
    
    @Test
    @DisplayName("거래 ID 고유성 테스트")
    void testTransactionIdUniqueness() {
        // Given
        int testCount = 1000;
        
        // When - 1000개의 거래 ID를 생성
        java.util.Set<String> transactionIds = new java.util.HashSet<>();
        for (int i = 0; i < testCount; i++) {
            transactionIds.add(TransactionIdGenerator.generate("TEST"));
        }
        
        // Then - 모든 거래 ID가 고유해야 함
        assertEquals(testCount, transactionIds.size());
    }
    
    @Test
    @DisplayName("거래 ID 형식 검증 테스트")
    void testTransactionIdFormat() {
        // When
        String transactionId = TransactionIdGenerator.generate("ABC");
        
        // Then
        // 형식: PREFIX-XXXXXXXX (PREFIX는 대소문자, - 하나, 8자리 대문자/숫자)
        String[] parts = transactionId.split("-");
        assertEquals(2, parts.length);
        assertEquals("ABC", parts[0]);
        assertEquals(8, parts[1].length());
        assertTrue(parts[1].matches("[A-Z0-9]{8}"));
    }
    
    @Test
    @DisplayName("빈 접두사 처리 테스트")
    void testEmptyPrefix() {
        // When
        String transactionId = TransactionIdGenerator.generate("");
        
        // Then
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("-"));
        assertEquals(9, transactionId.length()); // "-" (1) + UUID 8자리 (8) = 9
    }
    
    @Test
    @DisplayName("null 접두사 처리 테스트")
    void testNullPrefix() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            TransactionIdGenerator.generate(null);
        });
    }
}
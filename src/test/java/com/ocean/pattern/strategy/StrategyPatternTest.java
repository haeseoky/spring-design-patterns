package com.ocean.pattern.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 전략 패턴 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("전략 패턴 테스트")
class StrategyPatternTest {
    
    private PaymentProcessor paymentProcessor;
    private CreditCardPayment creditCardPayment;
    private CashPayment cashPayment;
    private PayPalPayment payPalPayment;
    private BankTransferPayment bankTransferPayment;
    
    @BeforeEach
    void setUp() {
        paymentProcessor = new PaymentProcessor();
        creditCardPayment = new CreditCardPayment("1234-5678-9012-3456", "김철수", "123", "12/25", 1000000);
        cashPayment = new CashPayment(500000);
        payPalPayment = new PayPalPayment("test@example.com", "password", 300000, true);
        bankTransferPayment = new BankTransferPayment("국민은행", "123-456-789012", "이영희", 2000000, "123456");
    }
    
    @Test
    @DisplayName("신용카드 결제 전략 테스트")
    void testCreditCardPaymentStrategy() {
        // Given
        paymentProcessor.setPaymentStrategy(creditCardPayment);
        double amount = 50000;
        
        // When
        PaymentResult result = paymentProcessor.processPayment(amount);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(amount, result.getAmount());
        assertEquals(amount * 0.025, result.getFee(), 0.01); // 2.5% 수수료
        assertTrue(result.getTransactionId().startsWith("CC-"));
        assertEquals("신용카드 (**** **** **** 3456)", result.getPaymentMethod());
    }
    
    @Test
    @DisplayName("현금 결제 전략 테스트")
    void testCashPaymentStrategy() {
        // Given
        paymentProcessor.setPaymentStrategy(cashPayment);
        double amount = 100000;
        double initialCash = cashPayment.getAvailableCash();
        
        // When
        PaymentResult result = paymentProcessor.processPayment(amount);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(amount, result.getAmount());
        assertEquals(0.0, result.getFee()); // 현금은 수수료 없음
        assertTrue(result.getTransactionId().startsWith("CASH-"));
        assertEquals("현금", result.getPaymentMethod());
        assertEquals(initialCash - amount, cashPayment.getAvailableCash());
    }
    
    @Test
    @DisplayName("PayPal 결제 전략 테스트")
    void testPayPalPaymentStrategy() {
        // Given
        paymentProcessor.setPaymentStrategy(payPalPayment);
        double amount = 80000;
        double initialBalance = payPalPayment.getBalance();
        
        // When
        PaymentResult result = paymentProcessor.processPayment(amount);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(amount, result.getAmount());
        assertEquals((amount * 0.034) + 35.0, result.getFee(), 0.01); // 3.4% + 35원
        assertTrue(result.getTransactionId().startsWith("PP-"));
        assertTrue(result.getPaymentMethod().contains("PayPal"));
        assertEquals(initialBalance - result.getTotalAmount(), payPalPayment.getBalance(), 0.01);
    }
    
    @Test
    @DisplayName("계좌이체 결제 전략 테스트")
    void testBankTransferPaymentStrategy() {
        // Given
        paymentProcessor.setPaymentStrategy(bankTransferPayment);
        double amount = 200000;
        double initialBalance = bankTransferPayment.getBalance();
        
        // When
        PaymentResult result = paymentProcessor.processPayment(amount);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(amount, result.getAmount());
        assertEquals(1000.0, result.getFee()); // 고정 수수료 1000원
        assertTrue(result.getTransactionId().startsWith("BT-"));
        assertTrue(result.getPaymentMethod().contains("계좌이체"));
        assertEquals(initialBalance - result.getTotalAmount(), bankTransferPayment.getBalance());
    }
    
    @Test
    @DisplayName("결제 전략 변경 테스트")
    void testPaymentStrategyChange() {
        // Given
        double amount = 50000;
        
        // When & Then - 신용카드 결제
        paymentProcessor.setPaymentStrategy(creditCardPayment);
        assertEquals("신용카드 (**** **** **** 3456)", paymentProcessor.getCurrentPaymentMethod());
        assertTrue(paymentProcessor.canProcessPayment(amount));
        
        // When & Then - 현금 결제로 변경
        paymentProcessor.setPaymentStrategy(cashPayment);
        assertEquals("현금", paymentProcessor.getCurrentPaymentMethod());
        assertTrue(paymentProcessor.canProcessPayment(amount));
        
        // When & Then - PayPal 결제로 변경
        paymentProcessor.setPaymentStrategy(payPalPayment);
        assertTrue(paymentProcessor.getCurrentPaymentMethod().contains("PayPal"));
        assertTrue(paymentProcessor.canProcessPayment(amount));
    }
    
    @Test
    @DisplayName("결제 불가능한 경우 테스트")
    void testPaymentNotPossible() {
        // Given - 보유 현금보다 큰 금액
        paymentProcessor.setPaymentStrategy(cashPayment);
        double largeAmount = 600000; // 보유 현금 500000보다 큰 금액
        
        // When
        PaymentResult result = paymentProcessor.processPayment(largeAmount);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("보유 현금이 부족"));
        assertNull(result.getTransactionId());
    }
    
    @Test
    @DisplayName("신용카드 한도 초과 테스트")
    void testCreditCardLimitExceeded() {
        // Given
        paymentProcessor.setPaymentStrategy(creditCardPayment);
        double largeAmount = 1100000; // 신용한도 1000000 + 수수료를 초과하는 금액
        
        // When
        PaymentResult result = paymentProcessor.processPayment(largeAmount);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("신용한도를 초과"));
    }
    
    @Test
    @DisplayName("PayPal 계정 미인증 테스트")
    void testPayPalUnverifiedAccount() {
        // Given
        PayPalPayment unverifiedPayPal = new PayPalPayment("test@example.com", "password", 300000, false);
        paymentProcessor.setPaymentStrategy(unverifiedPayPal);
        
        // When
        PaymentResult result = paymentProcessor.processPayment(50000);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("계정이 인증되지 않았습니다"));
    }
    
    @Test
    @DisplayName("결제 수수료 계산 테스트")
    void testFeeCalculation() {
        double amount = 100000;
        
        // 신용카드 수수료 (2.5%)
        assertEquals(2500.0, creditCardPayment.calculateFee(amount), 0.01);
        
        // 현금 수수료 (0%)
        assertEquals(0.0, cashPayment.calculateFee(amount));
        
        // PayPal 수수료 (3.4% + 35원)
        assertEquals(3435.0, payPalPayment.calculateFee(amount), 0.01);
        
        // 계좌이체 수수료 (고정 1000원)
        assertEquals(1000.0, bankTransferPayment.calculateFee(amount));
    }
    
    @Test
    @DisplayName("결제 내역 관리 테스트")
    void testPaymentHistoryManagement() {
        // Given
        paymentProcessor.setPaymentStrategy(creditCardPayment);
        
        // When
        paymentProcessor.processPayment(50000);  // 성공: 50000 + 1250 = 51250
        paymentProcessor.processPayment(30000);  // 성공: 30000 + 750 = 30750  
        paymentProcessor.processPayment(980000); // 실패: 980000 + 24500 = 1004500 > 1000000 한도 초과
        
        // Then
        assertEquals(3, paymentProcessor.getPaymentHistory().size());
        assertEquals(2, paymentProcessor.getSuccessfulPayments().size());
        assertEquals(1, paymentProcessor.getFailedPayments().size());
        assertEquals(80000, paymentProcessor.getTotalPaymentAmount());
        
        // 통계 확인
        PaymentProcessor.PaymentStatistics stats = paymentProcessor.getPaymentStatistics();
        assertEquals(3, stats.totalTransactions);
        assertEquals(2, stats.successfulTransactions);
        assertEquals(1, stats.failedTransactions);
        assertEquals(66.67, stats.successRate, 0.1);
    }
    
    @Test
    @DisplayName("결제 전략 미설정 시 테스트")
    void testPaymentWithoutStrategy() {
        // Given - 전략 설정하지 않음
        
        // When
        PaymentResult result = paymentProcessor.processPayment(50000);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("결제 방법이 선택되지 않았습니다"));
        assertEquals("결제 방법이 설정되지 않음", paymentProcessor.getCurrentPaymentMethod());
        assertFalse(paymentProcessor.canProcessPayment(50000));
    }
    
    @Test
    @DisplayName("유효하지 않은 결제 금액 테스트")
    void testInvalidPaymentAmount() {
        // Given
        paymentProcessor.setPaymentStrategy(cashPayment);
        
        // When - 음수 금액
        PaymentResult result1 = paymentProcessor.processPayment(-1000);
        
        // When - 0원
        PaymentResult result2 = paymentProcessor.processPayment(0);
        
        // Then
        assertFalse(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertTrue(result1.getMessage().contains("유효하지 않습니다"));
        assertTrue(result2.getMessage().contains("유효하지 않습니다"));
    }
    
    @Test
    @DisplayName("결제 내역 초기화 테스트")
    void testClearPaymentHistory() {
        // Given
        paymentProcessor.setPaymentStrategy(cashPayment);
        paymentProcessor.processPayment(10000);
        paymentProcessor.processPayment(20000);
        
        assertEquals(2, paymentProcessor.getPaymentHistory().size());
        
        // When
        paymentProcessor.clearPaymentHistory();
        
        // Then
        assertEquals(0, paymentProcessor.getPaymentHistory().size());
        assertEquals(0, paymentProcessor.getTotalPaymentAmount());
        assertEquals(0, paymentProcessor.getTotalFees());
    }
}
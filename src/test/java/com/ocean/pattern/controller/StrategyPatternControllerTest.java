package com.ocean.pattern.controller;

import com.ocean.pattern.strategy.PaymentProcessor;
import com.ocean.pattern.strategy.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

/**
 * 전략 패턴 컨트롤러 테스트
 */
@WebFluxTest(StrategyPatternController.class)
@DisplayName("전략 패턴 컨트롤러 테스트")
class StrategyPatternControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private PaymentProcessor paymentProcessor;
    
    @BeforeEach
    void setUp() {
        reset(paymentProcessor);
    }
    
    @Test
    @DisplayName("신용카드 결제 API 테스트 - 성공")
    void testCreditCardPayment_Success() {
        // Given
        PaymentResult mockResult = PaymentResult.success("CC-12345678", 50000, 1250, "신용카드");
        when(paymentProcessor.processPayment(anyDouble())).thenReturn(mockResult);
        
        Map<String, Object> requestBody = Map.of(
                "amount", "50000",
                "cardNumber", "1234-5678-9012-3456",
                "holderName", "김철수",
                "cvv", "123",
                "expiryDate", "12/25",
                "creditLimit", "1000000"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/payment/creditcard")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.amount").isEqualTo(50000)
                .jsonPath("$.fee").isEqualTo(1250)
                .jsonPath("$.transactionId").isEqualTo("CC-12345678")
                .jsonPath("$.paymentMethod").isEqualTo("신용카드");
        
        verify(paymentProcessor).setPaymentStrategy(any());
        verify(paymentProcessor).processPayment(50000);
    }
    
    @Test
    @DisplayName("현금 결제 API 테스트 - 성공")
    void testCashPayment_Success() {
        // Given
        PaymentResult mockResult = PaymentResult.success("CASH-87654321", 30000, 0, "현금");
        when(paymentProcessor.processPayment(anyDouble())).thenReturn(mockResult);
        
        Map<String, Object> requestBody = Map.of(
                "amount", "30000",
                "availableCash", "500000"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/payment/cash")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.amount").isEqualTo(30000)
                .jsonPath("$.fee").isEqualTo(0)
                .jsonPath("$.transactionId").isEqualTo("CASH-87654321")
                .jsonPath("$.paymentMethod").isEqualTo("현금");
        
        verify(paymentProcessor).setPaymentStrategy(any());
        verify(paymentProcessor).processPayment(30000);
    }
    
    @Test
    @DisplayName("PayPal 결제 API 테스트 - 성공")
    void testPayPalPayment_Success() {
        // Given
        PaymentResult mockResult = PaymentResult.success("PP-ABCD1234", 40000, 1395, "PayPal");
        when(paymentProcessor.processPayment(anyDouble())).thenReturn(mockResult);
        
        Map<String, Object> requestBody = Map.of(
                "amount", "40000",
                "email", "test@example.com",
                "password", "password123",
                "balance", "300000",
                "isVerified", "true"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/payment/paypal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.amount").isEqualTo(40000)
                .jsonPath("$.fee").isEqualTo(1395)
                .jsonPath("$.transactionId").isEqualTo("PP-ABCD1234")
                .jsonPath("$.paymentMethod").isEqualTo("PayPal");
        
        verify(paymentProcessor).setPaymentStrategy(any());
        verify(paymentProcessor).processPayment(40000);
    }
    
    @Test
    @DisplayName("계좌이체 결제 API 테스트 - 성공")
    void testBankTransferPayment_Success() {
        // Given
        PaymentResult mockResult = PaymentResult.success("BT-WXYZ9876", 80000, 1000, "계좌이체");
        when(paymentProcessor.processPayment(anyDouble())).thenReturn(mockResult);
        
        Map<String, Object> requestBody = Map.of(
                "amount", "80000",
                "bankName", "국민은행",
                "accountNumber", "123-456-789012",
                "accountHolder", "이영희",
                "balance", "2000000",
                "pin", "123456"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/payment/banktransfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.amount").isEqualTo(80000)
                .jsonPath("$.fee").isEqualTo(1000)
                .jsonPath("$.transactionId").isEqualTo("BT-WXYZ9876")
                .jsonPath("$.paymentMethod").isEqualTo("계좌이체");
        
        verify(paymentProcessor).setPaymentStrategy(any());
        verify(paymentProcessor).processPayment(80000);
    }
    
    @Test
    @DisplayName("결제 실패 API 테스트")
    void testPaymentFailure() {
        // Given
        PaymentResult mockResult = PaymentResult.failure("잔액 부족", 100000, "현금");
        when(paymentProcessor.processPayment(anyDouble())).thenReturn(mockResult);
        
        Map<String, Object> requestBody = Map.of(
                "amount", "100000",
                "availableCash", "50000"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/payment/cash")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("잔액 부족")
                .jsonPath("$.amount").isEqualTo(100000)
                .jsonPath("$.transactionId").isEmpty();
        
        verify(paymentProcessor).processPayment(100000);
    }
    
    @Test
    @DisplayName("수수료 계산 API 테스트")
    void testCalculateFee() {
        // Given
        Map<String, Object> requestBody = Map.of(
                "amount", "100000",
                "paymentMethod", "creditcard",
                "cardNumber", "1234-5678-9012-3456",
                "holderName", "김철수",
                "cvv", "123",
                "expiryDate", "12/25"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/calculate-fee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.amount").isEqualTo(100000)
                .jsonPath("$.fee").isEqualTo(2500) // 2.5%
                .jsonPath("$.totalAmount").isEqualTo(102500)
                .jsonPath("$.paymentPossible").isEqualTo(true);
    }
    
    @Test
    @DisplayName("결제 내역 조회 API 테스트")
    void testGetPaymentHistory() {
        // Given
        PaymentResult result1 = PaymentResult.success("TXN-001", 50000, 1250, "신용카드");
        PaymentResult result2 = PaymentResult.success("TXN-002", 30000, 0, "현금");
        PaymentProcessor.PaymentStatistics stats = new PaymentProcessor.PaymentStatistics(2, 2, 0, 80000, 1250);
        
        when(paymentProcessor.getPaymentHistory()).thenReturn(List.of(result1, result2));
        when(paymentProcessor.getPaymentStatistics()).thenReturn(stats);
        when(paymentProcessor.getTotalPaymentAmount()).thenReturn(80000.0);
        when(paymentProcessor.getTotalFees()).thenReturn(1250.0);
        
        // When & Then
        webTestClient.get()
                .uri("/api/strategy/payment-history")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.history").isArray()
                .jsonPath("$.history.length()").isEqualTo(2)
                .jsonPath("$.totalAmount").isEqualTo(80000)
                .jsonPath("$.totalFees").isEqualTo(1250);
        
        verify(paymentProcessor).getPaymentHistory();
        verify(paymentProcessor).getPaymentStatistics();
    }
    
    @Test
    @DisplayName("결제 통계 조회 API 테스트")
    void testGetPaymentStatistics() {
        // Given
        PaymentProcessor.PaymentStatistics stats = new PaymentProcessor.PaymentStatistics(5, 4, 1, 200000, 5000);
        when(paymentProcessor.getPaymentStatistics()).thenReturn(stats);
        
        // When & Then
        webTestClient.get()
                .uri("/api/strategy/statistics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalTransactions").isEqualTo(5)
                .jsonPath("$.successfulTransactions").isEqualTo(4)
                .jsonPath("$.failedTransactions").isEqualTo(1)
                .jsonPath("$.totalAmount").isEqualTo(200000)
                .jsonPath("$.totalFees").isEqualTo(5000)
                .jsonPath("$.successRate").isEqualTo(80.0);
        
        verify(paymentProcessor).getPaymentStatistics();
    }
    
    @Test
    @DisplayName("결제 내역 초기화 API 테스트")
    void testClearPaymentHistory() {
        // When & Then
        webTestClient.delete()
                .uri("/api/strategy/payment-history")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("결제 내역이 초기화되었습니다.");
        
        verify(paymentProcessor).clearPaymentHistory();
    }
    
    @Test
    @DisplayName("전략 패턴 데모 API 테스트")
    void testDemo() {
        // Given
        PaymentResult creditResult = PaymentResult.success("CC-001", 50000, 1250, "신용카드");
        PaymentResult cashResult = PaymentResult.success("CASH-002", 30000, 0, "현금");
        PaymentResult paypalResult = PaymentResult.success("PP-003", 40000, 1395, "PayPal");
        PaymentResult bankResult = PaymentResult.success("BT-004", 80000, 1000, "계좌이체");
        PaymentProcessor.PaymentStatistics stats = new PaymentProcessor.PaymentStatistics(4, 4, 0, 200000, 3645);
        
        when(paymentProcessor.processPayment(anyDouble()))
                .thenReturn(creditResult)
                .thenReturn(cashResult)
                .thenReturn(paypalResult)
                .thenReturn(bankResult);
        when(paymentProcessor.getPaymentStatistics()).thenReturn(stats);
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/demo")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("전략 패턴 데모가 완료되었습니다.")
                .jsonPath("$.results").exists()
                .jsonPath("$.results.creditCardPayment").exists()
                .jsonPath("$.results.cashPayment").exists()
                .jsonPath("$.results.paypalPayment").exists()
                .jsonPath("$.results.bankTransferPayment").exists()
                .jsonPath("$.statistics").exists();
        
        verify(paymentProcessor, times(4)).setPaymentStrategy(any());
        verify(paymentProcessor, times(4)).processPayment(anyDouble());
        verify(paymentProcessor).getPaymentStatistics();
    }
    
    @Test
    @DisplayName("잘못된 요청 데이터 테스트")
    void testInvalidRequestData() {
        // Given - amount 필드 누락
        Map<String, Object> invalidRequest = Map.of(
                "cardNumber", "1234-5678-9012-3456",
                "holderName", "김철수"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/payment/creditcard")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").exists();
        
        verify(paymentProcessor, never()).processPayment(anyDouble());
    }
    
    @Test
    @DisplayName("지원하지 않는 결제 방법 테스트")
    void testUnsupportedPaymentMethod() {
        // Given
        Map<String, Object> requestBody = Map.of(
                "amount", "50000",
                "paymentMethod", "bitcoin" // 지원하지 않는 결제 방법
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/strategy/calculate-fee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").value(org.hamcrest.Matchers.containsString("지원하지 않는 결제 방법"));
    }
}
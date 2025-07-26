package com.ocean.pattern.controller;

import com.ocean.pattern.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 전략 패턴 데모 컨트롤러
 * REST API를 통해 전략 패턴을 시연하고 테스트할 수 있는 엔드포인트들을 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/strategy")
@RequiredArgsConstructor
public class StrategyPatternController {
    
    private final PaymentProcessor paymentProcessor;
    
    /**
     * 신용카드 결제 처리
     */
    @PostMapping("/payment/creditcard")
    public ResponseEntity<Map<String, Object>> payCreditCard(@RequestBody Map<String, Object> request) {
        try {
            double amount = Double.parseDouble(request.get("amount").toString());
            String cardNumber = (String) request.get("cardNumber");
            String holderName = (String) request.get("holderName");
            String cvv = (String) request.get("cvv");
            String expiryDate = (String) request.get("expiryDate");
            double creditLimit = Double.parseDouble(request.getOrDefault("creditLimit", "1000000").toString());
            
            // 신용카드 결제 전략 설정
            CreditCardPayment creditCardPayment = new CreditCardPayment(
                cardNumber, holderName, cvv, expiryDate, creditLimit
            );
            paymentProcessor.setPaymentStrategy(creditCardPayment);
            
            // 결제 처리
            PaymentResult result = paymentProcessor.processPayment(amount);
            
            Map<String, Object> response = createPaymentResponse(result);
            response.put("paymentMethod", "신용카드");
            response.put("cardInfo", creditCardPayment.getCardInfo());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("신용카드 결제 처리 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "신용카드 결제 요청이 올바르지 않습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 현금 결제 처리
     */
    @PostMapping("/payment/cash")
    public ResponseEntity<Map<String, Object>> payCash(@RequestBody Map<String, Object> request) {
        try {
            double amount = Double.parseDouble(request.get("amount").toString());
            double availableCash = Double.parseDouble(request.getOrDefault("availableCash", "500000").toString());
            
            // 현금 결제 전략 설정
            CashPayment cashPayment = new CashPayment(availableCash);
            paymentProcessor.setPaymentStrategy(cashPayment);
            
            // 결제 처리
            PaymentResult result = paymentProcessor.processPayment(amount);
            
            Map<String, Object> response = createPaymentResponse(result);
            response.put("paymentMethod", "현금");
            response.put("remainingCash", cashPayment.getAvailableCash());
            response.put("cashInfo", cashPayment.getCashInfo());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("현금 결제 처리 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "현금 결제 요청이 올바르지 않습니다: " + e.getMessage()));
        }
    }
    
    /**
     * PayPal 결제 처리
     */
    @PostMapping("/payment/paypal")
    public ResponseEntity<Map<String, Object>> payPayPal(@RequestBody Map<String, Object> request) {
        try {
            double amount = Double.parseDouble(request.get("amount").toString());
            String email = (String) request.get("email");
            String password = (String) request.get("password");
            double balance = Double.parseDouble(request.getOrDefault("balance", "300000").toString());
            boolean isVerified = Boolean.parseBoolean(request.getOrDefault("isVerified", "true").toString());
            
            // PayPal 결제 전략 설정
            PayPalPayment payPalPayment = new PayPalPayment(email, password, balance, isVerified);
            paymentProcessor.setPaymentStrategy(payPalPayment);
            
            // 결제 처리
            PaymentResult result = paymentProcessor.processPayment(amount);
            
            Map<String, Object> response = createPaymentResponse(result);
            response.put("paymentMethod", "PayPal");
            response.put("accountInfo", payPalPayment.getAccountInfo());
            response.put("remainingBalance", payPalPayment.getBalance());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("PayPal 결제 처리 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "PayPal 결제 요청이 올바르지 않습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 계좌이체 결제 처리
     */
    @PostMapping("/payment/banktransfer")
    public ResponseEntity<Map<String, Object>> payBankTransfer(@RequestBody Map<String, Object> request) {
        try {
            double amount = Double.parseDouble(request.get("amount").toString());
            String bankName = (String) request.get("bankName");
            String accountNumber = (String) request.get("accountNumber");
            String accountHolder = (String) request.get("accountHolder");
            double balance = Double.parseDouble(request.getOrDefault("balance", "2000000").toString());
            String pin = (String) request.get("pin");
            
            // 계좌이체 결제 전략 설정
            BankTransferPayment bankTransferPayment = new BankTransferPayment(
                bankName, accountNumber, accountHolder, balance, pin
            );
            paymentProcessor.setPaymentStrategy(bankTransferPayment);
            
            // 결제 처리
            PaymentResult result = paymentProcessor.processPayment(amount);
            
            Map<String, Object> response = createPaymentResponse(result);
            response.put("paymentMethod", "계좌이체");
            response.put("accountInfo", bankTransferPayment.getAccountInfo());
            response.put("limitInfo", bankTransferPayment.getLimitInfo());
            response.put("remainingBalance", bankTransferPayment.getBalance());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("계좌이체 결제 처리 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "계좌이체 결제 요청이 올바르지 않습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 결제 수수료 계산
     */
    @PostMapping("/calculate-fee")
    public ResponseEntity<Map<String, Object>> calculateFee(@RequestBody Map<String, Object> request) {
        try {
            double amount = Double.parseDouble(request.get("amount").toString());
            String paymentMethod = (String) request.get("paymentMethod");
            
            PaymentStrategy strategy = createPaymentStrategy(paymentMethod, request);
            if (strategy == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "지원하지 않는 결제 방법입니다: " + paymentMethod));
            }
            
            double fee = strategy.calculateFee(amount);
            boolean possible = strategy.isPaymentPossible(amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("amount", amount);
            response.put("fee", fee);
            response.put("totalAmount", amount + fee);
            response.put("paymentMethod", strategy.getPaymentMethodName());
            response.put("paymentPossible", possible);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("수수료 계산 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "수수료 계산 요청이 올바르지 않습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 결제 내역 조회
     */
    @GetMapping("/payment-history")
    public ResponseEntity<Map<String, Object>> getPaymentHistory() {
        Map<String, Object> response = new HashMap<>();
        response.put("history", paymentProcessor.getPaymentHistory());
        response.put("statistics", paymentProcessor.getPaymentStatistics());
        response.put("totalAmount", paymentProcessor.getTotalPaymentAmount());
        response.put("totalFees", paymentProcessor.getTotalFees());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<PaymentProcessor.PaymentStatistics> getPaymentStatistics() {
        return ResponseEntity.ok(paymentProcessor.getPaymentStatistics());
    }
    
    /**
     * 결제 내역 초기화
     */
    @DeleteMapping("/payment-history")
    public ResponseEntity<Map<String, Object>> clearPaymentHistory() {
        paymentProcessor.clearPaymentHistory();
        return ResponseEntity.ok(Map.of("message", "결제 내역이 초기화되었습니다."));
    }
    
    /**
     * 전략 패턴 데모 실행
     */
    @PostMapping("/demo")
    public ResponseEntity<Map<String, Object>> runDemo() {
        log.info("전략 패턴 데모를 시작합니다.");
        
        Map<String, Object> demoResults = new HashMap<>();
        
        // 1. 신용카드 결제 데모
        CreditCardPayment creditCard = new CreditCardPayment(
            "1234-5678-9012-3456", "김철수", "123", "12/25", 1000000
        );
        paymentProcessor.setPaymentStrategy(creditCard);
        PaymentResult creditResult = paymentProcessor.processPayment(50000);
        demoResults.put("creditCardPayment", creditResult);
        
        // 2. 현금 결제 데모
        CashPayment cash = new CashPayment(100000);
        paymentProcessor.setPaymentStrategy(cash);
        PaymentResult cashResult = paymentProcessor.processPayment(30000);
        demoResults.put("cashPayment", cashResult);
        
        // 3. PayPal 결제 데모
        PayPalPayment paypal = new PayPalPayment("demo@example.com", "password", 200000, true);
        paymentProcessor.setPaymentStrategy(paypal);
        PaymentResult paypalResult = paymentProcessor.processPayment(40000);
        demoResults.put("paypalPayment", paypalResult);
        
        // 4. 계좌이체 결제 데모
        BankTransferPayment bankTransfer = new BankTransferPayment(
            "국민은행", "123-456-789012", "이영희", 500000, "123456"
        );
        paymentProcessor.setPaymentStrategy(bankTransfer);
        PaymentResult bankResult = paymentProcessor.processPayment(80000);
        demoResults.put("bankTransferPayment", bankResult);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "전략 패턴 데모가 완료되었습니다.");
        response.put("results", demoResults);
        response.put("statistics", paymentProcessor.getPaymentStatistics());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 결과 응답 객체 생성
     */
    private Map<String, Object> createPaymentResponse(PaymentResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("transactionId", result.getTransactionId());
        response.put("amount", result.getAmount());
        response.put("fee", result.getFee());
        response.put("totalAmount", result.getTotalAmount());
        response.put("timestamp", result.getTimestamp());
        
        return response;
    }
    
    /**
     * 결제 방법에 따른 전략 객체 생성
     */
    private PaymentStrategy createPaymentStrategy(String paymentMethod, Map<String, Object> params) {
        return switch (paymentMethod.toLowerCase()) {
            case "creditcard", "신용카드" -> new CreditCardPayment(
                (String) params.get("cardNumber"),
                (String) params.get("holderName"),
                (String) params.get("cvv"),
                (String) params.get("expiryDate"),
                Double.parseDouble(params.getOrDefault("creditLimit", "1000000").toString())
            );
            case "cash", "현금" -> new CashPayment(
                Double.parseDouble(params.getOrDefault("availableCash", "500000").toString())
            );
            case "paypal" -> new PayPalPayment(
                (String) params.get("email"),
                (String) params.get("password"),
                Double.parseDouble(params.getOrDefault("balance", "300000").toString()),
                Boolean.parseBoolean(params.getOrDefault("isVerified", "true").toString())
            );
            case "banktransfer", "계좌이체" -> new BankTransferPayment(
                (String) params.get("bankName"),
                (String) params.get("accountNumber"),
                (String) params.get("accountHolder"),
                Double.parseDouble(params.getOrDefault("balance", "2000000").toString()),
                (String) params.get("pin")
            );
            default -> null;
        };
    }
}
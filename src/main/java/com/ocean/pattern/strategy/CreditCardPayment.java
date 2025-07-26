package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 신용카드 결제 전략 - PaymentStrategy 구현체
 * 신용카드를 통한 결제 처리를 담당
 */
@Slf4j
public class CreditCardPayment implements PaymentStrategy {
    
    private final String cardNumber;
    private final String cardHolderName;
    private final String cvv;
    private final String expiryDate;
    private final double creditLimit;
    
    public CreditCardPayment(String cardNumber, String cardHolderName, String cvv, String expiryDate, double creditLimit) {
        this.cardNumber = maskCardNumber(cardNumber);
        this.cardHolderName = cardHolderName;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.creditLimit = creditLimit;
    }
    
    @Override
    public PaymentResult processPayment(double amount) {
        log.info("[신용카드 결제] 결제 시작 - 금액: {}원, 카드번호: {}", amount, cardNumber);
        
        if (!isPaymentPossible(amount)) {
            log.warn("[신용카드 결제] 결제 실패 - 신용한도 초과");
            return PaymentResult.failure("신용한도를 초과했습니다.", amount, getPaymentMethodName());
        }
        
        // 신용카드 결제 시뮬레이션
        try {
            // 카드 유효성 검증
            if (!validateCard()) {
                log.warn("[신용카드 결제] 결제 실패 - 카드 정보 유효성 검증 실패");
                return PaymentResult.failure("카드 정보가 유효하지 않습니다.", amount, getPaymentMethodName());
            }
            
            // 결제 처리 시뮬레이션 (은행 API 호출 등)
            Thread.sleep(1000); // 네트워크 지연 시뮬레이션
            
            double fee = calculateFee(amount);
            String transactionId = generateTransactionId();
            
            log.info("[신용카드 결제] 결제 성공 - 거래ID: {}, 수수료: {}원", transactionId, fee);
            
            return PaymentResult.success(transactionId, amount, fee, getPaymentMethodName());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[신용카드 결제] 결제 처리 중 오류 발생", e);
            return PaymentResult.failure("결제 처리 중 오류가 발생했습니다.", amount, getPaymentMethodName());
        }
    }
    
    @Override
    public String getPaymentMethodName() {
        return "신용카드 (" + cardNumber + ")";
    }
    
    @Override
    public boolean isPaymentPossible(double amount) {
        double totalAmount = amount + calculateFee(amount);
        return totalAmount <= creditLimit && amount > 0;
    }
    
    @Override
    public double calculateFee(double amount) {
        // 신용카드 수수료: 2.5%
        return amount * 0.025;
    }
    
    /**
     * 카드 유효성 검증
     */
    private boolean validateCard() {
        // 실제로는 카드번호 알고리즘, 만료일, CVV 등을 검증
        return cardNumber != null && !cardNumber.isEmpty() &&
               expiryDate != null && !expiryDate.isEmpty() &&
               cvv != null && cvv.length() == 3;
    }
    
    /**
     * 카드번호 마스킹
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
    
    /**
     * 거래 ID 생성
     */
    private String generateTransactionId() {
        return "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 카드 정보 조회 (마스킹된)
     */
    public String getCardInfo() {
        return String.format("카드번호: %s, 소유자: %s, 한도: %.0f원", 
                           cardNumber, cardHolderName, creditLimit);
    }
}
package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;

/**
 * 현금 결제 전략 - PaymentStrategy 구현체
 * 현금을 통한 결제 처리를 담당
 */
@Slf4j
public class CashPayment implements PaymentStrategy {
    
    // 상수 정의
    private static final double CASH_FEE_RATE = 0.0; // 현금은 수수료 없음
    private static final String PAYMENT_METHOD_NAME = "현금";
    private static final String LOG_PREFIX = "[현금 결제]";
    
    private double availableCash;
    
    public CashPayment(double availableCash) {
        if (availableCash < 0) {
            throw new IllegalArgumentException("보유 현금은 0 이상이어야 합니다.");
        }
        this.availableCash = availableCash;
    }
    
    @Override
    public PaymentResult processPayment(double amount) {
        log.info("{} 결제 시작 - 금액: {}원, 보유 현금: {}원", LOG_PREFIX, amount, availableCash);
        
        // 입력 유효성 검증
        if (amount <= 0) {
            log.warn("{} 결제 실패 - 유효하지 않은 금액: {}", LOG_PREFIX, amount);
            return PaymentResult.failure("결제 금액이 유효하지 않습니다.", amount, getPaymentMethodName());
        }
        
        if (!isPaymentPossible(amount)) {
            log.warn("{} 결제 실패 - 보유 현금 부족 (필요: {}, 보유: {})", LOG_PREFIX, amount, availableCash);
            return PaymentResult.failure("보유 현금이 부족합니다.", amount, getPaymentMethodName());
        }
        
        // 현금 결제 시뮬레이션
        try {
            // 현금 결제는 즉시 처리 (네트워크 지연 없음)
            double fee = calculateFee(amount);
            double totalAmount = amount + fee;
            
            // 현금 차감
            availableCash -= totalAmount;
            
            String transactionId = TransactionIdGenerator.generateCash();
            
            log.info("{} 결제 성공 - 거래ID: {}, 잔여 현금: {}원", LOG_PREFIX, transactionId, availableCash);
            
            return PaymentResult.success(transactionId, amount, fee, getPaymentMethodName());
            
        } catch (Exception e) {
            log.error("{} 결제 처리 중 오류 발생", LOG_PREFIX, e);
            return PaymentResult.failure("결제 처리 중 오류가 발생했습니다.", amount, getPaymentMethodName());
        }
    }
    
    @Override
    public String getPaymentMethodName() {
        return PAYMENT_METHOD_NAME;
    }
    
    @Override
    public boolean isPaymentPossible(double amount) {
        double totalAmount = amount + calculateFee(amount);
        return totalAmount <= availableCash && amount > 0;
    }
    
    @Override
    public double calculateFee(double amount) {
        // 현금 결제는 수수료 없음
        return amount * CASH_FEE_RATE;
    }
    
    /**
     * 현금 추가
     * @param amount 추가할 금액
     * @throws IllegalArgumentException 추가 금액이 0 이하인 경우
     */
    public void addCash(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("추가할 현금은 0보다 커야 합니다.");
        }
        
        availableCash += amount;
        log.info("{} 현금 추가 - 추가 금액: {}원, 총 보유액: {}원", LOG_PREFIX, amount, availableCash);
    }
    
    /**
     * 보유 현금 조회
     */
    public double getAvailableCash() {
        return availableCash;
    }
    
    
    /**
     * 현금 상태 정보
     */
    public String getCashInfo() {
        return String.format("보유 현금: %.0f원", availableCash);
    }
}
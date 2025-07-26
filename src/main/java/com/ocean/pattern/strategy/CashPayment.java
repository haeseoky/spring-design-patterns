package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 현금 결제 전략 - PaymentStrategy 구현체
 * 현금을 통한 결제 처리를 담당
 */
@Slf4j
public class CashPayment implements PaymentStrategy {
    
    private double availableCash;
    
    public CashPayment(double availableCash) {
        this.availableCash = availableCash;
    }
    
    @Override
    public PaymentResult processPayment(double amount) {
        log.info("[현금 결제] 결제 시작 - 금액: {}원, 보유 현금: {}원", amount, availableCash);
        
        if (!isPaymentPossible(amount)) {
            log.warn("[현금 결제] 결제 실패 - 보유 현금 부족");
            return PaymentResult.failure("보유 현금이 부족합니다.", amount, getPaymentMethodName());
        }
        
        // 현금 결제 시뮬레이션
        try {
            // 현금 결제는 즉시 처리 (네트워크 지연 없음)
            double fee = calculateFee(amount);
            double totalAmount = amount + fee;
            
            // 현금 차감
            availableCash -= totalAmount;
            
            String transactionId = generateTransactionId();
            
            log.info("[현금 결제] 결제 성공 - 거래ID: {}, 잔여 현금: {}원", transactionId, availableCash);
            
            return PaymentResult.success(transactionId, amount, fee, getPaymentMethodName());
            
        } catch (Exception e) {
            log.error("[현금 결제] 결제 처리 중 오류 발생", e);
            return PaymentResult.failure("결제 처리 중 오류가 발생했습니다.", amount, getPaymentMethodName());
        }
    }
    
    @Override
    public String getPaymentMethodName() {
        return "현금";
    }
    
    @Override
    public boolean isPaymentPossible(double amount) {
        double totalAmount = amount + calculateFee(amount);
        return totalAmount <= availableCash && amount > 0;
    }
    
    @Override
    public double calculateFee(double amount) {
        // 현금 결제는 수수료 없음
        return 0.0;
    }
    
    /**
     * 현금 추가
     */
    public void addCash(double amount) {
        if (amount > 0) {
            availableCash += amount;
            log.info("[현금 결제] 현금 추가 - 추가 금액: {}원, 총 보유액: {}원", amount, availableCash);
        }
    }
    
    /**
     * 보유 현금 조회
     */
    public double getAvailableCash() {
        return availableCash;
    }
    
    /**
     * 거래 ID 생성
     */
    private String generateTransactionId() {
        return "CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 현금 상태 정보
     */
    public String getCashInfo() {
        return String.format("보유 현금: %.0f원", availableCash);
    }
}
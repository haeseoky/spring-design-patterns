package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * PayPal 결제 전략 - PaymentStrategy 구현체
 * PayPal을 통한 온라인 결제 처리를 담당
 */
@Slf4j
public class PayPalPayment implements PaymentStrategy {
    
    private final String email;
    private final String password;
    private double balance;
    private boolean isVerified;
    
    public PayPalPayment(String email, String password, double balance, boolean isVerified) {
        this.email = email;
        this.password = maskPassword(password);
        this.balance = balance;
        this.isVerified = isVerified;
    }
    
    @Override
    public PaymentResult processPayment(double amount) {
        log.info("[PayPal 결제] 결제 시작 - 금액: {}원, 계정: {}", amount, email);
        
        if (!isPaymentPossible(amount)) {
            log.warn("[PayPal 결제] 결제 실패 - 잔액 부족 또는 계정 미인증");
            return PaymentResult.failure("PayPal 계정 잔액이 부족하거나 계정이 인증되지 않았습니다.", 
                                       amount, getPaymentMethodName());
        }
        
        // PayPal 결제 시뮬레이션
        try {
            // 계정 인증 확인
            if (!authenticate()) {
                log.warn("[PayPal 결제] 결제 실패 - 계정 인증 실패");
                return PaymentResult.failure("PayPal 계정 인증에 실패했습니다.", amount, getPaymentMethodName());
            }
            
            // PayPal API 호출 시뮬레이션
            Thread.sleep(1500); // 네트워크 지연 시뮬레이션 (PayPal은 조금 더 오래 걸림)
            
            double fee = calculateFee(amount);
            double totalAmount = amount + fee;
            
            // 잔액 차감
            balance -= totalAmount;
            
            String transactionId = generateTransactionId();
            
            log.info("[PayPal 결제] 결제 성공 - 거래ID: {}, 수수료: {}원, 잔여 잔액: {}원", 
                    transactionId, fee, balance);
            
            return PaymentResult.success(transactionId, amount, fee, getPaymentMethodName());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PayPal 결제] 결제 처리 중 오류 발생", e);
            return PaymentResult.failure("PayPal 결제 처리 중 오류가 발생했습니다.", amount, getPaymentMethodName());
        }
    }
    
    @Override
    public String getPaymentMethodName() {
        return "PayPal (" + email + ")";
    }
    
    @Override
    public boolean isPaymentPossible(double amount) {
        double totalAmount = amount + calculateFee(amount);
        return isVerified && totalAmount <= balance && amount > 0;
    }
    
    @Override
    public double calculateFee(double amount) {
        // PayPal 수수료: 3.4% + 고정 수수료 35원
        return (amount * 0.034) + 35.0;
    }
    
    /**
     * PayPal 계정 인증
     */
    private boolean authenticate() {
        // 실제로는 PayPal API를 통한 인증 처리
        return email != null && !email.isEmpty() && 
               password != null && !password.isEmpty() && 
               isVerified;
    }
    
    /**
     * 비밀번호 마스킹
     */
    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "****";
        }
        return "*".repeat(password.length());
    }
    
    /**
     * 거래 ID 생성
     */
    private String generateTransactionId() {
        return "PP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * PayPal 계정에 금액 추가 (충전)
     */
    public void addBalance(double amount) {
        if (amount > 0) {
            balance += amount;
            log.info("[PayPal] 계정 충전 - 충전 금액: {}원, 총 잔액: {}원", amount, balance);
        }
    }
    
    /**
     * 계정 인증 상태 변경
     */
    public void setVerified(boolean verified) {
        this.isVerified = verified;
        log.info("[PayPal] 계정 인증 상태 변경: {}", verified ? "인증됨" : "미인증");
    }
    
    /**
     * PayPal 계정 정보 조회
     */
    public String getAccountInfo() {
        return String.format("이메일: %s, 잔액: %.0f원, 인증상태: %s", 
                           email, balance, isVerified ? "인증됨" : "미인증");
    }
    
    /**
     * 잔액 조회
     */
    public double getBalance() {
        return balance;
    }
    
    /**
     * 인증 상태 조회
     */
    public boolean isVerified() {
        return isVerified;
    }
}
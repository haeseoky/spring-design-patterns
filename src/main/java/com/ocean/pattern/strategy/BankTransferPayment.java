package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 계좌이체 결제 전략 - PaymentStrategy 구현체
 * 은행 계좌이체를 통한 결제 처리를 담당
 */
@Slf4j
public class BankTransferPayment implements PaymentStrategy {
    
    private final String bankName;
    private final String accountNumber;
    private final String accountHolderName;
    private double balance;
    private final String pin;
    
    public BankTransferPayment(String bankName, String accountNumber, String accountHolderName, 
                               double balance, String pin) {
        this.bankName = bankName;
        this.accountNumber = maskAccountNumber(accountNumber);
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.pin = pin;
    }
    
    @Override
    public PaymentResult processPayment(double amount) {
        log.info("[계좌이체] 결제 시작 - 금액: {}원, 은행: {}, 계좌: {}", amount, bankName, accountNumber);
        
        if (!isPaymentPossible(amount)) {
            log.warn("[계좌이체] 결제 실패 - 계좌 잔액 부족");
            return PaymentResult.failure("계좌 잔액이 부족합니다.", amount, getPaymentMethodName());
        }
        
        // 계좌이체 결제 시뮬레이션
        try {
            // PIN 번호 검증
            if (!validatePin()) {
                log.warn("[계좌이체] 결제 실패 - PIN 번호 검증 실패");
                return PaymentResult.failure("PIN 번호가 올바르지 않습니다.", amount, getPaymentMethodName());
            }
            
            // 은행 시스템 연동 시뮬레이션
            Thread.sleep(2000); // 은행 시스템은 상대적으로 느림
            
            double fee = calculateFee(amount);
            double totalAmount = amount + fee;
            
            // 계좌 잔액 차감
            balance -= totalAmount;
            
            String transactionId = generateTransactionId();
            
            log.info("[계좌이체] 결제 성공 - 거래ID: {}, 수수료: {}원, 잔여 잔액: {}원", 
                    transactionId, fee, balance);
            
            return PaymentResult.success(transactionId, amount, fee, getPaymentMethodName());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[계좌이체] 결제 처리 중 오류 발생", e);
            return PaymentResult.failure("계좌이체 처리 중 오류가 발생했습니다.", amount, getPaymentMethodName());
        }
    }
    
    @Override
    public String getPaymentMethodName() {
        return "계좌이체 (" + bankName + " " + accountNumber + ")";
    }
    
    @Override
    public boolean isPaymentPossible(double amount) {
        double totalAmount = amount + calculateFee(amount);
        return totalAmount <= balance && amount > 0 && amount <= getDailyLimit();
    }
    
    @Override
    public double calculateFee(double amount) {
        // 계좌이체 수수료: 1,000원 고정 (금액에 관계없이)
        return 1000.0;
    }
    
    /**
     * PIN 번호 검증
     */
    private boolean validatePin() {
        // 실제로는 은행 시스템을 통한 PIN 검증
        return pin != null && pin.length() == 6;
    }
    
    /**
     * 계좌번호 마스킹
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "***-***-****";
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "***-***-" + lastFour;
    }
    
    /**
     * 거래 ID 생성
     */
    private String generateTransactionId() {
        return "BT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 일일 이체 한도 조회
     */
    private double getDailyLimit() {
        // 일일 이체 한도 500만원
        return 5_000_000.0;
    }
    
    /**
     * 계좌에 금액 입금
     */
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            log.info("[계좌이체] 입금 완료 - 입금 금액: {}원, 잔액: {}원", amount, balance);
        }
    }
    
    /**
     * 계좌 잔액 조회
     */
    public double getBalance() {
        return balance;
    }
    
    /**
     * 계좌 정보 조회
     */
    public String getAccountInfo() {
        return String.format("은행: %s, 계좌번호: %s, 예금주: %s, 잔액: %.0f원", 
                           bankName, accountNumber, accountHolderName, balance);
    }
    
    /**
     * 계좌 한도 정보 조회
     */
    public String getLimitInfo() {
        return String.format("일일 이체 한도: %.0f원", getDailyLimit());
    }
}
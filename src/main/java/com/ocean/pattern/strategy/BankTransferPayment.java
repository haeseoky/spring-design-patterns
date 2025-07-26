package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;

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
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("은행명은 필수입니다.");
        }
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("예금주 이름은 필수입니다.");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("계좌 잔액은 0 이상이어야 합니다.");
        }
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN은 6자리 숫자여야 합니다.");
        }
        
        this.bankName = bankName.trim();
        this.accountNumber = maskAccountNumber(accountNumber);
        this.accountHolderName = accountHolderName.trim();
        this.balance = balance;
        this.pin = pin;
    }
    
    @Override
    public PaymentResult processPayment(double amount) {
        log.info("[계좌이체] 결제 시작 - 금액: {}원, 은행: {}, 계좌: {}", amount, bankName, accountNumber);
        
        // 입력 유효성 검증
        if (amount <= 0) {
            log.warn("[계좌이체] 결제 실패 - 유효하지 않은 금액: {}", amount);
            return PaymentResult.failure("결제 금액이 유효하지 않습니다.", amount, getPaymentMethodName());
        }
        
        if (!isPaymentPossible(amount)) {
            double dailyLimit = getDailyLimit();
            double totalAmount = amount + calculateFee(amount);
            
            if (totalAmount > balance) {
                log.warn("[계좌이체] 결제 실패 - 계좌 잔액 부족 (필요: {}, 잔액: {})", totalAmount, balance);
                return PaymentResult.failure("계좌 잔액이 부족합니다.", amount, getPaymentMethodName());
            }
            
            if (amount > dailyLimit) {
                log.warn("[계좌이체] 결제 실패 - 일일 한도 초과 (요청: {}, 한도: {})", amount, dailyLimit);
                return PaymentResult.failure("일일 이체 한도를 초과했습니다.", amount, getPaymentMethodName());
            }
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
            
            String transactionId = TransactionIdGenerator.generateBankTransfer();
            
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
     * @param accountNumber 원본 계좌번호
     * @return 마스킹된 계좌번호
     * @throws IllegalArgumentException 계좌번호가 유효하지 않은 경우
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호가 유효하지 않습니다.");
        }
        
        String cleanAccountNumber = accountNumber.replaceAll("[^0-9]", "");
        if (cleanAccountNumber.length() < 4) {
            return "***-***-****";
        }
        
        String lastFour = cleanAccountNumber.substring(cleanAccountNumber.length() - 4);
        return "***-***-" + lastFour;
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
     * @param amount 입금 금액
     * @throws IllegalArgumentException 입금 금액이 0 이하인 경우
     */
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("입금 금액은 0보다 커야 합니다.");
        }
        
        balance += amount;
        log.info("[계좌이체] 입금 완료 - 입금 금액: {}원, 잔액: {}원", amount, balance);
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
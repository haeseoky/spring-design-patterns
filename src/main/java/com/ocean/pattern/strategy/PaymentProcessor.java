package com.ocean.pattern.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 결제 처리기 - Strategy Pattern의 Context 클래스
 * 다양한 결제 전략을 사용하여 결제를 처리하는 역할
 */
@Slf4j
@Component
public class PaymentProcessor {
    
    // 상수 정의
    private static final String NO_STRATEGY_MESSAGE = "결제 방법이 선택되지 않았습니다.";
    private static final String INVALID_AMOUNT_MESSAGE = "결제 금액이 유효하지 않습니다.";
    private static final String SYSTEM_ERROR_PREFIX = "시스템 오류로 인한 결제 실패: ";
    private static final String NO_PAYMENT_METHOD_SET = "결제 방법이 설정되지 않음";
    
    private PaymentStrategy paymentStrategy;
    private final List<PaymentResult> paymentHistory = new ArrayList<>();
    
    /**
     * 결제 전략 설정
     * @param paymentStrategy 사용할 결제 전략
     * @throws IllegalArgumentException paymentStrategy가 null인 경우
     */
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        if (paymentStrategy == null) {
            throw new IllegalArgumentException("결제 전략은 null일 수 없습니다.");
        }
        
        this.paymentStrategy = paymentStrategy;
        log.info("결제 전략이 변경되었습니다: {}", paymentStrategy.getPaymentMethodName());
    }
    
    /**
     * 결제 처리
     * @param amount 결제 금액
     * @return 결제 결과
     */
    public PaymentResult processPayment(double amount) {
        // 전략 유효성 검증
        if (paymentStrategy == null) {
            log.error("결제 전략이 설정되지 않았습니다.");
            PaymentResult errorResult = PaymentResult.failure(NO_STRATEGY_MESSAGE, amount, "없음");
            paymentHistory.add(errorResult);
            return errorResult;
        }
        
        // 금액 유효성 검증 강화
        if (amount <= 0) {
            log.error("유효하지 않은 결제 금액: {}", amount);
            PaymentResult errorResult = PaymentResult.failure(INVALID_AMOUNT_MESSAGE, amount, paymentStrategy.getPaymentMethodName());
            paymentHistory.add(errorResult);
            return errorResult;
        }
        
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            log.error("잘못된 결제 금액 형식: {}", amount);
            PaymentResult errorResult = PaymentResult.failure("결제 금액 형식이 올바르지 않습니다.", amount, paymentStrategy.getPaymentMethodName());
            paymentHistory.add(errorResult);
            return errorResult;
        }
        
        log.info("결제 처리 시작 - 방법: {}, 금액: {}원", paymentStrategy.getPaymentMethodName(), amount);
        
        try {
            // 전략 패턴의 핵심: 런타임에 결정된 전략에 따라 결제 처리
            PaymentResult result = paymentStrategy.processPayment(amount);
            
            // 결제 내역 저장
            paymentHistory.add(result);
            
            if (result.isSuccess()) {
                log.info("결제 성공 - 거래ID: {}, 총 금액: {}원 (수수료 포함)", 
                        result.getTransactionId(), result.getTotalAmount());
            } else {
                log.warn("결제 실패 - 사유: {}", result.getMessage());
            }
            
            return result;
            
        } catch (IllegalStateException e) {
            log.error("결제 전략 상태 오류", e);
            PaymentResult errorResult = PaymentResult.failure(
                "결제 시스템 상태 오류: " + e.getMessage(), 
                amount, 
                paymentStrategy.getPaymentMethodName()
            );
            paymentHistory.add(errorResult);
            return errorResult;
        } catch (SecurityException e) {
            log.error("결제 보안 오류", e);
            PaymentResult errorResult = PaymentResult.failure(
                "보안 검증 실패로 인한 결제 거부", 
                amount, 
                paymentStrategy.getPaymentMethodName()
            );
            paymentHistory.add(errorResult);
            return errorResult;
        } catch (Exception e) {
            log.error("결제 처리 중 예상치 못한 예외 발생", e);
            PaymentResult errorResult = PaymentResult.failure(
                SYSTEM_ERROR_PREFIX + e.getMessage(), 
                amount, 
                paymentStrategy.getPaymentMethodName()
            );
            paymentHistory.add(errorResult);
            return errorResult;
        }
    }
    
    /**
     * 현재 설정된 결제 전략으로 결제 가능 여부 확인
     * @param amount 결제 금액
     * @return 결제 가능 여부
     */
    public boolean canProcessPayment(double amount) {
        if (paymentStrategy == null || amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return false;
        }
        
        try {
            return paymentStrategy.isPaymentPossible(amount);
        } catch (Exception e) {
            log.warn("결제 가능 여부 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 현재 설정된 결제 전략의 수수료 계산
     * @param amount 결제 금액
     * @return 수수료
     * @throws IllegalArgumentException 금액이 유효하지 않은 경우
     */
    public double calculateFee(double amount) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("수수료 계산을 위한 금액이 유효하지 않습니다: " + amount);
        }
        
        if (paymentStrategy == null) {
            return 0.0;
        }
        
        try {
            return paymentStrategy.calculateFee(amount);
        } catch (Exception e) {
            log.warn("수수료 계산 중 오류 발생: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 현재 설정된 결제 방법 이름 조회
     * @return 결제 방법 이름
     */
    public String getCurrentPaymentMethod() {
        if (paymentStrategy == null) {
            return NO_PAYMENT_METHOD_SET;
        }
        
        try {
            return paymentStrategy.getPaymentMethodName();
        } catch (Exception e) {
            log.warn("결제 방법 이름 조회 중 오류 발생: {}", e.getMessage());
            return "결제 방법 조회 실패";
        }
    }
    
    /**
     * 결제 내역 조회
     * @return 결제 내역 목록
     */
    public List<PaymentResult> getPaymentHistory() {
        return new ArrayList<>(paymentHistory);
    }
    
    /**
     * 성공한 결제 내역만 조회
     * @return 성공한 결제 내역 목록
     */
    public List<PaymentResult> getSuccessfulPayments() {
        return paymentHistory.stream()
                .filter(PaymentResult::isSuccess)
                .toList();
    }
    
    /**
     * 실패한 결제 내역만 조회
     * @return 실패한 결제 내역 목록
     */
    public List<PaymentResult> getFailedPayments() {
        return paymentHistory.stream()
                .filter(result -> !result.isSuccess())
                .toList();
    }
    
    /**
     * 총 결제 금액 조회 (성공한 결제만)
     * @return 총 결제 금액
     */
    public double getTotalPaymentAmount() {
        return getSuccessfulPayments().stream()
                .mapToDouble(PaymentResult::getAmount)
                .sum();
    }
    
    /**
     * 총 수수료 조회 (성공한 결제만)
     * @return 총 수수료
     */
    public double getTotalFees() {
        return getSuccessfulPayments().stream()
                .mapToDouble(PaymentResult::getFee)
                .sum();
    }
    
    /**
     * 결제 내역 초기화
     */
    public void clearPaymentHistory() {
        paymentHistory.clear();
        log.info("결제 내역이 초기화되었습니다.");
    }
    
    /**
     * 결제 통계 정보 조회
     * @return 결제 통계 정보
     */
    public PaymentStatistics getPaymentStatistics() {
        int totalCount = paymentHistory.size();
        int successCount = getSuccessfulPayments().size();
        int failureCount = getFailedPayments().size();
        double totalAmount = getTotalPaymentAmount();
        double totalFees = getTotalFees();
        
        return new PaymentStatistics(totalCount, successCount, failureCount, totalAmount, totalFees);
    }
    
    /**
     * 결제 통계 정보를 담는 내부 클래스
     */
    public static class PaymentStatistics {
        public final int totalTransactions;
        public final int successfulTransactions;
        public final int failedTransactions;
        public final double totalAmount;
        public final double totalFees;
        public final double successRate;
        
        public PaymentStatistics(int total, int success, int failure, double amount, double fees) {
            this.totalTransactions = total;
            this.successfulTransactions = success;
            this.failedTransactions = failure;
            this.totalAmount = amount;
            this.totalFees = fees;
            this.successRate = total > 0 ? (double) success / total * 100 : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "결제 통계 - 총 %d건 (성공: %d건, 실패: %d건), 성공률: %.1f%%, 총 금액: %.0f원, 총 수수료: %.0f원",
                totalTransactions, successfulTransactions, failedTransactions, 
                successRate, totalAmount, totalFees
            );
        }
    }
}
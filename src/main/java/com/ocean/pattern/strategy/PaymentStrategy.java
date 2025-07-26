package com.ocean.pattern.strategy;

/**
 * 결제 전략 인터페이스 - Strategy Pattern의 핵심 인터페이스
 * 다양한 결제 방법들이 구현해야 하는 공통 인터페이스
 */
public interface PaymentStrategy {
    
    /**
     * 결제를 처리하는 메서드
     * @param amount 결제 금액
     * @return 결제 결과 정보
     */
    PaymentResult processPayment(double amount);
    
    /**
     * 결제 방법의 이름을 반환
     * @return 결제 방법명
     */
    String getPaymentMethodName();
    
    /**
     * 결제 가능 여부 확인
     * @param amount 결제 금액
     * @return 결제 가능 여부
     */
    boolean isPaymentPossible(double amount);
    
    /**
     * 결제 수수료 계산
     * @param amount 결제 금액
     * @return 수수료
     */
    double calculateFee(double amount);
}
package com.ocean.pattern.strategy;

/**
 * 결제 전략 인터페이스 - Strategy Pattern의 핵심 인터페이스
 * 
 * <p>다양한 결제 방법들이 구현해야 하는 공통 인터페이스입니다.
 * Strategy Pattern을 통해 런타임에 결제 방법을 동적으로 선택할 수 있습니다.</p>
 * 
 * <h3>구현 클래스:</h3>
 * <ul>
 *   <li>{@link CreditCardPayment} - 신용카드 결제</li>
 *   <li>{@link CashPayment} - 현금 결제</li>
 *   <li>{@link PayPalPayment} - PayPal 결제</li>
 *   <li>{@link BankTransferPayment} - 계좌이체 결제</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * PaymentProcessor processor = new PaymentProcessor();
 * PaymentStrategy strategy = new CreditCardPayment("1234-5678-9012-3456", "홍길동", "123", "12/25", 1000000);
 * processor.setPaymentStrategy(strategy);
 * PaymentResult result = processor.processPayment(50000);
 * }</pre>
 * 
 * @author Ocean Pattern Team
 * @version 1.0
 * @since 1.0
 * @see PaymentProcessor
 * @see PaymentResult
 */
public interface PaymentStrategy {
    
    /**
     * 결제를 처리하는 메서드
     * 
     * <p>각 결제 전략에 따라 구체적인 결제 로직을 수행합니다.
     * 결제 처리 과정에서 발생하는 모든 예외는 적절히 처리되어
     * 실패한 PaymentResult 객체로 반환됩니다.</p>
     * 
     * @param amount 결제 금액 (양수여야 함)
     * @return 결제 결과 정보 (성공/실패, 거래ID, 수수료 등 포함)
     * @throws IllegalArgumentException amount가 0 이하인 경우 (구현체에 따라 다를 수 있음)
     * @see PaymentResult
     */
    PaymentResult processPayment(double amount);
    
    /**
     * 결제 방법의 이름을 반환
     * 
     * <p>사용자에게 표시되는 결제 방법의 이름을 반환합니다.
     * 예: "신용카드 (**** **** **** 3456)", "현금", "PayPal (user@example.com)"</p>
     * 
     * @return 결제 방법명 (null이 아닌 문자열)
     */
    String getPaymentMethodName();
    
    /**
     * 결제 가능 여부 확인
     * 
     * <p>주어진 금액으로 결제가 가능한지 사전에 확인합니다.
     * 실제 결제 전에 호출하여 결제 가능성을 미리 판단할 수 있습니다.</p>
     * 
     * <h4>확인 항목:</h4>
     * <ul>
     *   <li>잔액 또는 한도 충분성</li>
     *   <li>수수료 포함 총 금액 검증</li>
     *   <li>일일 한도 등 제한 사항</li>
     *   <li>계정 상태 (활성화, 인증 등)</li>
     * </ul>
     * 
     * @param amount 결제 예정 금액 (양수여야 함)
     * @return 결제 가능하면 true, 불가능하면 false
     */
    boolean isPaymentPossible(double amount);
    
    /**
     * 결제 수수료 계산
     * 
     * <p>주어진 금액에 대한 수수료를 계산합니다.
     * 수수료는 결제 방법에 따라 다르게 적용됩니다.</p>
     * 
     * <h4>수수료 정책:</h4>
     * <ul>
     *   <li>신용카드: 결제 금액의 2.5%</li>
     *   <li>현금: 수수료 없음 (0%)</li>
     *   <li>PayPal: 결제 금액의 3.4% + 35원</li>
     *   <li>계좌이체: 고정 1,000원</li>
     * </ul>
     * 
     * @param amount 결제 금액 (양수여야 함)
     * @return 계산된 수수료 (0 이상의 값)
     */
    double calculateFee(double amount);
}
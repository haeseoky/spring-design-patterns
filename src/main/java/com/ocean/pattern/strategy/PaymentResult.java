package com.ocean.pattern.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 결제 결과 정보를 담는 클래스
 */
@Getter
@AllArgsConstructor
@ToString
public class PaymentResult {
    
    private final boolean success;
    private final String message;
    private final String transactionId;
    private final double amount;
    private final double fee;
    private final String paymentMethod;
    private final LocalDateTime timestamp;
    
    /**
     * 성공한 결제 결과 생성
     */
    public static PaymentResult success(String transactionId, double amount, double fee, String paymentMethod) {
        return new PaymentResult(
                true,
                "결제가 성공적으로 완료되었습니다.",
                transactionId,
                amount,
                fee,
                paymentMethod,
                LocalDateTime.now()
        );
    }
    
    /**
     * 실패한 결제 결과 생성
     */
    public static PaymentResult failure(String message, double amount, String paymentMethod) {
        return new PaymentResult(
                false,
                message,
                null,
                amount,
                0.0,
                paymentMethod,
                LocalDateTime.now()
        );
    }
    
    /**
     * 총 결제 금액 (원금 + 수수료)
     */
    public double getTotalAmount() {
        return amount + fee;
    }
}
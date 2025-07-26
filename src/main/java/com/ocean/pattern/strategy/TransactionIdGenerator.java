package com.ocean.pattern.strategy;

import java.util.UUID;

/**
 * 거래 ID 생성 유틸리티 클래스
 * 모든 결제 전략에서 공통으로 사용하는 거래 ID 생성 로직을 중앙화
 */
public final class TransactionIdGenerator {
    
    private TransactionIdGenerator() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
    
    /**
     * 주어진 접두사로 거래 ID 생성
     * @param prefix 거래 ID 접두사 (예: "CC", "CASH", "PP", "BT")
     * @return 생성된 거래 ID (예: "CC-A1B2C3D4")
     * @throws IllegalArgumentException prefix가 null인 경우
     */
    public static String generate(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + uuid;
    }
    
    /**
     * 신용카드 거래 ID 생성
     * @return 신용카드 거래 ID
     */
    public static String generateCreditCard() {
        return generate("CC");
    }
    
    /**
     * 현금 거래 ID 생성
     * @return 현금 거래 ID
     */
    public static String generateCash() {
        return generate("CASH");
    }
    
    /**
     * PayPal 거래 ID 생성
     * @return PayPal 거래 ID
     */
    public static String generatePayPal() {
        return generate("PP");
    }
    
    /**
     * 계좌이체 거래 ID 생성
     * @return 계좌이체 거래 ID
     */
    public static String generateBankTransfer() {
        return generate("BT");
    }
}
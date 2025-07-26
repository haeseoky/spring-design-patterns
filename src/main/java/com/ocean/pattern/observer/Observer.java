package com.ocean.pattern.observer;

/**
 * 옵저버 인터페이스 - 상태 변화를 통지받을 객체들이 구현해야 하는 인터페이스
 */
public interface Observer {
    /**
     * Subject로부터 알림을 받을 때 호출되는 메서드
     * @param message 전달받은 메시지
     */
    void update(String message);
    
    /**
     * 옵저버의 고유 이름을 반환
     * @return 옵저버 이름
     */
    String getName();
}
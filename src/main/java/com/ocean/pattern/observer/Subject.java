package com.ocean.pattern.observer;

/**
 * Subject 인터페이스 - 옵저버들을 관리하고 상태 변화를 통지하는 주체
 */
public interface Subject {
    /**
     * 옵저버를 등록
     * @param observer 등록할 옵저버
     */
    void addObserver(Observer observer);
    
    /**
     * 옵저버를 제거
     * @param observer 제거할 옵저버
     */
    void removeObserver(Observer observer);
    
    /**
     * 등록된 모든 옵저버에게 알림을 전송
     * @param message 전송할 메시지
     */
    void notifyObservers(String message);
}
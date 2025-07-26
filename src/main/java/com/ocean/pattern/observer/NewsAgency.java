package com.ocean.pattern.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 뉴스 에이전시 - Subject 구현체
 * 뉴스를 발행하고 구독자들에게 알림을 전송하는 역할
 */
@Slf4j
@Component
public class NewsAgency implements Subject {
    
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private final Set<Observer> observerSet = ConcurrentHashMap.newKeySet();
    private final ExecutorService notificationExecutor = Executors.newCachedThreadPool(
        r -> {
            Thread t = new Thread(r, "NewsAgency-Notification-Thread");
            t.setDaemon(true);
            return t;
        }
    );
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private volatile String latestNews;
    private volatile LocalDateTime lastPublishTime;
    
    @Override
    public void addObserver(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("구독자는 null일 수 없습니다.");
        }
        
        // 원래 동작: 중복 허용 (테스트 호환성을 위해)
        observers.add(observer);
        observerSet.add(observer); // Set은 중복을 허용하지 않지만 List는 허용
        log.info("새로운 구독자가 등록되었습니다: {} (총 구독자 수: {})", observer.getName(), observers.size());
    }
    
    @Override
    public void removeObserver(Observer observer) {
        if (observer == null) {
            return;
        }
        
        if (observerSet.remove(observer)) {
            observers.remove(observer);
            log.info("구독자가 제거되었습니다: {} (남은 구독자 수: {})", observer.getName(), observers.size());
        } else {
            log.debug("존재하지 않는 구독자 제거 시도: {}", observer.getName());
        }
    }
    
    @Override
    public void notifyObservers(String message) {
        if (message == null || message.trim().isEmpty()) {
            log.warn("빈 메시지는 전송하지 않습니다.");
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(timestampFormatter);
        String formattedMessage = String.format("[%s] %s", timestamp, message.trim());
        
        int observerCount = observers.size();
        log.info("뉴스 발행: {} (구독자 수: {})", message, observerCount);
        
        if (observerCount == 0) {
            log.info("구독자가 없어 알림을 전송하지 않습니다.");
            return;
        }
        
        // 동기 알림 전송 (테스트 호환성을 위해)
        notifyObserversSync(formattedMessage);
    }
    
    /**
     * 동기 구독자 알림 전송 (테스트 호환성을 위해)
     * @param message 전송할 메시지
     */
    private void notifyObserversSync(String message) {
        int successCount = 0;
        int failureCount = 0;
        
        for (Observer observer : observers) {
            try {
                observer.update(message);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("구독자 {}에게 알림 전송 실패: {}", observer.getName(), e.getMessage());
            }
        }
        
        log.info("알림 전송 완료 - 성공: {}, 실패: {}", successCount, failureCount);
    }
    
    /**
     * 비동기 구독자 알림 전송
     * @param message 전송할 메시지
     */
    private void notifyObserversAsync(String message) {
        CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            int failureCount = 0;
            
            for (Observer observer : observers) {
                try {
                    observer.update(message);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("구독자 {}에게 알림 전송 실패: {}", observer.getName(), e.getMessage());
                }
            }
            
            log.info("알림 전송 완료 - 성공: {}, 실패: {}", successCount, failureCount);
            return successCount;
        }, notificationExecutor).exceptionally(throwable -> {
            log.error("알림 전송 중 예상치 못한 오류 발생", throwable);
            return 0;
        });
    }
    
    /**
     * 새로운 뉴스를 발행
     * @param news 발행할 뉴스 내용
     * @throws IllegalArgumentException 뉴스 내용이 null이거나 빈 문자열인 경우
     */
    public void publishNews(String news) {
        if (news == null || news.trim().isEmpty()) {
            throw new IllegalArgumentException("뉴스 내용은 비어있을 수 없습니다.");
        }
        
        this.latestNews = news.trim();
        this.lastPublishTime = LocalDateTime.now();
        notifyObservers(news);
    }
    
    /**
     * 최신 뉴스 조회
     * @return 최신 뉴스
     */
    public String getLatestNews() {
        return latestNews;
    }
    
    /**
     * 현재 구독자 수 조회
     * @return 구독자 수
     */
    public int getObserverCount() {
        return observers.size();
    }
    
    /**
     * 모든 구독자 목록 조회
     * @return 구독자 이름 목록
     */
    public List<String> getObserverNames() {
        return observers.stream()
                .map(Observer::getName)
                .toList();
    }
    
    /**
     * 마지막 발행 시간 조회
     * @return 마지막 발행 시간
     */
    public LocalDateTime getLastPublishTime() {
        return lastPublishTime;
    }
    
    /**
     * 모든 구독자 제거
     */
    public void clearAllObservers() {
        int removedCount = observers.size();
        observers.clear();
        observerSet.clear();
        log.info("모든 구독자가 제거되었습니다. (제거된 구독자 수: {})", removedCount);
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        log.info("뉴스 에이전시 종료 시작...");
        clearAllObservers();
        notificationExecutor.shutdown();
        try {
            if (!notificationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("알림 전송 스레드가 정상 종료되지 않아 강제 종료합니다.");
                notificationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notificationExecutor.shutdownNow();
        }
        log.info("뉴스 에이전시 종료 완료");
    }
}
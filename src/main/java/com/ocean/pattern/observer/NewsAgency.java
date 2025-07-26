package com.ocean.pattern.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 뉴스 에이전시 - Subject 구현체
 * 뉴스를 발행하고 구독자들에게 알림을 전송하는 역할
 */
@Slf4j
@Component
public class NewsAgency implements Subject {
    
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private String latestNews;
    
    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
        log.info("새로운 구독자가 등록되었습니다: {}", observer.getName());
    }
    
    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
        log.info("구독자가 제거되었습니다: {}", observer.getName());
    }
    
    @Override
    public void notifyObservers(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String formattedMessage = String.format("[%s] %s", timestamp, message);
        
        log.info("뉴스 발행: {} (구독자 수: {})", message, observers.size());
        
        for (Observer observer : observers) {
            try {
                observer.update(formattedMessage);
            } catch (Exception e) {
                log.error("구독자 {}에게 알림 전송 실패: {}", observer.getName(), e.getMessage());
            }
        }
    }
    
    /**
     * 새로운 뉴스를 발행
     * @param news 발행할 뉴스 내용
     */
    public void publishNews(String news) {
        this.latestNews = news;
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
}
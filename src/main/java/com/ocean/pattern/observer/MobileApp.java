package com.ocean.pattern.observer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 모바일 앱 - Observer 구현체
 * 푸시 알림을 통해 뉴스를 받는 모바일 앱
 */
@Slf4j
@Getter
public class MobileApp implements Observer {
    
    private final String appName;
    private final String deviceId;
    private final List<String> notifications = new ArrayList<>();
    private boolean pushEnabled = true;
    
    public MobileApp(String appName, String deviceId) {
        this.appName = appName;
        this.deviceId = deviceId;
    }
    
    @Override
    public void update(String message) {
        if (!pushEnabled) {
            log.info("[{}] 푸시 알림이 비활성화되어 있습니다.", appName);
            return;
        }
        
        String notification = createNotification(message);
        notifications.add(notification);
        
        log.info("[모바일 앱: {}] 📱 푸시 알림 전송: {}", appName, deviceId);
        
        // 푸시 알림 전송 시뮬레이션
        sendPushNotification(notification);
    }
    
    @Override
    public String getName() {
        return String.format("%s (Device: %s)", appName, deviceId);
    }
    
    /**
     * 푸시 알림 생성
     */
    private String createNotification(String news) {
        // 모바일에 적합하도록 뉴스를 짧게 요약
        String shortNews = news.length() > 50 ? news.substring(0, 47) + "..." : news;
        return String.format("📰 속보: %s", shortNews);
    }
    
    /**
     * 푸시 알림 전송 시뮬레이션
     */
    private void sendPushNotification(String notification) {
        log.info("[📱] {} 기기로 푸시 알림 발송: {}", deviceId, notification);
        
        // 알림 배지 업데이트 시뮬레이션
        updateBadgeCount();
    }
    
    /**
     * 알림 배지 카운트 업데이트
     */
    private void updateBadgeCount() {
        log.debug("[{}] 알림 배지 업데이트: {}", appName, notifications.size());
    }
    
    /**
     * 푸시 알림 설정 변경
     */
    public void setPushEnabled(boolean enabled) {
        this.pushEnabled = enabled;
        log.info("[{}] 푸시 알림 설정: {}", appName, enabled ? "활성화" : "비활성화");
    }
    
    /**
     * 받은 알림 수 조회
     */
    public int getNotificationCount() {
        return notifications.size();
    }
    
    /**
     * 최신 알림 조회
     */
    public String getLatestNotification() {
        return notifications.isEmpty() ? "받은 알림이 없습니다." : notifications.get(notifications.size() - 1);
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    public void markAllAsRead() {
        log.info("[{}] 모든 알림을 읽음 처리했습니다. (총 {}개)", appName, notifications.size());
    }
}
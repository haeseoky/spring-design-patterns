package com.ocean.pattern.controller;

import com.ocean.pattern.observer.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 옵저버 패턴 데모 컨트롤러
 * REST API를 통해 옵저버 패턴을 시연하고 테스트할 수 있는 엔드포인트들을 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/observer")
@RequiredArgsConstructor
public class ObserverPatternController {
    
    private final NewsAgency newsAgency;
    private final Map<String, Observer> observerRegistry = new ConcurrentHashMap<>();
    
    /**
     * 뉴스 발행
     */
    @PostMapping("/news")
    public ResponseEntity<Map<String, Object>> publishNews(@RequestBody Map<String, String> request) {
        String news = request.get("news");
        if (news == null || news.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "뉴스 내용이 필요합니다."));
        }
        
        newsAgency.publishNews(news);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "뉴스가 성공적으로 발행되었습니다.");
        response.put("news", news);
        response.put("subscriberCount", newsAgency.getObserverCount());
        response.put("subscribers", newsAgency.getObserverNames());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 뉴스 채널 구독자 추가
     */
    @PostMapping("/subscribe/channel")
    public ResponseEntity<Map<String, Object>> addNewsChannel(@RequestBody Map<String, String> request) {
        String channelName = request.get("channelName");
        if (channelName == null || channelName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "채널명이 필요합니다."));
        }
        
        NewsChannel channel = new NewsChannel(channelName);
        newsAgency.addObserver(channel);
        observerRegistry.put("channel_" + channelName, channel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "뉴스 채널이 성공적으로 구독되었습니다.");
        response.put("channelName", channelName);
        response.put("totalSubscribers", newsAgency.getObserverCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 이메일 구독자 추가
     */
    @PostMapping("/subscribe/email")
    public ResponseEntity<Map<String, Object>> addEmailSubscriber(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        
        if (name == null || email == null || name.trim().isEmpty() || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "이름과 이메일이 모두 필요합니다."));
        }
        
        EmailSubscriber subscriber = new EmailSubscriber(name, email);
        newsAgency.addObserver(subscriber);
        observerRegistry.put("email_" + email, subscriber);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "이메일 구독자가 성공적으로 등록되었습니다.");
        response.put("name", name);
        response.put("email", email);
        response.put("totalSubscribers", newsAgency.getObserverCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모바일 앱 구독자 추가
     */
    @PostMapping("/subscribe/mobile")
    public ResponseEntity<Map<String, Object>> addMobileApp(@RequestBody Map<String, String> request) {
        String appName = request.get("appName");
        String deviceId = request.get("deviceId");
        
        if (appName == null || deviceId == null || appName.trim().isEmpty() || deviceId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "앱 이름과 기기 ID가 모두 필요합니다."));
        }
        
        MobileApp app = new MobileApp(appName, deviceId);
        newsAgency.addObserver(app);
        observerRegistry.put("mobile_" + deviceId, app);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "모바일 앱이 성공적으로 구독되었습니다.");
        response.put("appName", appName);
        response.put("deviceId", deviceId);
        response.put("totalSubscribers", newsAgency.getObserverCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 구독 해지
     */
    @DeleteMapping("/unsubscribe/{observerId}")
    public ResponseEntity<Map<String, Object>> unsubscribe(@PathVariable String observerId) {
        Observer observer = observerRegistry.remove(observerId);
        if (observer == null) {
            return ResponseEntity.notFound().build();
        }
        
        newsAgency.removeObserver(observer);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "구독이 성공적으로 해지되었습니다.");
        response.put("observerName", observer.getName());
        response.put("totalSubscribers", newsAgency.getObserverCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 현재 구독자 목록 조회
     */
    @GetMapping("/subscribers")
    public ResponseEntity<Map<String, Object>> getSubscribers() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalCount", newsAgency.getObserverCount());
        response.put("subscribers", newsAgency.getObserverNames());
        response.put("latestNews", newsAgency.getLatestNews());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 구독자 상세 정보 조회
     */
    @GetMapping("/subscriber/{observerId}")
    public ResponseEntity<Map<String, Object>> getSubscriberDetails(@PathVariable String observerId) {
        Observer observer = observerRegistry.get(observerId);
        if (observer == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("observerId", observerId);
        response.put("name", observer.getName());
        response.put("type", observer.getClass().getSimpleName());
        
        // 타입별 상세 정보 추가
        if (observer instanceof NewsChannel channel) {
            response.put("newsCount", channel.getNewsCount());
            response.put("latestNews", channel.getLatestNews());
        } else if (observer instanceof EmailSubscriber subscriber) {
            response.put("emailCount", subscriber.getEmailCount());
            response.put("email", subscriber.getEmail());
        } else if (observer instanceof MobileApp app) {
            response.put("notificationCount", app.getNotificationCount());
            response.put("deviceId", app.getDeviceId());
            response.put("pushEnabled", app.isPushEnabled());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 옵저버 패턴 데모 실행
     */
    @PostMapping("/demo")
    public ResponseEntity<Map<String, Object>> runDemo() {
        log.info("옵저버 패턴 데모를 시작합니다.");
        
        // 다양한 구독자들 추가
        NewsChannel kbsNews = new NewsChannel("KBS 뉴스");
        NewsChannel sbsNews = new NewsChannel("SBS 뉴스");
        EmailSubscriber emailUser = new EmailSubscriber("김철수", "user@example.com");
        MobileApp newsApp = new MobileApp("뉴스앱", "device123");
        
        newsAgency.addObserver(kbsNews);
        newsAgency.addObserver(sbsNews);
        newsAgency.addObserver(emailUser);
        newsAgency.addObserver(newsApp);
        
        // 레지스트리에 등록
        observerRegistry.put("channel_KBS", kbsNews);
        observerRegistry.put("channel_SBS", sbsNews);
        observerRegistry.put("email_user@example.com", emailUser);
        observerRegistry.put("mobile_device123", newsApp);
        
        // 뉴스 발행
        newsAgency.publishNews("긴급 속보: 옵저버 패턴 데모가 성공적으로 실행되었습니다!");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "데모가 성공적으로 실행되었습니다.");
        response.put("subscribersAdded", 4);
        response.put("newsPublished", 1);
        response.put("subscribers", newsAgency.getObserverNames());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 구독자 제거 (리셋)
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        observerRegistry.values().forEach(newsAgency::removeObserver);
        observerRegistry.clear();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "모든 구독자가 제거되었습니다.");
        response.put("totalSubscribers", newsAgency.getObserverCount());
        
        return ResponseEntity.ok(response);
    }
}
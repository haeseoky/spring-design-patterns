package com.ocean.pattern.observer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 채널 - Observer 구현체
 * 뉴스를 수신하고 저장하는 뉴스 채널
 */
@Slf4j
@Getter
public class NewsChannel implements Observer {
    
    private final String channelName;
    private final List<String> newsHistory = new ArrayList<>();
    
    public NewsChannel(String channelName) {
        this.channelName = channelName;
    }
    
    @Override
    public void update(String message) {
        newsHistory.add(message);
        log.info("[{}] 새로운 뉴스 수신: {}", channelName, message);
        
        // 뉴스 방송 시뮬레이션
        broadcastNews(message);
    }
    
    @Override
    public String getName() {
        return channelName;
    }
    
    /**
     * 뉴스 방송 시뮬레이션
     */
    private void broadcastNews(String news) {
        log.info("[{}] 📺 뉴스 방송 중: {}", channelName, news);
    }
    
    /**
     * 최신 뉴스 조회
     */
    public String getLatestNews() {
        return newsHistory.isEmpty() ? "아직 뉴스가 없습니다." : newsHistory.get(newsHistory.size() - 1);
    }
    
    /**
     * 뉴스 히스토리 크기 조회
     */
    public int getNewsCount() {
        return newsHistory.size();
    }
}
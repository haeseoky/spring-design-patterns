package com.ocean.pattern.observer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 이메일 구독자 - Observer 구현체
 * 이메일을 통해 뉴스 알림을 받는 구독자
 */
@Slf4j
@Getter
public class EmailSubscriber implements Observer {
    
    private final String email;
    private final String subscriberName;
    private final List<String> emailHistory = new ArrayList<>();
    
    public EmailSubscriber(String subscriberName, String email) {
        this.subscriberName = subscriberName;
        this.email = email;
    }
    
    @Override
    public void update(String message) {
        String emailContent = createEmailContent(message);
        emailHistory.add(emailContent);
        
        log.info("[이메일 구독자: {}] 📧 이메일 발송: {}", subscriberName, email);
        log.debug("이메일 내용: {}", emailContent);
        
        // 이메일 발송 시뮬레이션
        sendEmail(emailContent);
    }
    
    @Override
    public String getName() {
        return String.format("%s (%s)", subscriberName, email);
    }
    
    /**
     * 이메일 내용 생성
     */
    private String createEmailContent(String news) {
        return String.format("""
                안녕하세요 %s님,
                
                새로운 뉴스가 도착했습니다:
                %s
                
                더 많은 뉴스는 저희 웹사이트를 방문해주세요.
                
                감사합니다.
                뉴스 에이전시
                """, subscriberName, news);
    }
    
    /**
     * 이메일 발송 시뮬레이션
     */
    private void sendEmail(String content) {
        // 실제로는 이메일 서비스를 통해 발송
        log.info("[📧] {} 에게 이메일 발송 완료", email);
    }
    
    /**
     * 받은 이메일 수 조회
     */
    public int getEmailCount() {
        return emailHistory.size();
    }
    
    /**
     * 최신 이메일 내용 조회
     */
    public String getLatestEmail() {
        return emailHistory.isEmpty() ? "받은 이메일이 없습니다." : emailHistory.get(emailHistory.size() - 1);
    }
}
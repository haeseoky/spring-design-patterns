package com.ocean.pattern.integration;

import com.ocean.pattern.observer.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.Map;

/**
 * 옵저버 패턴 통합 테스트
 * 실제 Spring Boot 애플리케이션 컨텍스트를 사용하여 전체 워크플로우를 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("옵저버 패턴 통합 테스트")
class ObserverPatternIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private NewsAgency newsAgency;
    
    private WebTestClient webTestClient;
    
    @Test
    @DisplayName("전체 워크플로우 통합 테스트")
    void testCompleteWorkflow() {
        // WebTestClient 초기화
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        
        // 1. 초기 상태 확인 (구독자 없음)
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(0);
        
        // 2. 뉴스 채널 구독
        webTestClient.post()
                .uri("/api/observer/subscribe/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("channelName", "KBS 뉴스"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo("KBS 뉴스")
                .jsonPath("$.totalSubscribers").isEqualTo(1);
        
        // 3. 이메일 구독자 추가
        webTestClient.post()
                .uri("/api/observer/subscribe/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "name", "테스트유저",
                        "email", "test@integration.com"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("테스트유저")
                .jsonPath("$.totalSubscribers").isEqualTo(2);
        
        // 4. 모바일 앱 구독
        webTestClient.post()
                .uri("/api/observer/subscribe/mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "appName", "통합테스트앱",
                        "deviceId", "integration-device-123"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.appName").isEqualTo("통합테스트앱")
                .jsonPath("$.totalSubscribers").isEqualTo(3);
        
        // 5. 구독자 목록 확인
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(3)
                .jsonPath("$.subscribers").isArray()
                .jsonPath("$.subscribers.length()").isEqualTo(3);
        
        // 6. 뉴스 발행
        String testNews = "통합 테스트 뉴스: 모든 구독자에게 알림이 전송됩니다.";
        webTestClient.post()
                .uri("/api/observer/news")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("news", testNews))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("뉴스가 성공적으로 발행되었습니다.")
                .jsonPath("$.news").isEqualTo(testNews)
                .jsonPath("$.subscriberCount").isEqualTo(3);
        
        // 7. 뉴스 발행 후 상태 확인
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.latestNews").isEqualTo(testNews);
        
        // 8. 리셋
        webTestClient.delete()
                .uri("/api/observer/reset")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalSubscribers").isEqualTo(0);
        
        // 9. 리셋 후 상태 확인
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(0);
    }
    
    @Test
    @DisplayName("데모 실행 통합 테스트")
    void testDemoExecution() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        
        // 1. 데모 실행 전 상태 확인
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(0);
        
        // 2. 데모 실행
        webTestClient.post()
                .uri("/api/observer/demo")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("데모가 성공적으로 실행되었습니다.")
                .jsonPath("$.subscribersAdded").isEqualTo(4)
                .jsonPath("$.newsPublished").isEqualTo(1);
        
        // 3. 데모 실행 후 상태 확인
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(4)
                .jsonPath("$.latestNews").isNotEmpty();
        
        // 4. 정리
        webTestClient.delete()
                .uri("/api/observer/reset")
                .exchange()
                .expectStatus().isOk();
    }
    
    @Test
    @DisplayName("오류 상황 처리 통합 테스트")
    void testErrorHandling() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        
        // 1. 빈 뉴스 발행 시도
        webTestClient.post()
                .uri("/api/observer/news")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("news", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("뉴스 내용이 필요합니다.");
        
        // 2. 필수 필드 누락된 이메일 구독 시도
        webTestClient.post()
                .uri("/api/observer/subscribe/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "이름만있음"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("이름과 이메일이 모두 필요합니다.");
        
        // 3. 존재하지 않는 구독자 조회
        webTestClient.get()
                .uri("/api/observer/subscriber/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
        
        // 4. 존재하지 않는 구독자 해지 시도
        webTestClient.delete()
                .uri("/api/observer/unsubscribe/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }
    
    @Test
    @DisplayName("대용량 구독자 처리 테스트")
    void testLargeNumberOfSubscribers() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();
        
        // 1. 여러 구독자 추가
        for (int i = 1; i <= 10; i++) {
            // 뉴스 채널 추가
            webTestClient.post()
                    .uri("/api/observer/subscribe/channel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("channelName", "채널" + i))
                    .exchange()
                    .expectStatus().isOk();
            
            // 이메일 구독자 추가
            webTestClient.post()
                    .uri("/api/observer/subscribe/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "name", "사용자" + i,
                            "email", "user" + i + "@test.com"
                    ))
                    .exchange()
                    .expectStatus().isOk();
        }
        
        // 2. 총 구독자 수 확인 (채널 10개 + 이메일 10개 = 20개)
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(20);
        
        // 3. 뉴스 발행 (모든 구독자에게 알림 전송)
        webTestClient.post()
                .uri("/api/observer/news")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("news", "대용량 테스트 뉴스"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.subscriberCount").isEqualTo(20);
        
        // 4. 정리
        webTestClient.delete()
                .uri("/api/observer/reset")
                .exchange()
                .expectStatus().isOk();
    }
}
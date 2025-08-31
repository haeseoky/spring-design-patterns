package com.ocean.pattern.structured;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Structured Concurrency 기본 사용 패턴
 * 
 * 이 클래스는 StructuredTaskScope의 기본적인 사용 방법을 보여줍니다.
 * JDK 25의 Preview API인 Structured Concurrency를 시뮬레이션합니다.
 * 
 * 주요 학습 목표:
 * 1. 태스크 스코프 생성과 관리
 * 2. fork()를 통한 하위 태스크 생성
 * 3. join()을 통한 태스크 완료 대기
 * 4. 자동 리소스 관리 (try-with-resources)
 */
public class TaskScopeExample {
    
    private static final Logger logger = Logger.getLogger(TaskScopeExample.class.getName());
    
    /**
     * JDK 25 Preview API 사용법 (이론적 구현)
     * 
     * 실제 JDK 25에서는 다음과 같이 사용됩니다:
     * 
     * public UserProfile fetchUserProfile(String userId) throws InterruptedException {
     *     try (var scope = StructuredTaskScope.open()) {
     *         Subtask<User> userTask = scope.fork(() -> fetchUser(userId));
     *         Subtask<Preferences> prefsTask = scope.fork(() -> fetchPreferences(userId));
     *         Subtask<Permissions> permsTask = scope.fork(() -> fetchPermissions(userId));
     *         
     *         scope.join(); // 모든 태스크 완료 대기
     *         
     *         return new UserProfile(
     *             userTask.get(),
     *             prefsTask.get(),
     *             permsTask.get()
     *         );
     *     }
     * }
     */
    
    /**
     * 기본 병렬 처리 예제
     * 사용자 정보를 여러 소스에서 동시에 조회
     */
    public UserProfile fetchUserProfileAsync(String userId) throws InterruptedException, ExecutionException {
        logger.info("사용자 프로필 조회 시작: " + userId);
        
        // 현재 Java 버전에서의 시뮬레이션 구현
        // 실제 Structured Concurrency와 유사한 동작을 ExecutorService로 구현
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 세 개의 독립적인 태스크를 동시 실행
            Future<User> userTask = executor.submit(() -> fetchUser(userId));
            Future<Preferences> prefsTask = executor.submit(() -> fetchPreferences(userId));
            Future<Permissions> permsTask = executor.submit(() -> fetchPermissions(userId));
            
            // 모든 태스크 결과 수집
            User user = userTask.get();
            Preferences prefs = prefsTask.get();
            Permissions perms = permsTask.get();
            
            logger.info("사용자 프로필 조회 완료: " + userId);
            return new UserProfile(user, prefs, perms);
        }
    }
    
    /**
     * 타임아웃이 있는 병렬 처리 예제
     * 지정된 시간 내에 완료되지 않으면 취소
     */
    public UserProfile fetchUserProfileWithTimeout(String userId, Duration timeout) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        logger.info("타임아웃 설정된 사용자 프로필 조회: " + userId + ", 타임아웃: " + timeout);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            Future<UserProfile> profileTask = executor.submit(() -> {
                try {
                    return fetchUserProfileAsync(userId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            return profileTask.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 빠른 응답 패턴 예제
     * 여러 데이터 소스 중 가장 빠른 응답을 사용
     */
    public String fetchFastestResponse(String query) throws InterruptedException, ExecutionException {
        logger.info("가장 빠른 응답 조회: " + query);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // CompletionService를 사용하여 첫 번째 완료 결과 반환
            CompletionService<String> completionService = new ExecutorCompletionService<>(executor);
            
            // 세 개의 다른 데이터 소스에 동시 요청
            completionService.submit(() -> fetchFromPrimarySource(query));
            completionService.submit(() -> fetchFromSecondarySource(query));
            completionService.submit(() -> fetchFromCacheSource(query));
            
            // 첫 번째 완료된 결과 반환
            Future<String> firstCompleted = completionService.take();
            String result = firstCompleted.get();
            
            logger.info("가장 빠른 응답 수신: " + result);
            return result;
        }
    }
    
    // 시뮬레이션을 위한 더미 메서드들
    
    private User fetchUser(String userId) {
        simulateNetworkDelay(100, 300);
        logger.info("사용자 정보 조회 완료: " + userId);
        return new User(userId, "User " + userId, "user" + userId + "@example.com");
    }
    
    private Preferences fetchPreferences(String userId) {
        simulateNetworkDelay(150, 250);
        logger.info("사용자 설정 조회 완료: " + userId);
        return new Preferences("ko-KR", "dark", true);
    }
    
    private Permissions fetchPermissions(String userId) {
        simulateNetworkDelay(80, 200);
        logger.info("사용자 권한 조회 완료: " + userId);
        return new Permissions("read", "write");
    }
    
    private String fetchFromPrimarySource(String query) {
        simulateNetworkDelay(200, 400);
        return "Primary: " + query;
    }
    
    private String fetchFromSecondarySource(String query) {
        simulateNetworkDelay(100, 300);
        return "Secondary: " + query;
    }
    
    private String fetchFromCacheSource(String query) {
        simulateNetworkDelay(50, 100);
        return "Cache: " + query;
    }
    
    private void simulateNetworkDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + (int)(Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    // 데이터 클래스들
    
    public record User(String id, String name, String email) {}
    
    public record Preferences(String language, String theme, boolean notifications) {}
    
    public record Permissions(String... permissions) {}
    
    public record UserProfile(User user, Preferences preferences, Permissions permissions) {}
    
    // 실행 예제
    public static void main(String[] args) {
        TaskScopeExample example = new TaskScopeExample();
        
        try {
            // 기본 병렬 처리
            UserProfile profile = example.fetchUserProfileAsync("user123");
            System.out.println("조회된 프로필: " + profile);
            
            // 타임아웃이 있는 처리
            UserProfile profileWithTimeout = example.fetchUserProfileWithTimeout(
                "user456", Duration.ofSeconds(1)
            );
            System.out.println("타임아웃 프로필: " + profileWithTimeout);
            
            // 빠른 응답 처리
            String fastResponse = example.fetchFastestResponse("search query");
            System.out.println("빠른 응답: " + fastResponse);
            
        } catch (Exception e) {
            logger.severe("실행 중 오류 발생: " + e.getMessage());
        }
    }
}
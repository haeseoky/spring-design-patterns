package com.ocean.pattern.structured;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

/**
 * Structured Concurrency의 에러 처리 패턴
 * 
 * 이 클래스는 Structured Concurrency에서의 구조적 에러 처리를 보여줍니다.
 * 한 태스크에서 예외가 발생하면 관련된 모든 태스크가 자동으로 취소됩니다.
 * 
 * 주요 학습 목표:
 * 1. 예외 전파 메커니즘
 * 2. 자동 태스크 취소
 * 3. 타임아웃과 인터럽트 처리
 * 4. 부분 실패 시나리오 처리
 */
public class ErrorHandlingExample {
    
    private static final Logger logger = Logger.getLogger(ErrorHandlingExample.class.getName());
    
    /**
     * JDK 25 Structured Concurrency의 에러 처리 (이론적 구현)
     * 
     * 실제 구현에서는 다음과 같이 동작합니다:
     * 
     * try (var scope = StructuredTaskScope.open()) {
     *     Subtask<String> task1 = scope.fork(() -> riskyOperation1());
     *     Subtask<String> task2 = scope.fork(() -> riskyOperation2());
     *     Subtask<String> task3 = scope.fork(() -> riskyOperation3());
     *     
     *     scope.join(); // 하나라도 실패하면 나머지 자동 취소
     *     
     *     return List.of(task1.get(), task2.get(), task3.get());
     * } catch (Exception e) {
     *     // 모든 관련 태스크가 이미 정리됨
     *     logger.error("작업 실패: " + e.getMessage());
     *     throw e;
     * }
     */
    
    /**
     * 기본 에러 처리 예제
     * 하나의 태스크 실패 시 다른 태스크들도 취소
     */
    public List<String> processWithFailFast(List<String> inputs) throws InterruptedException {
        logger.info("Fail-fast 처리 시작, 입력 개수: " + inputs.size());
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            
            // 모든 입력에 대해 병렬 처리 시작
            for (int i = 0; i < inputs.size(); i++) {
                final int index = i;
                final String input = inputs.get(i);
                
                Future<String> future = executor.submit(() -> {
                    try {
                        return processRiskyOperation(input, index);
                    } catch (Exception e) {
                        // 예외 발생 시 다른 모든 태스크 취소
                        logger.warning("태스크 " + index + " 실패, 다른 태스크들 취소 중...");
                        futures.forEach(f -> f.cancel(true));
                        throw new RuntimeException("작업 실패: " + input, e);
                    }
                });
                
                futures.add(future);
            }
            
            // 모든 결과 수집
            List<String> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    String result = futures.get(i).get();
                    results.add(result);
                    logger.info("태스크 " + i + " 완료: " + result);
                } catch (CancellationException e) {
                    logger.info("태스크 " + i + " 취소됨");
                    throw new InterruptedException("작업이 취소되었습니다");
                } catch (ExecutionException e) {
                    logger.severe("태스크 " + i + " 실패: " + e.getCause().getMessage());
                    throw new RuntimeException(e.getCause());
                }
            }
            
            logger.info("모든 태스크 성공적으로 완료");
            return results;
        }
    }
    
    /**
     * 타임아웃 기반 에러 처리
     * 지정된 시간 내에 완료되지 않으면 모든 태스크 취소
     */
    public List<String> processWithTimeout(List<String> inputs, Duration timeout) throws InterruptedException, TimeoutException {
        logger.info("타임아웃 처리 시작: " + timeout + ", 입력 개수: " + inputs.size());
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 전체 처리를 하나의 태스크로 래핑
            Future<List<String>> overallTask = executor.submit(() -> {
                List<Future<String>> futures = new ArrayList<>();
                
                // 개별 태스크들 시작
                for (int i = 0; i < inputs.size(); i++) {
                    final int index = i;
                    final String input = inputs.get(i);
                    
                    futures.add(executor.submit(() -> processRiskyOperation(input, index)));
                }
                
                // 모든 결과 수집
                List<String> results = new ArrayList<>();
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        results.add(futures.get(i).get());
                    } catch (Exception e) {
                        // 하나라도 실패하면 나머지 취소
                        futures.forEach(f -> f.cancel(true));
                        throw new RuntimeException("태스크 " + i + " 실패", e);
                    }
                }
                
                return results;
            });
            
            try {
                List<String> results = overallTask.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                logger.info("타임아웃 내 모든 태스크 완료");
                return results;
            } catch (TimeoutException e) {
                logger.warning("타임아웃 발생, 모든 태스크 취소 중...");
                overallTask.cancel(true);
                throw e;
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
    
    /**
     * 부분 실패 허용 패턴
     * 일부 태스크 실패를 허용하고 성공한 결과만 반환
     */
    public ProcessingResult processWithPartialFailure(List<String> inputs) throws InterruptedException {
        logger.info("부분 실패 허용 처리 시작, 입력 개수: " + inputs.size());
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            
            // 모든 입력에 대해 병렬 처리 시작
            for (int i = 0; i < inputs.size(); i++) {
                final int index = i;
                final String input = inputs.get(i);
                
                futures.add(executor.submit(() -> processRiskyOperation(input, index)));
            }
            
            List<String> successResults = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            
            // 각 태스크 결과 개별 처리
            for (int i = 0; i < futures.size(); i++) {
                try {
                    String result = futures.get(i).get();
                    successResults.add(result);
                    logger.info("태스크 " + i + " 성공: " + result);
                } catch (ExecutionException e) {
                    String reason = "태스크 " + i + " 실패: " + e.getCause().getMessage();
                    failureReasons.add(reason);
                    logger.warning(reason);
                }
            }
            
            ProcessingResult result = new ProcessingResult(successResults, failureReasons);
            logger.info("부분 실패 처리 완료 - 성공: " + successResults.size() + ", 실패: " + failureReasons.size());
            
            return result;
        }
    }
    
    /**
     * 재시도 메커니즘이 있는 에러 처리
     */
    public String processWithRetry(String input, int maxRetries) throws InterruptedException {
        logger.info("재시도 처리 시작: " + input + ", 최대 재시도: " + maxRetries);
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                Future<String> future = executor.submit(() -> {
                    try {
                        return processRiskyOperation(input, attempt - 1);
                    } catch (Exception e) {
                        throw new RuntimeException("시도 " + attempt + " 실패", e);
                    }
                });
                
                String result = future.get();
                logger.info("시도 " + attempt + "에서 성공: " + result);
                return result;
                
            } catch (ExecutionException e) {
                lastException = (Exception) e.getCause();
                logger.warning("시도 " + attempt + " 실패: " + lastException.getMessage());
                
                if (attempt <= maxRetries) {
                    // 지수적 백오프로 재시도 대기
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info(waitTime + "ms 대기 후 재시도...");
                    Thread.sleep(waitTime);
                }
            }
        }
        
        logger.severe("모든 재시도 실패");
        throw new RuntimeException("최대 재시도 횟수 초과", lastException);
    }
    
    /**
     * 서킷 브레이커 패턴과 결합된 에러 처리
     */
    private volatile int consecutiveFailures = 0;
    private volatile long lastFailureTime = 0;
    private static final int FAILURE_THRESHOLD = 3;
    private static final long RECOVERY_TIME_MS = 10000; // 10초
    
    public String processWithCircuitBreaker(String input) throws InterruptedException {
        logger.info("서킷 브레이커 처리: " + input);
        
        // 서킷 브레이커 상태 확인
        if (consecutiveFailures >= FAILURE_THRESHOLD) {
            if (System.currentTimeMillis() - lastFailureTime < RECOVERY_TIME_MS) {
                throw new RuntimeException("서킷 브레이커 열림 상태 - 서비스 일시적 차단");
            } else {
                logger.info("서킷 브레이커 복구 시도");
                consecutiveFailures = 0; // 복구 시도
            }
        }
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            Future<String> future = executor.submit(() -> processRiskyOperation(input, 0));
            String result = future.get();
            
            // 성공 시 실패 카운터 리셋
            consecutiveFailures = 0;
            logger.info("서킷 브레이커 처리 성공: " + result);
            return result;
            
        } catch (ExecutionException e) {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
            
            logger.warning("서킷 브레이커 실패 카운트: " + consecutiveFailures);
            
            if (consecutiveFailures >= FAILURE_THRESHOLD) {
                logger.severe("서킷 브레이커 열림 - 연속 실패 임계치 도달");
            }
            
            throw new RuntimeException(e.getCause());
        }
    }
    
    // 시뮬레이션을 위한 위험한 작업 메서드
    private String processRiskyOperation(String input, int taskIndex) throws Exception {
        // 다양한 실패 시나리오 시뮬레이션
        int randomValue = (int) (Math.random() * 100);
        
        if (input.equals("fail")) {
            throw new RuntimeException("의도적 실패: " + input);
        }
        
        if (input.equals("timeout")) {
            Thread.sleep(5000); // 긴 처리 시간 시뮬레이션
        }
        
        if (taskIndex == 2 && randomValue < 30) {
            throw new RuntimeException("랜덤 실패 시뮬레이션: " + input);
        }
        
        // 정상 처리 시뮬레이션
        Thread.sleep(100 + randomValue * 10);
        return "처리된: " + input + " (태스크 " + taskIndex + ")";
    }
    
    // 결과 클래스
    public record ProcessingResult(List<String> successes, List<String> failures) {
        public boolean hasFailures() {
            return !failures.isEmpty();
        }
        
        public boolean allSucceeded() {
            return failures.isEmpty();
        }
        
        public double successRate() {
            int total = successes.size() + failures.size();
            return total == 0 ? 0.0 : (double) successes.size() / total;
        }
    }
    
    // 실행 예제
    public static void main(String[] args) {
        ErrorHandlingExample example = new ErrorHandlingExample();
        
        try {
            // 기본 에러 처리 테스트
            System.out.println("=== 기본 에러 처리 테스트 ===");
            List<String> inputs = List.of("data1", "data2", "data3");
            List<String> results = example.processWithFailFast(inputs);
            System.out.println("성공 결과: " + results);
            
        } catch (Exception e) {
            System.err.println("실패: " + e.getMessage());
        }
        
        try {
            // 타임아웃 처리 테스트
            System.out.println("\n=== 타임아웃 처리 테스트 ===");
            List<String> timeoutInputs = List.of("quick1", "quick2", "quick3");
            List<String> timeoutResults = example.processWithTimeout(timeoutInputs, Duration.ofSeconds(2));
            System.out.println("타임아웃 결과: " + timeoutResults);
            
        } catch (Exception e) {
            System.err.println("타임아웃 실패: " + e.getMessage());
        }
        
        try {
            // 부분 실패 허용 테스트
            System.out.println("\n=== 부분 실패 허용 테스트 ===");
            List<String> partialInputs = List.of("good1", "fail", "good2", "good3");
            ProcessingResult partialResult = example.processWithPartialFailure(partialInputs);
            System.out.println("부분 결과 - 성공: " + partialResult.successes());
            System.out.println("부분 결과 - 실패: " + partialResult.failures());
            System.out.println("성공률: " + String.format("%.2f%%", partialResult.successRate() * 100));
            
        } catch (Exception e) {
            System.err.println("부분 실패 처리 오류: " + e.getMessage());
        }
        
        try {
            // 재시도 메커니즘 테스트
            System.out.println("\n=== 재시도 메커니즘 테스트 ===");
            String retryResult = example.processWithRetry("retry-data", 3);
            System.out.println("재시도 성공: " + retryResult);
            
        } catch (Exception e) {
            System.err.println("재시도 최종 실패: " + e.getMessage());
        }
    }
}
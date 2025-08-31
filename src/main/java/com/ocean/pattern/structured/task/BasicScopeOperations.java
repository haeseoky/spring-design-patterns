package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * StructuredTaskScope 기본 연산 패턴
 * 
 * 이 클래스는 JDK 25의 StructuredTaskScope API의 핵심 메서드들인
 * open(), fork(), join(), close()의 사용법을 상세히 보여줍니다.
 * 
 * JDK 25 실제 API:
 * ```java
 * try (var scope = StructuredTaskScope.open()) {
 *     Subtask<String> task1 = scope.fork(() -> operation1());
 *     Subtask<String> task2 = scope.fork(() -> operation2());
 *     
 *     scope.join(); // 모든 subtask 완료 대기
 *     
 *     return processResults(task1.get(), task2.get());
 * }
 * ```
 * 
 * 학습 목표:
 * 1. StructuredTaskScope의 생명주기 이해
 * 2. 가상 스레드와의 통합 활용
 * 3. try-with-resources 패턴의 중요성
 * 4. Subtask의 상태 관리
 */
public class BasicScopeOperations {
    
    private static final Logger logger = Logger.getLogger(BasicScopeOperations.class.getName());
    
    /**
     * 기본 스코프 연산 데모
     * open → fork → join → close 패턴
     */
    public static class BasicScopeDemo {
        
        /**
         * JDK 25 실제 구현 (이론적)
         * 
         * public CompletedTasks processTasksWithScope() throws InterruptedException {
         *     try (var scope = StructuredTaskScope.open()) {
         *         Subtask<String> userTask = scope.fork(() -> fetchUserData());
         *         Subtask<String> configTask = scope.fork(() -> fetchConfiguration());
         *         Subtask<String> statusTask = scope.fork(() -> checkSystemStatus());
         *         
         *         scope.join(); // 모든 태스크 완료까지 대기
         *         
         *         return new CompletedTasks(
         *             userTask.get(),
         *             configTask.get(),
         *             statusTask.get()
         *         );
         *     }
         * }
         */
        
        /**
         * 시뮬레이션 구현: 기본 스코프 연산
         */
        public CompletedTasks processTasksSimulation() throws InterruptedException, ExecutionException {
            logger.info("기본 스코프 연산 시뮬레이션 시작");
            
            // StructuredTaskScope.open() 시뮬레이션
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // fork() 시뮬레이션 - 각 태스크를 별도 가상 스레드에서 실행
                Future<String> userTask = executor.submit(() -> fetchUserData());
                Future<String> configTask = executor.submit(() -> fetchConfiguration());
                Future<String> statusTask = executor.submit(() -> checkSystemStatus());
                
                logger.info("3개 태스크 포크 완료");
                
                // join() 시뮬레이션 - 모든 태스크 완료 대기
                String userData = userTask.get();
                String configData = configTask.get();
                String statusData = statusTask.get();
                
                logger.info("모든 태스크 조인 완료");
                
                // close()는 try-with-resources에 의해 자동 호출됨
                return new CompletedTasks(userData, configData, statusData);
            }
        }
        
        /**
         * 가상 스레드와 StructuredTaskScope의 시너지
         * 수천 개의 경량 태스크를 효율적으로 처리
         */
        public ProcessingResult processLightweightTasks(int taskCount) throws InterruptedException, ExecutionException {
            logger.info("경량 태스크 처리 시작: " + taskCount + "개");
            Instant startTime = Instant.now();
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<Future<String>> futures = new ArrayList<>();
                
                // 수천 개의 가상 스레드 생성 (매우 경량)
                for (int i = 0; i < taskCount; i++) {
                    final int taskId = i;
                    Future<String> future = executor.submit(() -> lightweightOperation(taskId));
                    futures.add(future);
                }
                
                logger.info(taskCount + "개 태스크 포크 완료");
                
                // 모든 결과 수집
                List<String> results = new ArrayList<>();
                for (Future<String> future : futures) {
                    results.add(future.get());
                }
                
                Duration totalTime = Duration.between(startTime, Instant.now());
                
                ProcessingResult result = new ProcessingResult(
                    results.size(),
                    totalTime,
                    calculateThroughput(results.size(), totalTime)
                );
                
                logger.info("경량 태스크 처리 완료: " + result);
                return result;
            }
        }
        
        /**
         * 구조적 스코프의 생명주기 관리
         * 예외 발생 시에도 모든 태스크가 정리됨을 보장
         */
        public LifecycleResult demonstrateLifecycleManagement(boolean simulateError) throws InterruptedException {
            logger.info("생명주기 관리 데모 시작 - 오류 시뮬레이션: " + simulateError);
            
            List<String> lifecycleEvents = new ArrayList<>();
            lifecycleEvents.add("스코프 열기");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // 여러 태스크 시작
                Future<String> task1 = executor.submit(() -> monitoredTask("Task-1", lifecycleEvents));
                Future<String> task2 = executor.submit(() -> monitoredTask("Task-2", lifecycleEvents));
                Future<String> task3 = executor.submit(() -> {
                    if (simulateError) {
                        throw new RuntimeException("시뮬레이션된 오류");
                    }
                    return monitoredTask("Task-3", lifecycleEvents);
                });
                
                lifecycleEvents.add("모든 태스크 포크 완료");
                
                try {
                    // 모든 태스크 완료 대기
                    String result1 = task1.get();
                    String result2 = task2.get();
                    String result3 = task3.get();
                    
                    lifecycleEvents.add("모든 태스크 성공적으로 완료");
                    
                    return new LifecycleResult(true, lifecycleEvents, 
                        List.of(result1, result2, result3), null);
                    
                } catch (ExecutionException e) {
                    lifecycleEvents.add("태스크 실패 감지: " + e.getCause().getMessage());
                    
                    // 실패한 태스크가 있어도 다른 태스크들은 정리됨
                    task1.cancel(true);
                    task2.cancel(true);
                    task3.cancel(true);
                    
                    lifecycleEvents.add("모든 태스크 정리 완료");
                    
                    return new LifecycleResult(false, lifecycleEvents, 
                        new ArrayList<>(), e.getCause().getMessage());
                }
                
            } finally {
                lifecycleEvents.add("스코프 닫기 (자동 리소스 정리)");
            }
        }
        
        /**
         * 중첩된 스코프 처리
         * 스코프 안에서 또 다른 스코프를 열 수 있음
         */
        public NestedScopeResult demonstrateNestedScopes() throws InterruptedException, ExecutionException {
            logger.info("중첩 스코프 데모 시작");
            
            // 외부 스코프
            try (ExecutorService outerExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                Future<String> outerTask = outerExecutor.submit(() -> {
                    
                    // 내부 스코프
                    try (ExecutorService innerExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                        
                        Future<String> innerTask1 = innerExecutor.submit(() -> "내부작업-1");
                        Future<String> innerTask2 = innerExecutor.submit(() -> "내부작업-2");
                        
                        return "외부작업[" + innerTask1.get() + ", " + innerTask2.get() + "]";
                        
                    } catch (Exception e) {
                        throw new RuntimeException("내부 스코프 오류", e);
                    }
                });
                
                Future<String> anotherOuterTask = outerExecutor.submit(() -> "외부작업-직접");
                
                String outerResult = outerTask.get();
                String anotherResult = anotherOuterTask.get();
                
                NestedScopeResult result = new NestedScopeResult(outerResult, anotherResult);
                logger.info("중첩 스코프 완료: " + result);
                
                return result;
            }
        }
        
        // 시뮬레이션용 헬퍼 메서드들
        
        private String fetchUserData() {
            simulateWork(200, 400);
            logger.info("사용자 데이터 조회 완료");
            return "UserData{id=123, name='사용자'}";
        }
        
        private String fetchConfiguration() {
            simulateWork(100, 300);
            logger.info("구성 데이터 조회 완료");
            return "Config{timeout=30s, maxConnections=100}";
        }
        
        private String checkSystemStatus() {
            simulateWork(150, 250);
            logger.info("시스템 상태 확인 완료");
            return "Status{health=OK, uptime=24h}";
        }
        
        private String lightweightOperation(int taskId) {
            // 매우 가벼운 작업 시뮬레이션
            simulateWork(10, 50);
            return "Result-" + taskId;
        }
        
        private String monitoredTask(String taskName, List<String> lifecycleEvents) {
            synchronized (lifecycleEvents) {
                lifecycleEvents.add(taskName + " 시작");
            }
            
            simulateWork(100, 300);
            
            synchronized (lifecycleEvents) {
                lifecycleEvents.add(taskName + " 완료");
            }
            
            return taskName + " 결과";
        }
        
        private void simulateWork(int minMs, int maxMs) {
            try {
                int delay = minMs + (int)(Math.random() * (maxMs - minMs));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        
        private double calculateThroughput(int taskCount, Duration duration) {
            if (duration.isZero()) return 0.0;
            return (double) taskCount / duration.toSeconds();
        }
    }
    
    /**
     * 스코프 설정과 최적화
     */
    public static class ScopeConfiguration {
        
        /**
         * 커스텀 ThreadFactory를 사용한 스코프 구성
         * JDK 25에서는 StructuredTaskScope.open(Joiner, Function<ScopeConfiguration, ?>) 형태
         */
        public ConfigurationResult demonstrateCustomConfiguration() throws InterruptedException, ExecutionException {
            logger.info("커스텀 스코프 구성 데모");
            
            // 커스텀 스레드 팩토리 (프로덕션 환경에서 모니터링/명명 등에 유용)
            ThreadFactory customFactory = Thread.ofVirtual()
                .name("CustomScope-", 0)
                .factory();
            
            try (ExecutorService executor = Executors.newThreadPerTaskExecutor(customFactory)) {
                
                Future<String> task1 = executor.submit(() -> {
                    String threadName = Thread.currentThread().getName();
                    logger.info("작업 실행 중인 스레드: " + threadName);
                    return "결과-1 from " + threadName;
                });
                
                Future<String> task2 = executor.submit(() -> {
                    String threadName = Thread.currentThread().getName();
                    logger.info("작업 실행 중인 스레드: " + threadName);
                    return "결과-2 from " + threadName;
                });
                
                String result1 = task1.get();
                String result2 = task2.get();
                
                return new ConfigurationResult(customFactory.getClass().getSimpleName(), 
                                             List.of(result1, result2));
            }
        }
        
        /**
         * 스코프 모니터링 및 관리
         */
        public MonitoringResult demonstrateMonitoring() throws InterruptedException, ExecutionException {
            logger.info("스코프 모니터링 데모 시작");
            
            Instant startTime = Instant.now();
            int taskCount = 10;
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<Future<TaskMetrics>> futures = new ArrayList<>();
                
                for (int i = 0; i < taskCount; i++) {
                    final int taskId = i;
                    Future<TaskMetrics> future = executor.submit(() -> createMonitoredTask(taskId));
                    futures.add(future);
                }
                
                // 모든 태스크 완료 및 메트릭 수집
                List<TaskMetrics> allMetrics = new ArrayList<>();
                for (Future<TaskMetrics> future : futures) {
                    allMetrics.add(future.get());
                }
                
                Duration totalDuration = Duration.between(startTime, Instant.now());
                
                // 메트릭 집계
                double avgDuration = allMetrics.stream()
                    .mapToLong(m -> m.durationMs())
                    .average()
                    .orElse(0.0);
                
                long totalMemoryUsed = allMetrics.stream()
                    .mapToLong(TaskMetrics::memoryUsedBytes)
                    .sum();
                
                MonitoringResult result = new MonitoringResult(
                    taskCount, totalDuration, avgDuration, totalMemoryUsed, allMetrics
                );
                
                logger.info("모니터링 완료: " + result);
                return result;
            }
        }
        
        private TaskMetrics createMonitoredTask(int taskId) {
            Instant taskStart = Instant.now();
            long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            // 작업 시뮬레이션
            simulateWork(50, 200);
            
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            Duration taskDuration = Duration.between(taskStart, Instant.now());
            
            return new TaskMetrics(taskId, taskDuration.toMillis(), 
                                 Math.abs(memoryAfter - memoryBefore));
        }
        
        private void simulateWork(int minMs, int maxMs) {
            try {
                int delay = minMs + (int)(Math.random() * (maxMs - minMs));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
    
    // 결과 데이터 클래스들
    
    public record CompletedTasks(String userData, String configData, String statusData) {}
    
    public record ProcessingResult(int taskCount, Duration totalDuration, double throughputPerSecond) {}
    
    public record LifecycleResult(
        boolean successful, 
        List<String> lifecycleEvents,
        List<String> results,
        String errorMessage
    ) {}
    
    public record NestedScopeResult(String outerResult, String anotherOuterResult) {}
    
    public record ConfigurationResult(String threadFactoryType, List<String> results) {}
    
    public record TaskMetrics(int taskId, long durationMs, long memoryUsedBytes) {}
    
    public record MonitoringResult(
        int totalTasks,
        Duration totalDuration,
        double avgTaskDurationMs,
        long totalMemoryUsedBytes,
        List<TaskMetrics> individualMetrics
    ) {}
    
    // 실행 예제
    public static void main(String[] args) {
        BasicScopeDemo demo = new BasicScopeDemo();
        ScopeConfiguration config = new ScopeConfiguration();
        
        try {
            // 기본 스코프 연산
            System.out.println("=== 기본 스코프 연산 데모 ===");
            CompletedTasks basicResult = demo.processTasksSimulation();
            System.out.println("완료된 작업: " + basicResult);
            
            // 경량 태스크 처리
            System.out.println("\n=== 경량 태스크 처리 데모 ===");
            ProcessingResult lightResult = demo.processLightweightTasks(100);
            System.out.println("처리 결과: " + lightResult.taskCount() + "개 작업, " +
                             String.format("%.2f", lightResult.throughputPerSecond()) + " tasks/sec");
            
            // 생명주기 관리 (성공 케이스)
            System.out.println("\n=== 생명주기 관리 (성공) ===");
            LifecycleResult successLifecycle = demo.demonstrateLifecycleManagement(false);
            System.out.println("성공: " + successLifecycle.successful());
            successLifecycle.lifecycleEvents().forEach(System.out::println);
            
            // 생명주기 관리 (실패 케이스)
            System.out.println("\n=== 생명주기 관리 (실패) ===");
            LifecycleResult failureLifecycle = demo.demonstrateLifecycleManagement(true);
            System.out.println("성공: " + failureLifecycle.successful());
            System.out.println("오류: " + failureLifecycle.errorMessage());
            failureLifecycle.lifecycleEvents().forEach(System.out::println);
            
            // 중첩 스코프
            System.out.println("\n=== 중첩 스코프 데모 ===");
            NestedScopeResult nestedResult = demo.demonstrateNestedScopes();
            System.out.println("중첩 결과: " + nestedResult);
            
            // 커스텀 구성
            System.out.println("\n=== 커스텀 구성 데모 ===");
            ConfigurationResult configResult = config.demonstrateCustomConfiguration();
            System.out.println("구성 결과: " + configResult);
            
            // 모니터링
            System.out.println("\n=== 모니터링 데모 ===");
            MonitoringResult monitorResult = config.demonstrateMonitoring();
            System.out.printf("모니터링: %d 작업, 평균 시간 %.2fms, 총 메모리 %d bytes%n",
                monitorResult.totalTasks(), monitorResult.avgTaskDurationMs(), 
                monitorResult.totalMemoryUsedBytes());
            
        } catch (Exception e) {
            System.err.println("데모 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
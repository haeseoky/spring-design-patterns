package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

/**
 * StructuredTaskScope 커스텀 조이너 구현
 * 
 * JDK 25의 StructuredTaskScope.Joiner 인터페이스를 구현하여
 * 비즈니스 로직에 특화된 완료 조건을 정의하는 방법을 보여줍니다.
 * 
 * Joiner 인터페이스 주요 메서드:
 * - onFork(Subtask): 태스크 시작 시 호출, boolean 반환으로 스코프 취소 가능
 * - onComplete(Subtask): 태스크 완료 시 호출, boolean 반환으로 스코프 취소 가능  
 * - result(): 최종 결과 생성, join() 메서드에서 반환될 값
 * 
 * JDK 25 실제 사용법:
 * ```java
 * public class CustomJoiner implements StructuredTaskScope.Joiner<String, List<String>> {
 *     public boolean onFork(Subtask<String> subtask) {
 *         // 태스크 포크 시 로직
 *         return false; // true면 스코프 취소
 *     }
 *     
 *     public boolean onComplete(Subtask<String> subtask) {
 *         // 태스크 완료 시 로직
 *         return shouldCancel; // 조건에 따른 스코프 취소
 *     }
 *     
 *     public List<String> result() {
 *         // 최종 결과 반환
 *         return collectedResults;
 *     }
 * }
 * 
 * try (var scope = StructuredTaskScope.open(new CustomJoiner())) {
 *     // 태스크들...
 *     List<String> results = scope.join();
 * }
 * ```
 * 
 * 학습 목표:
 * 1. Joiner 인터페이스 구현 패턴
 * 2. 스레드 안전한 상태 관리
 * 3. 비즈니스 로직 기반 완료 조건
 * 4. 동적 스코프 제어 메커니즘
 */
public class CustomJoinerImplementation {
    
    private static final Logger logger = Logger.getLogger(CustomJoinerImplementation.class.getName());
    
    /**
     * 품질 기반 조이너 - 지정된 품질 점수 이상의 첫 번째 결과를 반환
     */
    public static class QualityBasedJoiner {
        
        /**
         * JDK 25 실제 Joiner 구현 (이론적)
         * 
         * public static class QualityJoiner implements StructuredTaskScope.Joiner<QualityResult, QualityResult> {
         *     private final double qualityThreshold;
         *     private final AtomicReference<QualityResult> bestResult = new AtomicReference<>();
         *     
         *     public QualityJoiner(double threshold) {
         *         this.qualityThreshold = threshold;
         *     }
         *     
         *     @Override
         *     public boolean onFork(Subtask<QualityResult> subtask) {
         *         logger.info("품질 작업 시작: " + subtask);
         *         return false; // 계속 진행
         *     }
         *     
         *     @Override
         *     public boolean onComplete(Subtask<QualityResult> subtask) {
         *         try {
         *             if (subtask.state() == Subtask.State.SUCCESS) {
         *                 QualityResult result = subtask.get();
         *                 
         *                 // 임계치 이상이면 즉시 완료
         *                 if (result.quality() >= qualityThreshold) {
         *                     bestResult.set(result);
         *                     return true; // 스코프 취소 (조기 완료)
         *                 }
         *                 
         *                 // 최고 품질 결과 업데이트
         *                 bestResult.updateAndGet(current -> 
         *                     current == null || result.quality() > current.quality() ? result : current);
         *             }
         *         } catch (Exception e) {
         *             logger.warning("품질 결과 처리 실패: " + e.getMessage());
         *         }
         *         return false; // 계속 진행
         *     }
         *     
         *     @Override
         *     public QualityResult result() {
         *         QualityResult result = bestResult.get();
         *         if (result == null) {
         *             throw new RuntimeException("품질 기준을 만족하는 결과 없음");
         *         }
         *         return result;
         *     }
         * }
         */
        
        /**
         * 시뮬레이션: 품질 기반 조이너 구현
         */
        public QualityResult processWithQualityThreshold(List<QualityTask> tasks, double threshold) 
                throws InterruptedException, ExecutionException {
            
            logger.info("품질 기반 처리 시작 - 임계치: " + threshold + ", 작업 수: " + tasks.size());
            
            // 시뮬레이션된 조이너 상태
            AtomicReference<QualityResult> bestResult = new AtomicReference<>();
            AtomicBoolean thresholdMet = new AtomicBoolean(false);
            AtomicInteger completedCount = new AtomicInteger(0);
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // onComplete 시뮬레이션 - 완료된 작업들을 순차적으로 처리
                CompletionService<QualityResult> completionService = new ExecutorCompletionService<>(executor);
                List<Future<QualityResult>> futures = new ArrayList<>();
                
                // onFork 시뮬레이션 - 모든 작업 시작
                for (QualityTask task : tasks) {
                    logger.info("품질 작업 포크: " + task.name());
                    
                    Future<QualityResult> future = completionService.submit(() -> executeQualityTask(task));
                    futures.add(future);
                }
                
                while (completedCount.get() < tasks.size() && !thresholdMet.get()) {
                    try {
                        Future<QualityResult> completed = completionService.poll(100, TimeUnit.MILLISECONDS);
                        if (completed == null) continue;
                        
                        QualityResult result = completed.get();
                        completedCount.incrementAndGet();
                        
                        logger.info("품질 작업 완료: " + result.taskName() + " - 품질: " + result.quality());
                        
                        // 임계치 달성 확인
                        if (result.quality() >= threshold) {
                            bestResult.set(result);
                            thresholdMet.set(true);
                            
                            // 나머지 작업 취소 (조기 완료)
                            futures.forEach(f -> f.cancel(true));
                            
                            logger.info("품질 임계치 달성, 조기 완료: " + result.quality());
                            break;
                        }
                        
                        // 최고 품질 결과 업데이트
                        bestResult.updateAndGet(current -> 
                            current == null || result.quality() > current.quality() ? result : current);
                        
                    } catch (ExecutionException e) {
                        completedCount.incrementAndGet();
                        logger.warning("품질 작업 실패: " + e.getCause().getMessage());
                    }
                }
                
                // result() 시뮬레이션
                QualityResult finalResult = bestResult.get();
                if (finalResult == null) {
                    throw new RuntimeException("품질 기준을 만족하는 결과가 없습니다");
                }
                
                return finalResult;
            }
        }
        
        private QualityResult executeQualityTask(QualityTask task) throws Exception {
            Thread.sleep(task.duration());
            
            if (Math.random() < task.failureRate()) {
                throw new Exception("품질 작업 실패: " + task.name());
            }
            
            // 품질 점수 계산 (시뮬레이션)
            double quality = task.expectedQuality() + (Math.random() - 0.5) * 0.2;
            quality = Math.max(0.0, Math.min(1.0, quality)); // 0-1 범위로 제한
            
            return new QualityResult(task.name(), quality, "품질 작업 완료");
        }
    }
    
    /**
     * 과반수 합의 조이너 - 지정된 비율 이상이 합의에 도달하면 완료
     */
    public static class MajorityConsensusJoiner {
        
        /**
         * 시뮬레이션: 과반수 합의 기반 조이너
         */
        public ConsensusResult processWithMajorityConsensus(List<ConsensusTask> tasks, double requiredRatio) 
                throws InterruptedException, ExecutionException {
            
            logger.info("과반수 합의 처리 시작 - 필요 비율: " + (requiredRatio * 100) + "%, 참가자: " + tasks.size());
            
            int requiredCount = (int) Math.ceil(tasks.size() * requiredRatio);
            
            // 조이너 상태
            List<ConsensusVote> votes = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger completedCount = new AtomicInteger(0);
            AtomicBoolean consensusReached = new AtomicBoolean(false);
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // 합의 완료 모니터링
                CompletionService<ConsensusVote> completionService = new ExecutorCompletionService<>(executor);
                List<Future<ConsensusVote>> futures = new ArrayList<>();
                
                // 모든 합의 참가자 작업 시작
                for (ConsensusTask task : tasks) {
                    logger.info("합의 참가자 포크: " + task.participantId());
                    
                    Future<ConsensusVote> future = completionService.submit(() -> executeConsensusTask(task));
                    futures.add(future);
                }
                
                while (completedCount.get() < tasks.size() && !consensusReached.get()) {
                    try {
                        Future<ConsensusVote> completed = completionService.poll(200, TimeUnit.MILLISECONDS);
                        if (completed == null) continue;
                        
                        ConsensusVote vote = completed.get();
                        votes.add(vote);
                        completedCount.incrementAndGet();
                        
                        logger.info("합의 투표 완료: " + vote.participantId() + " - 결정: " + vote.decision());
                        
                        // 과반수 합의 확인
                        long agreeCount = votes.stream().filter(v -> v.decision().equals("AGREE")).count();
                        
                        if (agreeCount >= requiredCount) {
                            consensusReached.set(true);
                            
                            // 나머지 작업 취소 (합의 달성)
                            futures.forEach(f -> f.cancel(true));
                            
                            logger.info("과반수 합의 달성: " + agreeCount + "/" + tasks.size());
                            break;
                        }
                        
                        // 합의 불가능 확인 (남은 참가자로도 과반수 불가능)
                        int remainingTasks = tasks.size() - completedCount.get();
                        if (agreeCount + remainingTasks < requiredCount) {
                            logger.warning("합의 불가능, 조기 종료");
                            futures.forEach(f -> f.cancel(true));
                            break;
                        }
                        
                    } catch (ExecutionException e) {
                        completedCount.incrementAndGet();
                        logger.warning("합의 참가자 실패: " + e.getCause().getMessage());
                    }
                }
                
                return new ConsensusResult(
                    consensusReached.get(),
                    votes,
                    requiredCount,
                    tasks.size()
                );
            }
        }
        
        private ConsensusVote executeConsensusTask(ConsensusTask task) throws Exception {
            Thread.sleep(task.deliberationTime());
            
            if (Math.random() < task.unavailabilityRate()) {
                throw new Exception("합의 참가자 응답 불가: " + task.participantId());
            }
            
            // 합의 결정 시뮬레이션
            String decision = Math.random() < task.agreeRate() ? "AGREE" : "DISAGREE";
            String reasoning = "합의 이유: " + task.participantId() + " 판단";
            
            return new ConsensusVote(task.participantId(), decision, reasoning);
        }
    }
    
    /**
     * 리소스 제약 조이너 - 시스템 리소스 상태에 따라 동적으로 완료 조건 조정
     */
    public static class ResourceConstrainedJoiner {
        
        /**
         * 시뮬레이션: 리소스 제약 기반 조이너
         */
        public ResourceResult processWithResourceConstraints(List<ResourceTask> tasks, ResourceMonitor monitor) 
                throws InterruptedException, ExecutionException {
            
            logger.info("리소스 제약 처리 시작 - 작업 수: " + tasks.size());
            
            // 조이너 상태
            List<ResourceTaskResult> results = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger activeTaskCount = new AtomicInteger(0);
            AtomicBoolean resourceExhausted = new AtomicBoolean(false);
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<Future<ResourceTaskResult>> futures = new ArrayList<>();
                int maxConcurrentTasks = monitor.getMaxConcurrentTasks();
                
                // 리소스 모니터링 시작
                Future<?> monitoringTask = executor.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            ResourceStatus status = monitor.getCurrentStatus();
                            
                            if (status == ResourceStatus.CRITICAL) {
                                logger.warning("리소스 위험 수준 - 작업 중단 권고");
                                resourceExhausted.set(true);
                                break;
                            }
                            
                            Thread.sleep(100); // 모니터링 주기
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                
                // 리소스 상황에 따른 동적 작업 시작
                for (int i = 0; i < tasks.size() && !resourceExhausted.get(); i++) {
                    ResourceTask task = tasks.get(i);
                    
                    // 동시 실행 작업 수 제한
                    while (activeTaskCount.get() >= maxConcurrentTasks && !resourceExhausted.get()) {
                        Thread.sleep(50);
                    }
                    
                    if (resourceExhausted.get()) {
                        logger.info("리소스 제약으로 작업 " + task.taskId() + " 건너뜀");
                        continue;
                    }
                    
                    activeTaskCount.incrementAndGet();
                    logger.info("리소스 작업 포크: " + task.taskId() + " (활성: " + activeTaskCount.get() + ")");
                    
                    Future<ResourceTaskResult> future = executor.submit(() -> {
                        try {
                            ResourceTaskResult result = executeResourceTask(task);
                            results.add(result);
                            return result;
                        } finally {
                            activeTaskCount.decrementAndGet();
                        }
                    });
                    
                    futures.add(future);
                }
                
                // 시작된 작업들의 완료 대기
                List<Exception> failures = new ArrayList<>();
                
                for (Future<ResourceTaskResult> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        failures.add((Exception) e.getCause());
                        logger.warning("리소스 작업 실패: " + e.getCause().getMessage());
                    }
                }
                
                // 모니터링 종료
                monitoringTask.cancel(true);
                
                return new ResourceResult(
                    results,
                    failures,
                    resourceExhausted.get(),
                    monitor.getCurrentStatus()
                );
            }
        }
        
        private ResourceTaskResult executeResourceTask(ResourceTask task) throws Exception {
            Thread.sleep(task.duration());
            
            if (Math.random() < task.failureRate()) {
                throw new Exception("리소스 작업 실패: " + task.taskId());
            }
            
            return new ResourceTaskResult(task.taskId(), "완료", task.resourceUsage());
        }
    }
    
    /**
     * 시간 윈도우 조이너 - 지정된 시간 윈도우 내에서 최대한 많은 결과 수집
     */
    public static class TimeWindowJoiner {
        
        /**
         * 시뮬레이션: 시간 윈도우 기반 조이너
         */
        public TimeWindowResult processWithTimeWindow(List<TimeTask> tasks, Duration windowDuration) 
                throws InterruptedException, ExecutionException {
            
            logger.info("시간 윈도우 처리 시작 - 윈도우: " + windowDuration + ", 작업 수: " + tasks.size());
            Instant windowStart = Instant.now();
            Instant windowEnd = windowStart.plus(windowDuration);
            
            // 조이너 상태
            List<TimeTaskResult> results = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger completedCount = new AtomicInteger(0);
            AtomicBoolean windowExpired = new AtomicBoolean(false);
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // 시간 윈도우 모니터링
                CompletionService<TimeTaskResult> completionService = new ExecutorCompletionService<>(executor);
                List<Future<TimeTaskResult>> futures = new ArrayList<>();
                
                // 모든 작업 시작
                for (TimeTask task : tasks) {
                    logger.info("시간 윈도우 작업 포크: " + task.taskId());
                    
                    Future<TimeTaskResult> future = completionService.submit(() -> executeTimeTask(task));
                    futures.add(future);
                }
                
                while (completedCount.get() < tasks.size() && !windowExpired.get()) {
                    Instant now = Instant.now();
                    if (now.isAfter(windowEnd)) {
                        windowExpired.set(true);
                        logger.info("시간 윈도우 만료, 미완료 작업 취소");
                        futures.forEach(f -> f.cancel(true));
                        break;
                    }
                    
                    long remainingMs = Duration.between(now, windowEnd).toMillis();
                    
                    try {
                        Future<TimeTaskResult> completed = completionService.poll(
                            Math.min(remainingMs, 100), TimeUnit.MILLISECONDS);
                        
                        if (completed != null) {
                            TimeTaskResult result = completed.get();
                            results.add(result);
                            completedCount.incrementAndGet();
                            
                            logger.info("시간 윈도우 작업 완료: " + result.taskId() + 
                                       " (완료: " + completedCount.get() + "/" + tasks.size() + ")");
                        }
                        
                    } catch (ExecutionException e) {
                        completedCount.incrementAndGet();
                        logger.warning("시간 윈도우 작업 실패: " + e.getCause().getMessage());
                    }
                }
                
                Duration actualDuration = Duration.between(windowStart, Instant.now());
                
                return new TimeWindowResult(
                    results,
                    windowExpired.get(),
                    actualDuration,
                    completedCount.get(),
                    tasks.size()
                );
            }
        }
        
        private TimeTaskResult executeTimeTask(TimeTask task) throws Exception {
            Thread.sleep(task.expectedDuration());
            
            if (Math.random() < task.failureRate()) {
                throw new Exception("시간 윈도우 작업 실패: " + task.taskId());
            }
            
            return new TimeTaskResult(task.taskId(), "윈도우 내 완료", Instant.now());
        }
    }
    
    // 데이터 클래스들
    
    public record QualityTask(String name, int duration, double expectedQuality, double failureRate) {}
    
    public record QualityResult(String taskName, double quality, String message) {}
    
    public record ConsensusTask(String participantId, int deliberationTime, double agreeRate, double unavailabilityRate) {}
    
    public record ConsensusVote(String participantId, String decision, String reasoning) {}
    
    public record ConsensusResult(
        boolean consensusReached,
        List<ConsensusVote> votes,
        int requiredCount,
        int totalParticipants
    ) {
        public double consensusRatio() {
            long agreeCount = votes.stream().filter(v -> v.decision().equals("AGREE")).count();
            return (double) agreeCount / totalParticipants;
        }
    }
    
    public record ResourceTask(String taskId, int duration, double resourceUsage, double failureRate) {}
    
    public record ResourceTaskResult(String taskId, String status, double resourceUsed) {}
    
    public record ResourceResult(
        List<ResourceTaskResult> results,
        List<Exception> failures,
        boolean resourceExhausted,
        ResourceStatus finalStatus
    ) {}
    
    public enum ResourceStatus {
        NORMAL, HIGH_USAGE, CRITICAL
    }
    
    public interface ResourceMonitor {
        ResourceStatus getCurrentStatus();
        int getMaxConcurrentTasks();
    }
    
    public record TimeTask(String taskId, int expectedDuration, double failureRate) {}
    
    public record TimeTaskResult(String taskId, String status, Instant completionTime) {}
    
    public record TimeWindowResult(
        List<TimeTaskResult> results,
        boolean windowExpired,
        Duration actualDuration,
        int completedCount,
        int totalTasks
    ) {
        public double completionRatio() {
            return (double) completedCount / totalTasks;
        }
    }
    
    // 시뮬레이션용 리소스 모니터 구현
    public static class SimulatedResourceMonitor implements ResourceMonitor {
        private final AtomicInteger usageLevel = new AtomicInteger(0);
        
        @Override
        public ResourceStatus getCurrentStatus() {
            int level = usageLevel.getAndUpdate(current -> {
                // 리소스 사용량 시뮬레이션 (랜덤하게 변화)
                int delta = (int)(Math.random() * 21) - 10; // -10 ~ +10
                return Math.max(0, Math.min(100, current + delta));
            });
            
            if (level < 30) return ResourceStatus.NORMAL;
            if (level < 70) return ResourceStatus.HIGH_USAGE;
            return ResourceStatus.CRITICAL;
        }
        
        @Override
        public int getMaxConcurrentTasks() {
            return switch (getCurrentStatus()) {
                case NORMAL -> 8;
                case HIGH_USAGE -> 4;
                case CRITICAL -> 1;
            };
        }
    }
    
    // 실행 예제
    public static void main(String[] args) {
        QualityBasedJoiner qualityJoiner = new QualityBasedJoiner();
        MajorityConsensusJoiner consensusJoiner = new MajorityConsensusJoiner();
        ResourceConstrainedJoiner resourceJoiner = new ResourceConstrainedJoiner();
        TimeWindowJoiner timeWindowJoiner = new TimeWindowJoiner();
        
        try {
            // 품질 기반 조이너
            System.out.println("=== 품질 기반 조이너 ===");
            List<QualityTask> qualityTasks = List.of(
                new QualityTask("알고리즘-A", 300, 0.7, 0.1),
                new QualityTask("알고리즘-B", 500, 0.9, 0.2),
                new QualityTask("알고리즘-C", 200, 0.6, 0.05)
            );
            
            QualityResult qualityResult = qualityJoiner.processWithQualityThreshold(qualityTasks, 0.8);
            System.out.println("품질 결과: " + qualityResult);
            
            // 과반수 합의 조이너
            System.out.println("\n=== 과반수 합의 조이너 ===");
            List<ConsensusTask> consensusTasks = List.of(
                new ConsensusTask("노드-1", 200, 0.8, 0.05),
                new ConsensusTask("노드-2", 150, 0.6, 0.1),
                new ConsensusTask("노드-3", 300, 0.9, 0.05),
                new ConsensusTask("노드-4", 250, 0.7, 0.1),
                new ConsensusTask("노드-5", 180, 0.8, 0.05)
            );
            
            ConsensusResult consensusResult = consensusJoiner.processWithMajorityConsensus(consensusTasks, 0.6);
            System.out.println("합의 결과: 달성=" + consensusResult.consensusReached() + 
                             ", 비율=" + String.format("%.1f%%", consensusResult.consensusRatio() * 100));
            
            // 리소스 제약 조이너
            System.out.println("\n=== 리소스 제약 조이너 ===");
            List<ResourceTask> resourceTasks = List.of(
                new ResourceTask("CPU집약-1", 200, 0.8, 0.1),
                new ResourceTask("CPU집약-2", 300, 0.9, 0.15),
                new ResourceTask("메모리집약-1", 150, 0.6, 0.05),
                new ResourceTask("IO집약-1", 400, 0.3, 0.1)
            );
            
            ResourceResult resourceResult = resourceJoiner.processWithResourceConstraints(
                resourceTasks, new SimulatedResourceMonitor());
            System.out.println("리소스 결과: 완료=" + resourceResult.results().size() + 
                             ", 실패=" + resourceResult.failures().size() + 
                             ", 상태=" + resourceResult.finalStatus());
            
            // 시간 윈도우 조이너
            System.out.println("\n=== 시간 윈도우 조이너 ===");
            List<TimeTask> timeTasks = List.of(
                new TimeTask("빠른작업-1", 100, 0.05),
                new TimeTask("보통작업-1", 300, 0.1),
                new TimeTask("느린작업-1", 800, 0.05),
                new TimeTask("빠른작업-2", 150, 0.1),
                new TimeTask("보통작업-2", 400, 0.1)
            );
            
            TimeWindowResult timeResult = timeWindowJoiner.processWithTimeWindow(
                timeTasks, Duration.ofMillis(500));
            System.out.println("시간 윈도우 결과: 완료=" + timeResult.completedCount() + 
                             "/" + timeResult.totalTasks() + 
                             ", 만료=" + timeResult.windowExpired() + 
                             ", 실제시간=" + timeResult.actualDuration().toMillis() + "ms");
            
        } catch (Exception e) {
            System.err.println("커스텀 조이너 예제 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
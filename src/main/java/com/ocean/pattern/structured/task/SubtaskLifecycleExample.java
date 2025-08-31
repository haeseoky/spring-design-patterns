package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Subtask 생명주기 및 상태 관리 예제
 * 
 * JDK 25의 Subtask는 개별 태스크의 상태와 결과를 관리하는 핵심 컴포넌트입니다.
 * 
 * Subtask 상태 (State enum):
 * - UNAVAILABLE: 아직 시작되지 않음 또는 진행 중
 * - SUCCESS: 성공적으로 완료됨
 * - FAILED: 예외로 인해 실패함  
 * - CANCELLED: 취소됨
 * 
 * JDK 25 실제 사용법:
 * ```java
 * try (var scope = StructuredTaskScope.open()) {
 *     Subtask<String> subtask = scope.fork(() -> someOperation());
 *     
 *     scope.join();
 *     
 *     switch (subtask.state()) {
 *         case SUCCESS -> processResult(subtask.get());
 *         case FAILED -> handleException(subtask.exception());
 *         case CANCELLED -> handleCancellation();
 *         case UNAVAILABLE -> throw new IllegalStateException();
 *     }
 * }
 * ```
 * 
 * 학습 목표:
 * 1. Subtask 상태 전환 과정 이해
 * 2. 상태별 적절한 처리 방법
 * 3. 예외 및 취소 상황 처리
 * 4. 상태 기반 결과 수집 패턴
 */
public class SubtaskLifecycleExample {
    
    private static final Logger logger = Logger.getLogger(SubtaskLifecycleExample.class.getName());
    
    /**
     * Subtask 상태 시뮬레이션
     */
    public enum SubtaskState {
        UNAVAILABLE,  // 시작 전 또는 실행 중
        SUCCESS,      // 성공 완료
        FAILED,       // 실패 (예외)
        CANCELLED     // 취소됨
    }
    
    /**
     * Subtask 시뮬레이션 클래스
     */
    public static class SimulatedSubtask<T> {
        private final String taskId;
        private final Future<T> future;
        private volatile SubtaskState state = SubtaskState.UNAVAILABLE;
        private volatile T result;
        private volatile Exception exception;
        
        public SimulatedSubtask(String taskId, Future<T> future) {
            this.taskId = taskId;
            this.future = future;
        }
        
        public SubtaskState state() {
            if (state != SubtaskState.UNAVAILABLE) {
                return state;
            }
            
            if (future.isCancelled()) {
                state = SubtaskState.CANCELLED;
            } else if (future.isDone()) {
                try {
                    result = future.get();
                    state = SubtaskState.SUCCESS;
                } catch (ExecutionException e) {
                    exception = (Exception) e.getCause();
                    state = SubtaskState.FAILED;
                } catch (Exception e) {
                    exception = e;
                    state = SubtaskState.FAILED;
                }
            }
            
            return state;
        }
        
        public T get() throws IllegalStateException {
            if (state() != SubtaskState.SUCCESS) {
                throw new IllegalStateException("Subtask not in SUCCESS state: " + state());
            }
            return result;
        }
        
        public Exception exception() throws IllegalStateException {
            if (state() != SubtaskState.FAILED) {
                throw new IllegalStateException("Subtask not in FAILED state: " + state());
            }
            return exception;
        }
        
        public String taskId() {
            return taskId;
        }
        
        public boolean cancel() {
            return future.cancel(true);
        }
    }
    
    /**
     * 기본 생명주기 데모
     */
    public static class BasicLifecycleDemo {
        
        /**
         * Subtask 상태 전환 과정 시연
         */
        public LifecycleResult demonstrateBasicLifecycle() throws InterruptedException {
            logger.info("기본 생명주기 데모 시작");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<SimulatedSubtask<String>> subtasks = new ArrayList<>();
                
                // 다양한 시나리오의 태스크 생성
                subtasks.add(new SimulatedSubtask<>("성공-태스크", 
                    executor.submit(() -> successfulTask("성공-태스크"))));
                
                subtasks.add(new SimulatedSubtask<>("실패-태스크", 
                    executor.submit(() -> failingTask("실패-태스크"))));
                
                subtasks.add(new SimulatedSubtask<>("느린-태스크", 
                    executor.submit(() -> slowTask("느린-태스크"))));
                
                // 초기 상태 확인
                logger.info("=== 초기 상태 ===");
                subtasks.forEach(subtask -> 
                    logger.info(subtask.taskId() + ": " + subtask.state()));
                
                // 잠시 대기 후 중간 상태 확인
                Thread.sleep(300);
                
                logger.info("=== 중간 상태 (300ms 후) ===");
                subtasks.forEach(subtask -> 
                    logger.info(subtask.taskId() + ": " + subtask.state()));
                
                // 느린 태스크 취소
                subtasks.get(2).cancel();
                
                // 모든 완료 대기
                Thread.sleep(500);
                
                logger.info("=== 최종 상태 ===");
                List<StateTransition> transitions = new ArrayList<>();
                
                for (SimulatedSubtask<String> subtask : subtasks) {
                    SubtaskState finalState = subtask.state();
                    logger.info(subtask.taskId() + ": " + finalState);
                    
                    String result = null;
                    String error = null;
                    
                    switch (finalState) {
                        case SUCCESS -> {
                            result = subtask.get();
                            logger.info("  결과: " + result);
                        }
                        case FAILED -> {
                            error = subtask.exception().getMessage();
                            logger.info("  오류: " + error);
                        }
                        case CANCELLED -> {
                            logger.info("  취소됨");
                        }
                        default -> {
                            logger.info("  진행 중");
                        }
                    }
                    
                    transitions.add(new StateTransition(
                        subtask.taskId(), finalState, result, error));
                }
                
                return new LifecycleResult(transitions);
            }
        }
        
        /**
         * 상태 기반 결과 처리 패턴
         */
        public ProcessingResult processWithStateHandling(List<TaskDefinition> taskDefs) throws InterruptedException {
            logger.info("상태 기반 처리 시작: " + taskDefs.size() + "개 작업");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<SimulatedSubtask<String>> subtasks = new ArrayList<>();
                
                // 모든 태스크 시작
                for (TaskDefinition taskDef : taskDefs) {
                    Future<String> future = executor.submit(() -> executeTask(taskDef));
                    subtasks.add(new SimulatedSubtask<>(taskDef.taskId(), future));
                }
                
                // 완료 대기 (적절한 타임아웃으로)
                Thread.sleep(taskDefs.stream().mapToInt(TaskDefinition::maxDuration).max().orElse(1000) + 100);
                
                // 상태별 결과 수집
                List<String> successResults = new ArrayList<>();
                List<TaskFailure> failures = new ArrayList<>();
                List<String> cancelledTasks = new ArrayList<>();
                List<String> unavailableTasks = new ArrayList<>();
                
                for (SimulatedSubtask<String> subtask : subtasks) {
                    switch (subtask.state()) {
                        case SUCCESS -> {
                            successResults.add(subtask.get());
                            logger.info("성공: " + subtask.taskId());
                        }
                        case FAILED -> {
                            failures.add(new TaskFailure(subtask.taskId(), subtask.exception()));
                            logger.warning("실패: " + subtask.taskId() + " - " + subtask.exception().getMessage());
                        }
                        case CANCELLED -> {
                            cancelledTasks.add(subtask.taskId());
                            logger.info("취소: " + subtask.taskId());
                        }
                        case UNAVAILABLE -> {
                            unavailableTasks.add(subtask.taskId());
                            logger.warning("미완료: " + subtask.taskId());
                        }
                    }
                }
                
                return new ProcessingResult(
                    successResults, failures, cancelledTasks, unavailableTasks);
            }
        }
        
        private String successfulTask(String taskId) throws InterruptedException {
            Thread.sleep(100 + (int)(Math.random() * 200));
            return "성공 결과: " + taskId;
        }
        
        private String failingTask(String taskId) throws Exception {
            Thread.sleep(50 + (int)(Math.random() * 100));
            throw new Exception("의도된 실패: " + taskId);
        }
        
        private String slowTask(String taskId) throws InterruptedException {
            Thread.sleep(1000); // 1초 소요
            return "느린 결과: " + taskId;
        }
        
        private String executeTask(TaskDefinition taskDef) throws Exception {
            Thread.sleep(taskDef.duration());
            
            if (Math.random() < taskDef.failureRate()) {
                throw new Exception("작업 실패: " + taskDef.taskId());
            }
            
            return "완료: " + taskDef.taskId();
        }
    }
    
    /**
     * 고급 생명주기 패턴
     */
    public static class AdvancedLifecyclePatterns {
        
        /**
         * 조건부 취소 패턴
         * 특정 조건에서 관련 태스크들을 선택적으로 취소
         */
        public ConditionalCancellationResult demonstrateConditionalCancellation() throws InterruptedException {
            logger.info("조건부 취소 패턴 데모 시작");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<SimulatedSubtask<AnalysisResult>> subtasks = new ArrayList<>();
                
                // 분석 태스크들 시작
                subtasks.add(new SimulatedSubtask<>("보안-분석", 
                    executor.submit(() -> performSecurityAnalysis())));
                
                subtasks.add(new SimulatedSubtask<>("성능-분석", 
                    executor.submit(() -> performPerformanceAnalysis())));
                
                subtasks.add(new SimulatedSubtask<>("안정성-분석", 
                    executor.submit(() -> performReliabilityAnalysis())));
                
                // 주기적으로 상태 확인하여 조건부 취소 수행
                List<CancellationEvent> cancellationEvents = new ArrayList<>();
                
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(100);
                    
                    for (SimulatedSubtask<AnalysisResult> subtask : subtasks) {
                        SubtaskState currentState = subtask.state();
                        
                        // 보안 분석이 실패하면 나머지 분석도 취소
                        if ("보안-분석".equals(subtask.taskId()) && currentState == SubtaskState.FAILED) {
                            logger.warning("보안 분석 실패, 관련 분석 취소");
                            
                            for (SimulatedSubtask<AnalysisResult> other : subtasks) {
                                if (!other.taskId().equals("보안-분석") && 
                                    other.state() == SubtaskState.UNAVAILABLE) {
                                    
                                    other.cancel();
                                    cancellationEvents.add(new CancellationEvent(
                                        other.taskId(), "보안 분석 실패로 인한 취소", Instant.now()));
                                    
                                    logger.info("취소됨: " + other.taskId());
                                }
                            }
                            break;
                        }
                    }
                    
                    // 모든 작업이 완료/취소되면 종료
                    boolean allDone = subtasks.stream()
                        .allMatch(s -> s.state() != SubtaskState.UNAVAILABLE);
                    if (allDone) break;
                }
                
                // 최종 결과 수집
                List<AnalysisResult> results = new ArrayList<>();
                List<Exception> failures = new ArrayList<>();
                
                for (SimulatedSubtask<AnalysisResult> subtask : subtasks) {
                    switch (subtask.state()) {
                        case SUCCESS -> results.add(subtask.get());
                        case FAILED -> failures.add(subtask.exception());
                        default -> { /* 취소됨 또는 미완료 */ }
                    }
                }
                
                return new ConditionalCancellationResult(
                    results, failures, cancellationEvents);
            }
        }
        
        /**
         * 동적 태스크 관리 패턴
         * 실행 중에 새로운 태스크를 추가하고 관리
         */
        public DynamicTaskResult demonstrateDynamicTaskManagement() throws InterruptedException {
            logger.info("동적 태스크 관리 데모 시작");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<SimulatedSubtask<String>> activeTasks = new ArrayList<>();
                List<TaskCreationEvent> creationEvents = new ArrayList<>();
                AtomicReference<Integer> nextTaskId = new AtomicReference<>(1);
                
                // 초기 태스크들 시작
                for (int i = 0; i < 3; i++) {
                    String taskId = "초기-" + nextTaskId.getAndUpdate(id -> id + 1);
                    Future<String> future = executor.submit(() -> dynamicTask(taskId));
                    activeTasks.add(new SimulatedSubtask<>(taskId, future));
                    
                    creationEvents.add(new TaskCreationEvent(taskId, "초기 생성", Instant.now()));
                    logger.info("초기 태스크 생성: " + taskId);
                }
                
                // 동적 관리 루프
                for (int cycle = 0; cycle < 15; cycle++) {
                    Thread.sleep(100);
                    
                    // 완료된 태스크 확인
                    List<SimulatedSubtask<String>> completedTasks = activeTasks.stream()
                        .filter(task -> task.state() == SubtaskState.SUCCESS)
                        .toList();
                    
                    // 성공한 태스크가 있으면 추가 태스크 생성 (연쇄 작업)
                    for (SimulatedSubtask<String> completed : completedTasks) {
                        if (Math.random() < 0.3) { // 30% 확률로 연쇄 작업 생성
                            String newTaskId = "연쇄-" + nextTaskId.getAndUpdate(id -> id + 1);
                            Future<String> future = executor.submit(() -> 
                                chainedTask(newTaskId, completed.get()));
                            
                            activeTasks.add(new SimulatedSubtask<>(newTaskId, future));
                            creationEvents.add(new TaskCreationEvent(
                                newTaskId, "연쇄 작업: " + completed.taskId(), Instant.now()));
                            
                            logger.info("연쇄 태스크 생성: " + newTaskId + " ← " + completed.taskId());
                        }
                    }
                    
                    // 실패한 태스크 재시도
                    List<SimulatedSubtask<String>> failedTasks = activeTasks.stream()
                        .filter(task -> task.state() == SubtaskState.FAILED)
                        .toList();
                    
                    for (SimulatedSubtask<String> failed : failedTasks) {
                        if (Math.random() < 0.5) { // 50% 확률로 재시도
                            String retryTaskId = "재시도-" + failed.taskId();
                            Future<String> future = executor.submit(() -> retryTask(retryTaskId));
                            
                            activeTasks.add(new SimulatedSubtask<>(retryTaskId, future));
                            creationEvents.add(new TaskCreationEvent(
                                retryTaskId, "재시도: " + failed.taskId(), Instant.now()));
                            
                            logger.info("재시도 태스크 생성: " + retryTaskId);
                        }
                    }
                    
                    // 너무 많은 태스크가 활성화되면 일부 취소
                    long activeCount = activeTasks.stream()
                        .filter(task -> task.state() == SubtaskState.UNAVAILABLE)
                        .count();
                    
                    if (activeCount > 10) {
                        logger.warning("활성 태스크 과다, 일부 취소");
                        activeTasks.stream()
                            .filter(task -> task.state() == SubtaskState.UNAVAILABLE)
                            .limit(3)
                            .forEach(SimulatedSubtask::cancel);
                    }
                }
                
                // 최종 결과 수집
                List<String> allResults = activeTasks.stream()
                    .filter(task -> task.state() == SubtaskState.SUCCESS)
                    .map(SimulatedSubtask::get)
                    .toList();
                
                return new DynamicTaskResult(allResults, creationEvents, activeTasks.size());
            }
        }
        
        private AnalysisResult performSecurityAnalysis() throws Exception {
            Thread.sleep(200 + (int)(Math.random() * 300));
            
            if (Math.random() < 0.2) {
                throw new Exception("보안 분석 실패");
            }
            
            return new AnalysisResult("보안-분석", "보안 취약점 없음", 0.9);
        }
        
        private AnalysisResult performPerformanceAnalysis() throws Exception {
            Thread.sleep(300 + (int)(Math.random() * 200));
            return new AnalysisResult("성능-분석", "성능 양호", 0.8);
        }
        
        private AnalysisResult performReliabilityAnalysis() throws Exception {
            Thread.sleep(250 + (int)(Math.random() * 250));
            return new AnalysisResult("안정성-분석", "안정성 우수", 0.85);
        }
        
        private String dynamicTask(String taskId) throws Exception {
            Thread.sleep(200 + (int)(Math.random() * 300));
            
            if (Math.random() < 0.2) {
                throw new Exception("동적 태스크 실패: " + taskId);
            }
            
            return "동적 결과: " + taskId;
        }
        
        private String chainedTask(String taskId, String previousResult) throws Exception {
            Thread.sleep(150 + (int)(Math.random() * 100));
            return "연쇄 결과: " + taskId + " 기반 " + previousResult;
        }
        
        private String retryTask(String taskId) throws Exception {
            Thread.sleep(100 + (int)(Math.random() * 200));
            return "재시도 결과: " + taskId;
        }
    }
    
    // 데이터 클래스들
    
    public record StateTransition(String taskId, SubtaskState finalState, String result, String error) {}
    
    public record LifecycleResult(List<StateTransition> transitions) {}
    
    public record TaskDefinition(String taskId, int duration, int maxDuration, double failureRate) {}
    
    public record TaskFailure(String taskId, Exception exception) {}
    
    public record ProcessingResult(
        List<String> successResults,
        List<TaskFailure> failures,
        List<String> cancelledTasks,
        List<String> unavailableTasks
    ) {}
    
    public record AnalysisResult(String analysisType, String summary, double confidence) {}
    
    public record CancellationEvent(String taskId, String reason, Instant timestamp) {}
    
    public record ConditionalCancellationResult(
        List<AnalysisResult> results,
        List<Exception> failures,
        List<CancellationEvent> cancellationEvents
    ) {}
    
    public record TaskCreationEvent(String taskId, String reason, Instant timestamp) {}
    
    public record DynamicTaskResult(
        List<String> results,
        List<TaskCreationEvent> creationEvents,
        int totalTasksCreated
    ) {}
    
    // 실행 예제
    public static void main(String[] args) {
        BasicLifecycleDemo basicDemo = new BasicLifecycleDemo();
        AdvancedLifecyclePatterns advancedDemo = new AdvancedLifecyclePatterns();
        
        try {
            // 기본 생명주기 데모
            System.out.println("=== 기본 생명주기 데모 ===");
            LifecycleResult lifecycleResult = basicDemo.demonstrateBasicLifecycle();
            
            for (StateTransition transition : lifecycleResult.transitions()) {
                System.out.println(transition.taskId() + ": " + transition.finalState() +
                    (transition.result() != null ? " → " + transition.result() : "") +
                    (transition.error() != null ? " → " + transition.error() : ""));
            }
            
            // 상태 기반 처리
            System.out.println("\n=== 상태 기반 처리 ===");
            List<TaskDefinition> taskDefs = List.of(
                new TaskDefinition("빠른작업", 100, 200, 0.1),
                new TaskDefinition("보통작업", 300, 400, 0.2),
                new TaskDefinition("불안정작업", 200, 300, 0.4),
                new TaskDefinition("안정작업", 150, 250, 0.05)
            );
            
            ProcessingResult processingResult = basicDemo.processWithStateHandling(taskDefs);
            System.out.println("성공: " + processingResult.successResults().size());
            System.out.println("실패: " + processingResult.failures().size());
            System.out.println("취소: " + processingResult.cancelledTasks().size());
            System.out.println("미완료: " + processingResult.unavailableTasks().size());
            
            // 조건부 취소
            System.out.println("\n=== 조건부 취소 패턴 ===");
            ConditionalCancellationResult cancellationResult = 
                advancedDemo.demonstrateConditionalCancellation();
            System.out.println("분석 결과: " + cancellationResult.results().size());
            System.out.println("취소 이벤트: " + cancellationResult.cancellationEvents().size());
            
            // 동적 태스크 관리
            System.out.println("\n=== 동적 태스크 관리 ===");
            DynamicTaskResult dynamicResult = advancedDemo.demonstrateDynamicTaskManagement();
            System.out.println("최종 결과: " + dynamicResult.results().size());
            System.out.println("총 생성된 태스크: " + dynamicResult.totalTasksCreated());
            System.out.println("태스크 생성 이벤트: " + dynamicResult.creationEvents().size());
            
        } catch (Exception e) {
            System.err.println("Subtask 생명주기 예제 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
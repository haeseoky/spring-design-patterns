package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * StructuredTaskScope 타임아웃 및 취소 처리 예제
 * 
 * JEP 505: Structured Concurrency에서 제공하는
 * 타임아웃 기반 스코프 관리와 태스크 취소 전략을 구현합니다.
 * 
 * 주요 패턴:
 * 1. 글로벌 타임아웃 처리
 * 2. 개별 태스크 타임아웃
 * 3. 조건부 취소 전략
 * 4. 리소스 정리 보장
 * 5. 인터럽트 전파 메커니즘
 * 
 * JDK 25 Preview API:
 * - StructuredTaskScope.withDeadline()
 * - Subtask 취소 및 인터럽트 처리
 * - 자동 리소스 정리 (try-with-resources)
 * 
 * @since JDK 25 (Preview) 시뮬레이션
 * @author Pattern Study Team
 */
public class TimeoutAndCancellation {

    /**
     * StructuredTaskScope 시뮬레이션을 위한 타임아웃 처리 클래스
     */
    public static class SimulatedStructuredTaskScope implements AutoCloseable {
        private final ExecutorService executor;
        private final List<Future<?>> activeTasks = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicBoolean interrupted = new AtomicBoolean(false);
        private final AtomicReference<Instant> deadline = new AtomicReference<>();
        private final String scopeName;
        
        public SimulatedStructuredTaskScope(String scopeName) {
            this.scopeName = scopeName;
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
        }
        
        public SimulatedStructuredTaskScope withDeadline(Instant deadline) {
            this.deadline.set(deadline);
            return this;
        }
        
        public <T> SimulatedSubtask<T> fork(Callable<T> task) {
            if (closed.get()) {
                throw new IllegalStateException("TaskScope is closed");
            }
            
            CompletableFuture<T> future = new CompletableFuture<>();
            Future<?> runningTask = executor.submit(() -> {
                try {
                    if (interrupted.get()) {
                        future.cancel(true);
                        return;
                    }
                    
                    T result = task.call();
                    future.complete(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.cancel(true);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            
            activeTasks.add(runningTask);
            return new SimulatedSubtask<>(future, runningTask);
        }
        
        public SimulatedStructuredTaskScope join() throws InterruptedException {
            checkDeadline();
            
            // 모든 활성 태스크 완료 대기
            for (Future<?> task : activeTasks) {
                if (!task.isDone()) {
                    try {
                        if (deadline.get() != null) {
                            long remainingMs = Duration.between(Instant.now(), deadline.get()).toMillis();
                            if (remainingMs <= 0) {
                                throw new TimeoutException("Deadline exceeded");
                            }
                            task.get(remainingMs, TimeUnit.MILLISECONDS);
                        } else {
                            task.get();
                        }
                    } catch (TimeoutException e) {
                        System.out.println("  Timeout detected, cancelling remaining tasks...");
                        cancelRemainingTasks();
                        throw new InterruptedException("Task scope timed out");
                    } catch (ExecutionException e) {
                        // 개별 태스크 실패는 무시하고 계속 진행
                    }
                }
            }
            
            return this;
        }
        
        public void shutdown() {
            interrupted.set(true);
            cancelRemainingTasks();
        }
        
        private void cancelRemainingTasks() {
            System.out.println("  Cancelling " + activeTasks.size() + " remaining tasks in scope: " + scopeName);
            for (Future<?> task : activeTasks) {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            }
        }
        
        private void checkDeadline() throws InterruptedException {
            if (deadline.get() != null && Instant.now().isAfter(deadline.get())) {
                System.out.println("  Deadline exceeded for scope: " + scopeName);
                shutdown();
                throw new InterruptedException("Deadline exceeded");
            }
        }
        
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                System.out.println("  Closing task scope: " + scopeName);
                cancelRemainingTasks();
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * 시뮬레이션된 Subtask 클래스
     */
    public static class SimulatedSubtask<T> {
        private final CompletableFuture<T> future;
        private final Future<?> runningTask;
        private volatile SubtaskState state = SubtaskState.UNAVAILABLE;
        
        public SimulatedSubtask(CompletableFuture<T> future, Future<?> runningTask) {
            this.future = future;
            this.runningTask = runningTask;
        }
        
        public SubtaskState state() {
            if (future.isCancelled()) {
                state = SubtaskState.CANCELLED;
            } else if (future.isDone()) {
                if (future.isCompletedExceptionally()) {
                    state = SubtaskState.FAILED;
                } else {
                    state = SubtaskState.SUCCESS;
                }
            }
            return state;
        }
        
        public T get() throws ExecutionException, InterruptedException {
            return future.get();
        }
        
        public Exception exception() {
            if (state() == SubtaskState.FAILED) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    return (Exception) e.getCause();
                } catch (InterruptedException e) {
                    return e;
                }
            }
            return null;
        }
        
        public void cancel() {
            future.cancel(true);
            runningTask.cancel(true);
        }
    }
    
    public enum SubtaskState {
        UNAVAILABLE, SUCCESS, FAILED, CANCELLED
    }

    /**
     * 1. 글로벌 타임아웃 처리 예제
     * 전체 스코프에 대한 타임아웃 설정 및 처리
     */
    public CompletableFuture<TimeoutResult> globalTimeoutHandling(Duration timeout) {
        System.out.println("=== 글로벌 타임아웃 처리 패턴 ===");
        System.out.println("타임아웃 설정: " + timeout.toSeconds() + "초");
        
        return CompletableFuture.supplyAsync(() -> {
            Instant deadline = Instant.now().plus(timeout);
            
            try (SimulatedStructuredTaskScope scope = new SimulatedStructuredTaskScope("global-timeout")
                    .withDeadline(deadline)) {
                
                System.out.println("작업 시작 시간: " + LocalDateTime.now());
                
                // 다양한 실행 시간을 가진 태스크들
                var quickTask = scope.fork(() -> performQuickOperation());
                var mediumTask = scope.fork(() -> performMediumOperation());
                var slowTask = scope.fork(() -> performSlowOperation());
                var verySlowTask = scope.fork(() -> performVerySlowOperation());
                
                try {
                    scope.join();
                    
                    List<String> results = new ArrayList<>();
                    
                    // 완료된 태스크들의 결과 수집
                    if (quickTask.state() == SubtaskState.SUCCESS) {
                        results.add("Quick: " + quickTask.get());
                    }
                    if (mediumTask.state() == SubtaskState.SUCCESS) {
                        results.add("Medium: " + mediumTask.get());
                    }
                    if (slowTask.state() == SubtaskState.SUCCESS) {
                        results.add("Slow: " + slowTask.get());
                    }
                    if (verySlowTask.state() == SubtaskState.SUCCESS) {
                        results.add("Very Slow: " + verySlowTask.get());
                    }
                    
                    System.out.println("완료 시간: " + LocalDateTime.now());
                    return new TimeoutResult(results, true, "All tasks completed within timeout");
                    
                } catch (InterruptedException e) {
                    System.out.println("타임아웃 발생! 완료된 작업들만 수집...");
                    
                    List<String> partialResults = new ArrayList<>();
                    
                    // 타임아웃 시점에서 완료된 태스크들만 수집
                    if (quickTask.state() == SubtaskState.SUCCESS) {
                        try {
                            partialResults.add("Quick: " + quickTask.get());
                        } catch (Exception ignored) {}
                    }
                    if (mediumTask.state() == SubtaskState.SUCCESS) {
                        try {
                            partialResults.add("Medium: " + mediumTask.get());
                        } catch (Exception ignored) {}
                    }
                    
                    return new TimeoutResult(partialResults, false, "Timeout occurred, partial results collected");
                }
                
            } catch (Exception e) {
                return new TimeoutResult(List.of(), false, "Error: " + e.getMessage());
            }
        });
    }
    
    private String performQuickOperation() {
        try {
            Thread.sleep(500);  // 0.5초
            return "Quick operation completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Quick operation interrupted", e);
        }
    }
    
    private String performMediumOperation() {
        try {
            Thread.sleep(2000);  // 2초
            return "Medium operation completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Medium operation interrupted", e);
        }
    }
    
    private String performSlowOperation() {
        try {
            Thread.sleep(5000);  // 5초
            return "Slow operation completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Slow operation interrupted", e);
        }
    }
    
    private String performVerySlowOperation() {
        try {
            Thread.sleep(10000);  // 10초
            return "Very slow operation completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Very slow operation interrupted", e);
        }
    }

    /**
     * 2. 개별 태스크 타임아웃 처리 예제
     * 각 태스크별로 다른 타임아웃 설정
     */
    public CompletableFuture<IndividualTimeoutResult> individualTaskTimeouts() {
        System.out.println("\n=== 개별 태스크 타임아웃 처리 패턴 ===");
        
        return CompletableFuture.supplyAsync(() -> {
            try (SimulatedStructuredTaskScope scope = new SimulatedStructuredTaskScope("individual-timeouts")) {
                
                // 각 태스크에 대한 개별 타임아웃 관리
                var criticalTask = scope.fork(() -> performCriticalTask());
                var normalTask = scope.fork(() -> performNormalTask());
                var optionalTask = scope.fork(() -> performOptionalTask());
                
                // 개별 타임아웃 모니터링을 위한 스케줄러
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
                
                // 중요 태스크: 1초 타임아웃
                scheduler.schedule(() -> {
                    if (criticalTask.state() == SubtaskState.UNAVAILABLE) {
                        System.out.println("  Critical task timeout (1s) - 취소");
                        criticalTask.cancel();
                    }
                }, 1, TimeUnit.SECONDS);
                
                // 일반 태스크: 3초 타임아웃
                scheduler.schedule(() -> {
                    if (normalTask.state() == SubtaskState.UNAVAILABLE) {
                        System.out.println("  Normal task timeout (3s) - 취소");
                        normalTask.cancel();
                    }
                }, 3, TimeUnit.SECONDS);
                
                // 선택 태스크: 5초 타임아웃
                scheduler.schedule(() -> {
                    if (optionalTask.state() == SubtaskState.UNAVAILABLE) {
                        System.out.println("  Optional task timeout (5s) - 취소");
                        optionalTask.cancel();
                    }
                }, 5, TimeUnit.SECONDS);
                
                try {
                    scope.join();
                    
                    Map<String, TaskResult> results = new HashMap<>();
                    
                    // 각 태스크 결과 수집
                    results.put("critical", collectTaskResult(criticalTask, "Critical"));
                    results.put("normal", collectTaskResult(normalTask, "Normal"));
                    results.put("optional", collectTaskResult(optionalTask, "Optional"));
                    
                    return new IndividualTimeoutResult(results, true);
                    
                } finally {
                    scheduler.shutdown();
                }
                
            } catch (Exception e) {
                return new IndividualTimeoutResult(Map.of(), false);
            }
        });
    }
    
    private String performCriticalTask() {
        try {
            System.out.println("  Critical task 시작 (예상 실행시간: 2초)");
            Thread.sleep(2000);  // 2초 - 타임아웃될 예정
            return "Critical task completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Critical task interrupted", e);
        }
    }
    
    private String performNormalTask() {
        try {
            System.out.println("  Normal task 시작 (예상 실행시간: 1.5초)");
            Thread.sleep(1500);  // 1.5초 - 완료될 예정
            return "Normal task completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Normal task interrupted", e);
        }
    }
    
    private String performOptionalTask() {
        try {
            System.out.println("  Optional task 시작 (예상 실행시간: 4초)");
            Thread.sleep(4000);  // 4초 - 완료될 예정
            return "Optional task completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Optional task interrupted", e);
        }
    }
    
    private <T> TaskResult collectTaskResult(SimulatedSubtask<T> task, String taskName) {
        switch (task.state()) {
            case SUCCESS:
                try {
                    return new TaskResult(taskName + ": " + task.get(), TaskStatus.COMPLETED);
                } catch (Exception e) {
                    return new TaskResult(taskName + ": Error - " + e.getMessage(), TaskStatus.FAILED);
                }
            case FAILED:
                return new TaskResult(taskName + ": Failed - " + task.exception().getMessage(), TaskStatus.FAILED);
            case CANCELLED:
                return new TaskResult(taskName + ": Cancelled due to timeout", TaskStatus.CANCELLED);
            default:
                return new TaskResult(taskName + ": Still running", TaskStatus.RUNNING);
        }
    }

    /**
     * 3. 조건부 취소 전략 예제
     * 특정 조건에 따른 태스크 취소 전략
     */
    public CompletableFuture<ConditionalCancellationResult> conditionalCancellationStrategy() {
        System.out.println("\n=== 조건부 취소 전략 패턴 ===");
        
        return CompletableFuture.supplyAsync(() -> {
            try (SimulatedStructuredTaskScope scope = new SimulatedStructuredTaskScope("conditional-cancellation")) {
                
                AtomicBoolean earlySuccess = new AtomicBoolean(false);
                AtomicInteger completedTasks = new AtomicInteger(0);
                
                // 빠른 성공을 위한 여러 병렬 태스크
                var primaryTask = scope.fork(() -> performPrimaryDataRetrieval());
                var backupTask1 = scope.fork(() -> performBackupDataRetrieval1());
                var backupTask2 = scope.fork(() -> performBackupDataRetrieval2());
                var fallbackTask = scope.fork(() -> performFallbackDataRetrieval());
                
                // 조건부 취소 모니터링 스레드
                CompletableFuture.runAsync(() -> {
                    try {
                        while (!earlySuccess.get() && completedTasks.get() < 4) {
                            Thread.sleep(100);  // 100ms마다 확인
                            
                            // 첫 번째 성공한 태스크가 있으면 나머지 취소
                            if (primaryTask.state() == SubtaskState.SUCCESS) {
                                System.out.println("  Primary task 성공 - 백업 태스크들 취소");
                                earlySuccess.set(true);
                                backupTask1.cancel();
                                backupTask2.cancel();
                                fallbackTask.cancel();
                                break;
                            }
                            
                            if (backupTask1.state() == SubtaskState.SUCCESS) {
                                System.out.println("  Backup1 task 성공 - 다른 태스크들 취소");
                                earlySuccess.set(true);
                                primaryTask.cancel();
                                backupTask2.cancel();
                                fallbackTask.cancel();
                                break;
                            }
                            
                            if (backupTask2.state() == SubtaskState.SUCCESS) {
                                System.out.println("  Backup2 task 성공 - 다른 태스크들 취소");
                                earlySuccess.set(true);
                                primaryTask.cancel();
                                backupTask1.cancel();
                                fallbackTask.cancel();
                                break;
                            }
                            
                            // 3개 태스크가 실패하면 마지막 fallback만 계속
                            long failedTasks = Arrays.stream(new SimulatedSubtask[]{primaryTask, backupTask1, backupTask2})
                                    .mapToLong(task -> task.state() == SubtaskState.FAILED ? 1 : 0)
                                    .sum();
                            
                            if (failedTasks >= 2) {
                                System.out.println("  주요 태스크들 실패 - Fallback task만 계속");
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                
                scope.join();
                
                // 결과 수집
                List<TaskResult> results = new ArrayList<>();
                results.add(collectTaskResult(primaryTask, "Primary"));
                results.add(collectTaskResult(backupTask1, "Backup1"));
                results.add(collectTaskResult(backupTask2, "Backup2"));
                results.add(collectTaskResult(fallbackTask, "Fallback"));
                
                boolean hasSuccess = results.stream().anyMatch(r -> r.status == TaskStatus.COMPLETED);
                
                return new ConditionalCancellationResult(results, hasSuccess, earlySuccess.get() ? "Early success" : "All completed");
                
            } catch (Exception e) {
                return new ConditionalCancellationResult(List.of(), false, "Error: " + e.getMessage());
            }
        });
    }
    
    private String performPrimaryDataRetrieval() {
        try {
            System.out.println("  Primary data retrieval 시작");
            Thread.sleep(1000);  // 1초
            // 70% 확률로 성공
            if (Math.random() < 0.7) {
                return "Primary data retrieved successfully";
            } else {
                throw new RuntimeException("Primary data source failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Primary retrieval interrupted", e);
        }
    }
    
    private String performBackupDataRetrieval1() {
        try {
            System.out.println("  Backup1 data retrieval 시작");
            Thread.sleep(1500);  // 1.5초
            // 60% 확률로 성공
            if (Math.random() < 0.6) {
                return "Backup1 data retrieved successfully";
            } else {
                throw new RuntimeException("Backup1 data source failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Backup1 retrieval interrupted", e);
        }
    }
    
    private String performBackupDataRetrieval2() {
        try {
            System.out.println("  Backup2 data retrieval 시작");
            Thread.sleep(2000);  // 2초
            // 50% 확률로 성공
            if (Math.random() < 0.5) {
                return "Backup2 data retrieved successfully";
            } else {
                throw new RuntimeException("Backup2 data source failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Backup2 retrieval interrupted", e);
        }
    }
    
    private String performFallbackDataRetrieval() {
        try {
            System.out.println("  Fallback data retrieval 시작");
            Thread.sleep(3000);  // 3초
            // 항상 성공 (캐시된 데이터)
            return "Fallback data retrieved from cache";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Fallback retrieval interrupted", e);
        }
    }

    /**
     * 4. 리소스 정리 보장 예제
     * 취소나 타임아웃 발생 시 리소스 정리 보장
     */
    public CompletableFuture<ResourceCleanupResult> resourceCleanupGuarantee() {
        System.out.println("\n=== 리소스 정리 보장 패턴 ===");
        
        return CompletableFuture.supplyAsync(() -> {
            ResourceTracker tracker = new ResourceTracker();
            
            try (SimulatedStructuredTaskScope scope = new SimulatedStructuredTaskScope("resource-cleanup")
                    .withDeadline(Instant.now().plus(Duration.ofSeconds(2)))) {
                
                // 리소스를 사용하는 태스크들
                var dbTask = scope.fork(() -> performDatabaseOperation(tracker));
                var fileTask = scope.fork(() -> performFileOperation(tracker));
                var networkTask = scope.fork(() -> performNetworkOperation(tracker));
                
                try {
                    scope.join();
                    
                    List<String> results = new ArrayList<>();
                    
                    if (dbTask.state() == SubtaskState.SUCCESS) {
                        results.add(dbTask.get());
                    }
                    if (fileTask.state() == SubtaskState.SUCCESS) {
                        results.add(fileTask.get());
                    }
                    if (networkTask.state() == SubtaskState.SUCCESS) {
                        results.add(networkTask.get());
                    }
                    
                    return new ResourceCleanupResult(results, tracker.getCleanupLog(), true);
                    
                } catch (InterruptedException e) {
                    System.out.println("  타임아웃 또는 취소로 인한 리소스 정리 시작...");
                    return new ResourceCleanupResult(
                        List.of("Tasks cancelled due to timeout"), 
                        tracker.getCleanupLog(), 
                        false
                    );
                }
                
            } catch (Exception e) {
                return new ResourceCleanupResult(
                    List.of("Error: " + e.getMessage()), 
                    tracker.getCleanupLog(), 
                    false
                );
            } finally {
                // 스코프가 닫힐 때 남은 리소스 정리
                tracker.cleanupRemainingResources();
            }
        });
    }
    
    /**
     * 리소스 추적 및 정리 클래스
     */
    public static class ResourceTracker {
        private final AtomicInteger dbConnections = new AtomicInteger(0);
        private final AtomicInteger fileHandles = new AtomicInteger(0);
        private final AtomicInteger networkConnections = new AtomicInteger(0);
        private final List<String> cleanupLog = Collections.synchronizedList(new ArrayList<>());
        
        public int allocateDbConnection() {
            int count = dbConnections.incrementAndGet();
            cleanupLog.add("DB 연결 할당: " + count);
            return count;
        }
        
        public void releaseDbConnection() {
            int count = dbConnections.decrementAndGet();
            cleanupLog.add("DB 연결 해제: " + count + " 남음");
        }
        
        public int allocateFileHandle() {
            int count = fileHandles.incrementAndGet();
            cleanupLog.add("파일 핸들 할당: " + count);
            return count;
        }
        
        public void releaseFileHandle() {
            int count = fileHandles.decrementAndGet();
            cleanupLog.add("파일 핸들 해제: " + count + " 남음");
        }
        
        public int allocateNetworkConnection() {
            int count = networkConnections.incrementAndGet();
            cleanupLog.add("네트워크 연결 할당: " + count);
            return count;
        }
        
        public void releaseNetworkConnection() {
            int count = networkConnections.decrementAndGet();
            cleanupLog.add("네트워크 연결 해제: " + count + " 남음");
        }
        
        public void cleanupRemainingResources() {
            if (dbConnections.get() > 0) {
                cleanupLog.add("남은 DB 연결 " + dbConnections.get() + "개 강제 정리");
                dbConnections.set(0);
            }
            if (fileHandles.get() > 0) {
                cleanupLog.add("남은 파일 핸들 " + fileHandles.get() + "개 강제 정리");
                fileHandles.set(0);
            }
            if (networkConnections.get() > 0) {
                cleanupLog.add("남은 네트워크 연결 " + networkConnections.get() + "개 강제 정리");
                networkConnections.set(0);
            }
        }
        
        public List<String> getCleanupLog() {
            return new ArrayList<>(cleanupLog);
        }
    }
    
    private String performDatabaseOperation(ResourceTracker tracker) {
        try {
            tracker.allocateDbConnection();
            System.out.println("  DB 작업 시작 (예상 시간: 3초)");
            Thread.sleep(3000);  // 3초 - 타임아웃될 예정
            tracker.releaseDbConnection();
            return "Database operation completed";
        } catch (InterruptedException e) {
            tracker.releaseDbConnection();  // 인터럽트 시에도 리소스 정리
            Thread.currentThread().interrupt();
            throw new RuntimeException("Database operation interrupted", e);
        }
    }
    
    private String performFileOperation(ResourceTracker tracker) {
        try {
            tracker.allocateFileHandle();
            System.out.println("  파일 작업 시작 (예상 시간: 1초)");
            Thread.sleep(1000);  // 1초 - 완료될 예정
            tracker.releaseFileHandle();
            return "File operation completed";
        } catch (InterruptedException e) {
            tracker.releaseFileHandle();  // 인터럽트 시에도 리소스 정리
            Thread.currentThread().interrupt();
            throw new RuntimeException("File operation interrupted", e);
        }
    }
    
    private String performNetworkOperation(ResourceTracker tracker) {
        try {
            tracker.allocateNetworkConnection();
            System.out.println("  네트워크 작업 시작 (예상 시간: 4초)");
            Thread.sleep(4000);  // 4초 - 타임아웃될 예정
            tracker.releaseNetworkConnection();
            return "Network operation completed";
        } catch (InterruptedException e) {
            tracker.releaseNetworkConnection();  // 인터럽트 시에도 리소스 정리
            Thread.currentThread().interrupt();
            throw new RuntimeException("Network operation interrupted", e);
        }
    }

    // 결과 클래스들
    public static class TimeoutResult {
        private final List<String> completedTasks;
        private final boolean allCompleted;
        private final String message;
        
        public TimeoutResult(List<String> completedTasks, boolean allCompleted, String message) {
            this.completedTasks = new ArrayList<>(completedTasks);
            this.allCompleted = allCompleted;
            this.message = message;
        }
        
        public List<String> getCompletedTasks() { return new ArrayList<>(completedTasks); }
        public boolean isAllCompleted() { return allCompleted; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("TimeoutResult{completed=%d, allCompleted=%s, message='%s'}", 
                completedTasks.size(), allCompleted, message);
        }
    }
    
    public static class IndividualTimeoutResult {
        private final Map<String, TaskResult> taskResults;
        private final boolean success;
        
        public IndividualTimeoutResult(Map<String, TaskResult> taskResults, boolean success) {
            this.taskResults = new HashMap<>(taskResults);
            this.success = success;
        }
        
        public Map<String, TaskResult> getTaskResults() { return new HashMap<>(taskResults); }
        public boolean isSuccess() { return success; }
        
        @Override
        public String toString() {
            return String.format("IndividualTimeoutResult{results=%s, success=%s}", taskResults, success);
        }
    }
    
    public static class TaskResult {
        private final String result;
        private final TaskStatus status;
        
        public TaskResult(String result, TaskStatus status) {
            this.result = result;
            this.status = status;
        }
        
        public String getResult() { return result; }
        public TaskStatus getStatus() { return status; }
        
        @Override
        public String toString() {
            return String.format("TaskResult{result='%s', status=%s}", result, status);
        }
    }
    
    public enum TaskStatus {
        RUNNING, COMPLETED, FAILED, CANCELLED
    }
    
    public static class ConditionalCancellationResult {
        private final List<TaskResult> taskResults;
        private final boolean hasSuccess;
        private final String strategy;
        
        public ConditionalCancellationResult(List<TaskResult> taskResults, boolean hasSuccess, String strategy) {
            this.taskResults = new ArrayList<>(taskResults);
            this.hasSuccess = hasSuccess;
            this.strategy = strategy;
        }
        
        public List<TaskResult> getTaskResults() { return new ArrayList<>(taskResults); }
        public boolean hasSuccess() { return hasSuccess; }
        public String getStrategy() { return strategy; }
        
        @Override
        public String toString() {
            return String.format("ConditionalCancellationResult{results=%s, hasSuccess=%s, strategy='%s'}", 
                taskResults, hasSuccess, strategy);
        }
    }
    
    public static class ResourceCleanupResult {
        private final List<String> operationResults;
        private final List<String> cleanupLog;
        private final boolean success;
        
        public ResourceCleanupResult(List<String> operationResults, List<String> cleanupLog, boolean success) {
            this.operationResults = new ArrayList<>(operationResults);
            this.cleanupLog = new ArrayList<>(cleanupLog);
            this.success = success;
        }
        
        public List<String> getOperationResults() { return new ArrayList<>(operationResults); }
        public List<String> getCleanupLog() { return new ArrayList<>(cleanupLog); }
        public boolean isSuccess() { return success; }
        
        @Override
        public String toString() {
            return String.format("ResourceCleanupResult{operations=%d, cleanupEntries=%d, success=%s}", 
                operationResults.size(), cleanupLog.size(), success);
        }
    }

    /**
     * 데모 실행 메서드
     */
    public static void main(String[] args) throws Exception {
        TimeoutAndCancellation demo = new TimeoutAndCancellation();
        
        // 1. 글로벌 타임아웃 처리 (3초 타임아웃)
        System.out.println("1. 글로벌 타임아웃 처리 (3초) 실행...");
        TimeoutResult result1 = demo.globalTimeoutHandling(Duration.ofSeconds(3)).get(15, TimeUnit.SECONDS);
        System.out.println("결과1: " + result1);
        System.out.println("완료된 작업들: " + result1.getCompletedTasks());
        
        Thread.sleep(2000);
        
        // 2. 개별 태스크 타임아웃
        System.out.println("\n2. 개별 태스크 타임아웃 처리 실행...");
        IndividualTimeoutResult result2 = demo.individualTaskTimeouts().get(10, TimeUnit.SECONDS);
        System.out.println("결과2: " + result2);
        result2.getTaskResults().forEach((name, result) -> 
            System.out.println("  " + name + ": " + result));
        
        Thread.sleep(2000);
        
        // 3. 조건부 취소 전략
        System.out.println("\n3. 조건부 취소 전략 실행...");
        ConditionalCancellationResult result3 = demo.conditionalCancellationStrategy().get(8, TimeUnit.SECONDS);
        System.out.println("결과3: " + result3);
        System.out.println("전략: " + result3.getStrategy());
        result3.getTaskResults().forEach(result -> System.out.println("  " + result));
        
        Thread.sleep(2000);
        
        // 4. 리소스 정리 보장
        System.out.println("\n4. 리소스 정리 보장 실행...");
        ResourceCleanupResult result4 = demo.resourceCleanupGuarantee().get(5, TimeUnit.SECONDS);
        System.out.println("결과4: " + result4);
        System.out.println("작업 결과:");
        result4.getOperationResults().forEach(result -> System.out.println("  " + result));
        System.out.println("리소스 정리 로그:");
        result4.getCleanupLog().forEach(log -> System.out.println("  " + log));
        
        System.out.println("\n=== 타임아웃 및 취소 처리 패턴 데모 완료 ===");
    }
}
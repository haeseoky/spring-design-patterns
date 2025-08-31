package com.ocean.pattern.structured;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;

/**
 * Structured Concurrency를 활용한 병렬 데이터 처리
 * 
 * 이 클래스는 대용량 데이터를 효율적으로 병렬 처리하는 방법을 보여줍니다.
 * Structured Concurrency의 구조적 접근법을 통해 성능과 안전성을 동시에 확보합니다.
 * 
 * 주요 학습 목표:
 * 1. 데이터 분할 전략 (Data Partitioning)
 * 2. 작업 단위 최적화 (Work Unit Optimization)
 * 3. 메모리 효율적인 스트림 처리
 * 4. 동적 부하 분산 (Dynamic Load Balancing)
 */
public class ParallelDataProcessing {
    
    private static final Logger logger = Logger.getLogger(ParallelDataProcessing.class.getName());
    
    /**
     * JDK 25 Structured Concurrency를 활용한 데이터 처리 (이론적 구현)
     * 
     * try (var scope = StructuredTaskScope.open()) {
     *     List<Subtask<List<ProcessedData>>> tasks = new ArrayList<>();
     *     
     *     for (DataPartition partition : partitions) {
     *         tasks.add(scope.fork(() -> processPartition(partition)));
     *     }
     *     
     *     scope.join();
     *     
     *     return tasks.stream()
     *                 .map(Subtask::get)
     *                 .flatMap(List::stream)
     *                 .collect(Collectors.toList());
     * }
     */
    
    /**
     * 대용량 리스트를 병렬로 처리
     * 데이터를 적절한 크기로 분할하여 병렬 처리
     */
    public <T, R> List<R> processLargeDataset(List<T> dataset, 
                                            DataProcessor<T, R> processor,
                                            int optimalChunkSize) throws InterruptedException, ExecutionException {
        
        logger.info("대용량 데이터셋 처리 시작: " + dataset.size() + "개 항목, 청크 크기: " + optimalChunkSize);
        Instant startTime = Instant.now();
        
        if (dataset.isEmpty()) {
            return new ArrayList<>();
        }
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 데이터를 청크로 분할
            List<List<T>> chunks = partitionData(dataset, optimalChunkSize);
            logger.info("데이터를 " + chunks.size() + "개 청크로 분할");
            
            List<Future<List<R>>> futures = new ArrayList<>();
            
            // 각 청크를 병렬로 처리
            for (int i = 0; i < chunks.size(); i++) {
                final int chunkIndex = i;
                final List<T> chunk = chunks.get(i);
                
                Future<List<R>> future = executor.submit(() -> {
                    logger.info("청크 " + chunkIndex + " 처리 시작 (" + chunk.size() + "개 항목)");
                    Instant chunkStart = Instant.now();
                    
                    List<R> chunkResults = new ArrayList<>();
                    for (T item : chunk) {
                        try {
                            R result = processor.process(item);
                            chunkResults.add(result);
                        } catch (Exception e) {
                            logger.severe("청크 " + chunkIndex + " 처리 중 오류: " + e.getMessage());
                            throw new RuntimeException("청크 " + chunkIndex + " 처리 실패", e);
                        }
                    }
                    
                    Duration chunkDuration = Duration.between(chunkStart, Instant.now());
                    logger.info("청크 " + chunkIndex + " 처리 완료 (" + chunkDuration.toMillis() + "ms)");
                    
                    return chunkResults;
                });
                
                futures.add(future);
            }
            
            // 모든 결과 수집
            List<R> allResults = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                List<R> chunkResults = futures.get(i).get();
                allResults.addAll(chunkResults);
            }
            
            Duration totalDuration = Duration.between(startTime, Instant.now());
            logger.info("대용량 데이터셋 처리 완료: " + allResults.size() + "개 결과, 소요시간: " + totalDuration.toMillis() + "ms");
            
            return allResults;
        }
    }
    
    /**
     * Map-Reduce 스타일의 병렬 처리
     * 대용량 데이터를 맵핑하고 결과를 리듀싱
     */
    public <T, R> R mapReduceProcess(List<T> dataset,
                                    DataProcessor<T, R> mapper,
                                    ResultCombiner<R> reducer,
                                    R initialValue,
                                    int parallelism) throws InterruptedException, ExecutionException {
        
        logger.info("Map-Reduce 처리 시작: " + dataset.size() + "개 항목, 병렬도: " + parallelism);
        
        if (dataset.isEmpty()) {
            return initialValue;
        }
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 병렬도에 따라 데이터 분할
            int chunkSize = Math.max(1, dataset.size() / parallelism);
            List<List<T>> chunks = partitionData(dataset, chunkSize);
            
            List<Future<R>> mapFutures = new ArrayList<>();
            
            // Map 단계: 각 청크를 병렬로 처리
            for (int i = 0; i < chunks.size(); i++) {
                final int chunkIndex = i;
                final List<T> chunk = chunks.get(i);
                
                Future<R> mapFuture = executor.submit(() -> {
                    logger.info("Map 단계 - 청크 " + chunkIndex + " 처리 중");
                    
                    R chunkResult = initialValue;
                    for (T item : chunk) {
                        R mappedItem = mapper.process(item);
                        chunkResult = reducer.combine(chunkResult, mappedItem);
                    }
                    
                    logger.info("Map 단계 - 청크 " + chunkIndex + " 완료");
                    return chunkResult;
                });
                
                mapFutures.add(mapFuture);
            }
            
            // Reduce 단계: 모든 청크 결과를 결합
            logger.info("Reduce 단계 시작");
            R finalResult = initialValue;
            for (Future<R> future : mapFutures) {
                R chunkResult = future.get();
                finalResult = reducer.combine(finalResult, chunkResult);
            }
            
            logger.info("Map-Reduce 처리 완료");
            return finalResult;
        }
    }
    
    /**
     * 스트림 처리 패턴
     * 실시간으로 들어오는 데이터를 버퍼링하여 병렬 처리
     */
    public <T, R> void processDataStream(BlockingQueue<T> inputStream,
                                       BlockingQueue<R> outputStream,
                                       DataProcessor<T, R> processor,
                                       int batchSize,
                                       Duration batchTimeout) throws InterruptedException {
        
        logger.info("스트림 처리 시작 - 배치 크기: " + batchSize + ", 타임아웃: " + batchTimeout);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<T> currentBatch = new ArrayList<>();
            long lastBatchTime = System.currentTimeMillis();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 배치 타임아웃 내에서 데이터 수집
                    T item = inputStream.poll(batchTimeout.toMillis(), TimeUnit.MILLISECONDS);
                    
                    if (item != null) {
                        currentBatch.add(item);
                    }
                    
                    boolean shouldProcessBatch = currentBatch.size() >= batchSize ||
                                               (item == null && !currentBatch.isEmpty()) ||
                                               (System.currentTimeMillis() - lastBatchTime > batchTimeout.toMillis());
                    
                    if (shouldProcessBatch && !currentBatch.isEmpty()) {
                        // 현재 배치를 복사하여 병렬 처리
                        List<T> batchToProcess = new ArrayList<>(currentBatch);
                        currentBatch.clear();
                        lastBatchTime = System.currentTimeMillis();
                        
                        // 배치를 비동기로 처리
                        executor.submit(() -> {
                            logger.info("배치 처리 시작: " + batchToProcess.size() + "개 항목");
                            
                            for (T batchItem : batchToProcess) {
                                try {
                                    R result = processor.process(batchItem);
                                    outputStream.offer(result);
                                } catch (Exception e) {
                                    logger.severe("스트림 처리 중 오류: " + e.getMessage());
                                }
                            }
                            
                            logger.info("배치 처리 완료");
                        });
                    }
                    
                } catch (InterruptedException e) {
                    logger.info("스트림 처리 중단됨");
                    break;
                }
            }
        }
        
        logger.info("스트림 처리 종료");
    }
    
    /**
     * 동적 부하 분산 처리
     * 작업자들의 처리 속도에 따라 동적으로 작업 분배
     */
    public <T, R> List<R> processWithDynamicLoadBalancing(List<T> dataset,
                                                         DataProcessor<T, R> processor,
                                                         int numberOfWorkers) throws InterruptedException, ExecutionException {
        
        logger.info("동적 부하 분산 처리 시작: " + dataset.size() + "개 항목, 작업자: " + numberOfWorkers);
        
        BlockingQueue<T> workQueue = new LinkedBlockingQueue<>(dataset);
        BlockingQueue<R> resultQueue = new LinkedBlockingQueue<>();
        
        try (ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers)) {
            
            List<Future<Void>> workers = new ArrayList<>();
            
            // 작업자 생성
            for (int i = 0; i < numberOfWorkers; i++) {
                final int workerId = i;
                
                Future<Void> worker = executor.submit(() -> {
                    logger.info("작업자 " + workerId + " 시작");
                    int processedCount = 0;
                    
                    try {
                        while (true) {
                            T item = workQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (item == null) {
                                break; // 더 이상 작업 없음
                            }
                            
                            try {
                                R result = processor.process(item);
                                resultQueue.offer(result);
                                processedCount++;
                                
                                if (processedCount % 100 == 0) {
                                    logger.info("작업자 " + workerId + ": " + processedCount + "개 처리 완료");
                                }
                            } catch (Exception e) {
                                logger.severe("작업자 " + workerId + " 처리 오류: " + e.getMessage());
                            }
                        }
                    } catch (InterruptedException e) {
                        logger.info("작업자 " + workerId + " 중단됨");
                    }
                    
                    logger.info("작업자 " + workerId + " 완료: 총 " + processedCount + "개 처리");
                    return null;
                });
                
                workers.add(worker);
            }
            
            // 모든 작업자 완료 대기
            for (Future<Void> worker : workers) {
                worker.get();
            }
            
            // 결과 수집
            List<R> results = new ArrayList<>();
            R result;
            while ((result = resultQueue.poll()) != null) {
                results.add(result);
            }
            
            logger.info("동적 부하 분산 처리 완료: " + results.size() + "개 결과");
            return results;
        }
    }
    
    /**
     * 메모리 효율적인 대용량 파일 처리
     */
    public ProcessingStatistics processLargeDatasetWithStats(List<Integer> numbers) throws InterruptedException, ExecutionException {
        logger.info("통계 계산을 포함한 대용량 데이터 처리 시작: " + numbers.size() + "개 숫자");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 병렬로 다양한 통계 계산
            Future<Long> sumTask = executor.submit(() -> 
                numbers.parallelStream().mapToLong(Integer::longValue).sum());
            
            Future<Double> avgTask = executor.submit(() -> 
                numbers.parallelStream().mapToInt(Integer::intValue).average().orElse(0.0));
            
            Future<Integer> minTask = executor.submit(() -> 
                numbers.parallelStream().min(Integer::compareTo).orElse(0));
            
            Future<Integer> maxTask = executor.submit(() -> 
                numbers.parallelStream().max(Integer::compareTo).orElse(0));
            
            Future<Long> evenCountTask = executor.submit(() -> 
                numbers.parallelStream().filter(n -> n % 2 == 0).count());
            
            Future<Long> oddCountTask = executor.submit(() -> 
                numbers.parallelStream().filter(n -> n % 2 == 1).count());
            
            // 모든 통계 결과 수집
            ProcessingStatistics stats = new ProcessingStatistics(
                numbers.size(),
                sumTask.get(),
                avgTask.get(),
                minTask.get(),
                maxTask.get(),
                evenCountTask.get(),
                oddCountTask.get()
            );
            
            logger.info("통계 계산 완료");
            return stats;
        }
    }
    
    // 유틸리티 메서드들
    
    private <T> List<List<T>> partitionData(List<T> dataset, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        
        for (int i = 0; i < dataset.size(); i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, dataset.size());
            chunks.add(new ArrayList<>(dataset.subList(i, endIndex)));
        }
        
        return chunks;
    }
    
    // 함수형 인터페이스들
    
    @FunctionalInterface
    public interface DataProcessor<T, R> {
        R process(T item) throws Exception;
    }
    
    @FunctionalInterface
    public interface ResultCombiner<R> {
        R combine(R a, R b);
    }
    
    // 결과 클래스들
    
    public record ProcessingStatistics(
        int totalCount,
        long sum,
        double average,
        int minimum,
        int maximum,
        long evenCount,
        long oddCount
    ) {
        public void printReport() {
            System.out.println("=== 처리 통계 ===");
            System.out.println("총 개수: " + totalCount);
            System.out.println("합계: " + sum);
            System.out.println("평균: " + String.format("%.2f", average));
            System.out.println("최소값: " + minimum);
            System.out.println("최대값: " + maximum);
            System.out.println("짝수 개수: " + evenCount);
            System.out.println("홀수 개수: " + oddCount);
        }
    }
    
    // 실행 예제
    public static void main(String[] args) {
        ParallelDataProcessing processor = new ParallelDataProcessing();
        
        try {
            // 대용량 데이터셋 생성
            List<Integer> largeDataset = IntStream.rangeClosed(1, 10000)
                                                 .boxed()
                                                 .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            System.out.println("=== 대용량 데이터셋 처리 테스트 ===");
            
            // 단순 변환 처리
            List<String> transformed = processor.processLargeDataset(
                largeDataset,
                number -> "변환된-" + number,
                1000
            );
            
            System.out.println("변환된 첫 5개 항목: " + transformed.subList(0, 5));
            
            // Map-Reduce 처리
            System.out.println("\n=== Map-Reduce 테스트 ===");
            Integer sum = processor.mapReduceProcess(
                largeDataset,
                number -> number * number, // 제곱 계산
                Integer::sum,              // 합계
                0,
                Runtime.getRuntime().availableProcessors()
            );
            
            System.out.println("제곱의 합: " + sum);
            
            // 통계 계산
            System.out.println("\n=== 통계 계산 테스트 ===");
            ProcessingStatistics stats = processor.processLargeDatasetWithStats(largeDataset);
            stats.printReport();
            
            // 동적 부하 분산 테스트
            System.out.println("\n=== 동적 부하 분산 테스트 ===");
            List<Integer> balancedResults = processor.processWithDynamicLoadBalancing(
                largeDataset.subList(0, 1000),
                number -> number * 2,
                4
            );
            
            System.out.println("동적 부하 분산 처리 결과 개수: " + balancedResults.size());
            System.out.println("첫 5개 결과: " + balancedResults.subList(0, 5));
            
        } catch (Exception e) {
            System.err.println("처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
/**
 * StructuredTaskScope 심층 분석 및 실습 패키지
 * 
 * 이 패키지는 JDK 25의 StructuredTaskScope API를 상세히 분석하고
 * 다양한 사용 패턴을 실습할 수 있는 예제들을 제공합니다.
 * 
 * JEP 505: Structured Concurrency (Fifth Preview)
 * 
 * 패키지 구성:
 * 
 * 1. {@link BasicScopeOperations}
 *    - StructuredTaskScope의 기본 생명주기 관리
 *    - open(), fork(), join(), close() 패턴
 *    - try-with-resources 활용법
 *    - 가상 스레드와의 통합
 * 
 * 2. {@link BuiltInJoinersExample}
 *    - 내장 조이너들의 특성과 활용법
 *    - allSuccessfulOrThrow(): 모든 태스크 성공 필요
 *    - anySuccessfulResultOrThrow(): 첫 번째 성공 결과 반환
 *    - awaitAll(): 모든 완료 대기 (부분 실패 허용)
 *    - allUntil(Predicate): 조건부 완료
 * 
 * 3. {@link CustomJoinerImplementation}
 *    - Joiner 인터페이스 커스텀 구현
 *    - onFork() 및 onComplete() 메서드 활용
 *    - 비즈니스 로직 기반 완료 조건
 *    - 스레드 안전성 보장 기법
 * 
 * 4. {@link SubtaskLifecycleExample}
 *    - Subtask의 상태 관리와 생명주기
 *    - State enum (UNAVAILABLE, SUCCESS, FAILED, CANCELLED)
 *    - 태스크 결과 접근 패턴
 *    - 예외 처리 전략
 * 
 * 5. {@link ScopedValueInheritance}
 *    - ScopedValue의 상속 메커니즘
 *    - 스레드 간 컨텍스트 전파
 *    - 보안 컨텍스트 관리
 *    - 트랜잭션 경계 전파
 * 
 * 6. {@link TimeoutAndCancellation}
 *    - 타임아웃 기반 스코프 관리
 *    - 태스크 취소 전략
 *    - 인터럽트 처리
 *    - 리소스 정리 보장
 * 
 * 7. {@link AdvancedPatterns}
 *    - 고급 동시성 패턴
 *    - 성능 최적화 기법
 *    - 메모리 효율성
 *    - 실제 프로덕션 적용 가이드
 * 
 * API 호환성:
 * - JDK 25 Preview API 구조 시뮬레이션
 * - 실제 API와 최대한 일치하는 인터페이스
 * - 현재 JDK 버전에서 실행 가능한 구현
 * 
 * 실행 요구사항:
 * - JDK 21+ (가상 스레드 지원)
 * - JDK 25에서는 --enable-preview 플래그 필요
 * - 실제 구현: javac --enable-preview --release 25
 * 
 * 마이그레이션 가이드:
 * - ExecutorService → StructuredTaskScope 전환
 * - CompletableFuture → Subtask 패턴
 * - 기존 병렬 처리 코드 리팩토링 전략
 * 
 * 성능 고려사항:
 * - 가상 스레드의 경량성 활용
 * - 스코프 기반 리소스 관리
 * - 메모리 사용량 최적화
 * - CPU 바운드 vs I/O 바운드 작업 특성
 * 
 * @since JDK 25 (Preview)
 * @author Pattern Study Team
 * @version 1.0
 */
package com.ocean.pattern.structured.task;
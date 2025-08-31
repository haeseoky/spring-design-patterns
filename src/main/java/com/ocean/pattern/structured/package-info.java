/**
 * Structured Concurrency 패턴 구현 패키지
 * 
 * JEP 505: Structured Concurrency (Preview Feature in JDK 25)
 * 
 * 이 패키지는 Java의 Structured Concurrency 개념을 학습하고 실습하기 위한
 * 샘플 코드들을 포함합니다.
 * 
 * 주요 특징:
 * - 계층적 태스크 관리: 부모-자식 관계의 태스크 구조
 * - 자동 생명주기 관리: 스코프 종료 시 모든 하위 태스크 자동 정리
 * - 구조적 에러 처리: 예외 발생 시 관련 태스크들 자동 취소
 * - 가상 스레드 활용: 경량 스레드로 높은 동시성 지원
 * 
 * 실행 요구사항:
 * - JDK 25 이상 (Preview API)
 * - 컴파일 시: javac --enable-preview --release 25
 * - 실행 시: java --enable-preview
 * 
 * @since JDK 25
 * @author Pattern Study Team
 */
package com.ocean.pattern.structured;
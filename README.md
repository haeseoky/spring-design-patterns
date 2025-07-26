# 🎯 Spring Boot 디자인 패턴 학습 프로젝트

Spring Boot를 활용한 디자인 패턴 구현 및 학습을 위한 프로젝트입니다.

## 📋 프로젝트 개요

- **프레임워크**: Spring Boot 3.5.4
- **Java 버전**: 21
- **빌드 도구**: Gradle
- **주요 의존성**: WebFlux, Thymeleaf, Lombok, JUnit 5

## 🎨 구현된 디자인 패턴

### 1. 옵저버 패턴 (Observer Pattern)

실시간 뉴스 알림 시스템을 통해 옵저버 패턴을 구현했습니다.

#### 📁 구조
```
src/main/java/com/ocean/pattern/observer/
├── Observer.java              # 옵저버 인터페이스
├── Subject.java              # 서브젝트 인터페이스
├── NewsAgency.java           # 뉴스 발행자 (Subject 구현)
├── NewsChannel.java          # TV 뉴스 채널 (Observer)
├── EmailSubscriber.java      # 이메일 구독자 (Observer)
└── MobileApp.java           # 모바일 앱 (Observer)
```

#### 🔧 주요 특징

- **느슨한 결합**: Subject와 Observer 간 독립적인 관계
- **동적 구독관리**: 런타임에 구독자 추가/제거 가능
- **다양한 알림 방식**: TV 방송, 이메일, 모바일 푸시 알림
- **Thread-Safe**: CopyOnWriteArrayList를 활용한 안전한 동시성 처리

#### 🚀 실행 방법

1. **애플리케이션 시작**
```bash
./gradlew bootRun
```

2. **데모 실행** (기본 구독자들 자동 등록)
```bash
curl -X POST http://localhost:8080/api/observer/demo
```

3. **뉴스 발행**
```bash
curl -X POST http://localhost:8080/api/observer/news \
  -H "Content-Type: application/json" \
  -d '{"news":"긴급 속보: 새로운 기술이 발표되었습니다!"}'
```

4. **구독자 목록 조회**
```bash
curl http://localhost:8080/api/observer/subscribers
```

## 🌐 API 엔드포인트

### 뉴스 관리
- `POST /api/observer/news` - 뉴스 발행
- `GET /api/observer/subscribers` - 구독자 목록 조회

### 구독자 관리
- `POST /api/observer/subscribe/channel` - 뉴스 채널 구독
- `POST /api/observer/subscribe/email` - 이메일 구독
- `POST /api/observer/subscribe/mobile` - 모바일 앱 구독
- `DELETE /api/observer/unsubscribe/{observerId}` - 구독 해지

### 유틸리티
- `POST /api/observer/demo` - 데모 실행
- `DELETE /api/observer/reset` - 모든 구독자 제거

## 📝 사용 예시

### 뉴스 채널 구독
```bash
curl -X POST http://localhost:8080/api/observer/subscribe/channel \
  -H "Content-Type: application/json" \
  -d '{"channelName":"MBC 뉴스"}'
```

### 이메일 구독자 등록
```bash
curl -X POST http://localhost:8080/api/observer/subscribe/email \
  -H "Content-Type: application/json" \
  -d '{"name":"홍길동", "email":"hong@example.com"}'
```

### 모바일 앱 등록
```bash
curl -X POST http://localhost:8080/api/observer/subscribe/mobile \
  -H "Content-Type: application/json" \
  -d '{"appName":"뉴스알리미", "deviceId":"android123"}'
```

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 옵저버 패턴 테스트만 실행
./gradlew test --tests "*ObserverPattern*"
```

### 테스트 커버리지
- ✅ 옵저버 등록/제거 테스트
- ✅ 뉴스 발행 및 알림 전송 테스트
- ✅ 각 구독자 타입별 기능 테스트
- ✅ 예외 상황 처리 테스트
- ✅ REST API 컨트롤러 테스트

## 🎯 학습 포인트

### 옵저버 패턴의 장점
1. **확장성**: 새로운 Observer 타입을 쉽게 추가 가능
2. **재사용성**: Subject 하나를 여러 Observer가 공유
3. **유지보수성**: Subject와 Observer가 독립적으로 변경 가능
4. **실시간 반응**: 상태 변화에 즉시 반응하는 시스템 구축

### 실제 활용 사례
- 이벤트 기반 시스템 (Event-Driven Architecture)
- 모델-뷰 아키텍처 (MVC Pattern)
- 실시간 알림 시스템
- UI 상태 관리 (React, Vue 등)

## 🔧 기술 스택

- **Backend**: Spring Boot 3.5.4, Spring WebFlux
- **Testing**: JUnit 5, Mockito
- **Build**: Gradle 8.x
- **Java**: 21
- **Logging**: SLF4J + Logback

## 📚 추가 예정 패턴

- [ ] 팩토리 패턴 (Factory Pattern)
- [ ] 싱글톤 패턴 (Singleton Pattern)
- [ ] 전략 패턴 (Strategy Pattern)
- [ ] 데코레이터 패턴 (Decorator Pattern)
- [ ] 어댑터 패턴 (Adapter Pattern)

## 🤝 기여하기

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이센스

이 프로젝트는 학습 목적으로 작성되었습니다.

---

**Happy Learning! 🚀**
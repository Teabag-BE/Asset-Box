# syntax=docker/dockerfile:1

# ============================================================
# 1) 빌드 스테이지 — JDK 25로 소스를 받아 부트 jar 생성
# ============================================================
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# 1-1. 래퍼 + 빌드 스크립트 먼저 복사 → 의존성 다운로드 레이어 캐시 (소스만 바뀌면 재다운 X)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 1-2. 소스 복사 후 부트 jar 빌드
#      (테스트는 현재 dev에서 컴파일이 깨져 있어 -x test 로 제외; 테스트는 CI에서 별도 수행)
COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# ============================================================
# 2) 런타임 스테이지 — 가벼운 JRE 25 이미지에 jar만 올려 실행
# ============================================================
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# 비루트 유저로 실행 (보안)
RUN useradd -r -u 1001 appuser

# 빌드 스테이지에서 만든 부트 jar만 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

USER appuser
EXPOSE 8080

# ⚠️ .env.production(비밀값)은 이미지에 넣지 않는다.
#    실행 시 /app/.env.production 로 마운트하거나 env로 주입한다.
#    (application.yml: config.import: optional:file:.env.production)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

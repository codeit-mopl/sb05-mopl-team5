# 1. Build stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# Gradle 래퍼 및 의존성 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐싱 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 빌드 (테스트 스킵)
RUN ./gradlew bootJar -x test --no-daemon

# 2. Runtime stage
FROM amazoncorretto:17
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 포트
EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

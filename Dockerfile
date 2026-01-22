# ===============================
# 1) Build stage
# ===============================
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 1. Gradle 래퍼와 설정 파일만 먼저 복사 (캐시 효율 극대화)
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# 2. 윈도우 CRLF 제거 + 실행권한 부여 (매우 중요)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 3. 의존성 다운로드 (소스코드 복사 전에 실행해야 캐시가 깨지지 않음)
RUN ./gradlew dependencies --no-daemon || true

# 4. 소스 코드 복사 후 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ===============================
# 2) Runtime stage
# ===============================
FROM amazoncorretto:17-alpine
WORKDIR /app

# (선택 사항) 한국 시간대 설정 (로그 시간 확인용)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 보안을 위해 비트권한 사용자 생성 및 사용
RUN addgroup -S app && adduser -S app -G app
USER app

# 빌드 결과물 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 🚀 ENTRYPOINT 수정됨
# -XX:MaxRAMPercentage=75.0 : 컨테이너 메모리 제한의 75%를 힙 메모리로 사용 (AWS Fargate 필수 설정)
# sh -c 를 사용하여 환경변수($JVM_OPTS)가 제대로 동작하도록 변경
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 $JVM_OPTS -jar app.jar"]
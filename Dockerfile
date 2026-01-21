# ===============================
# 1) Build stage
# ===============================
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 캐시 효율
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle* build.gradle* ./
COPY settings.gradle.kts* build.gradle.kts* ./

RUN ./gradlew --no-daemon dependencies || true

# 소스 복사
COPY src ./src

# ✅ 테스트 스킵
RUN ./gradlew --no-daemon clean bootJar -x test

# ===============================
# 2) Runtime stage
# ===============================
FROM amazoncorretto:17-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]

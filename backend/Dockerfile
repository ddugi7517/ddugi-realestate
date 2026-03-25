FROM --platform=linux/amd64 eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar -x test

FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

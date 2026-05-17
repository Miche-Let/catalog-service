FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# SERVER_PORT 변수를 받아 EXPOSE에 적용
ARG SERVER_PORT
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

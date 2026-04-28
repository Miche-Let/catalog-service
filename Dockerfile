FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew
# 테스트와 Asciidoctor 문서 생성을 제외하고 빌드
RUN ./gradlew bootJar -x test -x asciidoctor --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

# 비관리자 실행을 위한 사용자 및 그룹 생성
RUN addgroup --system spring && adduser --system spring --ingroup spring

# 빌드된 jar 파일을 복사하면서 소유권을 spring 사용자에게 부여
COPY --from=builder --chown=spring:spring /app/build/libs/*-SNAPSHOT.jar app.jar

# 생성한 사용자로 전환
USER spring

EXPOSE 19800

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B clean package dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=target/lib

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system app && useradd --system --gid app --create-home app
WORKDIR /app

COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar
COPY --from=build --chown=app:app /build/target/lib /app/lib

USER app

ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*", "bot.Main"]

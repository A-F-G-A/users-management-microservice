FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/users-management-*.jar app.jar
USER spring:spring

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl --fail --silent "http://127.0.0.1:${PORT:-8080}/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

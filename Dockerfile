# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline || true
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/git-commit-mirror-0.0.1-SNAPSHOT.jar app.jar

ENV DISCORD_BOT_TOKEN=""
ENV DISCORD_CHANNEL_ID="0"
ENV GITHUB_REPO_URL=""
ENV GITHUB_PAT=""
ENV GIT_AUTHOR_NAME="Git Commit Mirror"
ENV GIT_AUTHOR_EMAIL="mirror@example.com"
ENV SCHEDULER_ENABLED="true"
ENV SCHEDULER_CRON="0 0 20 * * *"
ENV PORT=8080

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

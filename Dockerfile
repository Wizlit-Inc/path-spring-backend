# ──────────────── Stage 1: Build ───────────────────
FROM maven:3.8.5-openjdk-17 AS build

#WORKDIR /target
WORKDIR /app

# Copy only what’s needed to resolve dependencies first (caching)
COPY pom.xml .
# RUN mvn dependency:go-offline -B

# Copy source and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ──────────────── Stage 2: Runtime ────────────────
FROM openjdk:17-jdk-slim

#ARG JAR_FILE=target/*.jar
WORKDIR /app

# Pull in just the built artifact
#COPY ${JAR_FILE} app.jar
COPY --from=build /app/target/*.jar app.jar

#EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
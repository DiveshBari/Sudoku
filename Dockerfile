# ── Stage 1: Build the app using Maven ──────────────────
# Uses an image that has Java 21 + Maven pre-installed
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml first (so Maven dependencies are cached)
COPY pom.xml .

# Download all dependencies (cached layer — only re-runs if pom.xml changes)
RUN mvn dependency:go-offline -q

# Copy the rest of the source code
COPY src ./src

# Build the JAR, skip tests for faster build
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run the app with a lightweight Java image ───
# Much smaller than the build image — only needs JRE to run
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Render will inject $PORT automatically
EXPOSE 8080

# Start the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]

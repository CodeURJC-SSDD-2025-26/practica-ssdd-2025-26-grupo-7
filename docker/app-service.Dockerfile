# ============================================================
# Stage 1: Build with Maven (no JDK/Maven needed on host)
# ============================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first to cache dependencies layer
COPY code/app-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY code/app-service/src ./src
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2: Run with lean JRE image
# ============================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the keystore for HTTPS
COPY code/app-service/src/main/resources/keystore.p12 /app/keystore.p12

# Copy the compiled JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]

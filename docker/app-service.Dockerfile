FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY code/app-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY code/app-service/src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY code/app-service/src/main/resources/keystore.p12 /app/keystore.p12
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]

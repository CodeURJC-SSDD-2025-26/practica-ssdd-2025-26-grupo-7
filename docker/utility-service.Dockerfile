FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY code/utility-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY code/utility-service/src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

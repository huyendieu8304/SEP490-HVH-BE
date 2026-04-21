# build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# run stage
FROM mcr.microsoft.com/playwright/java:v1.43.0-jammy
WORKDIR /app

# copy jar từ stage build
COPY --from=builder /app/target/*.jar app.jar

# timezone
ENV TZ=Asia/Ho_Chi_Minh

ENTRYPOINT ["java","-jar","/app/app.jar"]
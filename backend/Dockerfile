# Build stage (Using Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage (Using Java 21 on Ubuntu/Debian to support LibreOffice)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install LibreOffice along with writer and calc packages for full export filters
RUN apt-get update && apt-get install -y \
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
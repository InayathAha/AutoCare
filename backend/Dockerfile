# Stage 1: Build application with Maven and Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight production JRE runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Security: Run as dedicated non-root user
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy application artifact
COPY --from=build /app/target/autocare-backend-*.jar app.jar

# Server port configured via PORT environment variable (Render / Railway), defaulting to 8080
ENV PORT=8080
EXPOSE ${PORT}

# Launch Spring Boot with dynamic PORT binding
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]

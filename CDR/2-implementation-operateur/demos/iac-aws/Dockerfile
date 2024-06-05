# Stage 1: Build the application
FROM maven:3.9.7-eclipse-temurin-21-jammy AS builder

# Set the working directory
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Create the final image
FROM openjdk:21-jdk

# Set the working directory
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/iac-aws-1.0-SNAPSHOT.jar /app/iac-aws-1.0-SNAPSHOT.jar

# Expose the application port (change according to your app)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/iac-aws-1.0-SNAPSHOT.jar"]

# Recommended: Eclipse Temurin (latest and supported)
FROM maven:3.9.6-eclipse-temurin-17

# Set working directory inside container
WORKDIR /app

# Copy only the pom.xml first for faster dependency caching
COPY pom.xml .

# Download Maven dependencies
#RUN apt-get update && apt-get install -y maven && mvn dependency:resolve

# Copy all your source code
COPY . .

# Expose port your Spring Boot app runs on
EXPOSE 8080

# Run Spring Boot in dev mode with Maven (hot reload)
CMD ["mvn", "spring-boot:run"]
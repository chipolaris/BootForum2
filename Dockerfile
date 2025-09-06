# Stage 1: Build the application using Maven and Node.js
# This stage will compile the Java code and build the Angular frontend.
FROM maven:3.9-eclipse-temurin-17-focal AS build
WORKDIR /app

# Copy pom.xml and download dependencies first to leverage Docker's layer caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application. This command will also trigger the frontend-maven-plugin
# to build the Angular application and place it in the target JAR.
# The -DskipTests flag is added to speed up the build process.
RUN mvn package -DskipTests

# Stage 2: Create the final lightweight runtime image
# We use a minimal JRE image based on Alpine Linux for a smaller footprint.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the executable JAR from the build stage to the final image
COPY --from=build /app/target/*.jar app.jar

# Define a mount point for persistent data.
# Your application.yml uses ${user.home} for data paths. In this Alpine image,
# the home directory for the root user is /root. We declare the parent
# directory as a volume to be managed by Docker.
VOLUME /root/BootForum2

# Expose the port the Spring Boot application runs on
EXPOSE 8080

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
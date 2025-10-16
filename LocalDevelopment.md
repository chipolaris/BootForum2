# Local Development Setup Guide

This guide provides instructions on how to set up and run the BootForum2 project in a local development environment. The project consists of a Spring Boot (Java/Maven) backend and an Angular (Node.js) frontend.

## Prerequisites

Before you begin, ensure you have the following software installed on your system:

*   **Git:** For cloning the repository.
*   **Java Development Kit (JDK):** Version 17 or later.
*   **Node.js:** Version 20.11.1 or later (includes npm 10.2.4+).

## 1. Checkout the Project

First, clone the project repository from Git to your local machine.

```
sh git clone <your-repository-url> cd BootForum2
```

## 2. Build the Entire Application (Backend + Frontend)

This project uses the Maven Wrapper (`mvnw`), which is the recommended way to build the application. The Maven build process is configured to automatically handle both the Java backend compilation and the Angular frontend build.

From the root directory of the project (`BootForum2/`), run the following command:

```
sh ./mvnw clean package
```

*(On Windows, use `mvnw.cmd clean package`)*

This single command will:
1.  Download all required Java (Maven) dependencies.
2.  Use the `frontend-maven-plugin` to:
    *   Download the correct Node.js and npm versions locally within the project.
    *   Run `npm install` to get all frontend dependencies.
    *   Run `npm run build` to compile the Angular application for production.
3.  Package the compiled Java code and the compiled Angular frontend into a single executable `.jar` file located in the `target/` directory.

## 3. Running the Application

There are two primary ways to run the application: as a single packaged file (simplest) or as separate processes for active development.

### Method A: Running the Packaged JAR (Recommended for most cases)

This is the easiest way to run the entire application after building it.

1.  Make sure you have completed the build step above.
2.  Run the application using the `java -jar` command on the generated file.
    
```
sh java -jar target/BootForum2-0.0.1-SNAPSHOT.jar
```
3.  Once the application starts, you can access it in your browser at: **`http://localhost:8080`**

### Method B: Separate Backend & Frontend Servers (For Active Development)

This method is ideal when you are actively developing the frontend or backend and want to take advantage of features like hot-reloading. You will need two separate terminal windows.

#### Terminal 1: Run the Spring Boot Backend

1.  In the root directory (`BootForum2/`), run the Spring Boot application using the Maven wrapper.
```
sh ./mvnw spring-boot:run
```

2.  The backend server will start and listen on port `8080`.

#### Terminal 2: Run the Angular Frontend Dev Server

1.  Navigate to the Angular application's directory.
    
```
sh cd src/main/ngapp
```

2.  Install the Node.js dependencies (if you haven't already or if they've changed).

```
sh npm install
```

3.  Start the Angular development server.

```
sh npm start
```

4.  The frontend server will start and listen on port `4200`. It is configured with a proxy, so any API requests it makes will be automatically forwarded to the backend server running on `http://localhost:8080`.
5.  You can now access the application in your browser at: **`http://localhost:4200`**

## 4. Running with Docker (Alternative)

If you have Docker installed, you can build and run the entire application in a container using Docker Compose. This is a great  way to run the application without needing to manage local Java or Node.js versions.

1.  From the root directory of the project, run the following command:

```
sh docker-compose up --build
```
2.  This command will:
    *   Build the Docker image as defined in the `Dockerfile`, which performs a multi-stage build similar to the Maven process.
    *   Start a container named `bootforum-app`.
    *   Map port `80` on your local machine to port `8080` inside the container.
    *   Create a persistent volume named `bootforum-data` to store the H2 database, file uploads, and search indexes, so your data is not lost when the container stops.
3.  Once the container is running, you can access the application in your browser at: **`http://localhost`**
    


# Configuring External Databases

The application is configured by default to use an embedded H2 file-based database for simplicity and ease of initial setup. For development or production environments, you will likely want to connect to a more robust, external database server.

This guide provides the necessary configuration for connecting to **PostgreSQL**, **MySQL**, and **Microsoft SQL Server**.

The process for switching databases involves two main steps:
1.  **Add the JDBC Driver Dependency:** You must add the appropriate Maven dependency for your chosen database to the `pom.xml` file.
2.  **Update Application Configuration:** You need to modify the `spring.datasource` section in the `src/main/resources/application.yml` file to point to your database instance.

---

## 1. PostgreSQL Configuration

### Step 1.1: Add PostgreSQL Dependency

Add the following dependency to your `pom.xml` inside the `<dependencies>` section:
```xml 
<dependency> 
    <groupId>org.postgresql</groupId> 
    <artifactId>postgresql</artifactId> 
    <scope>runtime</scope> 
</dependency>
```

### Step 1.2: Update `application.yml`

Comment out or remove the existing H2 `datasource` configuration and replace it with the following. Be sure to replace the placeholder values with your actual database details.

```yaml 
spring: 
  datasource: 
    url: jdbc:postgresql://localhost:5432/bootforum_db 
    driverClassName: org.postgresql.Driver 
    username: your_postgres_user 
    password: your_postgres_password 
  jpa: 
    properties: 
      hibernate: 
        # This tells Hibernate to generate SQL compatible with PostgreSQL. 
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

---

## 2. MySQL Configuration

### Step 2.1: Add MySQL Dependency

Add the following dependency to your `pom.xml` inside the `<dependencies>` section:

```xml
<dependency> 
    <groupId>com.mysql</groupId> 
    <artifactId>mysql-connector-j</artifactId> 
    <scope>runtime</scope> 
</dependency>
```

with the following.

```yaml
spring: 
  datasource: 
    url: jdbc:mysql://localhost:3306/bootforum_db?useSSL=false&serverTimezone=UTC
    driverClassName: com.mysql.cj.jdbc.Driver 
    username: your_mysql_user 
    password: your_mysql_password 
  jpa: 
    properties: 
      hibernate: 
        # This tells Hibernate to generate SQL compatible with MySQL. 
        dialect: org.hibernate.dialect.MySQLDialect
```

---

## 3. Microsoft SQL Server Configuration

### Step 3.1: Add MS SQL Server Dependency

Add the following dependency to your `pom.xml` inside the `<dependencies>` section:

```xml
<dependency> 
    <groupId>com.microsoft.sqlserver</groupId> 
    <artifactId>mssql-jdbc</artifactId> 
    <scope>runtime</scope> 
</dependency>
```


### Step 3.2: Update `application.yml`

Comment out or remove the existing H2 `datasource` configuration and replace it with the following.

```yaml 
spring: 
  datasource: 
    url: jdbc:sqlserver://localhost:1433;databaseName=bootforum_db;encrypt=false 
    driverClassName: com.microsoft.sqlserver.jdbc.SQLServerDriver 
    username: your_sqlserver_user 
    password: your_sqlserver_password 
  jpa: 
    properties: 
      hibernate: 
        # This tells Hibernate to generate SQL compatible with SQL Server. 
        dialect: org.hibernate.dialect.SQLServerDialect
```

---

## Important: First-Time Database Setup

When you connect the application to a **new, empty database** for the first time, you must ensure that Hibernate can create the necessary tables.

In your `application.yml`, verify that the `ddl-auto` property is set correctly:

```yaml 
spring: 
  jpa: 
    hibernate: 
      ddl-auto: update # or 'create'
```

*   **`update`**: This is the recommended setting for a new database. Hibernate will inspect the database and create any missing tables, columns, or constraints based on your entity definitions. It will not delete existing data or columns.
*   **`create`**: This will **drop all existing tables** and recreate the entire schema from scratch every time the application starts. Use this with extreme caution, as it will result in total data loss on every restart.
*   **`validate`**: After the schema is created, you can switch to this mode. Hibernate will check if the schema matches the entity definitions and fail to start if there are discrepancies.
*   **`none`**: Disables automatic schema management. This is typically used in production environments where schema changes are managed manually via migration scripts (e.g., using Flyway or Liquibase).
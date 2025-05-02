# README.md

## Main Stacks

### Back End
* Spring: 
  * Spring Boot
  * Spring Web
  * Spring Security
  * Spring Data (JPA)
* JPA/Hibernate/H2

### Front End
* Angular
* PrimeNG / PrimeIcons
* Tailwind CSS

### Other
* JWT security library
* Maven build tool

### Development Process
Front end and back end projects are started seperatedly

#### Start Spring Boot backend
From project's root folder:

    ./mvnw spring-boot:run

Backend app is run on http://localhost:8080 by default

#### Start Angular frontend
From **main/ngapp** folder:

    ng serve

Frontend app is run on http://localhost:4200 by default

### Packaging for Deployment
From project's root folder:

    ./mvnw package

If no error, a runnable jar file, **Bootgular-0-0-1-SNAPSHOT.jar**, is created in the target directory
The runnable jar file can be executed with

    java -jar Bootgular-0.0.1-SNAPSHOT.jar

For convenient, the default database is H2 at /data/h2/Bootgular/data directory. 
Individual runtime paramters/properties can be overwritten/configured. Or an external **application.properties** file 
can be specified through command line arguments. 
More references about this https://docs.spring.io/spring-boot/reference/features/external-config.html
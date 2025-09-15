# README.md

**BootForum2** is a modern implementation of Web based discussion forum using **Spring Boot 3** and **Angular 19** as main development frameworks.
Development is done through [Prompt Engineering](https://en.wikipedia.org/wiki/Prompt_engineering) with [Google's Gemini
Code Assist](https://codeassist.google/). In addition, [ChatGPT](https://chatgpt.com/) is used for more generic questions

\* *Currently, in development*

###  [**View BootForum2 Screenshots**](Screenshots.md "Screenshots")
###  [**Live Demo**](http://3.90.20.207:8080/home "BootForum2 Demo")

## Main Stacks

### Back End
* Spring: 
  * Spring Boot
  * Spring Web
  * Spring Security
  * Spring Data (JPA)
  * JPA/Hibernate/H2 (for development), any JDBC compliant database for deployment

### Front End
* Angular
* PrimeNG / PrimeIcons
* Tailwind CSS / HeroIcons

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

If no error, a runnable jar file, **BootForum2-0.0.1-SNAPSHOT.jar**, is created in the target directory
The runnable jar file can be executed with

    java -jar BootForum2-0.0.1-SNAPSHOT.jar

### Database
For convenient of development, the default database (configured in **application.yml**) is H2.
On deployment, any JDBC compliant database can be used: Postgresql, Oracle, MySql, MS SQL Server, etc. 
And as usual in Spring Boot fashion, the database properties can be specified externally through runtime 
properties file or command line arguments. For example:

    java -jar BootForum2-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:postgresql://localhost:5432/mydb \
      --spring.datasource.username=pguser --spring.datasource.password=pgpass \
      --spring.datasource.driver-class-name=org.postgresql.Driver

More references: https://docs.spring.io/spring-boot/reference/features/external-config.html

### Persistence Storage Configuration
* The default local H2 datastore is at ${user.home}/BootForum2/h2/data directory. 
* The default upload directory where users' attachments and images are uploaded is at ${user.home}/BootForum2/uploads
* The default Lucene data directory where search data is at ${user.home}/BootForum2/data

And as usual, individual runtime paramters/properties can be overwritten/configured.
**application.yml** or **application.properties** file can be specified through command line arguments. 
More references about this https://docs.spring.io/spring-boot/reference/features/external-config.html

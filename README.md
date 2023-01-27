# Twelve-Factor App with Spring Boot and Heroku

### Background
Running a system on a cloud platform requires that a component within the system is designed for running on a cloud platform. When components are built this way they can utilize the platform to the full extent by using available resources, fast scaling, and full automation, but also deal with the complications of the cloud like maintaining state and resilience against failures. The "Twelve-Factor App" principles are a set of rules that, when followed, will result in such a component.

[Twelve-Factor App](https://12factor.net)

### Practice

We will use Heroku to run our app in the cloud. This platform is a great example of how easy it should be to deploy an application.

Prerequisites:
* [Free Heroku account](https://signup.heroku.com/)
* [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli)
* [Heroku CLI Plugin for Java](https://github.com/heroku/plugin-java)
* JDK 17
* Docker
* Git

Verify the prerequisites:
```console
javac --version
docker --version
git --version
heroku --version
heroku login
```

Create a Heroku application with an URL routed to that application.
```console
heroku create --team <your-team-name>
```

Remember the application name and first URL, it is needed for later use.

## 1. Codebase
##### One codebase tracked in revision control, many deploys

### Background
The source code of an application should live in a version control system (VCS). Every application in a system should have a separate VCS. In this context an application is an independently runnable component in a system. This breaks with conventional approaches where a big project is in one repository, that consists of several components.

[Twelve-Factor App: factor 1](https://12factor.net/codebase)

### Practice
Git is the VCS of choice. Create a local copy (deploy) of the repository.

```console
git clone https://github.com/ebendal/twelve-factor.git
git checkout one
```

## 2. Dependencies
##### Explicitly declare and isolate dependencies

### Background

An application should have a dependency management system (DMS). All dependencies are listed in a configuration file including the version number of the dependency. The DMS is responsible for importing all the dependencies and creating an artifact that includes all the dependencies. This artifact can run on a runtime environment without pre-installed packages or libraries. This breaks with conventional approaches where an applicationserver has a lot of pre-installed libraries. The DMS is often incorporated in a build tool.

[Twelve-Factor App: factor 2](https://12factor.net/dependencies)

### Practice

The source code of an application should live in a version control system (VCS). Every application in a system should have a separate VCS. In this context an application is an independently runnable component in a system. This breaks with conventional approaches where a big project is in one repository, that consists of several components.

Open [http://start.spring.io](http://start.spring.io) and choose `Gradle - Groovy` and `Java`. Use `twelve-factor` as artifact name. Generate the project and unpack the contents in the previously cloned Git repo.

Replace the contents of `build.gradle` with the following:

```groovy
plugins {
	id 'org.springframework.boot' version '2.7.8'
	id 'io.spring.dependency-management' version '1.0.15.RELEASE'
	id 'java'
}

group = 'com.example'
sourceCompatibility = '17'

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-data-rest'
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.flywaydb:flyway-core'
	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'org.postgresql:postgresql'
	runtimeOnly 'com.h2database:h2'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
	useJUnitPlatform()
}
```

Gradle is the build tool of choice. To let Gradle fetch all the dependencies, compile the source code, and create an executable fat Jar, run:

```console
./gradlew assemble
```

The Jar file can be found at `build/libs/twelve-factor.jar`.

Before running the application give the application a name and disable some defaults. Rename the file `application.properties` to `application.yml` and add the following:

```yml
spring:
  application:
    name: twelve-factor
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
```

Run the application on your development machine:

```console
./gradlew bootRun
```

Check the proper functioning of the application by going to `http://localhost:8080/actuator/health` with your favourite HTTP request tool. For example Curl:

```console
curl http://localhost:8080/actuator/health
```
Response:
```json
{
  "status":"UP"
}
```

After this step the application looks like [this](https://github.com/ebendal/twelve-factor/tree/two).

## 3. Config
##### Store config in the environment

### Background

An application has a different configuration in every environment it runs. This configuration of the application should not be inside the artifact produced by the build tool. The configuration should be obtained upon startup of the application from environment variables or an external service. In this way the artifact can be used for an arbitrary amount of environments.

[Twelve-Factor App: factor 3](https://12factor.net/config)

### Practice

The platforms on which this application should run is the developer machine and on an arbitrary environment in Heroku (for example a `development`, `test`, `staging`, and `production` environment, from now on referred to as a `space`). Spring profiles can be used to make the distinction between a local machine and a cloud platform, but should not be used for distinction between different environments. Your app will start to show different behaviour in different environments if the profiles are not well maintained.

Edit the `application.yml` so it will look like this:

```yml
spring:
  application:
    name: twelve-factor
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
management:
  endpoints:
    web:
      exposure:
        include: health,info
  info:
    env:
      enabled: true
info:
  application:
    os: ${os.name}

---
spring:
  config:
    activate:
      on-profile: local
info:
  application:
    platform: Local Machine
    space: local-development
---
spring:
  config:
    activate:
      on-profile: heroku
info:
  application:
    platform: Heroku
    space: ${space.name}
```

The profile specific properties will only be set when that profile is active. In Heroku the `heroku` profile will be active. In the `application.yml` we configure the Spring actuator info endpoint. The `${}` values instruct Spring to find the values, among others, in environment variables. Note that the `info.application.os` property can be found in the same environment variable on both platform and therefore is configured in a non profile-specific manner.

#### Running the app on your local machine

```console
./gradlew bootRun --args='--spring.profiles.active=local'
```

You can see we set the Spring profile to `local`

Check the response of the actuator info endpoint:
```console
curl localhost:8080/actuator/info
```
```json
{
  "application": {
    "platform": "Local Machine",
    "space": "local-development",
    "os": "Mac OS X"
  }
}
```

#### Running the app on Heroku

In Heroku the `SPACE_NAME` environment variable is not present in the environment of your application. To set this manually we will use the CLI. When CI/CD is available this will not be a manual step but should be fully automated.

```console
heroku config:set SPACE_NAME=demo
heroku config
```

First we need to add some things to our repository to tell Heroku how to run the Jar we will deploy.

We need to tell Heroku that the JVM being uses should have version 17. Create a file named `system.properties` in the root of the repository. The file should have the following content:

```properties
java.runtime.version=11
```

Heroku will try to start an uploaded jar by using the start command `java $JAVA_OPTS -jar <uploaded.jar>  $JAR_OPTS`. With this start command Spring cannot bind to the desired port, because that is configured in the `PORT` environment variable and Spring reads the `SERVER_PORT` environment variable. In this command there is no Spring profile set and therefore our configuration does not know the app is running on Heroku.

Override the start command by create a file named `Procfile` in the root of the repository. The file should have the following content:

```
web: java -Dspring.profiles.active=heroku -Dserver.port=$PORT $JAVA_OPTS  -jar build/libs/twelve-factor.jar
```

Now the application can be built and deployed:
```console
./gradlew build
heroku deploy:jar build/libs/twelve-factor.jar
```

Check the response of the actuator info endpoint. The response should be filled with the values of the environment variables.
```console
curl <random-generated-uri>/actuator/info
```
```json
{
  "application": {
    "platform": "Heroku",
    "space": "demo",
    "os": "Linux"
  }
}
```
After this step the application looks like [this](https://github.com/ebendal/twelve-factor/tree/three).

## 4. Backing services
##### Treat backing services as attached resources

### Background

An application often uses backing services, like a data store, an external cache, or a message broker. When deploying and starting an application the assumption is that these services are already running and ready for being used. In this way the application is decoupled from the actual manifestation of the service which could be a docker image on a local machine or a cluster in the cloud. Credentials for accessing the backing service is configuration therefore should be obtained from environment variables (factor 3).

[Twelve-Factor App: factor 4](https://12factor.net/backing-services)

### Practice
We want our application to be able to store data in a database. Heroku provides a simple way to create a Postgres database on the platform.

```console 
heroku addons:create heroku-postgresql:mini
```

A database is now created and specific environment variables to configure the Spring datasource are automatically set. The datasource config is therefore provided according to the rules from factor 3.

```console 
heroku run printenv
```

This is the most simple example of a REST service that performs CRUD operations. For more information see the Spring documentation. Make sure the class is on the classpath of the application and Spring will wire it.
```java
import lombok.Data;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static javax.persistence.GenerationType.IDENTITY;

@RepositoryRestResource
public interface FactorRepository extends CrudRepository<FactorRepository.Factor, Long> {

    @Data
    @Entity(name = "factor")
    class Factor {

        @Id
        @GeneratedValue(strategy = IDENTITY)
        private Long id;
        private Integer number;
        private String name;
    }
}
```
Check whether the application is working in your local machine.
```console
./gradlew bootRun --args='--spring.profiles.active=local'
curl -d '{"number":"1", "name":"Codebase"}' -H "Content-Type: application/json" -X POST http://localhost:8080/factors
curl http://localhost:8080/factors
```
Deploy the app to Heroku and check if the database is configured correctly by checking that the state is not reset after a restart of the application.

```console
./gradlew build
heroku deploy:jar build/libs/twelve-factor.jar
curl -d '{"number":"1", "name":"Codebase"}' -H "Content-Type: application/json" -X POST https://<app-uri>/factors
heroku dyno:restart
curl http://<app-uri>/factors
```

After this step the application looks like [this](https://github.com/ebendal/twelve-factor/tree/four).

## 5. Build, release, run
##### Strictly separate build and run stages

### Background

An application has three different stages. In the build stage the codebase (factor 1) and the dependencies (factor 2) are combined into an artifact by a build tool. This artifact may be deployed to different environments and for every environment a different release should be created so the configuration (factor 3) and backing services credentials (factor 4) are in the runtime environment. This release should be available at the cloud platform and can be started by a start command. Upon starting a release the run phase begins.

[Twelve-Factor App: factor 5](https://12factor.net/build-release-run)

### Practice
We have seen the combination of the commands `./gradlew build` and `heroku deploy:jar` a lot.

The `./gradlew build` command is building an artifact from the source files and is referred to as the build step.

The `heroku deploy:jar` command does a lot as you can see in the output of the console. First it uploads the artifact to the platform. After that it launches the application. Uploading the artifact and storing it somewhere does not mean it is ready to run. Heroku builds an image using one of its [buildbacks](https://buildpacks.io/). In this case it uses the `heroku/jvm` buildpack. With the uploaded jar and this buildpack it creates an image and stores that image ready to run.

The running stage is where the image is used to create a new container and the start command of the application is executed. The app is monitored during startup and upon the first positive health response the app gets the status `up`.

## 6. Processes
##### Execute the app as one or more stateless processes

### Background

An application consists of one or more processes. These processes can handle triggers from the outside by exposing service endpoints or can run scheduled tasks. In both cases a process can utilize computing power, memory, and disk space. It should use those resources for optimizations and short state transfers (i.e. save an incoming stream on disk to process a file after upload is complete). Apart from that the application should be stateless and any state should be stored in a stateful backing service.

[Twelve-Factor App: factor 6](https://12factor.net/processes)

### Practice
An app in Heroku should be stateless. Heroku can restart an app at any moment. It could be triggered by a new deploy, by an upgrade of the buildpack, by any unknown failure, by updating the underlying infrastructure, and this free version of Heroku stops the app when it is idle and starts it again when traffic is coming its way. With every request that has any state change involved, the change should be persisted to a backing service. Other requests can pick up this state change once the transaction is committed. We already implemented this correctly in factor 4.

## 7. Port binding
##### Export services via port binding

### Background

An application can expose a service by binding to a port and listening on it. Let the port on which the application listens be configured in the release phase (factor 5).

[Twelve-Factor App: factor 7](https://12factor.net/port-binding)

### Practice
The best evidence that Heroku uses port binding is that in the start command of the app the port is configured by an environmental variable of the container it is running in. We have already implemented this correctly in factor 3. Also the router that handles all incoming requests, routes the traffic to a container by forwarding it to the right port of the container.

## 8. Concurrency
##### Scale out via the process model

### Background

An application can scale by using horizontal scaling. Because the application is stateless (factor 6) there is no need te share state between instances and scaling is just a matter of starting additional instances. The current instances finish the already running work. An additional instance helps with new load and work gets distributed equally over all instances.

[Twelve-Factor App: factor 8](https://12factor.net/concurrency)

### Practice
In Heroku you can scale your app vertically (increasing memory and CPU limit) and horizontally (increasing instances).

The memory available to an app is by default `512MB`. The required memory for an application needs to be found out by a mild load performance test. By changing your plan and Dyno type you can increase the memory and CPU limit. For our app `512MB` is sufficient. Most Spring Boot apps require more and `1G` is a good starting point.

``` console
heroku dyno:resize standard-2x
```

Now the app is good for a mild load. When the load starts increasing while the app is running, you want to scale horizontally. Increasing instances so the load can be devided over multiple instances.

``` console
heroku dyno:scale web=2
```

Make sure you configure the app back to the defaults.

## 9. Disposability
##### Maximize robustness with fast startup and graceful shutdown

### Background

An application running in a cloud platform must be able to restart at any time. A failing instance, an upgrade in different layers of virtualization or software, or a new release are all reasons for a potential restart. To make this as seemless as possible an applications should try to finish all in progress work after a shutdown signal is received. The startup of an application should be as fast as feasible. No heavy tasks should be performed during startup but should be started after startup.

[Twelve-Factor App: factor 9](https://12factor.net/disposability)

### Practice

Spring Boot starts typically in more than 10 seconds. By default Heroku requires an app to start within 60 seconds otherwise it considers the startup failed and staus will be `crashed`. For the `heroku/jvm` buildpack this seems to be extended to 90 seconds. You can submit a request to extend this timeout period up to 120 seconds. Spring Boot 3 will have the capability to reduce startup times with Spring AOT and even support for native images which reduces the startup time even further.

Shutdown of an app happens by Heroku sending the `SIGTERM` signal to the running process. Spring Boot will pickup this signal and shuts down its webserver according to the defined protocol. By default this is immediate, meaning it kills all requests still being processed. The default behavior is unwanted according to factor 9 and therefore should be configured te be graceful.

Create a Java class called `GracefulShutdownController`.

```java
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GracefulShutdownController {

    @GetMapping("/graceful-wait")
    @SneakyThrows
    public String waitGraceful(){
        Thread.sleep(20000);
        return "I waited for 20 seconds";
    }
}
```

Now lets try see the result of shutting down while a request is still being handled.

```console
./gradlew build
heroku deploy:jar build/libs/twelve-factor.jar
heroku ps:wait 
curl http://<app-uri>/graceful-wait
```
In a second console run the following command (within 20 seconds):

```console
heroku dyno:restart
```
You will see an error response from the request.

Now add the following at the beginning of your `application.yml`:

```yaml
server:
  shutdown: graceful
```

Repeat the above steps and see that it will result in a succesful response.

After this step the application looks like [this](https://github.com/ebendal/twelve-factor/tree/nine).

## 10. Dev/prod parity
##### Keep development, staging, and production as similar as possible

### Background

An application should be developed and tested against an environment that is as similar as possible to production. Therefore try to run real backing services such as a database on a development machine instead of using a fake in-memory one. Also test an application in the running stage on the cloud.

[Twelve-Factor App: factor 10](https://12factor.net/dev-prod-parity)

### Practice

When creating the application an environment variable `SPACE_NAME` was introduced. This is the way the application knows in which space it is running. The content of environment variables should be the only difference for the application between the `development`, `test`, `staging`, and `production` spaces.

That leaves the parity on a local machine. Up till now the app uses an in memory database when it is not able to find a real Postgres database. This violates the parity principle. Queries can perform different and might not even be supported by the H2 database. To make the local environment as similar as possible to production we will use docker-compose to start a container with a Postgres database.

Find the Postgres database version of the database attached to your application:

```console
heroku pg:info
```

Create a file called `docker-compose.yml` in the root of the repository with the following content:

```yaml
version: '3'
services:
  postgres:
    image: postgres:<postgres-version>
    container_name: twelve-factor-postgres
    environment:
      - POSTGRES_PASSWORD=password
    ports:
      - "5432:5432"
```

Now configure the datasource of the app when the `local` profile is active in the `application.yml`:
```yaml
---
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: password
info:
  application:
    platform: Local Machine
    space: local-development
```

Start the container and the application:

```shell script
docker compose up postgres
./gradlew bootRun --args='--spring.profiles.active=local'
```

After this step the application looks like [this](https://github.com/ebendal/twelve-factor/tree/ten).

## 11. Logs
##### Treat logs as event streams

### Background

An application should not be spending resources on handling logging. Logs are written to standard out and the cloud platform will have a mechanism in place to capture those streams. A special log aggregation tool will take care of storing and retrieving log entries.

[Twelve-Factor App: factor 11](https://12factor.net/logs)

### Practice

In Heroku your app should just log to the standard system out and system error. An agent will pick up those logs and send it to the log aggregator that aggregates all the logs from all the app instances and other relevant platform componentes. You can live tail the logs stream or get recent logs.

```shell script
heroku logs
heroku logs --tail
curl <app-uri>/actuator/info
```

You can choose to let the app logging be forwarded to your own log aggregator as well. You can add a log drain via the CLI. For simplicity we will use an add-on log drain.

```shell script
heroku addons:create papertrail:choklad
heroku addons:open papertrail
curl <app-uri>/actuator/info
```

## 12. Admin processes
##### Run admin/management tasks as one-off processes

### Background

An application might require some management tasks like for example a database migration. Such a task should be incorporated in the codebase of the application and be inside the artifact produced by the build phase. The task should be configured in the release phase and be able to run in the running phase. This should not happen on startup because startup times could be increased significantly (factor 9). Instead it should be a one-off process that can be triggered by an exposed service.

[Twelve-Factor App: factor 12](https://12factor.net/admin-processes)

### Practice

Up till now we used Hibernate to migrate our database. We will use a different library called Flyway to take of that for us. By default it will migrate the database on startup of the app, but it would be even better to let the migration be ran as a one-off process. Keep in mind fast startup from factor 9.

Create a new profile in the `application.yml` and configure Hibernate to only validate the schema:
```yaml
spring:
  application:
    name: twelve-factor
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
...
...
...
---
spring:
  config:
    activate:
      on-profile: database-migration
  flyway:
    enabled: true
    baseline-on-migrate: true
```

By default Spring will look in the `db/migration` folder for any migrations. Our current schema will be the first migration. Create a the file `src/resources/db/migration/V1__Initial_Schema.sql` with the following content:
```sql
CREATE TABLE factor
(
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	name VARCHAR(255),
	number INTEGER
)
```

We want to add an additional column to our factor table. Create the file `src/main/resources/db/migration/V2__Add_Statement.sql` with the following content:
```sql
ALTER TABLE factor ADD COLUMN statement VARCHAR(255)
```

For local development purposes we want the migration to run when starting the app.

```console
./gradlew bootRun --args='--spring.profiles.active=local,database-migration'
```

The database now has an additional column. Our app did not change the entity model yet. The new `FactorRepository.java` should look like this:

```java
import lombok.Data;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static javax.persistence.GenerationType.IDENTITY;

@RepositoryRestResource
public interface FactorRepository extends CrudRepository<FactorRepository.Factor, Long> {

    @Data
    @Entity(name = "factor")
    class Factor {

        @Id
        @GeneratedValue(strategy = IDENTITY)
        private Long id;
        private Integer number;
        private String name;
        private String statement;
    }
}
```

```console
./gradlew bootRun --args='--spring.profiles.active=local,database-migration'
curl -d '{"number":"2", "name":"Dependencies", "statement":"Explicitly declare and isolate dependencies"}' -H "Content-Type: application/json" -X POST https://<app-uri>/factors
```

But what about that one-off process. We do not want to start the application with the `database-migration` profile active. Factor 5 states there is a `build`, `release`, and `run` phase. Database migration typically is a task belonging to the `release` phase.

First we create yet another spring profile to be able to disable the web server of the application and be able to run the application as a finite process.

``` yaml
---
spring:
  config:
    activate:
      on-profile: no-web
  main:
    web-application-type: none
```

Now we let Heroku know to run the databse migration before startup of the application in the `release` phase. The `Procfile` should look like this:

```
web: java -Dspring.profiles.active=heroku -Dserver.port=$PORT $JAVA_OPTS  -jar build/libs/twelve-factor.jar
release: java -Dspring.profiles.active=heroku,database-migration,no-web $JAVA_OPTS  -jar build/libs/twelve-factor.jar
```

``` console
./gradlew build 
heroku logs --tail
heroku deploy:jar build/libs/twelve-factor.jar
heroku dyno:restart
```

Observe the database migration only happens when the application is deployed and not when it is restarted.

After this step the application looks like [this](https://github.com/ebendal/twelve-factor/tree/twelve).

## Conclusions

The Twelve-Factor App principals is a stepwise buildup towards an application that is designed to run on the cloud. It shows how an application can be build that is scalable, portable, and reliable.
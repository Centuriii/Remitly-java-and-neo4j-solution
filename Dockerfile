FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /build

COPY pom.xml .

RUN mkdir -p src/main/java/com/remitly/neo4j/controller
RUN mkdir -p src/main/java/com/remitly/neo4j/service
RUN mkdir -p src/main/java/com/remitly/neo4j/dto
RUN mkdir -p src/main/java/com/remitly/neo4j/exception
RUN mkdir -p src/main/resources

COPY src/main/java/com/remitly/neo4j/*.java src/main/java/com/remitly/neo4j/
COPY src/main/java/com/remitly/neo4j/controller/*.java src/main/java/com/remitly/neo4j/controller/
COPY src/main/java/com/remitly/neo4j/service/*.java src/main/java/com/remitly/neo4j/service/
COPY src/main/java/com/remitly/neo4j/dto/*.java src/main/java/com/remitly/neo4j/dto/
COPY src/main/java/com/remitly/neo4j/exception/*.java src/main/java/com/remitly/neo4j/exception/
COPY src/main/resources/*.* src/main/resources/

RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /build/target/neo4j-csv-importer-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
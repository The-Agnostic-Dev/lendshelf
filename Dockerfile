# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/lendshelf-*.jar /src/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/app.jar app.jar
ENV PORT=5108
EXPOSE 5108
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

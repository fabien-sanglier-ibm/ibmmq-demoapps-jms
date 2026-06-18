FROM maven:3.9.9-eclipse-temurin-8 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -Popenshift -DskipTests package

FROM eclipse-temurin:8-jre

WORKDIR /app

COPY --from=build /workspace/target/*-jar-with-dependencies.jar /app/app.jar

ENV MQ_APP_PASSWORD=""

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Made with Bob

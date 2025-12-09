FROM eclipse-temurin:21-jre

ARG JAR_FILE=services/card-service/target/card-service-0.0.1-SNAPSHOT.jar
WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]


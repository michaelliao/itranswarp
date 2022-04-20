FROM openjdk:17.0.2-slim

WORKDIR /app

ARG JAR_FILE
ADD target/${JAR_FILE} /app/itranswarp.jar

EXPOSE 2019

ENTRYPOINT ["java", "-jar", "/app/itranswarp.jar"]

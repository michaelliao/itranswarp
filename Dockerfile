FROM openjdk:11-jre-slim

WORKDIR /app

ARG JAR_FILE
ADD target/${JAR_FILE} /app/itranswarp.jar

EXPOSE 2019

ENTRYPOINT ["java", "-jar", "/app/itranswarp.jar"]

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ARG JAR_FILE
ADD target/${JAR_FILE} /app/itranswarp.jar

EXPOSE 2019

ENTRYPOINT ["java", "-jar", "/app/itranswarp.jar"]

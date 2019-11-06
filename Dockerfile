FROM openkbs/jdk11-mvn-py3:latest
MAINTAINER zz<546604336@qq.com>
WORKDIR /app
COPY ./ /app

RUN mvn -DskipTests=true clean package

EXPOSE 2019

CMD ["java", "-jar", "release/itranswarp.jar"]

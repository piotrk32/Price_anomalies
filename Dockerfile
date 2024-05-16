FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y openjdk-17-jdk findutils

WORKDIR /app

ADD . /app

RUN ls -la /app

RUN chmod +x ./gradlew

RUN ls -la /app/gradlew

RUN ./gradlew build -x test && ls -la /app/build/libs/

EXPOSE 8080

CMD ["java", "-jar", "/app/build/libs/Steamz-0.0.1-SNAPSHOT.jar"]
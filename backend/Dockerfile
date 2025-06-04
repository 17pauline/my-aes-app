FROM critoma/amd64_u24_noble_ism_security

RUN apt-get update && \
    apt-get install -y openjdk-21-jdk

WORKDIR /app

COPY build/libs/aes-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

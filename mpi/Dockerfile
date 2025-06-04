FROM ubuntu:20.04
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
    mpich \
    libomp-dev \
    openjdk-11-jdk \
    build-essential \
    wget


RUN wget https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/2.8.0/kafka-clients-2.8.0.jar -P /kafka/libs && \
    wget https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar -P /kafka/libs && \
    wget https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.30/slf4j-simple-1.7.30.jar -P /kafka/libs && \
    wget https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.23/mysql-connector-java-8.0.23.jar -P /kafka/libs

WORKDIR /mpi

COPY Source.c /mpi
COPY KafkaConsumerApp.java /mpi

RUN mpicc -fopenmp -o mpi_app Source.c

RUN javac -cp /kafka/libs/*:. KafkaConsumerApp.java

CMD ["java", "-cp", ".:/kafka/libs/*", "KafkaConsumerApp"]

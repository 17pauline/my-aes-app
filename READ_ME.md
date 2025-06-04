To **run the application and start the containers**:
- run `docker network create my-network` **one time only** from project root !!!
- run powershell script `start-all.ps1` from project root

```
# Start Springboot (backend) in a new PowerShell window
Start-Process powershell -ArgumentList "docker run --rm -p 8080:8080 --network my-network --name backend backend-app"

# Start MPI container in a new PowerShell window
Start-Process powershell -ArgumentList "docker run --rm --network my-network --name mpi mpi-app"

# Start Kafka, Zookeeper, MySQL
docker-compose -f kafka.yml up -d

# Start node.js server
docker-compose -f nodejs.yml up --build -d
```

To **stop the containers**:
- run powershell script `stop-all.ps1` from project root

```
# Stop backend and mpi (if still running)
docker stop backend
docker stop mpi

# Bring down Node.js server
docker-compose -f nodejs.yml down

# Bring down Kafka stack (Zookeeper, Kafka, MySQL)
docker-compose -f kafka.yml down
```


Obs: 
- use `img.bmp` for testing
- use AES key `1234567890abcdef`


~


The rest of this file is to document most of the steps taken while working on this assignment.

For the main aspect of the app, I worked in the folder 
	`my-aes-app\backend`

I started with the Springboot app by writing the following files in `src\main\java\com\example\backend`
	`AesAppApplication.java`
	`ImageController.java`

For the frontend, i created the following files in `src\resources\static` according to Springboot requirements
	`index.html`
	`styles.css`

I created the gradle wrapper:
	`gradle wrapper` 
And ran this command to compile the Springboot app:
	`.\gradlew.bat bootRun`

The server starts on port `8080`
http://localhost:8080/index.html

I updated my Dockerfile to start from the base image (https://hub.docker.com/r/critoma/amd64_u24_noble_ism_security), and copy the Springboot jar into `/app` and run it.

I ran the following command
`.\gradlew.bat build`
To create the `.jar` file which includes dependencies for Docker
`COPY build/libs/aes-backend-0.0.1-SNAPSHOT.jar app.jar`

Next, I started building my Docker network
```
docker --version
Docker version 27.5.1, build 9f9e405

docker network create my-network
b9826b160f038de0b501951d27fefc7c369cb804abc7add23f16c48806e6ec4b
```

Created Container 1
```
C:\Users\pauline\Documents\Repos\ism-dad\my-aes-app>docker build -t backend-app ./backend
[+] Building 64.8s (9/9) FINISHED                                  docker:desktop-linux
 => [internal] load build definition from Dockerfile                               0.1s
 => => transferring dockerfile: 272B                                               0.0s
 => [internal] load metadata for docker.io/critoma/amd64_u24_noble_ism_security:l  0.0s
 => [internal] load .dockerignore                                                  0.0s
 => => transferring context: 2B                                                    0.0s
 => [1/4] FROM docker.io/critoma/amd64_u24_noble_ism_security:latest@sha256:45ae8  1.4s
 => => resolve docker.io/critoma/amd64_u24_noble_ism_security:latest@sha256:45ae8  1.3s
 => [internal] load build context                                                  1.1s
 => => transferring context: 39.11MB                                               1.1s
 => [2/4] RUN apt-get update &&     apt-get install -y openjdk-21-jdk             37.0s
 => [3/4] WORKDIR /app                                                             0.1s
 => [4/4] COPY build/libs/aes-backend-0.0.1-SNAPSHOT.jar app.jar                   0.1s
 => exporting to image                                                            25.7s
 => => exporting layers                                                           21.6s
 => => exporting manifest sha256:93b19647122e710cc178875fd2404944f3cc38b01ae9b297  0.0s
 => => exporting config sha256:3489500904222daa701018e20eea53cb4e02aaf900fbf880e7  0.0s
 => => exporting attestation manifest sha256:968d3814f6793dd8a4f6f4eeb7a2878ec4e5  0.0s
 => => exporting manifest list sha256:66ea21b774a702fd6447f12e7a852215875efb2acb2  0.0s
 => => naming to docker.io/library/backend-app:latest                              0.0s
 => => unpacking to docker.io/library/backend-app:latest                           3.9s

View build details: docker-desktop://dashboard/build/desktop-linux/desktop-linux/tz0r7got0cfw6jsdfd1b38404
```

To run it and test the Springboot app inside C1,
`docker run -p 8080:8080 --network my-network --name backend backend-app`

Next I moved onto working on Container 2. I chose to replace RabbitMQ with Apache Kafka as a messaging service due to some errors I encountered with the former.
- RabbitMQ → Kafka
- Jakarta EE / EJB → Plain Java
- JMS → Kafka topic

I created the file `kafka.yml` to compose the docker container.

`docker-compose -f kafka.yml up -d

```
[+] Running 27/27
 ✔ mysql Pulled                                                                   36.4s
 ✔ kafka Pulled                                                                   34.9s
 ✔ zookeeper Pulled                                                               34.9s


[+] Running 3/3
 ✔ Container my-aes-app-mysql-1      Started                                       1.8s
 ✔ Container my-aes-app-zookeeper-1  Started                                       1.8s
 ✔ Container my-aes-app-kafka-1      Started                                       1.4s
```

I updated my `gradle` settings and my `ImageController.java` in order to send a message to Kafka from the backend.

I rebuilt the `.jar`:
`.\gradlew.bat build`

And ran the Springboot app inside C1 again to verify that Kafka messages were received
```
docker rm backend

docker run -p 8080:8080 --network my-network --name backend backend-app
```

Container 2 (Zookeeper) manages the Kafka broker. 
In Container 3 Kafka consumers which are subscribed to the C2 Kafka brokers, are served messages (topics).
Launching the encryption/decryption process as well as inserting into the database is done through `KafkaConsumerApp.java`.

In Container 4 the image is processed in parallel.

```
docker build -t mpi-app ./mpi
docker run --rm --network my-network --name mpi mpi-app

```
Recommended commands to auto-clean - in different tabs
```
docker run --rm -p 8080:8080 --network my-network --name backend backend-app
docker run --rm --network my-network --name mpi mpi-app
```

Next, Container 5 stores the `mysql` database. I initialized the database in `.\database\init.sql`

To access the database and query it, I ran the following commands in a separate ps:
```
cd C:\Users\pauline\Documents\Repos\ism-dad\my-aes-app

docker exec -it my-aes-app-mysql-1 bash
```

```
bash-5.1# mysql -uroot -p
Enter password:
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 8
Server version: 8.0.42 MySQL Community Server - GPL

Copyright (c) 2000, 2025, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> USE image_db; SHOW TABLES;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed
+--------------------+
| Tables_in_image_db |
+--------------------+
| images             |
+--------------------+
1 row in set (0.00 sec)

```

After testing and inserting one image in the database:
```
[KafkaConsumerApp] Got image: op=encryption, mode=ECB, key=1234567890abcdef
Saved result to output.bmp
[KafkaConsumerApp] MPI process completed.
[KafkaConsumerApp] Image inserted into MySQL with ID: 8b2342db-cc55-4a25-946a-082a0145ae48
```

Queried the images table to check:
```
mysql> SELECT id FROM images;
+--------------------------------------+
| id                                   |
+--------------------------------------+
| 8b2342db-cc55-4a25-946a-082a0145ae48 |
+--------------------------------------+
1 row in set (0.00 sec)
```

Next, for Container 6, I've created the docker compose file `nodejs.yml` and the files necessary for the nodejs server inside `nodejs`.

To compose this docker container:
```
docker-compose -f nodejs.yml up --build -d

```

To access the server logs
```
docker logs my-aes-app-nodejs-server-1
```

You can access the image at endpoint: http://localhost:3000/image/:id

Next - to do:
- update the Springboot app ~ ImageController.java to generate a uuid, include it in the kafka message, and send it to the client instead of the generic successful upload message
- update the Kafka consumer ~ KafkaConsumerApp.java to extract the uuid from the received kafka message and save that one in mysql
- upgrade Source.c logic




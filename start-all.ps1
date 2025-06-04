# Start Spring Boot (backend) in a new PowerShell window
Start-Process powershell -ArgumentList "docker run --rm -p 8080:8080 --network my-network --name backend backend-app"

# Start MPI container in a new PowerShell window
Start-Process powershell -ArgumentList "docker run --rm --network my-network --name mpi mpi-app"

# Start Kafka, Zookeeper, MySQL
docker-compose -f kafka.yml up -d
# Start Node.js server
docker-compose -f nodejs.yml up --build -d

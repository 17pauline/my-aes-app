# Stop backend and mpi (if still running)
docker stop backend
docker stop mpi

# Bring down Node.js server
docker-compose -f nodejs.yml down

# Bring down Kafka stack (Zookeeper, Kafka, MySQL)
docker-compose -f kafka.yml down

# Bitflask

A log-structured database implementation exposed by a server that accepts
the [Redis Protocal Specification](https://redis.io/topics/protocol). A client REPL implementation
for users to interact with the database.

## Gradle CLI Execution

### Server

- `./gradlew --console=plain :server:run`

### Client

- `./gradlew --console=plain :client:run`

## Docker

### Gradle image

This is a temporary solution until a proper gradle image is available that supports JDK 20

1. Create a `gradle-jdk20:latest` image using `Dockerfile-gradle`
    - `docker build -f Dockerfile-gradle -t gradle-jdk20:latest .`

### Base image

This image stores the common resources shared across the server and client

1. Create a `app-base:latest` image using `Dockerfile-app-base`
    - `docker build -f Dockerfile-app-base -t app-base:latest .`

### Server image

1. Create an executable server image manually:
    - `docker build -f Dockerfile-server -t bitflask-server .`
2. Two Docker Volumes should be created:
    - `bitflask-db`
        - This will be used to persist data between executions
    - `gradle-cache`
        - This will be used for gradle caching
3. Run the image using:
    - `docker run -p 9090:9090 --name bitflask-server -v bitflask-db:/root/app/.bitflask -v gradle-server-cache:/root/app/.gradle bitflask-server`

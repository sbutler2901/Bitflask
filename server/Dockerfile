# Intermediate build
FROM eclipse-temurin:19_36-jre-jammy AS builder
WORKDIR /app
COPY build/distributions/server-1.0.tar ./
RUN tar -xf ./server-1.0.tar

# Executable
FROM eclipse-temurin:19_36-jre-jammy
WORKDIR /app
COPY --from=0 /app/server-1.0/ ./
EXPOSE 9090
CMD ./bin/server

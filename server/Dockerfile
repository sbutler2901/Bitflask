# Prepare executable
FROM eclipse-temurin:19_36-jre-jammy AS executable-builder
WORKDIR /app
COPY build/distributions/server-1.0.tar ./
RUN tar -xf ./server-1.0.tar

# Executable
FROM eclipse-temurin:19_36-jre-jammy
WORKDIR /app
COPY --from=executable-builder /app/server-1.0/ ./
VOLUME ["/app/.bitflask"]
EXPOSE 9090
CMD ["./bin/server", "--storageStoreDirectoryPath", "/app/.bitflask"]

# Create user and directory
FROM eclipse-temurin:19_36-jre-jammy AS user-builder
RUN groupadd -r server && useradd --no-log-init -r -g server server
ENV HOME=/home/server
WORKDIR $HOME

# Prepare executable
FROM eclipse-temurin:19_36-jre-jammy AS executable-builder
WORKDIR /app
COPY build/distributions/server-1.0.tar ./
RUN tar -xf ./server-1.0.tar

# Executable
FROM user-builder
COPY --from=executable-builder /app/server-1.0/ ./
RUN chown -R server:server $HOME
USER server
EXPOSE 9090
CMD ["./bin/server"]

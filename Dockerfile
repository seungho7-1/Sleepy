# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Download and extract Scouter Agent
RUN wget https://github.com/scouter-project/scouter/releases/download/v2.20.0/scouter-min-2.20.0.tar.gz && \
    tar -xvf scouter-min-2.20.0.tar.gz && \
    rm scouter-min-2.20.0.tar.gz

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/app/scouter/agent.java/scouter.agent.jar", "-Dscouter.config=/app/scouter/agent.java/conf/scouter.conf", "-Dobj_name=sleepy-backend", "-Dnet_collector_ip=scouter-server", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

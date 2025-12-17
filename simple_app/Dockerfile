# Builder

FROM bellsoft/liberica-openjdk-alpine:25 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# Run

FROM bellsoft/liberica-openjdk-alpine:25
WORKDIR /app
ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=builder ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

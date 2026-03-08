FROM gradle:9.1.0-jdk21 AS build
WORKDIR /workspace

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

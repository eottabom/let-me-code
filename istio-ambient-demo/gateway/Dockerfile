FROM gradle:jdk25 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle :istio-ambient-demo:gateway:bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /home/gradle/project/istio-ambient-demo/gateway/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

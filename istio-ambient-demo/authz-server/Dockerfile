FROM gradle:jdk25 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle :istio-ambient-demo:authz-server:bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /home/gradle/project/istio-ambient-demo/authz-server/build/libs/*.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 블로그 실험과 같은 JDK 21 이미지. 로컬 빌드한 bootJar와 OTel Java agent를 함께 넣는다.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/app-0.0.1-SNAPSHOT.jar app.jar
COPY build/otel/opentelemetry-javaagent.jar /otel/agent.jar
EXPOSE 8080
# JVM 옵션은 Deployment의 JAVA_OPTS로 조건마다 바꾼다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

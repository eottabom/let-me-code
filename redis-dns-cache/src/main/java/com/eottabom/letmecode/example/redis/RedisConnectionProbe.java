package com.eottabom.letmecode.example.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;

import java.net.UnknownHostException;
import java.time.Duration;

public class RedisConnectionProbe {

    public void connect(String host, int port) {
        ClientResources clientResources = ClientResources.create();
        RedisURI redisUri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(Duration.ofSeconds(3))
                .build();

        RedisClient redisClient = RedisClient.create(clientResources, redisUri);

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            String pong = connection.sync().ping();
            System.out.printf("connected host=%s port=%d ping=%s%n", host, port, pong);
        } catch (RuntimeException exception) {
            printFailure(host, port, exception);
        } finally {
            redisClient.shutdown();
            clientResources.shutdown();
        }
    }

    private void printFailure(String host, int port, RuntimeException exception) {
        System.out.printf("failed host=%s port=%d exception=%s%n", host, port, exception.getClass().getName());

        Throwable current = exception;
        while (current != null) {
            System.out.printf("cause=%s message=%s%n", current.getClass().getName(), current.getMessage());

            if (current instanceof UnknownHostException) {
                System.out.println("unknown-host=true");
            }

            current = current.getCause();
        }
    }
}

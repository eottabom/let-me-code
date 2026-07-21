package com.eottabom.letmecode.example.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.time.Duration;

public class RedisConnectionProbe {

	private static final Logger log = LoggerFactory.getLogger(RedisConnectionProbe.class);

	public void connect(String host, int port) {
		ClientResources clientResources = ClientResources.create();
		doConnect(host, port, clientResources, "netty");
	}

	public void connectWithJvmResolver(String host, int port) {
		ClientResources clientResources = ClientResources.builder()
			.addressResolverGroup(DefaultAddressResolverGroup.INSTANCE)
			.build();
		doConnect(host, port, clientResources, "jvm");
	}

	private void doConnect(String host, int port, ClientResources clientResources, String resolver) {
		RedisURI redisUri = RedisURI.builder().withHost(host).withPort(port).withTimeout(Duration.ofSeconds(3)).build();

		RedisClient redisClient = RedisClient.create(clientResources, redisUri);

		log.info("resolver={} host={} port={}", resolver, host, port);

		try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
			String pong = connection.sync().ping();
			log.info("result=connected ping={}", pong);
		}
		catch (RuntimeException exception) {
			printFailure(exception);
		}
		finally {
			redisClient.shutdown();
			clientResources.shutdown();
		}
	}

	private void printFailure(RuntimeException exception) {
		log.warn("result=failed exception={}", exception.getClass().getName());

		Throwable current = exception;
		while (current != null) {
			if (current instanceof UnknownHostException) {
				log.warn("  cause={} message={} unknown-host=true", current.getClass().getName(), current.getMessage());
			}
			else {
				log.warn("  cause={} message={}", current.getClass().getName(), current.getMessage());
			}
			current = current.getCause();
		}
	}

}

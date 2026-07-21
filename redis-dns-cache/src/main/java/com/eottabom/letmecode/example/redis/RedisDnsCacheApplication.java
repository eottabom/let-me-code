package com.eottabom.letmecode.example.redis;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisDnsCacheApplication {

	public static void main(String[] args) {
		if (args.length < 2) {
			printUsage();
			return;
		}

		String command = args[0];
		String host = args[1];

		if ("inspect".equals(command)) {
			DnsCacheInspector inspector = new DnsCacheInspector();
			inspector.inspect(host);
			return;
		}

		if ("connect".equals(command)) {
			int port = parsePort(args);
			RedisConnectionProbe probe = new RedisConnectionProbe();
			probe.connect(host, port);
			return;
		}

		if ("connect-jvm".equals(command)) {
			int port = parsePort(args);
			RedisConnectionProbe probe = new RedisConnectionProbe();
			probe.connectWithJvmResolver(host, port);
			return;
		}

		printUsage();
	}

	private static int parsePort(String[] args) {
		if (args.length < 3) {
			return 6379;
		}

		return Integer.parseInt(args[2]);
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("  inspect <host>");
		System.out.println("  connect <host> [port]        -- Lettuce default (Netty resolver)");
		System.out.println("  connect-jvm <host> [port]    -- JVM InetAddress resolver");
	}

}

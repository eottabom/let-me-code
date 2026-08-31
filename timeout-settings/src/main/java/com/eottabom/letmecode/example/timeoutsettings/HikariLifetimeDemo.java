package com.eottabom.letmecode.example.timeoutsettings;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;

// 실제 MySQL 을 Docker(Testcontainers)로 띄우고, wait_timeout 보다 짧은 max-lifetime 을 설정하면
// HikariCP 가 DB 가 끊기 전에 알아서 커넥션을 교체(회전)해서 CommunicationsException 을 피한다는 것을 보여준다.
// 실패하는 쪽(교체가 안 됐을 때)은 DeadConnectionAfterWaitTimeoutTests 에서 확인한다.
//
// HikariCP 는 max-lifetime 을 30000ms(30초) 미만으로 주면 "그대로 쓰지 않고" 기본값인 30분으로
// 되돌려버린다(30초로 올리는 게 아니다). 그래서 이 데모는 max-lifetime 을 정확히 30000ms 로 맞춰서
// 그 클램핑을 피한다. wait_timeout=35, max-lifetime=30000 조합으로 30초 넘게 걸린다. Docker 데몬이 떠 있어야 한다.
final class HikariLifetimeDemo {

	private static final Logger log = LoggerFactory.getLogger(HikariLifetimeDemo.class);

	private HikariLifetimeDemo() {
	}

	static void run(int waitTimeoutSeconds, int borrowCount, long intervalMs) throws InterruptedException {
		try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
			mysql.start();

			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(mysql.getJdbcUrl());
			config.setUsername(mysql.getUsername());
			config.setPassword(mysql.getPassword());
			config.setMaximumPoolSize(1);
			config.setMinimumIdle(1);
			config.setConnectionInitSql("SET SESSION wait_timeout = " + waitTimeoutSeconds);
			// 30000ms 는 HikariCP 가 그대로 받아들이는 최소값이다(1ms라도 모자라면 30분 기본값으로 되돌아간다).
			// wait_timeout 을 그보다 여유 있게 잡아야 HikariCP 가 먼저 커넥션을 교체해서
			// MySQL 이 끊기 전에 회전이 끝난다.
			config.setMaxLifetime(30_000);

			try (HikariDataSource dataSource = new HikariDataSource(config)) {
				log.info("설정: mysql session wait_timeout={}s, hikari maxLifetime={}ms", waitTimeoutSeconds,
						config.getMaxLifetime());

				String previousConnectionId = null;
				for (int i = 0; i < borrowCount; i++) {
					String connectionId = currentConnectionId(dataSource);
					boolean reused = connectionId.equals(previousConnectionId);
					log.info("[{}/{}] mysql connection_id={} ({})", i + 1, borrowCount, connectionId,
							(previousConnectionId != null) ? (reused ? "재사용됨" : "새로 연결됨(max-lifetime 만료로 회전)")
									: "최초 연결");
					previousConnectionId = connectionId;

					if (i < borrowCount - 1) {
						Thread.sleep(intervalMs);
					}
				}
			}
		}
	}

	private static String currentConnectionId(HikariDataSource dataSource) {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT CONNECTION_ID()")) {
			resultSet.next();
			return resultSet.getString(1);
		}
		catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}

}

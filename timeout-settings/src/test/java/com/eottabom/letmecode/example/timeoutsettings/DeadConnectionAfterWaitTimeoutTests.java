package com.eottabom.letmecode.example.timeoutsettings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 블로그 글의 "HikariCP max-lifetime 미스매치" 시나리오를 커넥션 풀 없이, 가장 근본적인 형태로 재현한다.
// MySQL 은 wait_timeout(유휴 세션 허용 시간)을 넘긴 커넥션을 서버 쪽에서 먼저 끊어버린다.
// 애플리케이션이 그 사실을 모르고 "아직 살아있는 줄 알고" 같은 커넥션을 재사용하면 이 에러가 난다.
//
// docker 데몬이 떠 있어야 한다 (Testcontainers 가 MySQL 컨테이너를 띄운다).
@Testcontainers(disabledWithoutDocker = true)
class DeadConnectionAfterWaitTimeoutTests {

	@Container
	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

	@Test
	void reusingConnectionAfterWaitTimeoutThrowsCommunicationsException() throws Exception {
		try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
				MYSQL.getPassword())) {

			int waitTimeoutSeconds = 2;
			setSessionWaitTimeout(connection, waitTimeoutSeconds);

			// 이 커넥션을 아무도 쓰지 않고 wait_timeout 을 넘길 때까지 그냥 들고만 있는다.
			// 애플리케이션 코드 입장에서는 "풀에 반납해둔 유휴 커넥션을 다음에 다시 꺼내 쓰는" 상황과 같다.
			Thread.sleep((waitTimeoutSeconds + 1) * 1000L);

			// MySQL 은 이미 이 세션을 서버 쪽에서 끊었다. 클라이언트는 그걸 모른 채 재사용을 시도한다.
			assertThatThrownBy(() -> {
				try (Statement statement = connection.createStatement()) {
					statement.execute("SELECT 1");
				}
			}).isInstanceOf(SQLException.class)
				.satisfies((ex) -> assertThat(ex.getClass().getSimpleName()).contains("CommunicationsException"));
		}
	}

	private void setSessionWaitTimeout(Connection connection, int seconds) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET SESSION wait_timeout = " + seconds);
			statement.execute("SET SESSION interactive_timeout = " + seconds);
		}
	}

}

#!/usr/bin/env bash
# MySQL 컨테이너와 /external 용 스텁 서버를 띄우고 앱을 빌드한다.
source "$(dirname "$0")/lib.sh"

log "MySQL 8.4 컨테이너 (localhost:13306)"
if [ -z "$(docker ps -q -f name=^startup-mysql$)" ]; then
	docker rm -f startup-mysql >/dev/null 2>&1 || true
	docker run -d --name startup-mysql -p 13306:3306 \
		-e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=startup -e MYSQL_USER=startup -e MYSQL_PASSWORD=startup \
		mysql:8.4 >/dev/null
fi
# 처음 띄울 때는 MySQL 이 초기화 도중 한 번 재시작하므로 테이블 생성이 될 때까지 재시도한다.
for _ in $(seq 1 60); do
	docker exec -i startup-mysql mysql -ustartup -pstartup startup >/dev/null 2>&1 <<'SQL' && break
create table if not exists users (id bigint primary key, name varchar(50) not null);
insert ignore into users (id, name) values (1, 'eottabom'), (2, 'spring'), (3, 'jvm');
SQL
	sleep 1
done

log "스텁 서버 (localhost:18090)"
if [ -f "$DEMO_DIR/run/.stub.pid" ] && kill -0 "$(cat "$DEMO_DIR/run/.stub.pid")" 2>/dev/null; then
	echo "이미 실행 중"
else
	python3 -m http.server 18090 --bind 127.0.0.1 --directory "$DEMO_DIR/run/stub" >/dev/null 2>&1 &
	echo $! >"$DEMO_DIR/run/.stub.pid"
fi

log "앱 빌드"
(cd "$DEMO_DIR/.." && ./gradlew -q :spring-boot-startup:app:bootJar)
ls -l "$JAR"

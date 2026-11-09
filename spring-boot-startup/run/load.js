// E8 부하. VU 4개로 /users/1 을 10초 동안 쉬지 않고 호출한다. 다른 서버가 새로 뜬 이 서버를 계속 부르는 상황을 흉내 낸다.
// 부하를 시작한 뒤 지난 시간으로 요청을 0~1초, 1~3초, 3~10초 구간으로 나눠 따로 집계한다.
import http from 'k6/http';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SUMMARY_FILE = __ENV.SUMMARY_FILE || 'k6-summary.json';
const PHASES = [
	{ name: '요청 0~1초', from: 0, to: 1, trend: new Trend('phase_0_1', true) },
	{ name: '요청 1~3초', from: 1, to: 3, trend: new Trend('phase_1_3', true) },
	{ name: '요청 3~10초', from: 3, to: 10, trend: new Trend('phase_3_10', true) },
];

export const options = {
	vus: 4,
	duration: '10s',
	summaryTrendStats: ['avg', 'p(99)', 'max', 'count'],
};

export function setup() {
	return { start: Date.now() };
}

export default function (data) {
	const sentAt = (Date.now() - data.start) / 1000;
	const response = http.get(`${BASE_URL}/users/1`);
	const phase = PHASES.find((p) => sentAt >= p.from && sentAt < p.to);
	if (phase) {
		phase.trend.add(response.timings.duration);
	}
}

// 구간별 건수, 평균, p99, 최대와 부하 시작 시각(epoch ms)을 SUMMARY_FILE 로 남긴다.
export function handleSummary(data) {
	const phases = PHASES.map((p) => {
		const values = data.metrics[p.trend.name].values;
		return { name: p.name, seconds: p.to - p.from, count: values.count, avg: values.avg, p99: values['p(99)'], max: values.max };
	});
	return { [SUMMARY_FILE]: JSON.stringify({ start: data.setup_data.start, phases }) };
}

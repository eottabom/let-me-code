// 고정 RPS로 /feed 를 호출한다. warmup 구간과 measure 구간을 나눠 measure 구간만 집계한다.
import http from 'k6/http';

const target = __ENV.TARGET;
const rate = Number(__ENV.RATE || 100);
const warmup = __ENV.WARMUP || '60s';
const measure = __ENV.MEASURE || '180s';

export const options = {
    discardResponseBodies: true,
    summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
    scenarios: {
        warmup: {
            executor: 'constant-arrival-rate', rate, timeUnit: '1s', duration: warmup,
            preAllocatedVUs: 50, maxVUs: 400,
        },
        measure: {
            executor: 'constant-arrival-rate', rate, timeUnit: '1s', duration: measure, startTime: warmup,
            preAllocatedVUs: 50, maxVUs: 400,
        },
    },
    // 시나리오별 submetric을 summary에 남기기 위한 threshold. 판정에는 쓰지 않는다.
    thresholds: {
        'http_req_duration{scenario:measure}': ['max>=0'],
        'http_req_failed{scenario:measure}': ['rate>=0'],
        'http_reqs{scenario:measure}': ['count>=0'],
        'dropped_iterations{scenario:measure}': ['count>=0'],
    },
};

export default function () {
    http.get(`${target}/feed`, { timeout: '10s' });
}

export function handleSummary(data) {
    return { stdout: 'K6_SUMMARY ' + JSON.stringify(data.metrics) + '\n' };
}

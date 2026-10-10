"""matrix.sh 결과를 실행별로 집계해 summary.csv 를 만든다.

- 응답 시간: k6 measure 시나리오 (warmup 제외)
- GC pause: GC 로그 중 measure 구간에 끝난 STW pause (Concurrent 단계 제외)
- CPU throttling: measure 시작/종료 시점 cgroup cpu.stat 차이
"""
import csv
from datetime import datetime, timezone
import gzip
import json
import math
from pathlib import Path
import re
from statistics import median
import sys

result_dir = Path(sys.argv[1])
PAUSE = re.compile(r'^\[([^\]]+)\]\[[\d.]+s\]\[info\s*\]\[gc\s*\] GC\(\d+\) (Pause [^(]+?)\s*(?:\(.*\))? .* ([\d.]+)ms$')


def parse_time(value):
    return datetime.strptime(value, '%Y-%m-%dT%H:%M:%S.%f%z')


def percentile(values, fraction):
    return values[max(0, math.ceil(len(values) * fraction) - 1)] if values else 0.0


def cpu_stat(path):
    sections = path.read_text().split('MEASURE_END')
    def parse(section):
        stamp = re.search(r'\d{4}-\d\d-\d\dT[\d:]+', section).group(0)
        values = dict(re.findall(r'^(\w+) (\d+)$', section, re.MULTILINE))
        return datetime.strptime(stamp, '%Y-%m-%dT%H:%M:%S').replace(tzinfo=timezone.utc), values
    start, before = parse(sections[0])
    end, after = parse(sections[1])
    delta = {key: int(after[key]) - int(before[key]) for key in ('usage_usec', 'nr_periods', 'nr_throttled', 'throttled_usec')}
    return start, end, delta


rows = []
for k6_path in sorted(result_dir.glob('*-run-*.k6.json')):
    name, run = k6_path.name.removesuffix('.k6.json').rsplit('-run-', 1)
    prefix = result_dir / f'{name}-run-{run}'
    metrics = json.loads(k6_path.read_text().strip().splitlines()[-1])
    latency = metrics['http_req_duration{scenario:measure}']['values']
    start, end, cpu = cpu_stat(prefix.with_suffix('.cpustat.txt'))
    seconds = (end - start).total_seconds()
    pauses, full = [], 0
    with gzip.open(prefix.with_suffix('.gc.log.gz'), 'rt') as source:
        for line in source:
            match = PAUSE.match(line.strip())
            if not match or not start <= parse_time(match.group(1)) <= end:
                continue
            pauses.append(float(match.group(3)))
            full += match.group(2).startswith('Pause Full')
    pauses.sort()
    rows.append({
        'case': name, 'run': run, 'measure_seconds': round(seconds),
        'requests': metrics['http_reqs{scenario:measure}']['values']['count'],
        'dropped': metrics.get('dropped_iterations{scenario:measure}', {}).get('values', {}).get('count', 0),
        'failed_rate': metrics['http_req_failed{scenario:measure}']['values']['rate'],
        'http_p50_ms': round(latency['p(50)'], 2), 'http_p95_ms': round(latency['p(95)'], 2),
        'http_p99_ms': round(latency['p(99)'], 2), 'http_max_ms': round(latency['max'], 2),
        'gc_pause_count': len(pauses), 'gc_full_count': full,
        'gc_pause_p99_ms': percentile(pauses, 0.99), 'gc_pause_max_ms': max(pauses, default=0),
        'gc_pause_total_ms': round(sum(pauses), 1),
        'gc_time_percent': round(sum(pauses) / 10 / seconds, 2) if seconds else 0,
        'cpu_cores_used': round(cpu['usage_usec'] / 1e6 / seconds, 2) if seconds else 0,
        'throttled_periods_percent': round(100 * cpu['nr_throttled'] / cpu['nr_periods'], 1) if cpu['nr_periods'] else 0,
        'throttled_seconds': round(cpu['throttled_usec'] / 1e6, 2),
    })

order = ['default-large', 'default-small', 'g1-cpu-small', 'g1-small', 'g1-small-apc2']
rows.sort(key=lambda row: (order.index(row['case']) if row['case'] in order else 99, row['run']))
with (result_dir / 'summary.csv').open('w', newline='') as output:
    writer = csv.DictWriter(output, fieldnames=rows[0].keys())
    writer.writeheader()
    writer.writerows(rows)

# 조건별 중앙값
keys = [key for key in rows[0] if key not in ('case', 'run')]
with (result_dir / 'summary-median.csv').open('w', newline='') as output:
    writer = csv.DictWriter(output, fieldnames=['case', 'runs', *keys])
    writer.writeheader()
    for name in dict.fromkeys(row['case'] for row in rows):
        group = [row for row in rows if row['case'] == name]
        writer.writerow({'case': name, 'runs': len(group), **{key: round(median(float(row[key]) for row in group), 2) for key in keys}})
print((result_dir / 'summary-median.csv').read_text())

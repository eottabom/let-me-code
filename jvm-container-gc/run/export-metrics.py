"""리소스 변경 시나리오 구간의 대시보드 쿼리 결과를 Prometheus 에서 JSON 으로 저장한다.

Prometheus 는 클러스터 안에만 있어 클러스터를 지우면 데이터도 사라지므로, 글에 쓴 수치의 원본을 남겨둔다.
사용법: python3 run/export-metrics.py results/resize   (run/cluster.sh forward 실행 중이어야 한다)
"""
import json
from pathlib import Path
import sys
from urllib.parse import urlencode
from urllib.request import urlopen

root = Path(__file__).resolve().parents[1]
scenario = Path(sys.argv[1]).resolve()
timeline = dict(line.split() for line in (scenario / 'timeline.txt').read_text().splitlines() if line.strip())
start, end = int(timeline['LOAD_START']), int(timeline['LOAD_END'])
dashboard = json.loads((root / 'k8s/grafana/jvm-resize.json').read_text())

panels = []
for panel in dashboard['panels']:
    query = panel['targets'][0]['expr']
    params = urlencode({'query': query, 'start': start, 'end': end, 'step': 15})
    with urlopen(f'http://localhost:19090/api/v1/query_range?{params}', timeout=30) as response:
        data = json.load(response)
    panels.append({'title': panel['title'], 'query': query, 'result': data['data']['result']})

output = scenario / 'prometheus.json'
output.write_text(json.dumps({'start': start, 'end': end, 'step': 15, 'panels': panels}, indent=2, ensure_ascii=False) + '\n')
print(f'{len(panels)} panels → {output}')

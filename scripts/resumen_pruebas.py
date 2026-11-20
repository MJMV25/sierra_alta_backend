"""Resume resultados originales de Surefire, JaCoCo y Vitest sin inventar corridas."""
import csv
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
suites = []
for path in sorted((root / 'target/surefire-reports').glob('TEST-*.xml')):
    suite = ET.parse(path).getroot()
    suites.append({'name': suite.get('name'), **{key: int(suite.get(key, 0)) for key in
                   ('tests', 'failures', 'errors', 'skipped')}})
modules = {}
coverage_path = root / 'target/site/jacoco/jacoco.csv'
if coverage_path.exists():
    for row in csv.DictReader(coverage_path.read_text(encoding='utf-8').splitlines()):
        package = row['PACKAGE']
        module = next((name for name in ('auth', 'forecast', 'history', 'indicators')
                       if '.' + name in package), 'transversal')
        counts = modules.setdefault(module, dict.fromkeys(
            ('LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED'), 0))
        for key in counts:
            counts[key] += int(row[key])
result = {'commit': os.environ.get('GITHUB_SHA'), 'suites': suites, 'coverage': modules}
for name, path in [('frontend', root / 'frontend/coverage/test-results.json'),
                   ('browser', root / 'target/e2e/resultados.json')]:
    if path.exists():
        data = json.loads(path.read_text(encoding='utf-8'))
        result[name] = ({k: data.get(k) for k in ('numTotalTests', 'numPassedTests',
                        'numFailedTests', 'numPendingTests')} if name == 'frontend' else data)
out = root / 'target/e2e'
out.mkdir(exist_ok=True, parents=True)
(out / 'resumen-ci.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
print('RESUMEN_CI=' + json.dumps(result))

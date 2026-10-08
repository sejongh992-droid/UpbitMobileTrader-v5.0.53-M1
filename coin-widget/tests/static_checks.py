from pathlib import Path
import re, xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
xmls = list(res.rglob('*.xml')) + [root / 'app/src/main/AndroidManifest.xml']
for path in xmls:
    ET.parse(path)
print(f'PASS XML well-formed: {len(xmls)} files')
ids = set()
for path in xmls:
    ids.update(re.findall(r'@\+id/([a-zA-Z0-9_]+)', path.read_text()))
java = '\n'.join(p.read_text() for p in (root/'app/src/main/java').rglob('*.java'))
refs = set(re.findall(r'\bR\.id\.([A-Za-z0-9_]+)', java))
assert refs <= ids, f'Undefined layout IDs: {refs-ids}'
for kind in ('drawable', 'layout', 'xml'):
    for name in set(re.findall(r'\bR\.'+kind+r'\.([A-Za-z0-9_]+)', java)):
        assert (res/kind/(name+'.xml')).exists(), (kind,name)
print('PASS resource references')
assert 'setPeriodic(7_200_000L,600_000L)' in java
assert 'setRequestMethod("GET")' in java
assert '/v1/orders' not in java and '/v1/withdraws' not in java
assert 'x-cg-demo-api-key' in java and 'AES/GCM/NoPadding' in java
assert 'usesCleartextTraffic="false"' in (root/'app/src/main/AndroidManifest.xml').read_text()
assert 'READ_CONTACTS' not in java and 'ACCESS_FINE_LOCATION' not in java
print('PASS 2-hour schedule / read-only / encryption / HTTPS configuration')

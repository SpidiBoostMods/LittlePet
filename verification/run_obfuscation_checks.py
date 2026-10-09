from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = Path(os.environ.get('JAVA_HOME', '/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home'))
WORK = ROOT / 'build' / 'protection' / 'checks'
WORK.mkdir(parents=True, exist_ok=True)
dependencies = (ROOT / 'build/protection/classpath.txt').read_text().splitlines()
version=__import__('re').search(r'^mod_version=(.+)$',(ROOT/'gradle.properties').read_text(),__import__('re').M).group(1)
original = ROOT / f'build/devlibs/LittlePet-1.21.4-{version}-dev.jar'
protected = ROOT / f'build/protection/LittlePet-Standalone-SpidiBoost-{version}-production-named.jar'
mapping = ROOT / 'build/protection/named-mapping.json'
named_map = json.loads(mapping.read_text())
release_map = json.loads((ROOT / 'build/protection/release-mapping.json').read_text())
if named_map['classes'] != release_map['classes'] or set(named_map['methods'].values()) != set(release_map['methods'].values()) or set(named_map['fields'].values()) != set(release_map['fields'].values()):
    raise AssertionError('Verification mappings do not describe the production build')
probes = ['ModelProbe', 'FootContactProbe', 'CapybaraProbe', 'DachshundProbe', 'DumplingProbe', 'CustomSteveProbe', 'SkinProbe', 'TaksaProbe', 'PlayerNameProbe', 'ProbeNames']
baseline_signature = {'jar': hashlib.sha256(original.read_bytes()).hexdigest(), 'sources': {probe: hashlib.sha256((ROOT / 'verification' / (probe + '.java')).read_bytes()).hexdigest() for probe in probes}, 'classpath': hashlib.sha256((ROOT / 'build/protection/classpath.txt').read_bytes()).hexdigest()}
baseline_stamp = WORK / 'baseline-success.json'
reuse_baseline = '--reuse-baseline' in sys.argv and baseline_stamp.exists() and json.loads(baseline_stamp.read_text()) == baseline_signature
original_classes = WORK / 'baseline-classes'
protected_classes = WORK / 'protected-classes'
original_classes.mkdir(exist_ok=True)
original_cp = os.pathsep.join(map(str, [original, *dependencies]))
subprocess.run([str(JAVA_ROOT / 'bin/javac'), '-proc:none', '-cp', original_cp, '-d', str(original_classes), *[str(ROOT / 'verification' / (probe + '.java')) for probe in probes]], check=True)
tools_cp = (ROOT / 'build/protection/obfuscator-classpath.txt').read_text().strip()
subprocess.run([str(JAVA_ROOT / 'bin/java'), '-cp', tools_cp, 'com.spidiboost.build.RemapPetProbes', str(mapping), str(original_classes), str(protected_classes)], check=True)
results = {}
for mode, classes, jar in [('baseline', original_classes, original), ('protected', protected_classes, protected)]:
    cp = os.pathsep.join(map(str, [classes, jar, *dependencies]))
    results[mode] = {}
    for probe in probes[:-1]:
        out = WORK / mode / probe
        out.mkdir(parents=True, exist_ok=True)
        if mode == 'baseline' and reuse_baseline:
            results[mode][probe] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in out.iterdir() if p.suffix in ('.json', '.txt')}
            print('Reusing verified baseline', probe, flush=True)
            continue
        cmd = [str(JAVA_ROOT / 'bin/java'), '-Xverify:all', '-Xmx1G', '-Djava.awt.headless=true', '-Dprotection.map=' + str(mapping), '-cp', cp, probe, str(out)]
        started = time.monotonic()
        run = subprocess.run(cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (out / 'run.log').write_text(run.stdout)
        print(mode, probe, run.returncode, round(time.monotonic() - started, 2), flush=True)
        print(run.stdout, flush=True)
        if run.returncode:
            raise SystemExit(run.returncode)
        results[mode][probe] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in out.iterdir() if p.suffix in ('.json', '.txt')}
    if mode == 'baseline':
        baseline_stamp.write_text(json.dumps(baseline_signature, indent=2))
if results['baseline'] != results['protected']:
    raise AssertionError('Transformed vertex/rig/face results differ from baseline')
report = {'author': 'SpidiBoost', 'status': 'PASS', 'checks': results, 'namedJarSHA256': hashlib.sha256(protected.read_bytes()).hexdigest(), 'scope': 'JVM verification, pose/ground sweeps, exact geometry/rig/UV comparison and face composition. Live Minecraft GUI/native dialogs not exercised.'}
(WORK / 'report.json').write_text(json.dumps(report, indent=2))
print('PASS: all baseline/protected geometry, rig animation and face-image hashes match exactly.', flush=True)

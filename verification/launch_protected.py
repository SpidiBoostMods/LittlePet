from pathlib import Path
import subprocess,os,json
root=Path(__file__).resolve().parents[1]
run=root/'build'/os.environ.get('LITTLEPET_RUN','runtime-test-1.1.0')
paths=(root/'build/protection/classpath.txt').read_text().splitlines()
paths=[p for p in paths if 'minecraft-merged-d1fae903ae' not in p and 'dev-launch-injector' not in p and '/remapped_mods/' not in p]
api=next(Path('/Users/danil/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.119.4+1.21.4').glob('*/*[!s].jar'))
paths.append(str(api))
jar=root/'build/libs/LittlePet-v1.1.0.jar'
if os.environ.get('LITTLEPET_TEST_COMPAT')=='1':
 prism=Path('/Users/danil/Library/Application Support/PrismLauncher')
 paths=[p for p in paths if not any(s in p for s in ('/fabric-loader/','/sponge-mixin/','/org.ow2.asm/','/org/ow2/asm/'))]
 libraries=json.loads((prism/'meta/net.fabricmc.fabric-loader/0.19.3.json').read_text())['libraries']
 for library in libraries:
  group,artifact,version=library['name'].split(':')[:3]
  jar=prism/'libraries'/group.replace('.','/')/artifact/version/(artifact+'-'+version+'.jar')
  if not jar.exists():raise FileNotFoundError(jar)
  paths.append(str(jar))
cp=os.pathsep.join(paths)
command=['/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java','-XstartOnFirstThread','-Xmx2G','-Dfabric.development=false','-Dfabric.gameVersion=1.21.4','-cp',cp,'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','SpidiBoostTest','--uuid','d4374c5b608d493d834ca2eb2c75d986','--accessToken','0','--version','LittlePetTest','--gameDir',str(run),'--assetsDir','/Users/danil/Library/Application Support/PrismLauncher/assets','--assetIndex','19','--width','1100','--height','720','--userType','legacy','--quickPlaySingleplayer','LittlePet-Test']
if os.environ.get('LITTLEPET_TEST_COMPAT')=='1': command.insert(1,'-Dlittlepet.qa.compat=true')
if os.environ.get('LITTLEPET_TEST_CATALOG')=='1': command.insert(1,'-Dlittlepet.qa.catalog=true')
if os.environ.get('LITTLEPET_TEST_SKIN_FIXTURE')=='1': command.insert(1,'-Dlittlepet.qa.skinFixture=true')
if os.environ.get('LITTLEPET_TEST_SCALES_ONLY')=='1': command.insert(1,'-Dlittlepet.qa.scalesOnly=true')
if os.environ.get('LITTLEPET_TEST_VISUAL_ONLY')=='1': command.insert(1,'-Dlittlepet.qa.visualOnly=true')
with (run/'launcher.log').open('w') as log:
 process=subprocess.Popen(command,cwd=run,stdout=log,stderr=subprocess.STDOUT)
 (run/'pid.txt').write_text(str(process.pid))
 print('Started isolated protected client, PID',process.pid,flush=True)
 print('Client exit',process.wait(),flush=True)

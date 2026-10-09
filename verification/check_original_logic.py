from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
results={}
for original,ported in [('TaksaBrain','PetBrain'),('SmoothValue','SmoothValue')]:
 source=(root/'upstream/atools'/('Original'+original+'.java')).read_text().strip()
 expected=source.replace('package com.rwstafftool.modules.visuals.taksa;','package com.spidiboost.littlepet;').replace('import com.rwstafftool.manager.IMinecraft;','').replace('import com.rwstafftool.modules.visuals.taksa.SmoothValue;','').replace('TaksaBrain','PetBrain').replace('\nimplements IMinecraft','')
 actual=(root/'src/main/java/com/spidiboost/littlepet'/(ported+'.java')).read_text().strip()
 assert actual==expected,ported+' no longer matches ATools algorithm'
 results[ported]=hashlib.sha256(actual.encode()).hexdigest()
assert '.getYaw()' not in (root/'src/main/java/com/spidiboost/littlepet/PetBrain.java').read_text()
(root/'build/protection/original-logic-parity.json').write_text(json.dumps({'status':'PASS','sourceHashes':results,'scope':'Exact ATools method bodies, including movement, rotation, AFK, attack, gravity, ground search, teleport and wall clock smoothing.'},indent=2))
print('PASS: exact ATools TaksaBrain + SmoothValue logic preserved')

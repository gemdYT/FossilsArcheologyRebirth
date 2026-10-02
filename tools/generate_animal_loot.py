import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]; assets=root/'src/main/resources/assets/fossil'
items=set(json.loads((assets/'items.json').read_text(encoding='utf-8')))
for species in json.loads((assets/'species.json').read_text(encoding='utf-8')):
    name=species['name']; pools=[]
    if 'meat_'+name in items: pools.append({'rolls':2,'entries':[{'type':'minecraft:item','name':'fossil:meat_'+name}]})
    bones=[item for item in items if item.startswith('bone_') and item.endswith('_'+name)]
    if bones: pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':'fossil:'+bone} for bone in sorted(bones)]})
    path=root/f'src/main/resources/data/fossil/loot_table/entities/{name}.json'; path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps({'type':'minecraft:entity','pools':pools},indent=2)+'\n')


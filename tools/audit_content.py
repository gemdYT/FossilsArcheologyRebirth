"""Audit the active 26.3 data; no old checkout or generator inputs are needed."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'src/main/resources'
DATA = RESOURCES / 'data/fossil'
items = set(json.loads((RESOURCES / 'assets/fossil/items.json').read_text(encoding='utf-8')))
blocks = json.loads((RESOURCES / 'assets/fossil/blocks.json').read_text(encoding='utf-8'))
species = json.loads((RESOURCES / 'assets/fossil/species.json').read_text(encoding='utf-8'))
registered = items | {b['name'] for b in blocks} | {'bio_fossil', 'bio_goo', 'dodo_dna', 'fossil_ore', 'analyzer', 'culture_vat', 'dodo_egg'}
registered |= {'spawn_egg_' + s['name'] for s in species} | {'spawn_egg_anu', 'spawn_egg_sentry_piglin', 'spawn_egg_tar_slime', 'spawn_egg_failuresaurus'}
outputs, errors = set(), []

# A valid filename alone does not prove that the species can animate or move.
for animal in species:
    name = animal['name']
    if name == 'quagga':  # Uses the native horse renderer and locomotion.
        continue
    animation_file = RESOURCES / 'assets/fossil/geckolib/animations' / (animal['model'] + '.json')
    animations = json.loads(animation_file.read_text(encoding='utf-8'))['animations']
    for role in ('idle', 'walk', 'run', 'swim', 'fastSwim', 'fly', 'fastFly', 'attack'):
        if animal[role] not in animations:
            errors.append(f'{name}: missing {role} animation {animal[role]}')
    for field in ('speed', 'swimSpeed', 'flightSpeed', 'cruiseSpeed', 'chaseSpeed', 'turnRate', 'flightHeight'):
        if not math.isfinite(animal[field]) or animal[field] <= 0:
            errors.append(f'{name}: invalid {field} {animal[field]}')
    if animal['chaseSpeed'] <= animal['cruiseSpeed']:
        errors.append(f'{name}: chase speed must exceed cruise speed')
    if animal['movement'] == 'FLIGHT' and animal['fly'] == animal['idle']:
        errors.append(f'{name}: flight incorrectly uses idle animation')
    if animal['movement'] == 'FLIGHT':
        geometry_file = RESOURCES / 'assets/fossil/geckolib/models/entity' / (animal['model'] + '.json')
        geometry = json.loads(geometry_file.read_text(encoding='utf-8'))['minecraft:geometry']
        bones = {bone['name'] for model in geometry for bone in model['bones']}
        animated = set(animations[animal['fly']].get('bones', {}))
        if not animated.intersection(bones):
            errors.append(f'{name}: flight animation does not animate any model bones')
    if animal['movement'] == 'AQUATIC' and animal['swim'] == animal['idle']:
        errors.append(f'{name}: swimming incorrectly uses idle animation')

def loot_items(value):
    if isinstance(value, dict):
        if value.get('type') == 'minecraft:item':
            yield value['name']
        for child in value.values():
            yield from loot_items(child)
    elif isinstance(value, list):
        for child in value:
            yield from loot_items(child)

def check_item(value, path):
    if value.startswith('fossil:') and value.split(':', 1)[1] not in registered:
        errors.append(f'{path.relative_to(ROOT)}: unknown item {value}')

def check_loot(value, path):
    if isinstance(value, dict):
        for obsolete in ('conditions', 'functions', 'function'):
            if obsolete in value:
                errors.append(f'{path.relative_to(ROOT)}: obsolete loot key {obsolete}')
        for key in ('entries', 'children'):
            for entry in value.get(key, []):
                if entry.get('type') not in {'minecraft:item', 'minecraft:empty', 'minecraft:loot_table', 'minecraft:tag', 'minecraft:alternatives', 'minecraft:group', 'minecraft:sequence', 'minecraft:dynamic'}:
                    errors.append(f'{path.relative_to(ROOT)}: invalid loot entry type {entry.get("type")}')
        for child in value.values():
            check_loot(child, path)
    elif isinstance(value, list):
        for child in value:
            check_loot(child, path)

for path in sorted(DATA.rglob('*.json')):
    value = json.loads(path.read_text(encoding='utf-8'))
    relative = path.relative_to(DATA)
    if relative.parts[0] == 'recipe':
        result = value.get('result', {})
        result = result.get('id', '') if isinstance(result, dict) else result
        outputs.add(result)
        check_item(result, path)
    elif relative.parts[0] == 'fossil_processing':
        for item in value['inputs'] + ([value['fuel']] if value.get('fuel') else []):
            check_item(item, path)
        for result in value['results']:
            outputs.add(result['item'])
            check_item(result['item'], path)
    elif relative.parts[0] == 'loot_table':
        check_loot(value, path)
        for item in loot_items(value):
            check_item(item, path)
            outputs.add(item)
    elif relative.parts[0] == 'villager_trade':
        outputs.add(value['gives']['id'])
        check_item(value['gives']['id'], path)
    elif relative.parts[:2] == ('worldgen', 'template_pool'):
        for entry in value['elements']:
            location = entry['element'].get('location', '')
            if location.startswith('fossil:') and not (DATA / 'structure' / (location.split(':')[1] + '.nbt')).is_file():
                errors.append(f'{relative}: missing template {location}')

# These are created by server-owned interactions rather than recipe or loot JSON.
interaction_sources = {i: 'aquatic capture' for i in sorted(items) if i.startswith('bucket_item_')}
interaction_sources.update({i: 'adult bird laying' for i in sorted(items) if i.startswith('egg_') and not i.startswith('egg_item_')})
interaction_sources.update({i: 'mature plant harvest' for i in sorted(items) if i.startswith('berry_')})
interaction_sources.update({'tar_bucket': 'bucket pickup', 'music_disc_scarab': 'treasury chest'})
missing = sorted(i for i in items if 'fossil:' + i not in outputs and i not in interaction_sources)
report = {'species': len(species), 'catalogBlocks': len(blocks), 'catalogItems': len(items),
          'processingRecipes': len(list((DATA / 'fossil_processing').glob('*.json'))),
          'itemsWithoutAcquisition': missing, 'interactionAcquisition': interaction_sources, 'errors': errors}
destination = ROOT / 'build/content-audit.json'
destination.parent.mkdir(exist_ok=True)
destination.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
if errors or missing:
    raise SystemExit(1)

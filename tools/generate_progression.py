"""Maintain shared survival recipes and loot using only the current content catalog."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/fossil'
ASSETS = ROOT / 'src/main/resources/assets/fossil'
ITEMS = set(json.loads((ASSETS / 'items.json').read_text(encoding='utf-8')))
BLOCKS = {b['name'] for b in json.loads((ASSETS / 'blocks.json').read_text(encoding='utf-8'))}

def write(path, data):
    target = DATA / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def shaped(name, pattern, key, count=1):
    write(f'recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': key, 'result': {'id': 'fossil:' + name, 'count': count}})

def shapeless(name, ingredients, count=1):
    write(f'recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': ingredients, 'result': {'id': 'fossil:' + name, 'count': count}})

def loot_pool(item, rolls=1):
    return {'rolls': rolls, 'entries': [{'type': 'minecraft:item', 'name': 'fossil:' + item}]}

def loot_entries(entries):
    for entry in entries:
        yield entry
        yield from loot_entries(entry.get('children', []))

for material, ingredient in [('wooden', '#minecraft:planks'), ('stone', '#minecraft:stone_tool_materials'), ('iron', 'minecraft:iron_ingot'), ('gold', 'minecraft:gold_ingot'), ('diamond', 'minecraft:diamond')]:
    shaped(material + '_javelin', ['  M', ' S ', 'S  '], {'M': ingredient, 'S': 'minecraft:stick'})
shaped('javelin', ['  M', ' S ', 'S  '], {'M': 'minecraft:flint', 'S': 'minecraft:stick'})
shaped('ancient_clock', [' G ', 'GCG', ' R '], {'G': 'minecraft:gold_ingot', 'C': 'minecraft:clock', 'R': 'fossil:relic_scrap'})
shaped('artificial_honeycomb', ['GGG', 'GHG', 'GGG'], {'G': 'fossil:bio_goo', 'H': 'minecraft:honeycomb'})
shaped('anu_statue', [' R ', 'SGS', 'SSS'], {'R': 'fossil:relic_scrap', 'S': 'minecraft:stone_bricks', 'G': 'minecraft:gold_block'})
shaped('anubite_statue', [' R ', 'SSS', 'SSS'], {'R': 'fossil:relic_scrap', 'S': 'minecraft:stone_bricks'})
shaped('ancient_chest', ['PPP', 'PCP', 'PPP'], {'P': '#minecraft:planks', 'C': 'minecraft:chest'})
shaped('sarcophagus', ['SSS', 'SCS', 'SSS'], {'S': 'fossil:ancient_stone', 'C': 'minecraft:barrel'})
shaped('slime_trail', ['S S', 'S S', 'S S'], {'S': 'minecraft:slime_ball'}, 16)
shapeless('ancient_stone', ['minecraft:stone', 'fossil:relic_scrap'], 4)
shapeless('ancient_glass', ['minecraft:glass', 'fossil:relic_scrap'], 4)
shapeless('ancient_wood_log', ['#minecraft:logs', 'fossil:relic_scrap'], 4)
shapeless('mutant_tree_sapling', ['minecraft:oak_sapling', 'fossil:bio_goo'])
shapeless('mutant_tree_tumor', ['fossil:mutant_tree_log', 'fossil:bio_goo'])
shapeless('tarred_dirt', ['minecraft:dirt', 'fossil:fossil_tar'])
shapeless('permafrost_block', ['minecraft:dirt', 'minecraft:packed_ice'])
shapeless('iced_dirt', ['minecraft:dirt', 'minecraft:ice'])
shapeless('shell', ['minecraft:nautilus_shell'])
shapeless('skull', ['minecraft:bone', 'minecraft:bone', 'fossil:relic_scrap'])
shapeless('fake_obsidian', ['minecraft:obsidian', 'fossil:relic_scrap'])
shapeless('ash_vent', ['fossil:volcanic_rock', 'minecraft:magma_block'])
shapeless('volcanic_ash', ['fossil:volcanic_rock', 'minecraft:charcoal'], 2)
shapeless('volcanic_rock', ['minecraft:basalt', 'minecraft:blackstone'], 2)
shapeless('tempskya_leaf', ['fossil:tempskya_sapling', 'minecraft:bone_meal'], 2)
shapeless('tempskya_top', ['fossil:tempskya_log', 'fossil:tempskya_leaf'])
for family in ['calamites', 'cordaites', 'palm', 'sigillaria', 'tempskya', 'mutant_tree', 'ancient_wood']:
    shapeless(family + '_planks', ['fossil:' + family + '_log'], 4)
for variant, ingredient in [('dominican', 'fossil:fossil_plant'), ('mosquito', 'fossil:bio_fossil')]:
    shapeless('amber_chunk_' + variant, ['fossil:amber_chunk', ingredient])
write('loot_table/blocks/amber_ore.json', {'type': 'minecraft:block', 'pools': [loot_pool('amber_chunk')]})
for name, pattern in [('helmet', ['BBB', 'B B']), ('chestplate', ['B B', 'BBB', 'BBB']), ('leggings', ['BBB', 'B B', 'B B']), ('boots', ['B B', 'B B'])]:
    shaped('bone_' + name, pattern, {'B': 'minecraft:bone'})

# The inactive laser pointer is the sole tool identity. The visual variant remains
# obtainable as a crafting variant, with exactly the same reusable interaction.
write('recipe/laser_pointer_active.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': ['fossil:laser_pointer'], 'result': {'id': 'fossil:laser_pointer_active'}})
write('recipe/laser_pointer_from_active.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': ['fossil:laser_pointer_active'], 'result': {'id': 'fossil:laser_pointer'}})

write('recipe/cooked_egg.json', {'type': 'minecraft:smelting', 'ingredient': '#fossil:edible_eggs', 'result': {'id': 'fossil:cooked_egg'}, 'experience': .1, 'cookingtime': 100})
write('tags/item/edible_eggs.json', {'replace': False, 'values': ['minecraft:egg'] + ['fossil:' + i for i in sorted(ITEMS) if i.startswith('egg_') and not i.startswith('egg_item_')]})

plant = json.loads((DATA / 'fossil_processing/plant_analysis.json').read_text(encoding='utf-8'))
plant['inputs'] = ['fossil:fossil_plant']
plant['results'] = [{'item': 'fossil:' + i, 'count': 1, 'weight': 1} for i in sorted(ITEMS) if i.startswith(('fossil_seed_', 'fossil_sapling_'))]
write('fossil_processing/plant_analysis.json', plant)
write('fossil_processing/fossil_seed_fern.json', {'machine': 'analyzer', 'inputs': ['fossil:fossil_seed_fern'], 'fuel': None, 'ticks': 100, 'results': [{'item': 'fossil:fern_seed', 'count': 1, 'weight': 1}]})

relic = json.loads((DATA / 'fossil_processing/relic_analysis.json').read_text(encoding='utf-8'))
for item in ['ancient_javelin', 'music_disc_bones', 'music_disc_discovery']:
    if not any(r['item'] == 'fossil:' + item for r in relic['results']): relic['results'].append({'item': 'fossil:' + item, 'count': 1, 'weight': 1})
write('fossil_processing/relic_analysis.json', relic)
write('fossil_processing/unstable_culture.json', {'machine': 'culture_vat', 'inputs': ['minecraft:rotten_flesh'], 'fuel': 'fossil:bio_goo', 'ticks': 200, 'results': [{'item': 'fossil:spawn_egg_failuresaurus', 'count': 1, 'weight': 1}]})
shapeless('spawn_egg_tar_slime', ['fossil:fossil_tar'] * 8 + ['fossil:bio_goo'])

# A second conservation pass lets players finish museum-quality artifacts.
for restored in sorted(name for name in BLOCKS if name.endswith('_restored')):
    pristine = restored.removesuffix('_restored') + '_pristine'
    if pristine in BLOCKS:
        write(f'fossil_processing/finish_{pristine}.json', {'machine': 'worktable', 'inputs': ['fossil:' + restored], 'fuel': 'fossil:pottery_shard', 'ticks': 160, 'results': [{'item': 'fossil:' + pristine, 'count': 1, 'weight': 1}]})

for species in ['elasmotherium', 'mammoth', 'therizinosaurus']:
    path = DATA / f'loot_table/entities/{species}.json'
    loot = json.loads(path.read_text(encoding='utf-8'))
    item = 'fur_' + species
    loot['pools'] = [p for p in loot['pools'] if not any(e.get('name') == 'fossil:' + item for e in p['entries'])]
    loot['pools'].append(loot_pool(item, 2))
    if species == 'mammoth' and not any(e.get('name') == 'fossil:frozen_meat' for p in loot['pools'] for e in p['entries']):
        loot['pools'].append(loot_pool('frozen_meat'))
    write(f'loot_table/entities/{species}.json', loot)
write('loot_table/entities/failuresaurus.json', {'type': 'minecraft:entity', 'pools': [loot_pool('failuresaurus_flesh', 2)]})
anu = json.loads((DATA / 'loot_table/entities/anu_boss.json').read_text(encoding='utf-8'))
if not any(e.get('name') == 'fossil:music_disc_anu' for p in anu['pools'] for e in p['entries']): anu['pools'].append(loot_pool('music_disc_anu'))
write('loot_table/entities/anu_boss.json', anu)

# Fossil variants communicate their source while remaining interchangeable inputs.
for name, fossil in [('fossil_stone', 'fossil_bio'), ('fossil_deepslate', 'fossil_shale'), ('fossil_tuff', 'fossil_shale'), ('fossil_dripstone', 'fossil_shale'), ('fossil_calcite', 'fossil_bio'), ('fossil_sandstone', 'fossil_bio'), ('fossil_red_sandstone', 'fossil_bio')]:
    write(f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'fossil:' + fossil, 'weight': 6}, {'type': 'minecraft:item', 'name': 'fossil:fossil_plant', 'weight': 2}, {'type': 'minecraft:item', 'name': 'fossil:relic_scrap', 'weight': 1}], 'condition': {'type': 'minecraft:survives_explosion'}}]})
write('loot_table/blocks/tar.json', {'type': 'minecraft:block', 'pools': [loot_pool('fossil_tar')]})

for chest in ['anu_castle', 'archeologist_house', 'aztec/aztec_temple', 'aztec/aztec_weapon_shop', 'egyptian_academy', 'hell_boat', 'paleontologist_house']:
    finds = ['bio_fossil', 'fossil_plant', 'bio_goo'] if chest == 'paleontologist_house' else ['relic_scrap', 'pottery_shard', 'stone_tablet']
    if 'house' not in chest: finds += ['ancient_key', 'broken_sword', 'broken_helmet', 'music_disc_discovery']
    write(f'loot_table/chests/{chest}.json', {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, 'entries': [{'type': 'minecraft:item', 'name': 'fossil:' + item} for item in finds]}]})

for family in ['calamites', 'cordaites', 'palm', 'sigillaria', 'tempskya', 'mutant_tree']:
    if family + '_leaves' not in BLOCKS: continue
    write(f'loot_table/blocks/{family}_leaves.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [{'type': 'minecraft:item', 'name': f'fossil:{family}_leaves', 'condition': 'minecraft:tool/can_shear'}, {'type': 'minecraft:item', 'name': f'fossil:{family}_sapling', 'condition': {'type': 'minecraft:all_of', 'terms': [{'type': 'minecraft:random_chance', 'chance': .08}, {'type': 'minecraft:survives_explosion'}]}}]}]}]})

for role in ['archeology', 'paleontology']:
    write(f'enchantment/{role}.json', {'anvil_cost': 3, 'description': {'translate': 'enchantment.fossil.' + role}, 'exclusive_set': '#minecraft:exclusive_set/mining', 'max_cost': {'base': 50, 'per_level_above_first': 9}, 'min_cost': {'base': 10, 'per_level_above_first': 9}, 'max_level': 3, 'slots': ['mainhand'], 'supported_items': '#minecraft:enchantable/mining_loot', 'weight': 3})
    for path in (DATA / 'loot_table/blocks').glob('fossil_*.json'):
        loot = json.loads(path.read_text(encoding='utf-8'))
        for pool in loot['pools']:
            for entry in loot_entries(pool['entries']):
                target = 'relic_scrap' if role == 'archeology' else ('fossil_bio', 'bio_fossil', 'fossil_shale', 'fossil_plant')
                if entry.get('name', '').split(':')[-1] in ([target] if isinstance(target, str) else target):
                    entry['modifier'] = [{'type': 'minecraft:apply_bonus', 'enchantment': 'fossil:' + role, 'formula': 'minecraft:uniform_bonus_count', 'parameters': {'bonusMultiplier': 1}}]
        write(str(path.relative_to(DATA)), loot)

profiles = json.loads((ASSETS / 'blocks.json').read_text(encoding='utf-8'))
tags = {'mineable/pickaxe': [], 'mineable/axe': [], 'mineable/shovel': [], 'logs': [], 'planks': [], 'leaves': []}
for block in profiles:
    name, kind = block['name'], block['kind']
    if kind in ['leaves']: tags['leaves'].append('fossil:' + name)
    if kind == 'pillar' and block['sound'] == 'wood': tags['logs'].append('fossil:' + name)
    if name.endswith('_planks'): tags['planks'].append('fossil:' + name)
    if 'plant' in kind or kind in ['sapling', 'leaves', 'vine', 'barrier', 'portal', 'tar']: continue
    tool = 'mineable/axe' if block['sound'] == 'wood' else 'mineable/shovel' if kind == 'sand' or name in ['volcanic_ash', 'iced_dirt', 'permafrost_block', 'tarred_dirt'] else 'mineable/pickaxe'
    tags[tool].append('fossil:' + name)
tags['mineable/pickaxe'] += ['fossil:fossil_ore', 'fossil:analyzer', 'fossil:culture_vat']
for tag, values in tags.items():
    target = DATA.parent / 'minecraft/tags/block' / (tag + '.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({'replace': False, 'values': values}, indent=2) + '\n', encoding='utf-8')
for tag in ['in_enchanting_table', 'tradeable', 'on_random_loot', 'exclusive_set/mining']:
    target = DATA.parent / 'minecraft/tags/enchantment' / (tag + '.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({'replace': False, 'values': ['fossil:archeology', 'fossil:paleontology']}, indent=2) + '\n', encoding='utf-8')
print('Updated native survival progression.')

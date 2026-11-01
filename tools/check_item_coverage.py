"""Compare the independently enumerated pinned item universe with authored routes.

The expected universe is extracted from Mekanism source registrations and reconciled
with the fixed JAR, never from this guide's list. Runtime additionally compares it
with BuiltInRegistries and the real Ponder registration. This cheap check protects
the reviewed routing plan before that expensive check.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / 'docs/item-coverage-expectations.json'
GUIDE = ROOT / 'src/main/resources/assets/mekanismponder/guide'


def routes(lessons, descriptions):
    ponder, shift = {}, {}
    for lesson in lessons:
        for item in dict.fromkeys([lesson['icon'], *lesson['components']]):
            ponder.setdefault(item, []).append(lesson['id'])
    for description in descriptions:
        for item in description['items']:
            if item in shift:
                raise ValueError(f'Duplicate Shift registration: {item}')
            shift[item] = description['id']
    return ponder, shift


def check(expectations, lessons, descriptions):
    ponder, shift = routes(lessons, descriptions)
    ids = [item['id'] for item in expectations['items']]
    if len(ids) != len(set(ids)) or len(ids) != expectations['registryCount']:
        raise ValueError('Independent registry universe is duplicated or incomplete')
    if (set(ponder) | set(shift)) - set(ids):
        raise ValueError('Registered item outside independent universe')
    for item in expectations['items']:
        key = item['id']
        if ponder.get(key, []) != item['ponder'] or shift.get(key) != item['description']:
            raise ValueError(f'Item entry differs from reviewed plan: {key}')
        if item.get('exclusion') and key != 'mekanism:bounding_block':
            raise ValueError(f'Unapproved internal exclusion: {key}')
        if not item['ponder'] and not item['description']:
            native, excluded = item.get('nativeTooltip'), item.get('exclusion')
            module = (key.startswith('mekanism:module_') and key != 'mekanism:module_base'
                      and native and native['kind'] == 'always'
                      and native['translationKey'] == 'description.mekanism.' + key.split(':')[1][7:])
            upgrade = (key in {'mekanism:upgrade_chemical', 'mekanism:upgrade_filter',
                               'mekanism:upgrade_muffling', 'mekanism:upgrade_stone_generator'}
                       and native and native['kind'] == 'shift'
                       and native['translationKey'] == 'upgrade.mekanism.' + key.split(':')[1][8:] + '.description')
            if not excluded and not (module or upgrade):
                raise ValueError(f'Unexplained item with no entry: {key}')
    # These identities come from the fixed source FactoryType/FactoryTier enums,
    # independently of the guide. The common factory lesson teaches parallel slots
    # and upgrades; process-specific lessons/descriptions retain the recipe identity.
    process = {'enriching': 'enrichment', 'smelting': 'enrichment', 'infusing': 'infusion',
               'crushing': 'ore_tripling', 'purifying': 'ore_tripling', 'injecting': 'ore_quadrupling'}
    short = {'compressing': 'compressor', 'combining': 'combiner', 'sawing': 'sawmill'}
    for tier in ('basic', 'advanced', 'elite', 'ultimate'):
        for kind in (*process, *short):
            key = f'mekanism:{tier}_{kind}_factory'
            if 'factories' not in ponder.get(key, []):
                raise ValueError(f'Factory missing common operations: {key}')
            if kind in process and process[kind] not in ponder[key]:
                raise ValueError(f'Factory missing matching process: {key}')
            if kind in short and shift.get(key) != 'coverage_' + short[kind]:
                raise ValueError(f'Factory missing distinct process description: {key}')
    # Exactly the seven primary resources with these processing stages in the
    # pinned RecipeProvider. Diamond/fluorite/netherite do not share this chain.
    for metal in ('iron', 'gold', 'copper', 'osmium', 'tin', 'lead', 'uranium'):
        for prefix, lesson in [('clump', 'ore_tripling'), ('dirty_dust', 'ore_tripling'),
                               ('shard', 'ore_quadrupling'), ('crystal', 'ore_quintupling'),
                               ('dust', 'enrichment')]:
            key = f'mekanism:{prefix}_{metal}'
            if lesson not in ponder.get(key, []):
                raise ValueError(f'Processing intermediate missing matching stage: {key}')
    return {'passed': True, 'registryItems': len(ids), 'ponderItems': len(ponder),
            'shiftItems': len(shift), 'both': len(set(ponder) & set(shift)),
            'nativeOnly': sum(not x['ponder'] and not x['description'] and bool(x.get('nativeTooltip'))
                              for x in expectations['items']),
            'excluded': sum(bool(x.get('exclusion')) for x in expectations['items'])}


if __name__ == '__main__':
    read = lambda path: json.loads(path.read_text(encoding='utf8'))
    result = check(read(MANIFEST), read(GUIDE/'lessons.json'), read(GUIDE/'item_descriptions.json'))
    print(json.dumps(result))

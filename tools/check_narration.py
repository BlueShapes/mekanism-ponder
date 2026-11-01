"""Audit the historical 0.1.4 narration release against the accepted 0.1.3 data.

Protect models, chapter/item identity, quantities and named mechanisms while
allowing reviewed, language-specific narrative phrasing and unit boundaries.
Grammar and meaning remain author reviews, not imperative-word-count tests.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

from guide_data import ROOT, content_source, content_hash

BASELINE = '2a05f83'
NARRATION_RELEASE = '3bc230a'
GUIDE = 'src/main/resources/assets/mekanismponder/guide/'
QUANTITY = re.compile(r'\d+(?:[.,/:×–〜-]\d+)*|%(?:\d+\$)?[sd]|(?:minecraft|mekanism):[a-z0-9_./-]+')
NAMES = ('Metallurgic Infuser', 'Universal Cable', 'Configurator', 'Enriched Iron', 'Steel Dust',
         'Steel Ingot', 'Electric Pump', 'Mechanical Pipe', 'Pressurized Tube', 'Chemical Tank',
         'Logistical Transporter', 'Logistical Sorter', 'Enrichment Chamber', 'Energized Smelter',
         'Electrolytic Separator', 'Dumping Excess', 'Thermal Evaporation Plant', 'Resistive Heater',
         'Chemical Infuser', 'Chemical Injection Chamber', 'Chemical Oxidizer', 'Rotary Condensentrator',
         'Pressurized Reaction Chamber', 'HDPE Pellet', 'Purification Chamber', 'Dirty Iron Dust',
         'Chemical Dissolution Chamber', 'Chemical Washer', 'Chemical Crystallizer', 'Induction Casing',
         'Digital Miner', 'Silk Touch', 'QIO Drive Array', 'QIO Dashboard', 'QIO Importer', 'QIO Exporter',
         'Teleporter Frame', 'Portable Teleporter', 'Formulaic Assemblicator', 'Crafting Formula',
         'Radioactive Waste Barrel', 'Mekanism: Generators', 'Solar Neutron Activator',
         'Isotopic Centrifuge', 'Gauge Dropper', 'Refined Obsidian Dust', 'Enriched Obsidian')


def read(path, ref=None):
    if ref:
        return json.loads(subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=ROOT))
    return json.loads((ROOT / path).read_text(encoding='utf8'))


def sources(ref=None):
    return read(GUIDE + 'lessons.json', ref), read(GUIDE + 'item_descriptions.json', ref), read('src/main/templates/ui.json', ref)


def structure(lessons, descriptions):
    lessons = json.loads(json.dumps(lessons))
    descriptions = json.loads(json.dumps(descriptions))
    for lesson in lessons:
        for field in ('title', 'summary'):
            lesson.pop(field)
        for step in lesson['steps']:
            step.pop('text')
    for description in descriptions:
        description.pop('text')
    return lessons, descriptions


def check():
    before, after = sources(BASELINE), sources(NARRATION_RELEASE)
    assert structure(*before[:2]) == structure(*after[:2]), 'Narration changed models, routing, chapters or item identity'
    old_source = {**before[2], **content_source(*before)}
    new_source = {**after[2], **content_source(*after)}
    assert set(old_source) == set(new_source), 'Narration changed teaching keys'
    changes = []
    for key, old in old_source.items():
        new = new_source[key]
        for language in ('ja', 'en'):
            assert sorted(QUANTITY.findall(old[language])) == sorted(QUANTITY.findall(new[language])), f'Quantity/ID changed: {key}/{language}'
        for name in NAMES:
            if name.lower() in old['en'].lower():
                assert name.lower() in new['en'].lower(), f'Named mechanism dropped: {name}: {key}'
        if old != new:
            changes.append({'key':key, 'before':old, 'after':new})
    owned = after[0]
    refs = [lesson['icon'] for lesson in owned] + [item for lesson in owned for item in lesson['components']]
    refs += [node['block'] for lesson in owned for node in lesson['nodes']]
    refs += [callout['item'] for lesson in owned for step in lesson['steps'] for callout in step.get('callouts',[])]
    assert all(not lesson.get('requiresMods') for lesson in owned), 'Unexpected new optional lesson'
    assert set(ref.split(':')[0] for ref in refs) <= {'minecraft','mekanism'}, 'Unexpected add-on element'
    translation_audit = {}
    old_navigation = read('src/main/translations/navigation.json', BASELINE)['translations']
    new_navigation = read('src/main/translations/navigation.json', NARRATION_RELEASE)['translations']
    for locale, old_nav in old_navigation.items():
        old_values = {**old_nav, **read(f'src/main/translations/content/{locale}.json', BASELINE)['translations']}
        new_values = {**new_navigation[locale], **read(f'src/main/translations/content/{locale}.json', NARRATION_RELEASE)['translations']}
        assert set(old_values) == set(new_values), f'Translation keys changed: {locale}'
        for key, text in old_values.items():
            assert sorted(QUANTITY.findall(text)) == sorted(QUANTITY.findall(new_values[key])), f'Translated quantity/ID changed: {locale}/{key}'
        translation_audit[locale] = {'reviewed_keys':len(new_values),
            'changed_keys':sum(text != new_values[key] for key,text in old_values.items()),
            'boundary_changes':[key for key,text in old_values.items() if text.count('\n') != new_values[key].count('\n')],
            'quantities_and_identifiers_preserved':True}
    report = {'baseline':BASELINE, 'passed':True, 'reviewed_source_keys':len(new_source),
              'changed_source_keys':len(changes), 'geometry_and_identity_unchanged':True,
              'quantities_and_named_mechanisms_preserved':True,
              'source_hash':content_hash(content_source(*after)),
              'nomenclature_correction':{'locale':'ja_jp','key':'mekanismponder.ponder.shared.nuclear_waste_1',
                  'before':'太陽中性子照射装置','after':'太陽中性子反応機',
                  'registry_key':'block.mekanism.solar_neutron_activator',
                  'evidence':'Mekanism 10.7.19.85 assets/mekanism/lang/ja_jp.json; same underlying machine ID'},
              'translations':translation_audit,
              'addon_audit':{'lessons':len(owned),'optional_lessons':0,'chapters':6,
                             'namespaces':sorted(set(ref.split(':')[0] for ref in refs)),
                             'new_third_party_mods':[],
                             'scope':'Core Mekanism only; Generators is mentioned as a separate mod, not taught'},
              'changes':changes}
    if len(sys.argv) == 2:
        Path(sys.argv[1]).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(f'Narration source: PASS ({len(new_source)} keys reviewed, {len(changes)} changed; models, IDs and quantities preserved)')


if __name__ == '__main__':
    check()

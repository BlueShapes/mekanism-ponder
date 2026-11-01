"""Prove the historical 0.1.3 prose migration against the reviewed 0.1.2 snapshot.

This release audit is not a runtime or generation-time sentence splitter.
It permits only ordinary-stop removal, authored tooltip sentence boundaries,
one removed button label, and the renamed world-required command key.
"""
import json
import subprocess
import sys
from pathlib import Path

from caption_units import FULL_STOP_CHARACTERS, full_stops
from guide_data import ROOT, content_source

BASELINE = '993ed06'
MIGRATED = '2a05f83'


def read(path, ref=None):
    if ref:
        return json.loads(subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=ROOT))
    return json.loads((ROOT / path).read_text(encoding='utf8'))


def all_text(ref=None):
    guide = 'src/main/resources/assets/mekanismponder/guide/'
    ui = read('src/main/templates/ui.json', ref)
    source = {**ui, **content_source(read(guide + 'lessons.json', ref),
                                   read(guide + 'item_descriptions.json', ref), ui)}
    result = {locale: {key: value[field] for key, value in source.items()}
              for locale, field in [('ja_jp', 'ja'), ('en_us', 'en')]}
    nav = read('src/main/translations/navigation.json', ref)['translations']
    for locale, values in nav.items():
        result[locale] = {**values, **read(f'src/main/translations/content/{locale}.json', ref)['translations']}
    return result


def migrated(text, description):
    for stop in reversed(full_stops(text)):
        end = stop.start()
        while end < stop.end() and text[end] in FULL_STOP_CHARACTERS:
            end += 1
        text = text[:stop.start()] + text[end:]
        if description and text[stop.start():].strip():
            text = text[:stop.start()] + '\n' + text[stop.start():].lstrip()
    return text


def check():
    # Narration changes in 0.1.4 are a separate audit, not punctuation-only evidence.
    previous, current = all_text(BASELINE), all_text(MIGRATED)
    reports = {}
    for locale, values in previous.items():
        expected, removed, changed = {}, 0, 0
        for key, text in values.items():
            if key == 'mekanismponder.button.guide':
                continue
            renamed = key.replace('mekanismponder.button.requires_world', 'mekanismponder.command.requires_world')
            result = migrated(text, key.startswith('mekanismponder.description.'))
            expected[renamed] = result
            removed += sum(sum(char in FULL_STOP_CHARACTERS for char in stop[0]) for stop in full_stops(text))
            changed += result != text
        assert current[locale] == expected, f'Unexpected prose change beyond punctuation/boundaries: {locale}'
        text = ''.join(current[locale].values())
        reports[locale] = {'keys': len(expected), 'changedStrings': changed, 'removedFullStops': removed,
                           'retainedCommas': sum(text.count(mark) for mark in ',、，،'),
                           'retainedDots': text.count('.'), 'retainedContinuationMarks': text.count('…')}
    report = {'baseline': BASELINE, 'passed': True, 'locales': reports,
              'allowedChanges': ['ordinary full stops removed', 'tooltip sentence boundaries authored as LF',
                                 'unused button label removed', 'world-required key renamed to command']}
    if len(sys.argv) == 2:
        Path(sys.argv[1]).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(f'Punctuation migration: PASS ({len(reports)} locales, '
          f'{sum(row["removedFullStops"] for row in reports.values())} stops removed; all other prose preserved)')


if __name__ == '__main__':
    check()

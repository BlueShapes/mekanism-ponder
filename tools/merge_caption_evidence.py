"""Combine real-font reports only where their measured text is still current."""
import gzip
import hashlib
import json
import sys
import zipfile
from pathlib import Path

from guide_data import content_source, load, load_content, content_hash, require

WIDTHS = (320, 480, 960)
CAPTION_WIDTH = 180


def merge_measurements(values, reports):
    expected = {}
    for locale, translations in values.items():
        for key, text in translations.items():
            if not key.startswith('mekanismponder.ponder.shared.'):
                continue
            lesson, _, step = key.removeprefix('mekanismponder.ponder.shared.').rpartition('_')
            for unit, body in enumerate(text.split('\n')):
                for gui in WIDTHS:
                    expected[(locale, lesson, int(step), unit, gui)] = body
    measured = {}
    for report in reports:
        require(report['nativeCaptionWidth'] == CAPTION_WIDTH and tuple(report['guiWidths']) == WIDTHS,
                'Font report used different window bounds')
        require(all(issue['type'] in ('line-count', 'glyph-overflow') for issue in report.get('issues', [])),
                'Resolve non-width font/boundary issues before combining evidence')
        for row in report['measurements']:
            key = (row['locale'], row['lesson'], row['step'], row['unit'], row['guiWidth'])
            # A changed sentence or shifted unit cannot inherit an old measurement.
            if expected.get(key) != row['text']:
                continue
            # A newer failure for the same current text invalidates an earlier pass.
            measured.pop(key, None)
            if row['captionWidth'] != CAPTION_WIDTH or not 1 <= row['lineCount'] <= 3:
                continue
            if len(row['visualGlyphWidths']) != row['lineCount'] or max(row['visualGlyphWidths']) > CAPTION_WIDTH:
                continue
            measured[key] = row
    missing = set(expected) - set(measured)
    require(not missing, f'Missing current valid font measurements: {len(missing)}; first={next(iter(missing), None)}')
    return {'passed':True, 'nativeCaptionWidth':CAPTION_WIDTH, 'guiWidths':list(WIDTHS),
            'unitsChecked':len(expected) // len(WIDTHS), 'guiWidthChecks':len(expected),
            'maximumLines':max(row['lineCount'] for row in measured.values()), 'issues':[],
            'measuredLocales':list(values), 'measurements':list(measured.values()),
            'localeTextHashes':{locale:content_hash({k:v for k,v in text.items() if k.startswith('mekanismponder.ponder.shared.')})
                                for locale,text in values.items()}}


def main():
    require(len(sys.argv) == 6, 'Usage: merge_caption_evidence.py <first-report> <final-report> <first-jar> <final-jar> <output.json.gz>')
    first, final, first_jar, final_jar, output = map(Path, sys.argv[1:])
    # Main rendering/layout bytecode must be identical between these two runs.
    with zipfile.ZipFile(first_jar) as old, zipfile.ZipFile(final_jar) as new:
        classes = {name for name in old.namelist() if name.endswith('.class')}
        require(classes == {name for name in new.namelist() if name.endswith('.class')}
                and all(old.read(name) == new.read(name) for name in classes), 'Rendering bytecode changed between font runs')
    data = load()
    source = content_source(*data)
    values = {locale:{key:body[field] for key,body in source.items()} for locale,field in [('ja_jp','ja'),('en_us','en')]}
    values.update(load_content(*data))
    reports = [json.loads(path.read_text(encoding='utf8')) for path in (first, final)]
    require(reports[-1]['passed'], 'Latest targeted font run did not pass')
    result = merge_measurements(values, reports)
    result['sourceReports'] = [{'file':path.name,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
                                for path in (first,final)]
    result['renderingBytecodeUnchanged'] = True
    result['jarSha256'] = hashlib.sha256(final_jar.read_bytes()).hexdigest()
    output.write_bytes(gzip.compress((json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n').encode('utf8'),mtime=0))
    print(f'Current font evidence: PASS ({len(values)} locales, {result["unitsChecked"]} units, '
          f'{result["guiWidthChecks"]} bounds, max {result["maximumLines"]} lines)')


if __name__ == '__main__':
    main()

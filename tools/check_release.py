"""Inspect a completed main JAR without starting Minecraft."""
import json
import struct
import sys
import tomllib
import zipfile
from pathlib import Path

from guide_data import content_source, load, load_content, load_navigation, load_presentation, require


def check(path):
    lessons, descriptions, ui = load()
    source = {**ui, **content_source(lessons, descriptions, ui)}
    expected = {locale: {key: value[field] for key, value in source.items()}
                for locale, field in [('ja_jp', 'ja'), ('en_us', 'en')]}
    content = load_content(lessons, descriptions, ui)
    expected.update({locale: {**values, **content[locale]} for locale, values in load_navigation(ui).items()})
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        require(jar.testzip() is None, 'Corrupt JAR entry')
        require(not any('validation/' in name or name.endswith('GuideScreen.class') or name.endswith('.jar')
                        for name in names), 'Validation, obsolete screen, or dependency JAR bundled')
        require(not any(name.startswith(('mekanism/', 'net/createmod/', 'assets/mekanism/')) for name in names),
                'Dependency classes/assets bundled')
        metadata = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode('utf-8'))
        require(len(metadata['mods']) == 1 and metadata['mods'][0]['modId'] == 'mekanismponder', 'Wrong mod identity')
        require(metadata['mods'][0]['version'] == '0.1.6', 'Wrong release version')
        require(json.loads(jar.read('assets/mekanismponder/guide/presentation.json')) ==
                {'locales': load_presentation(lessons, descriptions, ui)}, 'Packaged presentation differs from authors')
        for resource in ('LICENSE', 'THIRD_PARTY_NOTICES.md', 'LICENSES/MIT-Ponder.txt', 'LICENSES/MIT-Mekanism.txt',
                         'mekanismponder.mixins.json', 'assets/mekanismponder/ponder/stage.nbt'):
            require(resource in names, f'Missing release resource: {resource}')
        locale_names = {Path(name).stem for name in names if name.startswith('assets/mekanismponder/lang/') and name.endswith('.json')}
        require(locale_names == set(expected), 'Incomplete packaged locale roster')
        for locale, values in expected.items():
            packaged = json.loads(jar.read(f'assets/mekanismponder/lang/{locale}.json'))
            require(packaged == values, f'Packaged translations differ from source: {locale}')
        classes = [name for name in names if name.endswith('.class')]
        require(classes and all(struct.unpack('>H', jar.read(name)[6:8])[0] == 65 for name in classes),
                'Expected Java 21 class files')
    print(f'Release JAR: PASS ({len(expected)} locales, {len(source)} keys each, {len(classes)} Java 21 classes)')


if __name__ == '__main__':
    require(len(sys.argv) == 2, 'Usage: python tools/check_release.py <main-jar>')
    check(Path(sys.argv[1]))

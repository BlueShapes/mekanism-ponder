"""Validate bilingual teaching sources and 22 translations; generate resources.

Only Python's standard library is needed. Generated assets live under build/.
"""
import gzip
import hashlib
import json
import math
import re
import struct
import sys
from pathlib import Path

from caption_units import authored_units, full_stops, validate_caption

ROOT = Path(__file__).resolve().parents[1]
GUIDE = ROOT / "src/main/resources/assets/mekanismponder/guide"
CHAPTERS = {"start", "transport", "processing", "chemistry", "advanced", "troubleshooting"}
RESOURCE = re.compile(r"(?:minecraft|mekanism):[a-z0-9_./-]+\Z")
IDENTIFIER = re.compile(r"[a-z0-9_]+\Z")
TRANSLATIONS = ROOT / "src/main/translations"
NAVIGATION_KEYS = {"mekanismponder.command.unknown_chapter",
                   "mekanismponder.command.unknown_item"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def localized(value, context):
    require(isinstance(value, dict) and set(value) == {"ja", "en"}, f"Missing bilingual text: {context}")
    require(all(isinstance(v, str) and v.strip() for v in value.values()), f"Empty text: {context}")
    require(not any(full_stops(text) for text in value.values()), f"Sentence-ending full stop: {context}")
    # Keep formatting arguments equivalent in both source languages.
    arguments = [sorted(re.findall(r"%(?:\d+\$)?[sd]", value[lang])) for lang in ("ja", "en")]
    require(arguments[0] == arguments[1], f"Mismatched placeholders: {context}")


def resource(value, context):
    require(isinstance(value, str) and RESOURCE.fullmatch(value), f"Invalid resource: {context}: {value}")


def navigation_source(ui):
    return {key: value for key, value in sorted(ui.items())
            if key.startswith("mekanismponder.ponder.tag.") or key in NAVIGATION_KEYS}


def navigation_hash(ui):
    source = json.dumps(navigation_source(ui), ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(source.encode("utf-8")).hexdigest()


def validate_navigation(ui, languages, bundle):
    source = navigation_source(ui)
    require(len(source) == 14, "Navigation source must contain six chapter titles/descriptions and two command messages")
    require(len(languages) == 22 and {"ja_jp", "en_us"} <= set(languages), "Expected the shared 22-language scope")
    require(bundle.get("source_hash") == navigation_hash(ui), "Navigation translations need source review")
    translations = bundle.get("translations", {})
    require(set(translations) == set(languages) - {"ja_jp", "en_us"}, "Missing or unexpected navigation locale")
    for locale, values in translations.items():
        require(set(values) == set(source), f"Missing or unexpected navigation key: {locale}")
        for key, value in values.items():
            require(isinstance(value, str) and value.strip(), f"Empty navigation translation: {locale}/{key}")
            require(not full_stops(value), f"Sentence-ending full stop: {locale}/{key}")
            expected = sorted(re.findall(r"%(?:\d+\$)?[sd]", source[key]["en"]))
            require(sorted(re.findall(r"%(?:\d+\$)?[sd]", value)) == expected,
                    f"Mismatched navigation placeholders: {locale}/{key}")
    return translations


def load_navigation(ui):
    languages = json.loads((TRANSLATIONS / "languages.json").read_text(encoding="utf-8"))
    shared = ROOT.parent / "docs/languages.json"
    if shared.is_file():
        require(languages == json.loads(shared.read_text(encoding="utf-8")), "Local languages differ from the shared policy")
    bundle = json.loads((TRANSLATIONS / "navigation.json").read_text(encoding="utf-8"))
    return validate_navigation(ui, languages, bundle)


def content_source(lessons, descriptions, ui):
    source = {key: value for key, value in ui.items() if key not in navigation_source(ui)}
    for lesson in lessons:
        source[f"mekanismponder.ponder.{lesson['id']}.header"] = lesson['title']
        source[f"mekanismponder.lesson.{lesson['id']}.summary"] = lesson['summary']
        for index, step in enumerate(lesson['steps']):
            source[f"mekanismponder.ponder.shared.{lesson['id']}_{index}"] = step['text']
    for entry in descriptions:
        source[f"mekanismponder.description.{entry['id']}"] = entry['text']
    return source


def content_hash(source):
    canonical = json.dumps(source, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode('utf-8')).hexdigest()


def validate_content(source, bundle, locale):
    require(bundle.get('source_hash') == content_hash(source), f"Content translations need source review: {locale}")
    values = bundle.get('translations', {})
    require(set(values) == set(source), f"Missing or unexpected content key: {locale}")
    for key, value in values.items():
        require(isinstance(value, str) and value.strip(), f"Empty content translation: {locale}/{key}")
        require(not full_stops(value), f"Sentence-ending full stop: {locale}/{key}")
        if key.startswith('mekanismponder.ponder.shared.'):
            validate_caption(value, locale, key)
        expected = sorted(re.findall(r"%(?:\d+\$)?[sd]", source[key]['en']))
        require(sorted(re.findall(r"%(?:\d+\$)?[sd]", value)) == expected,
                f"Mismatched content placeholders: {locale}/{key}")
        # Keep quantities/ratios and resource identities; localize surrounding prose.
        for token in re.findall(r"\d+(?:[./:]\d+)*|(?:minecraft|mekanism):[a-z0-9_./-]+", source[key]['en']):
            require(token in value, f"Missing source number/ID {token}: {locale}/{key}")
    return values


def load_content(lessons, descriptions, ui):
    source = content_source(lessons, descriptions, ui)
    locales = set(load_navigation(ui))
    folder = TRANSLATIONS / 'content'
    require({path.stem for path in folder.glob('*.json')} == locales, 'Missing or unexpected content locale')
    return {locale: validate_content(source, json.loads((folder / f'{locale}.json').read_text(encoding='utf-8')), locale)
            for locale in sorted(locales)}


def presentation_text(values):
    return {key: value for key, value in values.items()
            if key.startswith(('mekanismponder.ponder.shared.', 'mekanismponder.description.'))}


def validate_presentation(values, bundle, locale):
    """Every locale owns its unit intentions, even when its boundaries differ."""
    values = presentation_text(values)
    require(bundle.get('text_hash') == content_hash(values), f'Presentation needs text review: {locale}')
    units = bundle.get('units', {})
    require(set(units) == set(values), f'Missing or unexpected presentation key: {locale}')
    for key, text in values.items():
        validate_caption(text, locale, key)
        intents = units[key]
        require(isinstance(intents, list) and len(intents) == len(authored_units(text)),
                f'Presentation unit count differs from authored boundaries: {locale}/{key}')
        require(all(intent in ('NORMAL', 'WARNING', 'HINT') for intent in intents),
                f'Unknown presentation intent: {locale}/{key}')
    return units


def load_presentation(lessons, descriptions, ui):
    source = content_source(lessons, descriptions, ui)
    values = {locale: {key: text[field] for key, text in source.items()}
              for locale, field in [('ja_jp', 'ja'), ('en_us', 'en')]}
    values.update(load_content(lessons, descriptions, ui))
    folder = TRANSLATIONS / 'presentation'
    require({path.stem for path in folder.glob('*.json')} == set(values), 'Incomplete presentation locale roster')
    return {locale: validate_presentation(text, json.loads((folder / f'{locale}.json').read_text(encoding='utf8')), locale)
            for locale, text in values.items()}


def validate(lessons, descriptions, ui):
    require(isinstance(lessons, list) and lessons, "Empty catalogue")
    ids = set()
    chapters = set()
    for lesson in lessons:
        name = lesson["id"]
        require(IDENTIFIER.fullmatch(name) and name not in ids, f"Duplicate/invalid lesson ID: {name}")
        ids.add(name)
        require(lesson["chapter"] in CHAPTERS, f"Invalid chapter: {name}")
        chapters.add(lesson["chapter"])
        for field in ("title", "summary"):
            localized(lesson[field], f"{name}/{field}")
        require(not lesson.get("requiresMods"), f"Core lesson must not require an add-on: {name}")
        require(lesson["components"] and len(set(lesson["components"])) == len(lesson["components"]), f"Invalid components: {name}")
        for item in [lesson["icon"], *lesson["components"]]:
            resource(item, name)
        nodes = set()
        positions = set()
        for node in lesson["nodes"]:
            require(IDENTIFIER.fullmatch(node["id"]) and node["id"] not in nodes, f"Duplicate/invalid node: {name}")
            nodes.add(node["id"])
            resource(node["block"], name)
            pos = node["pos"]
            require(len(pos) == 3 and all(type(p) is int for p in pos)
                    and 0 <= pos[0] <= 8 and 1 <= pos[1] <= 4 and 0 <= pos[2] <= 8, f"Out of bounds: {name}")
            require(tuple(pos) not in positions, f"Overlapping nodes: {name}")
            positions.add(tuple(pos))
        require(nodes and len(lesson["steps"]) >= 4, f"Incomplete lesson: {name}")
        visible, revealed = set(), set()
        callout_count = 0
        for i, step in enumerate(lesson["steps"]):
            context = f"{name}/{i}"
            localized(step["text"], context)
            for language, text in step['text'].items():
                validate_caption(text, language, context)
            show, hide, focus = [set(step.get(field, [])) for field in ("show", "hide", "focus")]
            require(not show & hide, f"Simultaneous show/hide: {context}")
            require(not show & visible, f"Already visible node: {context}")
            require(hide <= visible, f"Hiding invisible node: {context}")
            visible = (visible - hide) | show
            revealed |= show
            refs = show | hide | focus | set(step.get("nbt", {}))
            require(refs <= nodes, f"Unknown node: {context}")
            require(focus <= visible, f"Focus on invisible node: {context}")
            callouts = step.get("callouts", [])
            require(len(callouts) <= 3, f"Too many callouts: {context}")
            callout_count += len(callouts)
            for callout in callouts:
                require(callout["anchor"] in visible, f"Hidden callout anchor: {context}")
                resource(callout["item"], context)
                action = callout.get("action", "none")
                require(action in ("none", "right_click", "left_click", "scroll"), f"Invalid action: {context}")
                require(not callout.get("sneak") or action != "none", f"Sneak without action: {context}")
                require(callout.get("pointing", "down") in ("down", "up", "left", "right"), f"Invalid pointing: {context}")
                offset = callout.get("offset", [0, .25, 0])
                require(len(offset) == 3 and all(isinstance(x, (float, int)) and math.isfinite(x) and abs(x) <= 2 for x in offset), f"Invalid offset: {context}")
            if flow := step.get("flow"):
                require({flow["from"], flow["to"]} <= visible, f"Hidden flow endpoint: {context}")
                resource(flow["item"], context)
        require(revealed == nodes and callout_count, f"Unshown node or missing callouts: {name}")
    require(chapters == CHAPTERS, "Missing chapter")
    description_ids, items = set(), set()
    for entry in descriptions:
        require(IDENTIFIER.fullmatch(entry["id"]) and entry["id"] not in description_ids, "Duplicate/invalid description")
        description_ids.add(entry["id"])
        localized(entry["text"], entry["id"])
        require(entry["items"], "Empty description item list")
        for item in entry["items"]:
            resource(item, entry["id"])
            require(item not in items, f"Duplicate described item: {item}")
            items.add(item)
    for key, value in ui.items():
        require(key.startswith("mekanismponder."), f"Foreign UI key: {key}")
        localized(value, key)


def load():
    def read(path):
        return json.loads(path.read_text(encoding="utf-8"))
    data = (read(GUIDE / "lessons.json"), read(GUIDE / "item_descriptions.json"),
            read(ROOT / "src/main/templates/ui.json"))
    validate(*data)
    load_navigation(data[2])
    load_content(*data)
    load_presentation(*data)
    return data


def stage():
    def text(value):
        raw = value.encode("utf-8")
        return struct.pack(">H", len(raw)) + raw
    def tag(kind, name, value):
        return bytes([kind]) + text(name) + value
    def integer(name, value):
        return tag(3, name, struct.pack(">i", value))
    def integers(name, values):
        return tag(9, name, bytes([3]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values))
    def compounds(name, values):
        return tag(9, name, bytes([10]) + struct.pack(">i", len(values)) + b"".join(values))
    palette = [tag(8, "Name", text(block)) + b"\0" for block in ("minecraft:smooth_stone", "minecraft:air")]
    blocks = [integer("state", int(y != 0)) + integers("pos", [x, y, z]) + b"\0"
              for x in range(9) for y in range(5) for z in range(9)]
    # Explicit air gives Ponder its complete virtual-world bounds.
    root = (integer("DataVersion", 3955) + integers("size", [9, 5, 9]) + compounds("palette", palette)
            + compounds("blocks", blocks) + compounds("entities", []) + b"\0")
    return gzip.compress(tag(10, "", root), mtime=0)


def generate(destination):
    lessons, descriptions, ui = load()
    for locale, field in (("ja_jp", "ja"), ("en_us", "en")):
        lang = {key: value[field] for key, value in ui.items()}
        for lesson in lessons:
            prefix = "mekanismponder.ponder."
            lang[prefix + lesson["id"] + ".header"] = lesson["title"][field]
            lang[f'mekanismponder.lesson.{lesson["id"]}.summary'] = lesson["summary"][field]
            for i, step in enumerate(lesson["steps"]):
                lang[f'{prefix}shared.{lesson["id"]}_{i}'] = step["text"][field]
        for entry in descriptions:
            lang[f'mekanismponder.description.{entry["id"]}'] = entry["text"][field]
        output = destination / f"assets/mekanismponder/lang/{locale}.json"
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    content = load_content(lessons, descriptions, ui)
    for locale, values in load_navigation(ui).items():
        output = destination / f"assets/mekanismponder/lang/{locale}.json"
        values = {**values, **content[locale]}
        output.write_text(json.dumps(values, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    output = destination / "assets/mekanismponder/ponder/stage.nbt"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(stage())
    roster = destination / 'assets/mekanismponder/guide/languages.json'
    roster.parent.mkdir(parents=True, exist_ok=True)
    roster.write_text((TRANSLATIONS / 'languages.json').read_text(encoding='utf-8'), encoding='utf-8')
    (roster.parent / 'presentation.json').write_text(
        json.dumps({'locales': load_presentation(lessons, descriptions, ui)}, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(f"Validated {len(lessons)} lessons, {sum(len(x['steps']) for x in lessons)} steps, {len(descriptions)} descriptions; generated complete 22-language resources and stage")


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "generate":
        generate(Path(sys.argv[2]))
    elif sys.argv[1:] == ["check"]:
        load()
        print("Guide data: PASS")
    else:
        raise SystemExit("Usage: python tools/guide_data.py check | generate <output-directory>")

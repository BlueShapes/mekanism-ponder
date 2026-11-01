"""Authoring checks for explicit newline-separated caption units.

This is a diagnostic, never a runtime sentence/page splitter. Translators choose
each boundary in their own language. Real font fit is checked by the harness.
"""
import re


_STOP = re.compile(r'(?:[。．｡！？।॥؟۔]+[”’"\)»」』]*|[.!?]+[”’"\)»」』]*(?=\s|$))')
FULL_STOP_CHARACTERS = '.。．｡।॥۔'
_ABBREVIATION = re.compile(
    r'\b(?:e\.g\.|i\.e\.|z\.B\.|d\.h\.|p\.ex\.|p\.ej\.|'
    r'(?:Dr|Mr|Mrs|Ms|Prof|Sr|Jr|vs|etc)\.)', re.IGNORECASE)
_RESOURCE = re.compile(r'\b[a-z0-9_]+:[a-z0-9_./-]*[a-z0-9_/-]')
_URL_OR_FILE = re.compile(r'https?://[^\s<>"“”]+|\b[\w.-]+\.(?:json|toml|jar|nbt|cfg|txt|png|zip)\b', re.IGNORECASE)
_INCOMPLETE_ENGLISH_END = re.compile(r'\b(?:a|an|the|does|do|is|are|was|were|has|have|'
                                    r'not|to|of|from|with|into|can|may|must|should|will|would)$', re.IGNORECASE)


def authored_units(text):
    """Return authored units; never infer a split from punctuation or length."""
    return text.split('\n')


def sentence_stops(text):
    protected = _ABBREVIATION.sub(lambda match: match[0].replace('.', '\u2060'), text)
    protected = re.sub(r'(?<=\d)\.(?=\d)', '\u2060', protected)
    protected = _RESOURCE.sub(lambda match: match[0].replace('.', '\u2060'), protected)
    protected = _URL_OR_FILE.sub(lambda match: match[0].rstrip('.').replace('.', '\u2060')
                                + match[0][len(match[0].rstrip('.')):], protected)
    return list(_STOP.finditer(protected))


def full_stops(text):
    """Ordinary prose stops only; retain significant dots and continuation marks."""
    return [stop for stop in sentence_stops(text) if stop[0][0] in FULL_STOP_CHARACTERS]


def validate_caption(text, locale, context):
    units = authored_units(text)
    for index, unit in enumerate(units):
        label = f'{locale}/{context}/unit {index + 1}'
        if not unit or unit != unit.strip() or '\r' in unit:
            raise ValueError(f'Empty or padded authored caption: {label}')
        leading, trailing = unit.startswith('…'), unit.endswith('…')
        if leading != (index > 0 and units[index - 1].endswith('…')):
            raise ValueError(f'Unmatched continuation start: {label}')
        if trailing != (index + 1 < len(units) and units[index + 1].startswith('…')):
            raise ValueError(f'Missing continuation: {label}')
        body = unit[1:] if leading else unit
        body = body[:-1] if trailing else body
        if not body.strip() or body.strip('… .') == '':
            raise ValueError(f'Isolated continuation mark: {label}')
        stops = sentence_stops(body)
        if full_stops(body):
            raise ValueError(f'Sentence-ending full stop in authored caption: {label}')
        if locale in ('en', 'en_us') and _INCOMPLETE_ENGLISH_END.search(body.strip()):
            raise ValueError(f'Unnatural English caption fragment: {label}')
        if trailing:
            if stops:
                raise ValueError(f'Complete sentence joined to a continuation: {label}')
        elif stops and (len(stops) != 1 or stops[-1].end() != len(body)):
            raise ValueError(f'Multiple sentences in one authored unit: {label}')
    return units

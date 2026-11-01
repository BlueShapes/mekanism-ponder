import copy
import gzip
import json
import unittest
from guide_data import TRANSLATIONS, content_hash, content_source, load, navigation_hash, stage, validate, validate_content, validate_navigation


class GuideDataTests(unittest.TestCase):
    def setUp(self):
        self.lessons, self.descriptions, self.ui = copy.deepcopy(load())

    def reject(self):
        with self.assertRaises(ValueError):
            validate(self.lessons, self.descriptions, self.ui)

    def test_catalogue(self):
        validate(self.lessons, self.descriptions, self.ui)

    def test_missing_translation(self):
        self.lessons[0]['steps'][0]['text']['en'] = ''
        self.reject()

    def test_unknown_reference(self):
        self.lessons[0]['steps'][0]['show'].append('missing')
        self.reject()

    def test_hidden_focus(self):
        self.lessons[0]['steps'][0]['focus'] = ['machine']
        self.reject()

    def test_hidden_callout(self):
        self.lessons[0]['steps'][0]['callouts'][0]['anchor'] = 'machine'
        self.reject()

    def test_hidden_flow(self):
        self.lessons[0]['steps'][0]['flow'] = {'from':'cube','to':'machine','item':'minecraft:iron_ingot'}
        self.reject()

    def test_overlapping_nodes(self):
        self.lessons[0]['nodes'][1]['pos'] = self.lessons[0]['nodes'][0]['pos']
        self.reject()

    def test_duplicate_described_item(self):
        self.descriptions[1]['items'] = self.descriptions[0]['items']
        self.reject()

    def test_placeholder_mismatch(self):
        self.ui['mekanismponder.command.unknown_chapter']['ja'] = 'Unknown'
        self.reject()

    def test_deterministic_stage(self):
        self.assertEqual(stage(), stage())
        data = gzip.decompress(stage())
        self.assertIn(b'DataVersion', data)
        self.assertIn(b'minecraft:air', data)


class ContentTranslationTests(unittest.TestCase):
    def setUp(self):
        self.source = {'mekanismponder.test': {'ja': '%s を2個使います', 'en': 'Use 2 of %s'}}
        self.bundle = {'source_hash': content_hash(self.source), 'translations': {'mekanismponder.test': 'Utilisez 2 unités de %s'}}

    def reject(self):
        with self.assertRaises(ValueError):
            validate_content(self.source, self.bundle, 'fr_fr')

    def test_complete_translation(self):
        self.assertEqual(validate_content(self.source, self.bundle, 'fr_fr'), self.bundle['translations'])

    def test_missing_key(self):
        self.bundle['translations'].clear()
        self.reject()

    def test_empty_translation(self):
        self.bundle['translations']['mekanismponder.test'] = ''
        self.reject()

    def test_lost_placeholder(self):
        self.bundle['translations']['mekanismponder.test'] = 'Utilisez 2 unités.'
        self.reject()

    def test_lost_quantity(self):
        self.bundle['translations']['mekanismponder.test'] = 'Utilisez %s.'
        self.reject()

    def test_stale_review(self):
        self.source['mekanismponder.test']['ja'] += '変更'
        self.reject()

    def test_full_content_coverage(self):
        source = content_source(*load())
        for path in (TRANSLATIONS / 'content').glob('*.json'):
            validate_content(source, json.loads(path.read_text(encoding='utf-8')), path.stem)


class NavigationTranslationTests(unittest.TestCase):
    def setUp(self):
        self.ui = load()[2]
        self.languages = json.loads((TRANSLATIONS / 'languages.json').read_text(encoding='utf-8'))
        self.bundle = json.loads((TRANSLATIONS / 'navigation.json').read_text(encoding='utf-8'))

    def reject(self):
        with self.assertRaises(ValueError):
            validate_navigation(self.ui, self.languages, self.bundle)

    def test_all_twenty_two_locales(self):
        self.assertEqual(len(validate_navigation(self.ui, self.languages, self.bundle)) + 2, 22)

    def test_missing_locale(self):
        self.bundle['translations'].pop('zh_hk')
        self.reject()

    def test_missing_chapter_description(self):
        self.bundle['translations']['pt_pt'].pop('mekanismponder.ponder.tag.start.description')
        self.reject()

    def test_stale_source_review(self):
        self.ui['mekanismponder.command.unknown_chapter']['en'] += '!'
        self.reject()

    def test_placeholder_missing(self):
        self.bundle['translations']['tr_tr']['mekanismponder.command.unknown_item'] = 'Bulunamadı'
        self.reject()

    def test_review_hash_matches_both_sources(self):
        self.assertEqual(self.bundle['source_hash'], navigation_hash(self.ui))


if __name__ == '__main__':
    unittest.main()

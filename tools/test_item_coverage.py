import copy
import json
import unittest
from check_item_coverage import GUIDE, MANIFEST, check


class ItemCoverageTests(unittest.TestCase):
    def setUp(self):
        read = lambda p: json.loads(p.read_text(encoding='utf8'))
        self.expected = read(MANIFEST)
        self.lessons = read(GUIDE/'lessons.json')
        self.descriptions = read(GUIDE/'item_descriptions.json')

    def test_reviewed_coverage(self):
        result = check(self.expected, self.lessons, self.descriptions)
        self.assertEqual(result['registryItems'], 381)
        self.assertEqual(result['nativeOnly'], 35)

    def test_missing_factory_alias_is_detected(self):
        for lesson in self.lessons:
            lesson['components'] = [i for i in lesson['components'] if i != 'mekanism:elite_smelting_factory']
        with self.assertRaisesRegex(ValueError, 'elite_smelting_factory'):
            check(self.expected, self.lessons, self.descriptions)

    def test_wrong_stage_cannot_pass_a_self_matching_plan(self):
        for lesson in self.lessons:
            lesson['components'] = [i for i in lesson['components'] if i != 'mekanism:crystal_gold']
            if lesson['id'] == 'enrichment':
                lesson['components'].append('mekanism:crystal_gold')
        for item in self.expected['items']:
            if item['id'] == 'mekanism:crystal_gold':
                item['ponder'] = ['enrichment']
                item['description'] = None
        with self.assertRaisesRegex(ValueError, 'crystal_gold'):
            check(self.expected, self.lessons, self.descriptions)

    def test_unexplained_exclusion_is_rejected(self):
        for item in self.expected['items']:
            if item['id'] == 'mekanism:bounding_block':
                item['exclusion'] = None
        with self.assertRaisesRegex(ValueError, 'no entry'):
            check(self.expected, self.lessons, self.descriptions)

    def test_fake_native_tooltip_cannot_hide_a_missing_entry(self):
        self.descriptions = [d for d in self.descriptions if d['id'] != 'coverage_robit']
        for item in self.expected['items']:
            if item['id'] == 'mekanism:robit':
                item['description'] = None
                item['nativeTooltip'] = {'kind': 'always', 'translationKey': 'fake.does.not.exist'}
        with self.assertRaisesRegex(ValueError, 'no entry'):
            check(self.expected, self.lessons, self.descriptions)

    def test_arbitrary_exclusion_cannot_hide_a_missing_entry(self):
        self.descriptions = [d for d in self.descriptions if d['id'] != 'coverage_robit']
        for item in self.expected['items']:
            if item['id'] == 'mekanism:robit':
                item['description'] = None
                item['exclusion'] = 'not checked'
        with self.assertRaisesRegex(ValueError, 'Unapproved internal exclusion'):
            check(self.expected, self.lessons, self.descriptions)


if __name__ == '__main__':
    unittest.main()

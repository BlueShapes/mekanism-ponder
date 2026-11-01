import copy
import unittest
from guide_data import content_hash, validate_presentation


class PresentationTests(unittest.TestCase):
    def setUp(self):
        self.values = {'mekanismponder.ponder.shared.test_0': 'The output is full\nClear the output',
                       'mekanismponder.description.test': 'A crafting material'}
        self.bundle = {'text_hash': content_hash(self.values), 'units': {
            'mekanismponder.ponder.shared.test_0': ['WARNING', 'HINT'],
            'mekanismponder.description.test': ['NORMAL']}}

    def test_mixed_intentions_are_authored_per_unit(self):
        self.assertEqual(validate_presentation(self.values, self.bundle, 'en_us'), self.bundle['units'])

    def test_other_locale_may_have_different_authored_count(self):
        other = copy.deepcopy(self.bundle)
        values = {**self.values, 'mekanismponder.ponder.shared.test_0': '出力が満杯なら搬出してください'}
        other['text_hash'] = content_hash(values)
        with self.assertRaisesRegex(ValueError, 'unit count'):
            validate_presentation(values, other, 'ja_jp')
        other['units']['mekanismponder.ponder.shared.test_0'] = ['HINT']
        validate_presentation(values, other, 'ja_jp')

    def test_merged_units_require_review(self):
        self.values['mekanismponder.ponder.shared.test_0'] = self.values['mekanismponder.ponder.shared.test_0'].replace('\n', ' ')
        with self.assertRaisesRegex(ValueError, 'review'):
            validate_presentation(self.values, self.bundle, 'en_us')
        self.bundle['text_hash'] = content_hash(self.values)
        with self.assertRaisesRegex(ValueError, 'unit count'):
            validate_presentation(self.values, self.bundle, 'en_us')

    def test_stale_text_review_is_rejected(self):
        self.values['mekanismponder.description.test'] += ' for machines'
        with self.assertRaisesRegex(ValueError, 'review'):
            validate_presentation(self.values, self.bundle, 'en_us')

    def test_unknown_and_missing_intents_are_rejected(self):
        self.bundle['units']['mekanismponder.description.test'] = ['BLUE']
        with self.assertRaisesRegex(ValueError, 'Unknown'):
            validate_presentation(self.values, self.bundle, 'en_us')
        self.bundle['units'].pop('mekanismponder.description.test')
        with self.assertRaisesRegex(ValueError, 'key'):
            validate_presentation(self.values, self.bundle, 'en_us')

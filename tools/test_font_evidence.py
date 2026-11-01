import unittest
from merge_caption_evidence import WIDTHS, merge_measurements


KEY = 'mekanismponder.ponder.shared.power_0'


def report(locale, text, lines=1):
    return {'nativeCaptionWidth':180,'guiWidths':list(WIDTHS),'issues':[],
            'measurements':[{'locale':locale,'lesson':'power','step':0,'unit':0,'guiWidth':gui,
                             'text':text,'captionWidth':180,'lineCount':lines,'visualGlyphWidths':[50]*lines}
                            for gui in WIDTHS]}


class FontEvidenceTests(unittest.TestCase):
    def test_targeted_remeasure_replaces_only_changed_locale(self):
        values = {'en_us':{KEY:'Current text'},'ja_jp':{KEY:'現在の説明'}}
        result = merge_measurements(values,[report('en_us','Old text'),report('ja_jp','現在の説明'),report('en_us','Current text')])
        self.assertTrue(result['passed'])
        self.assertEqual(result['unitsChecked'],2)

    def test_changed_text_cannot_inherit_old_width(self):
        with self.assertRaisesRegex(ValueError,'Missing current'):
            merge_measurements({'en_us':{KEY:'Changed text'}},[report('en_us','Old text')])

    def test_four_line_measurement_does_not_count_as_valid(self):
        with self.assertRaisesRegex(ValueError,'Missing current'):
            merge_measurements({'en_us':{KEY:'Current text'}},[report('en_us','Current text',4)])

    def test_corrected_measurement_can_resolve_overflow(self):
        result = merge_measurements({'en_us':{KEY:'Current text'}},[report('en_us','Current text',4),report('en_us','Current text',2)])
        self.assertEqual(result['maximumLines'],2)

    def test_later_overflow_invalidates_prior_pass(self):
        with self.assertRaisesRegex(ValueError,'Missing current'):
            merge_measurements({'en_us':{KEY:'Current text'}},[report('en_us','Current text',2),report('en_us','Current text',4)])

    def test_different_native_window_is_rejected(self):
        changed = report('en_us','Current text')
        changed['nativeCaptionWidth'] = 240
        with self.assertRaisesRegex(ValueError,'different window'):
            merge_measurements({'en_us':{KEY:'Current text'}},[changed])

import unittest
from caption_units import authored_units, validate_caption


class CaptionAuthoringTests(unittest.TestCase):
    def accept(self, text, locale='en_us'):
        self.assertEqual(validate_caption(text, locale, 'test'), text.split('\n'))

    def reject(self, text, locale='en_us'):
        with self.assertRaises(ValueError):
            validate_caption(text, locale, 'test')

    def test_complete_individual_sentences(self):
        self.accept('The processing machine does not accept fluids\nSupply power before starting')

    def test_no_automatic_sentence_merging(self):
        self.reject('The processing machine does not accept fluids. Supply power before starting.')
        self.reject('Does it have power? Check the output!')

    def test_unmarked_mid_sentence_cut_is_rejected(self):
        self.reject('The processing machine does\nnot accept fluids')
        self.reject('The processing machine does')

    def test_abbreviations_and_identifiers_are_not_sentence_stops(self):
        self.accept('Dr. Smith uses e.g. 2.5 mB in version 1.21.1\nUse mekanism:chemical.infuser')
        self.accept('Open config.json or https://example.org/help\nKeep the label v1.21.1')

    def test_ordinary_full_stops_are_rejected_without_losing_commas(self):
        self.accept('If the output is full, processing stops')
        for text in ['Processing stops.', '機械は停止します。', '停止．', '停止｡',
                     'मशीन बंद होती है।', 'مکمل۔']:
            self.reject(text)

    def test_cjk_hindi_arabic_boundaries(self):
        for text, locale in [('機械には電力が必要です\n電源を接続してください', 'ja_jp'),
                             ('机器需要电力\n请连接电源', 'zh_cn'),
                             ('मशीन को बिजली चाहिए\nपहले बिजली दें', 'hi_in'),
                             ('تحتاج الآلة إلى طاقة\nوصّل الطاقة أولًا', 'ar_sa')]:
            self.accept(text, locale)

    def test_adjacent_cjk_and_danda_sentences_cannot_share_a_unit(self):
        for text, locale in [('機械には電力が必要です。電源を接続してください。', 'ja_jp'),
                             ('机器需要电力。请连接电源。', 'zh_cn'),
                             ('मशीन को बिजली चाहिए।पहले बिजली दें।', 'hi_in'),
                             ('هل الآلة جاهزة؟وصّل الطاقة أولًا.', 'ar_sa'),
                             ('機械は停止します。出力が満杯なら…\n…製品を取り出してください。', 'ja_jp')]:
            self.reject(text, locale)

    def test_valid_two_and_three_unit_continuation(self):
        self.accept('If the available stress is exceeded…\n…all connected machines stop')
        self.accept('If the available stress is exceeded…\n…and the network stays overloaded…\n…all connected machines stop')

    def test_missing_or_unmatched_continuations(self):
        for text in ['If the network is overloaded…', '…all machines stop.',
                     'If the network is overloaded…\nAll machines stop.',
                     'The network is overloaded.\n…all machines stop.',
                     'If overloaded…\n…\n…all machines stop.']:
            self.reject(text)

    def test_unnatural_english_continuation(self):
        self.reject('The processing machine does…\n…not accept fluids')

    def test_internal_ellipsis_does_not_define_a_boundary(self):
        self.accept('The label “Waiting…” means that processing has not started')

    def test_long_word_and_sentence_are_not_cut_by_authoring_parser(self):
        text = 'Pneumonoultramicroscopicsilicovolcanoconiosis is a long word'
        self.assertEqual(authored_units(text), [text])
        self.accept(text)

    def test_continuation_preserves_every_authored_clause_in_order(self):
        text = 'If capacity is exceeded…\n…and the output remains blocked…\n…processing stops'
        self.assertEqual(validate_caption(text, 'en_us', 'test'),
                         ['If capacity is exceeded…', '…and the output remains blocked…', '…processing stops'])

    def test_empty_units_and_padding_are_rejected(self):
        for text in ['', 'First.\n\nLast.', ' First.', 'First. ', 'First.\r\nLast.']:
            self.reject(text)

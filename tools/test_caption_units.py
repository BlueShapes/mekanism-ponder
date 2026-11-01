"""Exercise production caption boundaries, reflow, and search without Minecraft.

The tiny font substitutes deliberately vary glyph widths. They cannot prove real
rendering fit; they prove that font metrics never choose a page boundary and that
same-bubble reflow preserves the authored text.
"""
import base64
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class CaptionBoundaryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        temporary_root = ROOT / 'build/caption-unit-tests'
        temporary_root.mkdir(parents=True, exist_ok=True)
        cls.temp = tempfile.TemporaryDirectory(dir=temporary_root)
        cls.folder = Path(cls.temp.name)
        stubs = {
            'net/minecraft/client/gui/Font.java': '''package net.minecraft.client.gui;
                public class Font {
                    public int width(String text) { return text.codePointCount(0, text.length()) * Integer.getInteger("glyphWidth", 6); }
                }''',
            'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
                import net.minecraft.client.gui.Font;
                public class Minecraft {
                    public final Font font = new Font();
                    public static Minecraft getInstance() { return new Minecraft(); }
                }''',
            'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat;
                public record Component(String text) {
                    public static Component literal(String text) { return new Component(text); }
                    public String getVisualOrderText() { return text; }
                }''',
            'CaptionProbe.java': '''import java.nio.charset.StandardCharsets;
                import java.util.Base64;
                import dev.mekanismponder.client.GuideTextLayout;
                    public class CaptionProbe {
                        public static void main(String[] args) {
                            if (args[0].equals("display")) {
                                String text = new String(Base64.getDecoder().decode(args[1]), StandardCharsets.UTF_8);
                                System.out.println(Base64.getEncoder().encodeToString(
                                        GuideTextLayout.captionDisplayText(
                                                net.minecraft.client.Minecraft.getInstance().font, text)
                                                .getBytes(StandardCharsets.UTF_8)));
                                return;
                            }
                            if (args[0].equals("search")) {
                                String text = new String(Base64.getDecoder().decode(args[1]), StandardCharsets.UTF_8);
                                System.out.println(Base64.getEncoder().encodeToString(
                                        GuideTextLayout.searchableCaption(text).getBytes(StandardCharsets.UTF_8)));
                                return;
                            }
                            String text = new String(Base64.getDecoder().decode(args[0]), StandardCharsets.UTF_8);
                            for (String page : GuideTextLayout.captionPages(text))
                                System.out.println(Base64.getEncoder().encodeToString(page.getBytes(StandardCharsets.UTF_8)));
                        }
                    }'''
        }
        sources = []
        for name, code in stubs.items():
            path = cls.folder / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(code, encoding='utf-8')
            sources.append(str(path))
        java_home = Path(os.environ.get('JAVA_HOME', '')) / 'bin'
        javac = str(java_home / 'javac.exe') if (java_home / 'javac.exe').is_file() else shutil.which('javac')
        cls.java = str(java_home / 'java.exe') if (java_home / 'java.exe').is_file() else shutil.which('java')
        if not javac or not cls.java:
            raise RuntimeError('A JDK is required for caption boundary regression tests')
        production = ROOT / 'src/main/java/dev/mekanismponder/client/GuideTextLayout.java'
        subprocess.run([javac, '-encoding', 'UTF-8', '-d', str(cls.folder), *sources, str(production)], check=True, capture_output=True)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def pages(self, text, glyph_width=6):
        encoded = base64.b64encode(text.encode()).decode()
        result = subprocess.run([self.java, f'-DglyphWidth={glyph_width}', '-cp', str(self.folder), 'CaptionProbe', encoded],
                                check=True, capture_output=True, text=True)
        return [base64.b64decode(line).decode() for line in result.stdout.splitlines()]

    def searchable(self, text):
        encoded = base64.b64encode(text.encode()).decode()
        result = subprocess.run([self.java, '-cp', str(self.folder), 'CaptionProbe', 'search', encoded],
                                check=True, capture_output=True, text=True)
        return base64.b64decode(result.stdout.strip()).decode()

    def display(self, text, glyph_width=6):
        encoded = base64.b64encode(text.encode()).decode()
        result = subprocess.run([self.java, f'-DglyphWidth={glyph_width}', '-cp', str(self.folder),
                                 'CaptionProbe', 'display', encoded],
                                check=True, capture_output=True, text=True)
        return base64.b64decode(result.stdout.strip()).decode()

    def test_two_authored_complete_sentences_stay_separate(self):
        units = ['The processing machine does not accept fluids.', 'Supply power before starting.']
        self.assertEqual(self.pages('\n'.join(units)), units)

    def test_long_sentence_remains_complete_for_authoring_to_reject(self):
        sentence = 'The processing machine does not accept fluids even when its adjacent fluid tank contains water.'
        self.assertEqual(self.pages(sentence), [sentence])

    def test_font_width_cannot_change_boundaries(self):
        units = ['Universal Cables carry energy.', 'An empty cube cannot supply power.']
        for width in (2, 6, 20):
            self.assertEqual(self.pages('\n'.join(units), width), units)

    def test_abbreviations_decimals_versions_and_ids_are_unchanged(self):
        units = ['Dr. Smith uses e.g. 2.5 mB in version 1.21.1.', 'Use mekanism:chemical_infuser.']
        self.assertEqual(self.pages('\n'.join(units)), units)

    def test_cjk_and_hindi_and_arabic_authored_units(self):
        for units in [
            ['機械には電力が必要です。', '電源を接続してください。'],
            ['机器需要电力。', '请连接电源。'],
            ['मशीन को बिजली चाहिए।', 'पहले बिजली दें।'],
            ['تحتاج الآلة إلى طاقة.', 'وصّل الطاقة أولًا.']
        ]:
            self.assertEqual(self.pages('\n'.join(units)), units)

    def test_long_word_is_never_moved_to_another_page(self):
        sentence = 'Pneumonoultramicroscopicsilicovolcanoconiosis is a long word.'
        self.assertEqual(self.pages(sentence), [sentence])

    def test_unreviewed_unicode_line_separator_is_not_an_authored_boundary(self):
        text = 'The machine\u2028does not accept fluids.'
        self.assertEqual(self.pages(text), [text])

    def test_unreviewed_paragraph_is_preserved_at_runtime(self):
        paragraph = 'The processing machine does not accept fluids. Supply power before starting.'
        self.assertEqual(self.pages(paragraph), [paragraph])

    def test_authored_two_and_three_stage_continuations_keep_order(self):
        for units in [
            ['If the available stress is exceeded…', '…all connected machines stop.'],
            ['If the available stress is exceeded…', '…and the network stays overloaded…', '…all connected machines stop.']
        ]:
            self.assertEqual(self.pages('\n'.join(units)), units)

    def test_ellipsis_inside_a_unit_does_not_create_a_boundary(self):
        sentence = 'The label “Waiting…” means that processing has not started.'
        self.assertEqual(self.pages(sentence), [sentence])

    def test_search_matches_a_phrase_across_authored_units(self):
        caption = 'Universal Cables carry energy.\nAn empty cube stores power.'
        query = 'carry energy. An empty'
        self.assertIn(self.searchable(query), self.searchable(caption))

    def test_search_removes_paired_two_and_three_stage_continuations_in_order(self):
        two_stage = 'If the available stress is exceeded…\n…all connected machines stop.'
        three_stage = ('If the available stress is exceeded…\n'
                       '…and the network stays overloaded…\n'
                       '…all connected machines stop.')
        self.assertEqual(self.searchable(two_stage), 'Iftheavailablestressisexceededallconnectedmachinesstop.')
        self.assertEqual(self.searchable(three_stage),
                         'Iftheavailablestressisexceededandthenetworkstaysoverloadedallconnectedmachinesstop.')

    def test_search_preserves_internal_and_unpaired_ellipsis(self):
        self.assertEqual(self.searchable('The label “Waiting…” is shown.'), 'Thelabel“Waiting…”isshown.')
        self.assertEqual(self.searchable('A label…\nThis is separate.'), 'Alabel…Thisisseparate.')

    def test_display_reflow_adds_lines_without_creating_caption_pages(self):
        unit = 'Universal Cables carry energy through the network.'
        display = self.display(unit, glyph_width=20)
        self.assertIn('\n', display)
        self.assertEqual(display.replace('\n', ''), unit)
        self.assertEqual(self.pages(unit), [unit])

    def test_display_reflow_preserves_arabic_and_long_words_in_one_unit(self):
        for unit in [
            'تحتاج الآلة إلى طاقة قبل بدء المعالجة.',
            'Pneumonoultramicroscopicsilicovolcanoconiosis is a long word.'
        ]:
            display = self.display(unit, glyph_width=20)
            self.assertIn('\n', display)
            self.assertEqual(display.replace('\n', ''), unit)
            self.assertEqual(self.pages(unit), [unit])

    def test_exactly_fitting_words_are_not_moved_by_the_following_space(self):
        fitting_words = 'A' * 10 + ' ' + 'B' * 19
        unit = fitting_words + ' C.'
        display = self.display(unit, glyph_width=6)
        self.assertEqual(display.split('\n')[0], fitting_words)
        self.assertEqual(display.replace('\n', ''), unit)

    def test_display_reflow_keeps_boundary_whitespace_on_fitting_lines(self):
        unit = '123456789012345678901234567890 next'
        display = self.display(unit, glyph_width=6)
        lines = display.split('\n')
        self.assertTrue(all(len(line) * 6 <= 180 for line in lines), display)
        self.assertEqual(''.join(lines), unit)
        self.assertEqual(self.pages(unit), [unit])


if __name__ == '__main__':
    unittest.main()

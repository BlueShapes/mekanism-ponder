"""Small regression for the real pure Java matcher, without Minecraft startup."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class SearchNormalizationTests(unittest.TestCase):
    def test_canonical_and_locale_matching(self):
        java_home = os.environ.get('JAVA_HOME')
        bin_path = Path(java_home) / 'bin' if java_home else None
        executable = lambda name: str(bin_path / name) if bin_path else shutil.which(name)
        javac, java = executable('javac.exe' if os.name == 'nt' else 'javac'), executable('java.exe' if os.name == 'nt' else 'java')
        if not javac or not java:
            self.skipTest('JDK 21 is required for the pure Java search regression')
        temp_root = ROOT / '.work'
        temp_root.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(prefix='mekanismponder-search-', dir=temp_root) as output:
            self.assertTrue(Path(output).resolve().is_relative_to(temp_root.resolve()))
            compile_result = subprocess.run([javac, '-J-Duser.language=en', '--release', '21', '-encoding', 'UTF-8', '-d', output,
                str(ROOT/'src/main/java/dev/mekanismponder/client/GuideSearchText.java'),
                str(ROOT/'tools/GuideSearchTest.java')], capture_output=True, text=True, encoding='utf8', errors='replace')
            self.assertEqual(compile_result.returncode, 0, compile_result.stderr)
            result = subprocess.run([java, '-cp', output, 'GuideSearchTest'],
                capture_output=True, text=True, encoding='utf8', errors='replace')
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn('PASS (16 cases', result.stdout)
            print(result.stdout.strip())


if __name__ == '__main__':
    unittest.main()

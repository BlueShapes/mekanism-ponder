# Mekanism Ponder Guide

1. This project is AI-driven (AI駆動).

2. Target only Minecraft 1.21.1, NeoForge and Java 21. Do not add other loaders or versions without a request.

3. Keep guide code client-only. Never change the player's real world or register replacement machines.

4. Support all 22 languages listed in `../docs/languages.json`; keep Japanese and English as the teaching source languages. Japanese explanations use です・ます and naturally explain operations, mechanisms, conditions and results. Reserve 〜してください for necessary instructions, diagnosis, remedies and safety; follow shared requirement 73 without mechanical ending replacement.

5. `src/main/resources/assets/mekanismponder/guide/lessons.json` and `item_descriptions.json` are the teaching sources. `src/main/templates/ui.json` owns UI text. Generated language files and the stage live under `build/`.

6. Match Mekanism 10.7.19.85 registry IDs, terminology and recipes. Distinguish ore blocks from raw ore. Recipe/config changes in modpacks take precedence.

7. Keep Ponder's normal W-key entry, timeline, pause, previous/next and history. Avoid adding global redirects to Ponder or copying GuideME integration.

8. Keep identifiers under `mekanismponder` / `dev.mekanismponder`; preserve third-party notices when reusing code.

9. Use short tooltip explanations for materials; add Ponder only when a spatial or changing demonstration helps.

10. Start verification with `python tools/guide_data.py check` or the data unit tests, then compile. Minecraft startup is expensive; use the opt-in validation harness only for registry, playback, UI and rendering changes.

11. Check for an existing matching process before starting a client; stop only clients started for this task.

12. Never push to a remote. Suggest an English Conventional Commit message when finishing work.

## Shared series policy

13. Read [the shared authoring policy](../docs/mod-guide-authoring.md) and [the 22-language list](../docs/languages.json). Complete supported translations; do not treat English fallback as support for those languages.

## Numbered common requirements

14. Read the numbered requirements in [the shared policy](../docs/mod-guide-authoring.md). Shared requirement numbers are global references within that document; the numbers in this file are local references. This includes autonomous local commits, completed-JAR placement, English fallback, the three-line maximum, client commands, standard Ponder UI, and implementation-versus-guide conflict handling.

## Chapter navigation

15. Use the native `PonderTagIndexScreen` and `PonderTagScreen` for chapters and item selection. Keep `/ponder-mekanism` and native item-hover W connected to this flow. Do not add guide buttons to Options or other ordinary screens, or restore the removed custom catalogue or chapter filters.

16. `GuidePonderScreen` only adapts the selected lesson to the ordered native playback course. Keep the adapter limited to this mod's tags or an entirely owned item scene list; preserve third-party scenes and native history.

17. For navigation changes, run `tools/validate-navigation.ps1`. It holds `Local\PonderSeriesValidation` to serialize Minecraft validation across the series. Compile and check data/translations first.

18. Shared requirement 74 separates existing optional add-on lessons by the actual add-on's standard tag. This catalogue currently teaches core Mekanism only; a reference to Mekanism: Generators does not constitute a Generators lesson. New Mekanism add-ons require an explicit scope request. `tools/check_narration.py` preserves the historical 0.1.3–0.1.4 narration audit. `tools/check_item_coverage.py` checks the current routing plan against the independently enumerated pinned registry, factory types and processing stages.

19. Shared requirement 75 keeps W registration, native tags and item commands aligned for all relevant Tier variants. Preserve complete ItemStack components; transporter routing colors are placed-block state, not separate colored item IDs. Native Mekanism module and upgrade explanations are retained, with their scope and language limitations recorded separately. Run `tools/validate-navigation.ps1 -ValidationCoverage` for registry/entry changes after data checks and compilation.

## Translation status

The Japanese/English teaching sources stay in the guide data and UI template. The other 20 locales live in `src/main/translations/navigation.json` (14 navigation keys) and `src/main/translations/content/<locale>.json` (233 content keys), each with a reviewed source hash. Generate all 247 keys in all 22 language files; never replace these translations with English fallback. Caption strings contain explicit authored newline-separated display units, normally one short complete sentence each. Runtime must never split sentences by punctuation, font width, word count, or line count. Paired `…` markers may link authored natural clauses under shared requirement 67; validate their order and matching. Omit ordinary sentence-ending full stops under requirement 69; retain meaningful dots and commas. Each locale explicitly authors NORMAL/WARNING/HINT per caption and tooltip unit in `src/main/translations/presentation/<locale>.json`, with a reviewed text hash. Use native WHITE/RED/GREEN text palettes independently of outline colors. Keep each whole unit within three real-font lines, preferably two, using the native 180px window at GUI widths of at least 320px. Revise source and translations when a unit is too long. The scoped text-width adapter includes visual glyph width without shaping stored text. See `docs/caption-authoring.md`; validate all locales, authored units, visual glyphs, translated search, and English fallback when editing translation/layout behavior. For punctuation-only removal and palette/navigation changes, reuse unchanged font/glyph evidence and run representative GUI validation after data and compile checks.

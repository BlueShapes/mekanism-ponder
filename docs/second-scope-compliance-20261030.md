# Mekanism Ponder 0.1.6 — 第二言語範囲20言語監査（2026-10-30）

Minecraft 1.21.1 / NeoForge / Java 21、Mekanism 10.7.19.85が対象です。全22言語を維持しています。

28レッスン・6章・247キー／言語（題名28、要約28、字幕113、説明60、UI18）を原本・生成物・完成JARで照合しました。

完成JAR: `mekanism-ponder-guide-0.1.6-neoforge-1.21.1.jar`
SHA-256: `b5b28a13bbfcd492d98907aa21186d28edae480369e293e833efda0bd6e52208`

蛍石の宝石ドロップ、放射性工程に限定した注意、正式なレーザー用語を修正しました。13言語の電力出力の結果、2言語のゲージドロッパー名、6つの行数超過単位も推敲しました。
検索はNFC正規化と選択言語の大小文字規則を使い、トルコ語のKüpと分解表記、I/İ、アラビア語の論理順など16ケースが通りました。

現在の全字幕・説明単位×3幅の24858条件に、現行の文字列と一致する実フォント証拠があります。最大3行です。修正範囲は今回測定し、変更のない範囲はクラス・文字列・役割・幅が一致する旧証拠を再利用しました。

新しい検証は15説明群と2字幕キーを22言語で測定しました。最初の超過は文を推敲して解消しました。2回目の検証用Options画面のフォント破棄エラーを受け、最後は画面遷移を省いた限定検証で成功しています。製品UIに変更はありません。

## 言語別の根拠

|言語|キー|字幕単位|説明単位|最大行（字幕／説明）|今回の変更キー数|
|---|---:|---:|---:|---:|---:|
|hi_in|247|273|123|3／3|4|
|zh_hk|247|247|118|3／3|2|
|zh_tw|247|243|117|3／1|2|
|zh_cn|247|243|117|2／1|2|
|ru_ru|247|265|118|3／3|4|
|pt_br|247|266|118|3／3|4|
|pt_pt|247|262|118|3／3|5|
|es_es|247|265|118|3／3|4|
|fr_fr|247|267|118|3／3|4|
|de_de|247|269|118|3／3|4|
|cs_cz|247|257|119|3／3|5|
|pl_pl|247|255|118|3／3|4|
|ms_my|247|304|122|3／3|5|
|ro_ro|247|256|118|3／3|4|
|ar_sa|247|285|119|2／3|3|
|id_id|247|245|117|3／3|3|
|ko_kr|247|245|119|3／2|2|
|vi_vn|247|246|118|3／2|2|
|tr_tr|247|253|118|3／3|3|
|it_it|247|264|118|3／3|5|

日英は247組を通読しました。第二範囲20言語は全キーを構造検査し、各言語の修正3説明と既存7代表キーを意味・条件・数値・用語・自然さ・色について読み直しています。変更のない既存レビューは文字列・役割とハッシュを結び付けて再利用しています。各言語の具体例・判断・原文／訳文は同名JSONに収録しています。

## 配置

配置先: `~\AppData\Roaming\PrismLauncher\instances\1.21.1 mod creation test\minecraft\mods\mekanism-ponder-guide-0.1.6-neoforge-1.21.1.jar`

完成物と配置物のSHAが一致しています。同MODの旧コピー1件を除去し、拡張子に依存しない再帰メタデータ走査で新JAR1件だけであることを確認しました。無関係な78ファイルと.indexの32ファイルを保持しています。Mekanism・Ponder・Flywheelは適合する既存版を再利用しました。

## 共通要件1〜75

|番号|判断|根拠・範囲|
|---|---|---|
|1|pass|Existing independent Mekanism repository; MC 1.21.1 / NeoForge / Java 21 only; shared numbering retained|
|2|pass|Existing independent Mekanism repository; MC 1.21.1 / NeoForge / Java 21 only; shared numbering retained|
|3|pass|Configured SSH signed local commit of task changes only; no push; pre-task index empty|
|4|pass|Existing independent Mekanism repository; MC 1.21.1 / NeoForge / Java 21 only; shared numbering retained|
|5|pass|28 spatial lessons and 60 concise description groups; material-only explanations do not create empty scenes|
|6|pass|28 spatial lessons and 60 concise description groups; material-only explanations do not create empty scenes|
|7|pass|Pinned Mekanism implementation, recipes and merged native names used; fluorite/raw ore, laser names and radioactive-stage condition corrected; pack recipe/config caveat retained|
|8|pass|Pinned Mekanism implementation, recipes and merged native names used; fluorite/raw ore, laser names and radioactive-stage condition corrected; pack recipe/config caveat retained|
|9|pass|GuideBook plus generated guide sources own titles, summaries, captions, chapter order, item routes and search; client adapters separated|
|10|pass|GuideBook plus generated guide sources own titles, summaries, captions, chapter order, item routes and search; client adapters separated|
|11|pass|Guide scenes operate on Ponder virtual worlds; no guide server networking or real-world mutation|
|12|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|13|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|14|pass|Pinned Mekanism implementation, recipes and merged native names used; fluorite/raw ore, laser names and radioactive-stage condition corrected; pack recipe/config caveat retained|
|15|pass|Pinned Mekanism implementation, recipes and merged native names used; fluorite/raw ore, laser names and radioactive-stage condition corrected; pack recipe/config caveat retained|
|16|pass|Pinned Mekanism implementation, recipes and merged native names used; fluorite/raw ore, laser names and radioactive-stage condition corrected; pack recipe/config caveat retained|
|17|pass|28 ordered lessons in six native chapters; prerequisite/goal and normal operation, result, failure-condition sequence retained|
|18|conditional|184 nodes and 102 callouts checked statically; timing/target/material icons have real rendering evidence for six scenes and 17 callouts, not full playback of all 28|
|19|pass|Every current authored caption and description unit has an exact-text real-font measurement at GUI widths 320/480/960; max three lines, caption180px/tooltip240px; six old overflowing units rewritten as complete sentences|
|20|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|21|conditional|184 nodes and 102 callouts checked statically; timing/target/material icons have real rendering evidence for six scenes and 17 callouts, not full playback of all 28|
|22|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|23|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|24|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|25|pass|English text and presentation fallback retained; supported locales contain their own complete translations|
|26|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|27|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|28|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|29|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|30|pass|22 locales ×247 keys generated from JA/EN plus 20 reviewed translations; hashes/placeholders/numbers/IDs validated, HK/TW and BR/PT separately reviewed; no untranslated explanation fallback|
|31|pass|Selected-language title/summary/caption search uses logical authored strings; native translated search evidence reused and 16 pure cases verify NFC+locale casing, including Turkish I/İ and real decomposed Küp|
|32|pass|Arabic logical search text remains unshaped; visual glyph width uses the unchanged layout adapter; canonical normalization does not reverse text|
|33|pass|Default Minecraft resource stack glyphs and font widths checked in all22; long controls/native names retain prior exact input UI evidence; no custom resource-pack guarantee|
|34|pass|Stable IDs, recipes, block state/NBT and real pinned registry preserved; data coordinates/references checked; all geometry unchanged|
|35|pass|Stable IDs, recipes, block state/NBT and real pinned registry preserved; data coordinates/references checked; all geometry unchanged|
|36|conditional|Rendered faces/model data/rewind verified for six representative scenes; all28 geometry unchanged and statically visible; no new full28 rendering claim|
|37|conditional|Rendered faces/model data/rewind verified for six representative scenes; all28 geometry unchanged and statically visible; no new full28 rendering claim|
|38|pass|Existing Mekanism tooltip text retained; expanded own descriptions short and localized with actual unit colors; complete ItemStack components copied|
|39|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|40|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|41|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|42|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|43|conditional|Client-only entrypoints, client mixins and client command registration; no item/gameplay/network registration; actual join to a server without this guide remains not_verified|
|44|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|45|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|46|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|47|not_applicable|Core Mekanism only; mention of Mekanism: Generators is not a Generators lesson; no addon scope requested, so no addon chapter or new dependency|
|48|not_applicable|Core Mekanism only; mention of Mekanism: Generators is not a Generators lesson; no addon scope requested, so no addon chapter or new dependency|
|49|not_applicable|Core Mekanism only; mention of Mekanism: Generators is not a Generators lesson; no addon scope requested, so no addon chapter or new dependency|
|50|pass|Only target loader/version; Java21 main classes, metadata, resource paths and mixin namespace/refmap checked; validation classes excluded from deliverable|
|51|pass|Only target loader/version; Java21 main classes, metadata, resource paths and mixin namespace/refmap checked; validation classes excluded from deliverable|
|52|pass|Only target loader/version; Java21 main classes, metadata, resource paths and mixin namespace/refmap checked; validation classes excluded from deliverable|
|53|conditional|No global @Redirect; own namespace/list/type guards and foreign-scene/history behavior retained; simultaneous startup of all eight guides remains not_verified|
|54|pass|Cheap data/tests/compile before bounded hidden runtime; baseline process check/shared mutex; first overflows corrected, second Options reload crash diagnosed, scoped third run passed; no unrelated process stopped|
|55|pass|Cheap data/tests/compile before bounded hidden runtime; baseline process check/shared mutex; first overflows corrected, second Options reload crash diagnosed, scoped third run passed; no unrelated process stopped|
|56|pass|Cheap data/tests/compile before bounded hidden runtime; baseline process check/shared mutex; first overflows corrected, second Options reload crash diagnosed, scoped third run passed; no unrelated process stopped|
|57|pass|Cheap data/tests/compile before bounded hidden runtime; baseline process check/shared mutex; first overflows corrected, second Options reload crash diagnosed, scoped third run passed; no unrelated process stopped|
|58|pass|Cheap data/tests/compile before bounded hidden runtime; baseline process check/shared mutex; first overflows corrected, second Options reload crash diagnosed, scoped third run passed; no unrelated process stopped|
|59|pass|Required licenses/notices and NeoForge metadata preserved; validation classes and dependency JARs excluded; release checker passes|
|60|pass|Required licenses/notices and NeoForge metadata preserved; validation classes and dependency JARs excluded; release checker passes|
|61|pass|Successful compatible0.1.6 main JAR placed in specified Prism; installed hash matches, metadata-identified old own copies removed including renamed scan; unrelated mods/.index preserved, compatible pinned dependencies reused|
|62|pass|Successful compatible0.1.6 main JAR placed in specified Prism; installed hash matches, metadata-identified old own copies removed including renamed scan; unrelated mods/.index preserved, compatible pinned dependencies reused|
|63|pass|Successful compatible0.1.6 main JAR placed in specified Prism; installed hash matches, metadata-identified old own copies removed including renamed scan; unrelated mods/.index preserved, compatible pinned dependencies reused|
|64|pass|Successful compatible0.1.6 main JAR placed in specified Prism; installed hash matches, metadata-identified old own copies removed including renamed scan; unrelated mods/.index preserved, compatible pinned dependencies reused|
|65|pass|Successful compatible0.1.6 main JAR placed in specified Prism; installed hash matches, metadata-identified old own copies removed including renamed scan; unrelated mods/.index preserved, compatible pinned dependencies reused|
|66|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|67|not_applicable|No authored continuation markers in current22 locale caption/description units; paired-marker parser and order checks remain available and tested|
|68|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|69|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|70|pass|Per-locale per-unit NORMAL/WARNING/HINT roles explicitly authored; native WHITE/RED/GREEN independently of outlines; changed roles actual append colors measured, palette implementation unchanged|
|71|pass|Native item W path, PonderTagIndexScreen/TagScreen, course/history and chapter/item commands retained; no ordinary-screen guide buttons; 320/480/960 layout/pagination evidence reused|
|72|pass|Own project files/artifacts only; parent owns common docs; existing reviewers used with bounded ownership and evaluated results; no new chat/repository/push|
|73|pass|All JA/EN keys read; 20 locale fresh samples plus exact-text old reviews retain complete authored units, natural explanations, necessary safety requests, punctuation policy and timing|
|74|not_applicable|Core Mekanism only; mention of Mekanism: Generators is not a Generators lesson; no addon scope requested, so no addon chapter or new dependency|
|75|pass|Independent381 registry items,36 factories, tiers/creative component variants and routing-color placed state checked;175W/192 ownShift/22both/35native-only/1internal; concrete material/process descriptions cover guide omissions, native module/upgrade scope separately recorded|

## 未検証の範囲

- No actual shared Prism all-mod startup or simultaneous eight-guide test; deployment/dependency metadata verification only
- No actual connection to a remote server lacking the guide; static client-only boundaries are a separate check
- Six representative scenes have full playback/rewind/model evidence, not all28; all28 geometry is unchanged and statically checked
- Native W factory and expanded tooltip append paths are programmatic; physical W/Shift keys were not synthesized
- GUI initialized at320/480/960; no physical OS-window resizing or arbitrary resource-pack/font verification
- 20 second-scope locales: complete structural validation and exact unchanged prior semantic-review reuse; current fresh review is3 corrected description keys plus7 representative keys per locale (extra font revisions as recorded), not a fresh reading of every unchanged key
- Native Mekanism module31(always) and upgrade4(Shift) explanations retained; their own translation coverage is not counted as this guide22-language support
- The scoped final runtime skips native UI walkthrough after an OptionsScreen resource-reload disposed-image crash; unchanged UI proofs are explicitly reused and current false UI flags remain false

## 検証証拠

同名JSONには、全75要件の原文と判定、検証コマンド、全言語の単位数・役割数・変更キー・入力SHA、証拠ファイルSHA、JARクラスとの対応、配置前後の全ファイルSHAを保存しています。

- [今回の実行結果（ローカルに保持する過去の証跡）](language-runtime-20261030.json.gz)
- [日英の記録](jaen-compliance-20261030.json)
- [20言語の記録](second-scope-compliance-20261030.json)
- [従来の実レジストリ・UI確認](item-coverage-validation.json)
- [従来の6場面の描画・巻き戻し](validation-0.1.2.json)

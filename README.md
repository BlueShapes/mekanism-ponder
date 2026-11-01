# Mekanism Ponder Guide

Minecraft **1.21.1 / NeoForge** 向けの、Mekanismを模型で学ぶクライアント用解説MODです。日本語・英語を原文とし、教材の題名・要約・字幕・アイテム説明・章・操作文言の247キーを共通22言語に翻訳しています。

28教材・6章を収録します。Wの入口は175種類、通常のShift説明は192種類の実アイテムに対応します。

| 章 | 内容 |
| --- | --- |
| はじめに | 電力、冶金吹込機、吹込合金、鋼鉄 |
| 輸送と側面設定 | 電力・アイテム・液体・化学物質、入出力、自動搬出、ソーター |
| 鉱石と工場 | 濃縮、工場化、酸素・塩化水素・スラリーによる加工 |
| 化学処理 | 電気分解、蒸発濃縮塔、塩化水素、硫酸、エチレン、HDPE |
| 高度な設備 | インダクションマトリックス、デジタルマイナー、QIO、テレポーター、定式組立機、放射性物質 |
| 困ったときは | 電力・レッドストーン制御・入出力・副産物・スラリーの端数 |

合金・回路・アップグレード・SPS部材などには、Shiftで展開できる60種類の短い説明も付けています。SPSの組み立て、Generatorsの発電機・原子炉・タービンのPonderは含みません。

## 導入

Java 21、Minecraft 1.21.1、NeoForge 21.1.256以降の同系列を使用します。次のMODをクライアントの `mods` フォルダーへ入れてください。

- Mekanism **10.7.19.85**（教材・開発環境の確認対象）
- Ponder **1.0.87+mc1.21.1** と配布ページで指定された依存MOD
- `mekanism-ponder-guide-0.1.6-neoforge-1.21.1.jar`

Create、AE2、GuideME、Mekanism: Generatorsは必須ではありません。依存MODはガイドのJARに同梱していません。

ワールドへ入った後、関連アイテムへカーソルを合わせて **Wを長押し**してください。クライアントコマンド `/ponder-mekanism` でPonder標準のタグ一覧も開けます。Mekanism Ponder Guideの章タグを選び、タグ画面のアイテムから教材を開きます。

選んだアイテムと章に対応する教材から全28教材のコースを開始し、標準の左右操作で前後へ進めます。左上の標準タグは現在の教材の章へ進み、章をまたぐと表示も更新されます。再生・一時停止・キーフレーム・巻き戻し・戻る操作も標準UIを使います。学習済みの記録はローカルの `config/mekanismponder-progress.json` に保存します。説明の通常本文は白、失敗条件や警告は薄い赤、ヒントや対処の助言は薄い緑です。

Tierが違う工場は、共通の並列処理の教材に加え、加工種別に合う教材または短文へ接続します。鉱石中間素材の別名は、固定版の7金属の実レシピを照合したものです。本体が既に説明する31モジュールと4アップグレードは、その説明とShift詳細を保持します。本体の説明キーの既定値はShift＋N、詳細はShiftです。これら本体由来の翻訳を、本ガイドの22言語翻訳として数えていません。

通常操作は、模型の仕組みや条件と結果を説明する口調にしています。動作不良の診断や放射性物質の安全確認など、必要な呼びかけは残しています。全22言語で短い完結した表示単位と色の役割を確認します。現在の教材はMekanism本体だけが対象で、Generators等の専用教材や新規アドオンを追加したものではありません。

```text
/ponder-mekanism                     標準タグ一覧
/ponder-mekanism 2                   第2章の最初の教材
/ponder-mekanism transport           安定した章IDでも指定可能
/ponder-mekanism 輸送と側面設定       選択言語の章名でも指定可能
/ponder-mekanism item energy_cube    関連アイテムから開始
/ponder-mekanism search 酸素         選択言語の題名・要約・字幕から最初の一致を開く
/mekanismponder lesson steel         従来のコマンドも維持
```

標準のタグとアイテム選択を利用します。狭い画面では、自MODのタグ内の関連アイテムをページに分けます。共通22言語の対象外ロケールや翻訳欠落時の最終フォールバックは英語です。検索では選択言語の大小文字規則とNFC正規化を使い、合成文字と分解表記を同じように扱います。翻訳原文は検索用の論理順で保存し、アラビア語の文字整形はMinecraftの描画に委ねます。

字幕は原則として短く完結した1文を1吹き出しに表示します。教材・訳文で明示した単位だけを順に表示し、文の途中を幅や行数で切りません。どうしても長い文は、作者が自然な節を選び、前の末尾と次の先頭に「…」を付けて継続できます。320ピクセル幅以上のGUIで標準Ponderの180pxの字幕幅を使い、完結した表示単位を原則2行・最大3行に収めます。公式の項目名がない言語では、英語名と翻訳案を初出に併記します。

模型はPonderの仮想ワールドで動作します。線や移動アイテムは説明用の演出です。機械の電力、化学物質、実際の処理速度をシミュレーションするものではありません。鉱石ブロックと原鉱の標準レシピは区別して説明し、MODパック独自のレシピ・設定を優先します。

ガイド本体はクライアント限定です。ガイド未導入の専用サーバーへの実接続は未検証です。Mekanism等の通常のサーバー導入要件はそれぞれのMODに従ってください。

## ビルド

JDK 21とPython 3.11以降が必要です。Pythonは標準ライブラリのみを使います。

```powershell
python tools/guide_data.py check
.\gradlew.bat build
```

成果物は `build/libs/` に生成されます。Python実行ファイルを指定する場合は `-Ppython=C:/path/to/python.exe` を使えます。`JAVA_HOME` がJava 17を指している場合は、JDK 21の場所へ設定してください。

教材のID・座標・参照・可視状態・吹き出し・日英原文・プレースホルダを検査してから、言語JSONと共有ステージを生成します。生成物は直接編集しません。

```powershell
python -m unittest discover -s tools -p 'test_*.py'
python tools/check_item_coverage.py
# 過去の口調改訂の独立記録
python tools/check_narration.py
.\gradlew.bat compileJava
```

描画・接続NBT・巻き戻し・標準UIの確認には、配布JARに含まれない検証ハーネスを使います。

```powershell
# 今回の全実ID・W/Shift・保存データ・新説明22言語だけの検証
.\tools\validate-navigation.ps1 -ValidationCoverage
# 標準タグ導線と22言語のフォント・字幕・検索の検証
powershell -NoProfile -File tools/validate-navigation.ps1
# 句点除去・配色・タグ同期の代表GUI確認（全22言語の実フォント再測定は省略）
powershell -NoProfile -File tools/validate-navigation.ps1 -RepresentativeGui
```

このスクリプトは `Local\PonderSeriesValidation` ミューテックスを取得してから起動するため、他のPonderシリーズのMinecraft検証と直列化します。同じプロジェクトの既存クライアントも確認します。再生も確認する場合は `-FullPlayback`、対象を絞る場合はさらに `-ValidationLessons 'power,cables'` を指定できます。字幕変更では代表教材の再生と全22言語の全字幕の実フォント検査を組み合わせます。今回の対象と結果は [検証記録](docs/verification.md) を参照してください。

実測後に一部の訳文だけを直した場合は、`-ValidationLocales 'pl_pl,ms_my'` のように再測定する言語を指定できます。`tools/merge_caption_evidence.py` は現在の本文と完全一致する実測だけを採用し、全単位・全対象幅の不足や3行超過を拒否します。描画クラスが変わった場合も、以前の測定との合成を拒否します。

専用の `build/validation-run/` に非表示クライアント・新規フラットワールドを作り、結果JSONと画像を保存して終了します。Gradleは今回の `validation-result.json` の `passed: true` を必須とし、Ponderが捕捉した仮想ブロックエンティティの描画例外もログから検出します。通常のプレイ用ワールドは使用しません。

Mekanismの空の蒸発濃縮コントローラーと、未設定のテレポーターは、静的なブロック模型として表示します。このガイドの仮想ワールドの二種類だけ、描画側本来の `shouldRender` 条件を尊重します。実ワールドや他MODの教材には適用しません。他の解説MODとの同時導入による実起動試験は未実施です。

## 教材の編集

- `src/main/resources/assets/mekanismponder/guide/lessons.json`: 章・関連アイテム・模型・手順・日英原文
- `src/main/resources/assets/mekanismponder/guide/item_descriptions.json`: Shift説明
- `src/main/templates/ui.json`: 日英のUI・章・タグ原文
- `src/main/translations/navigation.json`: 章と今回の導線・コマンド文言の20言語訳と原文レビューのハッシュ
- `src/main/translations/content/*.json`: 本文・題名・要約・アイテム説明等の20言語訳と原文レビューのハッシュ
- `src/main/translations/presentation/*.json`: 全22言語の各表示単位の通常・警告・ヒント指定と本文レビューのハッシュ
- `src/main/translations/languages.json`: 共通22ロケールの写し（親の正本があれば一致を検査）
- `docs/caption-authoring.md`: 明示的な字幕単位・継続記号の形式と制作時検査
- `tools/guide_data.py`: 検査と配布用リソース生成
- `tools/check_release.py`: 完成JARの言語・メタデータ・同梱物・Java版の検査
- `tools/check_narration.py`: 0.1.3からの口調変更で模型・ID・章・数量・正式名称を保持する検査
- `src/validation/java/`: ゲーム内の検証専用コード

対象版の[公式ソース](https://github.com/mekanism/Mekanism/tree/v1.21.1-10.7.19.85)で仕様を照合しています。参照した作成方針と再利用コードの出典は [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) に記載しています。独自部分はMITライセンスです。

## 標準UIへの接続

共通要件40・41に従い、章編成はPonder 1.0.87の `PonderTagIndexScreen` / `PonderTagScreen` へ委ねています。6章は登録順、章内のアイテムは教材登録順で標準画面へ渡します。

この版の `PonderChapter.of` は未実装で、タグ登録APIに独立したコース登録機能がありません。そのため `GuidePonderScreen` は標準 `PonderUI` の開始位置を設定する小さなアダプターとして維持します。`PonderUI.of(ItemStack, PonderTag)` の戻り値のうち自教材だけを対象にし、タグとアイテムに対応する開始位置を決めます。他MODのタグや混在した教材には介入しません。章の描画や独自の前後移動は実装しません。共有ポリシー本文は変更していません。

標準タグ画面の固定列・最大3行では、320×240のGUIに輸送章の29アイテムが収まりません。`PonderTagLayoutMixin` は自MODのタグだけ、標準のアイテムボタンを画面サイズに応じて配置・ページ分割します。ページ操作は標準の説明文を避けて左右端へ配置します。標準の章・再生・履歴操作を保持し、別の章画面は作成しません。

## 2026-10-30の言語監査

[日英の確認記録](docs/jaen-compliance-20261030.md)と[第二範囲20言語の確認記録](docs/second-scope-compliance-20261030.md)に、共通要件1〜75、具体的な意味レビュー、実測と再利用した証拠、完成JARの配置、未検証の範囲を記録しています。

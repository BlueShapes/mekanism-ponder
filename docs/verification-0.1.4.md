# 検証記録

2026-10-22、自然な解説口調へ推敲した0.1.4を、Minecraft 1.21.1 / NeoForge 21.1.256 / Java 21で確認しました。
Mekanism 10.7.19.85、Ponder 1.0.87+mc1.21.1、Flywheel 1.0.4を使用しています。

## 口調と教材の確認

日本語・英語の199キー、他20言語の各185本文キーと14ナビゲーション値をレビューしました。
通常操作は機構・状態・条件と結果として説明し、安全確認や診断に必要な呼びかけは残しています。
原文112キー、他20言語の計2215キーを改訂しました。
依頼語の件数だけを合否基準にはしていません。

| 場所 | 修正前 | 修正後 |
| --- | --- | --- |
| 入力面 | 材料を入れる面を入力に設定してください | 入力に設定した面から材料を受け入れます |
| 炭素 | 冶金吹込機へ石炭または木炭を補給し、炭素を用意してください | 石炭や木炭を冶金吹込機へ入れると、炭素が補給されます |
| 自動クラフト | 在庫管理と自動クラフトの設定を確認し、完成品を出力面から取り出せるようにしてください | 在庫管理と自動クラフトの設定が、機械の動作を決めます／完成品は出力に設定した面から取り出せます |

最後の例は二つの完結した表示単位で、HINT/NORMALを指定しました。
「撤去前に内容物を安全な処理先へ移し、GUIで空を確認してください」は安全確認として保持しました。
側面設定の操作場所、出力面、他化学物質出力の管理、原鉱と鉱石の倍率なども確認しています。
日本語とポーランド語のSolar Neutron Activatorの既存名を、対象JARの公式名へ統一しました。

模型、ID、章順、関連アイテム、数量、NBT・表示手順・搬送演出は0.1.3と一致します。
通常本文は白、警告は薄い赤、ヒントは薄い緑で、全22言語の6,248表示単位へ明示した意図を再確認しました。
輪郭色と本文色は独立です。文末句点省略、読点・必要な点、継続記号、英語フォールバックも保っています。

## アドオンの調査範囲

全28教材のrequiresMods、アイコン・関連アイテム・模型・吹き出しを調べました。
名前空間はmekanism/minecraftのみで、任意アドオン教材・汎用アドオン章はありません。
Generatorsは別MODとしての言及だけです。本体の6章を保持し、今回は新しいアドオンを追加していません。
新規第三者MOD・導入元情報の追加は0件です。既存のMekanism・Ponder・Flywheelを再利用しています。

## 検証結果

- 65件のテストが成功しました。教材・翻訳・ハッシュ・表示単位・継続・検索・フォント証拠の合成を対象にしています。
- 初回の全22言語実測では5,678字幕単位・17,034条件を確認し、PL/MSの核廃棄物2番目の助言だけが4行でした。
- 夜間停止と悪天候減速に備える助言を、それぞれ短く完結した文へ書き直しました。言葉や行数で断片を切ったものではありません。
- 対象2言語だけ再測定し、559単位すべてが最大3行で成功しました。現在の全文と実測文字列を完全照合し、描画クラスの同一性も確認した合成結果は、22言語・5,680単位・17,040条件で最大3行、幅超過0です。
- 字形・翻訳リソース・説明の折り返し・翻訳検索も確認しました。以前の句点除去だけの幅証拠は、今回の新しい本文の検証には使っていません。
- 変更した接続設定の日本語と各言語の字幕を画像保存し、標準WHITE/RED/GREENの実表示とツールチップ色を確認しました。
- 通常画面の独自ボタン0件、標準タグ・関連部品の起動・前後移動・戻る・言語とコースの履歴分離・英語本文と色へのフォールバックが成功しました。
- 代表3教材を描画しました。仮想描画例外は0件です。今回の検証クライアントと統合サーバーは終了しました。
- 配布JARは22言語×199キー・Java 21クラス38件を含み、検証クラスや依存MOD本体を同梱していません。

```powershell
python tools/guide_data.py check
python tools/check_narration.py
python -m unittest discover -s tools -p 'test_*.py'
.\gradlew.bat compileJava compileValidationJava jar --offline
python tools/check_release.py build/libs/mekanism-ponder-guide-0.1.4-neoforge-1.21.1.jar
powershell -NoProfile -File tools/validate-navigation.ps1 -MutexTimeoutMinutes 30
# 2箇所の推敲後、変更言語だけ再測定
powershell -NoProfile -File tools/validate-navigation.ps1 -ValidationLocales 'pl_pl,ms_my' -MutexTimeoutMinutes 30
```

共有ミューテックス`Local\PonderSeriesValidation`を使用しました。他担当の検証待ちを経て、必要な専用非表示クライアントだけ起動しています。
全28教材の全再生・巻き戻しは今回繰り返していません。変更していない模型・NBT・ModelDataは[過去の検証](verification-0.1.2.md)を引き継ぎます。
Wは物理キーを合成せずnativeファクトリーで確認しました。GUI寸法の再初期化による確認で、OSウィンドウのリサイズ試験ではありません。
指定Prismの全MOD構成での実起動と、実サーバーへの接続は今回行っていません。

## 完成JARと配置

- ファイル: `mekanism-ponder-guide-0.1.4-neoforge-1.21.1.jar`
- SHA-256: `cab82a9a7369f2e639491eb290ebf1c55200884b168fe926d6a49a85eb7f818e`
- 配置先: 指定PrismのMinecraft 1.21.1 / NeoForge 21.1.262
- 同じMOD IDと確認した旧0.1.3だけを除去し、0.1.4の配置後ハッシュ一致を確認しました。
- 開始基準は34ファイル/22.indexです。配置操作で他の33 JARと既存22.indexを保持しました。インスタンスは停止中でした。

記録: [新しい成功JSON](narration-validation-success.json)、[集計](validation-summary.json)、
[全22言語の幅集計](narration-font-summary.json)、[圧縮した全実測](narration-font-validation.json.gz)、
[日本語・英語と構造の比較](narration-source-review.json)、[色の意図](narration-presentation-review.json)、
[アジア7言語](narration-review-asia.json)、[西側7言語](narration-review-west.json)、[他6言語](narration-review-other.json)、[配置](deployment.json)。
生ログ・画像・最初の測定は`build/validation-run/`と`.work/`に残しています。

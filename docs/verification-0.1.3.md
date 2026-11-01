# 検証記録

2026-10-22、0.1.3をMinecraft 1.21.1 / NeoForge 21.1.256 / Java 21で確認しました。
依存はMekanism 10.7.19.85、Ponder 1.0.87+mc1.21.1、Flywheel 1.0.4です。

## 今回の結果

- 教材データと59件のテストが成功しました。28教材・113ステップ・12説明、22言語×199キーを検査しました。
- 22言語の原本・訳文から通常の文末句点7,040個を除きました。版番号・略語等の必要な点、読点、継続記号を保持しています。0.1.2の`993ed06`との機械比較で、句点除去・説明の文境界改行・未使用ボタンキー削除・コマンドキー改名以外の本文変更がないことを確認しました。
- 全22言語の125キー、計6,245表示単位へ通常・警告・ヒントを明示し、意味と各訳文独自の分割をレビューしました。本文と輪郭の色は別です。
- Title/Optionsに独自ガイドボタンがないこと、関連部品からのnative Wファクトリー、選択中教材の標準カテゴリー、同カテゴリーの別教材、章境界をまたぐ前後移動、戻る履歴を検証しました。
- nativeコースと全教材コースの双方向の履歴分離、異なるロケールの履歴分離、他MODの教材・混在コースへの非介入を確認しました。実際の`ScreenOpener.tryBackTracking`も使っています。
- 日本語の通常・ヒント・警告をnative TextWindowElementで表示し、WHITE/RED/GREENの実要素と画像を確認しました。日英では全説明のツールチップ追加行色と展開ヒント色も確認しました。
- アラビア語の全285字幕単位を実フォントで再測定し、180px・GUI幅320/480/960の855条件で最大2行、幅超過0でした。描画用グリフ188種、説明の折り返し、翻訳検索、代表画像を確認しました。
- 対象外ロケール`xx_zz`で英語本文と英語の色指定へのフォールバックを確認しました。
- 配布JARは22言語×199キー、Java 21クラス38件、言語別の色指定、メタデータ・Mixin・ライセンスを含みます。検証クラスと依存JAR・依存MODのクラスや素材は含みません。
- 仮想描画例外は0件で、今回起動した検証クライアントと統合サーバーは終了しました。

```powershell
python tools/guide_data.py check
python -m unittest discover -s tools -p 'test_*.py'
python tools/check_punctuation_migration.py
.\gradlew.bat compileJava compileValidationJava jar --offline
python tools/check_release.py build/libs/mekanism-ponder-guide-0.1.3-neoforge-1.21.1.jar
powershell -NoProfile -File tools/validate-navigation.ps1 -RepresentativeGui -MutexTimeoutMinutes 30
```

Minecraft検証は`Local\PonderSeriesValidation`で直列化し、専用の非表示クライアントと新規フラットワールドを使いました。
色比較のARGB/RGB不一致と、言語再読込中に旧フォントを描画するハーネスの問題を診断・修正してから成功しました。
文末句点を要求していた旧検査で新しい14テスト中8件が失敗することを確認してから修正しました（RED `af8a881`）。

## 引き継いだ検証と今回の範囲

全22言語の5,677字幕単位・17,031条件の実フォントとグリフ検査、代表6教材の全再生・巻き戻し・ModelDataは
[0.1.2の記録](verification-0.1.2.md)と[JSON](validation-0.1.2.json)を引き継ぎます。
今回は字幕境界・レイアウト処理・模型・NBT・タイミングを変更していないため、全再生と全22言語のMinecraft検証を繰り返していません。
句点除去でも字形や折り返しが変わり得るアラビア語は全単位を再測定しました。

W導線は標準の一引数ファクトリーから関連部品を開いて確認しており、物理キー入力の合成試験ではありません。
GUI幅の検証は画面を320/480/960へ再初期化したもので、OSウィンドウのリサイズ試験ではありません。
共用Prism環境へのJAR配置は確認していますが、その全MOD構成での実起動・実サーバー接続は今回行っていません。

## 成果物と配置

- JAR: `mekanism-ponder-guide-0.1.3-neoforge-1.21.1.jar`
- SHA-256: `aec301ab93050042521f1b1037004f8efffccfbcf1b3ef15386dee5e39c0f8a0`
- 指定Prismインスタンス: Minecraft 1.21.1 / NeoForge 21.1.262
- 0.1.3を配置し、MOD IDで確認した旧0.1.2だけを除去しました。Mekanism・Ponder・Flywheelは既存の適合版を再利用しました。
- 他の31 JARと`.index`内20ファイルは変更していません。配置時にインスタンスは停止していました。

詳細は[集計](validation-summary.json)、[句点の比較](punctuation-review.json)、[色のレビュー](presentation-review.json)、
[アラビア語の実測](arabic-font-validation.json)、[配置記録](deployment.json)にあります。
今回の生JSON・15枚の画像・ログは`build/validation-run/`と`.work/`に保存しています。

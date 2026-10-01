# 漢字落ちクイズ（Android）

仕様は `CLAUDE.md` を参照。Android Studio で開いて実機/エミュレータで実行する（Gradle Wrapper は Android Studio が生成/同期する）。

## 注意
- 書き問題は初回に ML Kit の日本語モデルをダウンロードします（ネット接続必須・初回のみ）。
- `app/src/main/assets/questions.json` は AI 生成のオリジナル例文です。**特に1級は読みや用法に誤りが混ざりやすいので、必ず内容を確認してください。**
- 問題データ生成元の語彙は各級50語。同じ語を読み問題・書き問題の両方に使っています。

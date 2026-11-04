# FloatingImage

ワッ…！！空中に……画像が浮かぶ……ってコト！？！？
しかも、ずっとこっちを見てくる……！！ヤハーーーッ！！！

Paper 1.21.11 用のプラグイン！！Java 21 で動く！！
前提プラグインはいらない！！ひとりで立てる……えらい……！！

- ソースコード … [AzisabaNetwork/FloatingImage](https://github.com/AzisabaNetwork/FloatingImage) ！！ここ！！
- リソースパックたち … [azisaba/resourcepaks](https://github.com/azisaba/resourcepacks) ！！画像はこっちに置く！！

## つかいかた！！

1. [azisaba/resourcepaks](https://github.com/azisaba/resourcepaks) に画像を置いて push する！！
2. GitHub Actions がリソースパックに変換してくれる！！なんか……勝手にやってくれる……！！
3. いつも通りリソースパックを更新する！！(sha1 とか)
4. サーバーで `/fimg reload` ！！
5. `/fimg place <ID>` で、目の前にドン！！ウラ！！

置いた画像はワールドに保存される！！再起動しても消えない！！やったーーッ！！

### 画像の名前！！

ファイル名がそのまま画像 ID になる！！
大文字は小文字になって、英数字と `_` `.` `-` 以外は `_` になる……ってコト！？

- `Server Logo.png` → `server_logo` ！！
- 長辺が 512px より大きい画像は、自動で縮む！！ちいさくなっちゃった……！！
- PNG の透明なところは、ちゃんと透明！！スケスケ……！！

### 向き！！(billboard)

| 値 | どうなる！？ |
|:--|:--|
| `center` | 上下左右、ずっとこっちを見てくる！！(最初はこれ) 見てる……！！ |
| `vertical` | 左右だけこっちを向く！！看板っぽい……！！ |
| `horizontal` | 上下だけこっちを向く！！使いどころ……ある……？ |
| `fixed` | 動かない！！置いたときにこっちを向いて、そのまま……フ…… |

## コマンド！！

権限は `floatingimage.admin` ！！(OP には自動で付く)
別名で `/floatingimage` も使える！！長い……！！

| コマンド | 内容 |
|:--|:--|
| `/fimg place <ID> [大きさ] [billboard]` | 目の前に画像を置く！！大きさは長辺のブロック数！！デカい……！！ |
| `/fimg remove [半径]` | 近くの画像を 1 つ消す！！(半径の初期値は 5 ブロック) 討伐ーーーッ！！ |
| `/fimg list` | 読み込んである画像の一覧！！いっぱいある……！！ |
| `/fimg reload` | 設定と画像の一覧を読み直す！！ |

`place` と `remove` はプレイヤーだけ！！コンソールからは……できない……泣いちゃった……！！

## セットアップ！！

### 1. azisaba/resourcepaks に追加する！！

```
floatingimage/images/        画像 (PNG / JPG) を置くところ！！
<パックのフォルダ>/          いつものパック！！ここの assets/floatingimage/ に変換結果が入る！！
```

- `floatingimage/` には `pack.mcmeta` がないから、zip が勝手に増えたりしない！！安心……！！
- jar は置かなくていい！！CI が毎回、最新版を取ってくる！！(下の「リリース！！」を見てね)
- 変換結果はコミットしない！！`<パックのフォルダ>/assets/floatingimage/` は `.gitignore` に入れておくと安全！！

### 2. CI に変換ステップを足す！！

いまのワークフローの `actions/checkout` のすぐあとに、これを 3 つ足すだけ！！
(全体の例は [`examples/resourcepack-ci.yml`](examples/resourcepack-ci.yml) ！！)

```yaml
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: FloatingImage の最新版を取得する
        run: curl -fsSL -o "$RUNNER_TEMP/FloatingImage.jar" https://github.com/AzisabaNetwork/FloatingImage/releases/latest/download/FloatingImage.jar

      - name: FloatingImage の画像をリソースパックに変換する
        run: java -jar "$RUNNER_TEMP/FloatingImage.jar" generate --images floatingimage/images --out <パックのフォルダ>
```

jar は `$RUNNER_TEMP` に置くから、パックの zip には入らない！！まぎれこまない……！！

zip を作る前に画像が組み込まれるから、そのあとの zip 化と Release はそのまま！！なんとかなれーーーッ！！！

### 3. プラグインを入れる！！

[Release](https://github.com/AzisabaNetwork/FloatingImage/releases/latest) の `FloatingImage.jar` を `plugins/` に入れて起動する！！

画像の一覧は、`server.properties` の `resource-pack` の zip から読む！！
別の URL から読みたいときは `plugins/FloatingImage/config.yml` に書く！！

```yaml
source: remote
remote:
  pack-url: https://github.com/<owner>/<repo>/releases/latest/download/<パック名>.zip
```

## しくみ！！

```
AzisabaNetwork/FloatingImage
  main に push → Actions がビルドして、Release に FloatingImage.jar を付ける！！
      │
      ▼ CI が最新版を取ってくる！！
azisaba/resourcepaks
  floatingimage/images/*.png を push！！
      │  Actions が CLI で変換して、パックの assets/floatingimage/ に出す！！
      │  → いつもの zip 化と Release！！
      ▼
サーバー (このプラグイン)
  Release の zip から assets/floatingimage/index.json を読んで、画像 ID を覚える！！
      ▼
/fimg place <ID> でドン！！
```

- **画像** … `item_model` を付けた紙を `ItemDisplay` で浮かせてる！！紙……！？
- **モデル** … 画像の縦横比に合わせた、厚みゼロの板！！ペラペラ……！！
- **明るさ** … いつでも最大！！夜でも洞窟でも、くっきり見える！！まぶしい……！！
- **パックの配布** … プラグインはやらない！！今まで通り `server.properties` とかでやる！！

## API！！

他のプラグインからも画像を置ける！！メインスレッドから呼んでね！！

```kotlin
FloatingImageAPI.spawn("server_logo", location, 3f)
```

```java
FloatingImageAPI.spawn("server_logo", location, 3f, Display.Billboard.CENTER);
```

| メソッド | 内容 |
|:--|:--|
| `ids()` | 読み込んである画像 ID の一覧！！ |
| `exists(id)` | その画像 ID ある！？ |
| `spawn(id, location, size, billboard)` | 画像を置いて、`ItemDisplay` を返す！！ |
| `isImageDisplay(display)` | このプラグインが置いた画像！？ |

## 設定！！

`config.yml` を見てね！！

| キー | 初期値 | 内容 |
|:--|:--|:--|
| `source` | `remote` | `remote`: 配布中のパックから画像の一覧を読む！！ / `local`: サーバーでパックを作る！！ |
| `load-on-enable` | `true` | 起動したときに画像の一覧を読む！！ |
| `remote.pack-url` | `""` | 画像を組み込んだパックの zip の URL！！空なら `server.properties` の `resource-pack` ！！ |
| `display.default-size` | `2.0` | 大きさを省略したときの長辺のブロック数！！ |
| `display.default-billboard` | `CENTER` | billboard を省略したときの向き！！ |
| `display.view-range` | `1.0` | 見える距離の倍率！！(1.0 で 64 ブロックくらい) 遠い……！！ |
| `display.place-distance` | `2.0` | `/fimg place` で目の前の何ブロック先に置くか！！ |

### local モード！！

GitHub を使わないで、`plugins/FloatingImage/images/` の画像からサーバーでパックを作る！！
できた `pack/` をいつものパックに混ぜるか、`pack.zip` を自分で配る！！プラグインは配らない……！！フ……

| キー | 初期値 | 内容 |
|:--|:--|:--|
| `pack.output-dir` | `pack` | 出力先！！(プラグインフォルダからの相対パスか、絶対パス) |
| `pack.create-zip` | `true` | zip にもまとめる！！ |
| `pack.zip-file` | `pack.zip` | zip の出力先！！ |
| `pack.pack-format` | `75` | `pack.mcmeta` がないときに書き込む値！！ |
| `pack.max-texture-size` | `512` | テクスチャの長辺の最大ピクセル数！！ |

## CLI！！

jar はひとりでも動く！！Paper がなくても、画像をリソースパックに変換できる！！えらい……！！

```bash
java -jar FloatingImage.jar generate --images <画像フォルダ> --out <出力先> [--max-size 512] [--pack-format 75]
```

出力先にできるもの！！`assets/floatingimage/` は毎回作り直す！！
もともとある `pack.mcmeta` は上書きしない！！さわらない……！！

```
pack.mcmeta                                  (ないときだけ)
assets/floatingimage/index.json              画像 ID の一覧！！サーバーが読む！！
assets/floatingimage/items/<ID>.json         item_model の定義！！
assets/floatingimage/models/item/<ID>.json   縦横比に合わせた板のモデル！！
assets/floatingimage/textures/item/<ID>.png  テクスチャ！！
```

## リリース！！

[AzisabaNetwork/FloatingImage](https://github.com/AzisabaNetwork/FloatingImage) の main に push すると、
Actions がビルドして Release を作る！！タグは `action-v<番号>` ！！勝手にできる……！！

Release には `FloatingImage.jar` が付く！！名前はいつも同じ！！
だから `releases/latest/download/FloatingImage.jar` で、いつでも最新版が取れる……ってコト！？！？

- サーバーに入れるプラグインも、resourcepaks の CI が使う CLI も、この同じ jar ！！
- 壊れた版を main に push すると、resourcepaks のビルドも一緒に壊れる！！ヤダーーーッ！！！
  push する前に、手元でビルドと動作を確かめてね！！

## ビルド！！

```bash
./gradlew build
```

`build/libs/FloatingImage-<version>-all.jar` が、プラグインにも CLI にもなる！！一石二鳥……ってコト！？！？

プルルルル……ヤハ！！！

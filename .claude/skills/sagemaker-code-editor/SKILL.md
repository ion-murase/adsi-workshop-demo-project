---
name: sagemaker-code-editor
description: SageMaker Studio Code Editor 上で SPA をブラウザプレビューするための設定・起動・トラブルシュート。新規プロジェクト立ち上げ、dev:sagemaker 設定、プレビュー不具合時に使う。
---

# SageMaker Code Editor ブラウザプレビュー

SageMaker Studio **Code Editor**（code-server, base-path `/codeeditor/default`）上で
Next.js 等の SPA をブラウザから安定してプレビューするための知識。

## いつ使う

- 新規プロジェクトで SageMaker プレビュー環境を構築するとき
- `Unsupported URL path` / 404 / API 全滅などプレビュー不具合の調査
- `dev:sagemaker` スクリプト・`next.config`・プロキシの変更前後

## 最重要ルール（ミス禁止）

| やる | やらない |
|------|---------|
| アクセス URL は **`absports`** 形式にする | **`ports`** 形式のまま使う（SPA で二重化 → Unsupported URL path） |
| **`npm run dev:sagemaker`** で起動 | SageMaker 上で `npm run dev`（basePath なし → 404） |
| basePath = **フルパス** `/codeeditor/default/absports/3000` | basePath = `/absports/3000` のみ（リダイレクトで URL 欠落） |
| 全 `fetch` に **`withBasePath()`** を適用 | `fetch("/api/...")` の絶対パス（ゲートウェイ直下に飛ぶ） |
| プロキシは **`packages/frontend/scripts/sagemaker-proxy.mjs`** | リポジトリ直下の旧プロキシ（存在しても使わない） |
| 分岐は **`SAGEMAKER=1`** フラグ | `NODE_ENV` だけで SageMaker モード判定 |

## ブラウザで開く（PORTS タブ）

**地球儀ボタンを使ってよい。** ただし URL を `absports` 形式に直す。

1. `npm run dev:sagemaker` で起動
2. PORTS タブで 3000 の**地球儀ボタン**を押す
3. 開いた URL の **`ports` を `absports` に置換**して Enter

```
# 地球儀が開く URL（そのままだと SPA で壊れる）
https://<studio-domain>/codeeditor/default/ports/3000/

# ブラウザのアドレスバーで置換後（正しい入口）
https://<studio-domain>/codeeditor/default/absports/3000/
```

手打ちで `absports` URL を開いてもよい。

## なぜ `ports` のままではダメか

code-server のプロキシは `ports` と `absports` の2形式を持つ:

- **`ports`**: 応答 `Location` に `/ports/<port>` を前置 → SPA の basePath 付きリダイレクトと二重化
  → `/ports/3000/ports/3000` → **Unsupported URL path**
- **`absports`**: 前置しない → フル basePath + 復元プロキシと組み合わせれば安定

地球儀は `ports` 形式の URL を生成する。**ボタン自体は問題なく、URL を `absports` に直せばよい。**

## アーキテクチャ（正しい構成）

```
ブラウザ  https://<domain>/codeeditor/default/absports/3000/...
  └ ゲートウェイ → code-server(8888) が "/codeeditor/default" を剥がす
    └ absports は剥がさず → proxy(:3000) に "/absports/3000/..." が届く
      └ "/codeeditor/default" を前置して next(:3001) へ転送
```

| ポート | 役割 |
|--------|------|
| 8080 | Backend（workshop プロファイル, H2） |
| 3001 | Next.js `next start`（127.0.0.1 にバインド） |
| 3000 | 復元プロキシ（外から見える入口） |

## 新規プロジェクト立ち上げチェックリスト

SPA + API バックエンドを SageMaker Code Editor でプレビューする場合、以下をすべて満たす:

### 1. 環境変数・フラグ

- [ ] `SAGEMAKER=1` — static export 無効化、rewrites 有効化の分岐に使う（`NODE_ENV` と独立）
- [ ] `NEXT_PUBLIC_BASE_PATH=/codeeditor/default/absports/3000` — フルパス

### 2. Next.js 設定 (`next.config.ts`)

- [ ] `isSagemaker = process.env.SAGEMAKER === "1"`
- [ ] SageMaker 時: `output: "export"` を**無効**
- [ ] `basePath` / `assetPrefix` = `NEXT_PUBLIC_BASE_PATH`
- [ ] `skipTrailingSlashRedirect: true`（SageMaker 時）
- [ ] `/api/*` → `localhost:8080` の rewrites（SageMaker 時も有効）
- [ ] `turbopack.root` を frontend パッケージに明示（monorepo 誤認防止）

### 3. 復元プロキシ (`packages/frontend/scripts/sagemaker-proxy.mjs`)

- [ ] 3000 で listen、`/codeeditor/default` を前置して 3001 へ転送
- [ ] next の basePath がフルパスなので、next 側の Location / アセット URL は正しいパスになる

### 4. API クライアント

- [ ] `withBasePath(path)` を実装し、**すべての fetch** と `window.location` 遷移に適用
- [ ] basePath 空（ローカル / 本番 static）ではパスをそのまま返す

### 5. 起動スクリプト (`scripts/dev-sagemaker.sh`)

- [ ] 既存プロセス停止（3000/3001/8080）
- [ ] `SAGEMAKER=1` + `NEXT_PUBLIC_BASE_PATH` を export
- [ ] `next build` → backend `bootRun --spring.profiles.active=workshop`
- [ ] `next start -H 127.0.0.1 -p 3001`（IPv6 バインド回避）
- [ ] `node packages/frontend/scripts/sagemaker-proxy.mjs`
- [ ] SageMaker 環境では `JAVA_HOME=/opt/mise/installs/java/corretto-21`（存在する場合）

### 6. 依存関係

- [ ] `cd packages/frontend && npm install`（ルートの setup とは別に必要）

### 7. 検証

```bash
curl -s -o /dev/null -w "status=%{http_code} location=%{redirect_url}\n" \
  http://localhost:3000/absports/3000/
# 期待: status=307 location=.../codeeditor/default/absports/3000/dashboard（接頭辞欠落なし）
```

## 起動・停止（このプロジェクト）

```bash
npm run dev:sagemaker        # ビルド + 全サービス起動
npm run dev:sagemaker:stop   # 停止
```

## トラブルシューティング

| 症状 | 原因 | 対処 |
|------|------|------|
| `/ports/3000/ports/3000` Unsupported URL path | URL が `ports` 形式のまま | アドレスバーで `ports` → `absports` に置換 |
| `/codeeditor/default/absports/3000` が 404 | `npm run dev` で起動している | `npm run dev:sagemaker` に切替 |
| `next: not found` / Turbopack workspace エラー | frontend の `node_modules` 未インストール | `cd packages/frontend && npm install` |
| 画面は出るが API 全滅 | `fetch("/api/...")` が basePath 未適用 | `withBasePath()` を全 fetch に適用 |
| 307 後に `/absports/3000/...`（codeeditor 欠落） | basePath が短いパスのみ | フル basePath + 復元プロキシに戻す |

## 参照

- 経緯・実測: `docs/path/14-sagemaker-codeeditor-preview.md`
- 自動適用ルール: `.claude/rules/sagemaker-preview.md`
- 起動コマンド一覧: `.claude/skills/dev-environment/SKILL.md`
- AWS へのデプロイ（プレビューではなく本番配信）: `.claude/skills/sagemaker-aws-deploy/SKILL.md`

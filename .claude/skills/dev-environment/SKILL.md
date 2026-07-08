---
name: dev-environment
description: ローカル開発と SageMaker Code Editor での起動・アクセス方法。アプリ起動、環境確認、新規セットアップ時に使う。
---

# 開発環境 — 起動とアクセス

## 初回セットアップ

```bash
npm run setup    # backend Gradle + frontend npm + infra npm
```

frontend 単体で作業する場合も **`cd packages/frontend && npm install`** が必要（SageMaker ビルド失敗の典型原因）。

## ローカル開発

| 目的 | コマンド | アクセス先 |
|------|---------|-----------|
| Backend | `npm run boot` | http://localhost:8080 |
| Backend (H2, Docker 不要) | `npm run boot:workshop` | http://localhost:8080 |
| Frontend dev | `npm run dev` | http://localhost:3000 |
| Backend チェック | `npm run check:backend` | — |
| Frontend lint | `npm run lint:frontend` | — |

## SageMaker Code Editor

**詳細・チェックリストは [sagemaker-code-editor スキル](../sagemaker-code-editor/SKILL.md) を必読。**

```bash
npm run dev:sagemaker        # ビルド + backend + frontend + プロキシ一括起動
npm run dev:sagemaker:stop   # 停止
```

### ブラウザで開く

1. PORTS タブで 3000 の**地球儀ボタン**を押す
2. 開いた URL の **`ports` を `absports` に置換**して Enter

```
# 地球儀が開く URL
https://<studio-domain>/codeeditor/default/ports/3000/

# 置換後（正しい入口）
https://<studio-domain>/codeeditor/default/absports/3000/
```

`ports` のままだと SPA で `/ports/3000/ports/3000` となり Unsupported URL path になる。

## 環境の見分け

| 環境 | Frontend 起動 | basePath | Backend DB |
|------|----------------|----------|------------|
| ローカル dev | `npm run dev` | なし | PostgreSQL or H2 |
| SageMaker | `npm run dev:sagemaker` | `/codeeditor/default/absports/3000` | H2 (workshop) |
| 本番 | static export | なし | RDS 等 |

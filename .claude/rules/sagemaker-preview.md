---
description: SageMaker Code Editor プレビュー設定の必須制約。dev スクリプト・next.config・プロキシ・API クライアント変更時に適用する。
---

# SageMaker プレビュー制約

> 背景・チェックリスト: `.claude/skills/sagemaker-code-editor/SKILL.md`

## ブラウザアクセス

- PORTS タブの**地球儀ボタンは使ってよい**
- 開いた URL の **`ports` を `absports` に置換**してから使う
- 正しい形式: `https://<studio-domain>/codeeditor/default/absports/3000/`
- `ports` 形式のまま SPA を開くと Location 二重化で **Unsupported URL path**

## 禁止事項

- SageMaker 上での `npm run dev` をプレビュー手順として案内
- basePath を `/absports/3000` のみにする
- `fetch("/api/...")` 等、basePath 未適用の絶対パス fetch
- `SAGEMAKER` モード判定を `NODE_ENV` のみに依存

## 必須構成

1. フル basePath: `/codeeditor/default/absports/3000`
2. `SAGEMAKER=1`: static export 無効 + API rewrites 有効
3. proxy :3000 → next :3001、backend :8080
4. 復元プロキシで `/codeeditor/default` を受信時に前置
5. `withBasePath()` を全クライアント fetch / location 遷移に適用
6. `next start -H 127.0.0.1 -p 3001`

## 変更時の確認

```bash
curl -s -o /dev/null -w "status=%{http_code} location=%{redirect_url}\n" \
  http://localhost:3000/absports/3000/
```

307 の Location に `/codeeditor/default/absports/3000/...` が含まれること。

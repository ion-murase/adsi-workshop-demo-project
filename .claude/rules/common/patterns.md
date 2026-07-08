---
description: プロジェクト共通のアーキテクチャパターン。設計・実装時に適用する。
---

# 共通パターン

> Backend 実装詳細: [../java-spring-boot.md](../java-spring-boot.md)
> 開発フロー: [development-process.md](./development-process.md)

## レイヤード + ライト DDD

| 使う | 使わない（必要になったら導入） |
|------|---------------------------|
| Entity — DB テーブル対応 | Aggregate Root — Entity が担う |
| Value Object — 勤務時間、時刻等 | Domain Event |
| Repository — interface で定義 | 仕様パターン |
| Service — ドメインロジックの調整役 | |

## 依存方向

```
Controller → Service (interface) → Repository (interface) → Entity
```

- Controller は Repository を直接呼ばない
- Entity は他レイヤーに依存しない

## API 設計

- OpenAPI (YAML) で定義（`docs/design/`）
- 適切な HTTP ステータスコード
- バリデーションエラーはフィールド名 + メッセージ
- 内部エラー詳細をクライアントに返さない

---
name: multi-agent-review
description: 複数タイプの subagent に並列レビューさせるコードレビュー。コード変更後・コミット前に使う。subagent は sonnet モデル。.claude/agents/ を使用。
---

# Multi-Agent Code Review

`.claude/agents/` の専門 subagent が独立した視点で diff をレビューし、親エージェントが結果を統合する。

## いつ使う

- コード変更後
- コミット / PR 前
- 「レビューして」と依頼されたとき

## Subagent モデル

**すべて `model: sonnet`**

- `.claude/agents/*.md` の frontmatter に設定済み
- Task 起動時も `model: sonnet` を明示

## 起動する subagent

| 変更 | subagent_type |
|------|---------------|
| `packages/backend/**/*.java` | `java-reviewer` |
| `packages/frontend/**/*.{ts,tsx}` | `typescript-reviewer` |
| 認証・入力・DB・設定 | `security-reviewer`（原則常時） |
| テスト・新機能 | `test-reviewer` |

## ワークフロー

### 1. 事前確認

```bash
git diff --stat && git diff
npm run check:backend    # backend 変更時
npm run lint:frontend    # frontend 変更時
```

### 2. 並列起動

**1 メッセージで複数 Task を並列 dispatch**（直列化しない）。

```
readonly: true
model: sonnet
subagent_type: <上表から選択>
```

prompt:

```text
Full Repository Path: <repo 絶対パス>
Diff: uncommitted changes
Custom Instructions: .claude/rules/ に準拠。findings のみ、修正しない。80% 以上確信できる問題のみ。
```

### 3. 統合・判定

| Verdict | 条件 |
|---------|------|
| Approve | CRITICAL / HIGH なし |
| Warning | HIGH のみ |
| Block | CRITICAL あり |

## Agents 定義

| ファイル | 役割 |
|---------|------|
| `agents/java-reviewer.md` | Backend |
| `agents/typescript-reviewer.md` | Frontend |
| `agents/security-reviewer.md` | セキュリティ |
| `agents/test-reviewer.md` | テスト |

## 参照

- `.claude/rules/common/code-review.md`
- `.claude/skills/verify/SKILL.md`

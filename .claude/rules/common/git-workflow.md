---
description: Git コミット・PR のルール。コミットや PR 作成時に適用する。
---

# Git ワークフロー

> 実装前の Plan → TDD → レビューの流れは [development-process.md](./development-process.md) を参照。

## コミットメッセージ形式

```
<type>: <description>

<optional body>
```

**type**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

- 1–2 文で「なぜ」を説明する（「何を」だけにしない）
- ユーザーが明示的に依頼したときだけコミットする

## Pull Request

PR 作成時:

1. ベースブランチからの全コミット履歴を確認する（最新 1 件だけ見ない）
2. `git diff [base-branch]...HEAD` で変更全体を把握する
3. Summary と Test plan を含む PR 本文を書く
4. 新規ブランチは `git push -u origin HEAD` で push する

## 安全な Git 操作

- `git config` は変更しない
- `--force`, `hard reset` 等の破壊的操作はユーザー明示指示がない限り行わない
- `--no-verify` 等でフックをスキップしない
- main/master への force push は警告する

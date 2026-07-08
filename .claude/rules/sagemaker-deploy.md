---
description: SageMaker Code Editor から AWS へデプロイする際の必須制約。cdk deploy / docker build / s3 sync / cloudformation 操作を SageMaker 上で行う変更・案内時に適用する。
---

# SageMaker デプロイ制約

> 背景・症状・コード例: `.claude/skills/sagemaker-aws-deploy/SKILL.md`
> ※ GitHub Actions（ランナー実行）には SageMaker 特有の制約（Docker/S3/CFN）は当てはまらない。

## 大原則

SageMaker Execution Role は **CDK bootstrap ロールの assume のみ**許可され、
アプリリソースや CloudFormation を直接操作できない。
→ **Docker ビルドも S3 アップロードもすべて CDK（CloudFormation）経由に寄せる**。

## 禁止事項（SageMaker 上で素の実行）

- ネットワーク指定なしの `docker build`（Forbidden になる）
- アプリ用バケットへの `aws s3 sync` / 直接アップロード（AccessDenied）
- `aws cloudformation ...` を SageMaker Role のまま直接実行（AccessDenied）
- 空の ECR を前提に `fromEcrRepository()` で ECS を先に立てる（クラッシュループ）

## 必須構成

1. Docker ビルドは **`--network=sagemaker`**（CDK は `NetworkMode.custom("sagemaker")`）
2. backend イメージは **`fromAsset()`** でビルド＋push を CDK 内に一体化
3. frontend 配信は **`BucketDeployment`** で CFN 経由アップロード
4. CloudFormation / スタック操作は **CDK deploy ロールを assume** してから実行
5. `ROLLBACK_COMPLETE` は自動復旧しない → **delete → recreate**

## つまずき早見表

| # | 問題 | SageMaker 特有? | 解決策 |
|---|------|:---:|--------|
| 1 | `docker build` Forbidden | Yes | `--network=sagemaker` |
| 2 | ECR 空でタスク失敗 | No | `fromAsset()` で一体化 |
| 3 | S3 直接操作不可 | Yes | `BucketDeployment` |
| 4 | `ROLLBACK_COMPLETE` | No | delete → recreate |
| 5 | CloudFormation 直接操作不可 | Yes | deploy ロールを assume |

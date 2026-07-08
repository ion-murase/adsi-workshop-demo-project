---
name: sagemaker-aws-deploy
description: SageMaker Studio Code Editor 上から AWS（CDK / ECS / S3+CloudFront）へデプロイする際のつまずきと回避策。docker build forbidden、ECR 空でのタスク失敗、S3 直接アップロード不可、ROLLBACK_COMPLETE、CloudFormation 直接操作不可を扱う。
---

# SageMaker Code Editor から AWS へデプロイ

SageMaker Studio **Code Editor** の実行環境は、通常の開発マシンと **Docker デーモンと IAM 権限が違う**。
そのため `cdk deploy` / `docker build` / `aws s3 sync` を素直に叩くと詰まる箇所がある。
ここでは実際に踏んだ 5 つのつまずきと回避策をまとめる。

> このスキルは **SageMaker Code Editor から手動デプロイする** ケース向け。
> `main` push で回る GitHub Actions（`.github/workflows/deploy.yml`）はランナー上で動くため、
> ここの SageMaker 特有の制約（1・3・5）は当てはまらない。経緯は `docs/path/14-aws-deploy-infrastructure.md`。

## つまずき早見表

| # | 問題 | SageMaker 特有? | 解決策 |
|---|------|:---:|--------|
| 1 | `docker build` が Forbidden | Yes | `--network=sagemaker` を指定 |
| 2 | ECR が空で ECS タスク起動失敗 | No | `fromAsset()` でビルド＋push を一体化 |
| 3 | S3 直接アップロードが AccessDenied | Yes | CDK `BucketDeployment` で CFN 経由にする |
| 4 | `ROLLBACK_COMPLETE` で再デプロイ不可 | No | スタックを delete → recreate |
| 5 | CloudFormation 直接操作が AccessDenied | Yes | CDK deploy ロールを assume してから実行 |

**推奨方針**: SageMaker からデプロイするなら、Docker ビルドも S3 アップロードも
**すべて CDK（CloudFormation）経由に寄せる**。SageMaker Execution Role は
「CDK bootstrap ロールを assume する」ことだけが許されており、アプリリソースを
直接触る権限がない。CDK に寄せれば Lambda / CodeBuild が代わりに実行してくれる。

---

## 1. Docker ビルドがブロックされる【SageMaker 特有】

### 症状

```
docker build ...
Forbidden. Reason: [ImageBuild] 'sagemaker' is the only user allowed network input
```

### 原因

SageMaker Studio の Docker デーモンはネットワークアクセスを制限しており、
`sagemaker` という名前のネットワーク経由でしかビルド中の通信（依存取得等）を許可しない。

### 解決策

`docker build` に `--network=sagemaker` を付ける。CDK の `fromAsset()` を使う場合は
`networkMode` を指定する:

```ts
import * as ecrassets from "aws-cdk-lib/aws-ecr-assets";

const image = ecs.ContainerImage.fromAsset("path/to/backend", {
  networkMode: ecrassets.NetworkMode.custom("sagemaker"),
});
```

手動 build の場合:

```bash
docker build --network=sagemaker -t backend .
```

---

## 2. ECR が空で ECS タスク起動失敗【一般的な CDK 設計問題】

### 症状

`fromEcrRepository()` でデプロイしたが、ECR にイメージがない状態のため
ECS Service が起動 → イメージ pull 失敗 → クラッシュループ → スタック `ROLLBACK`。

### 原因

**インフラ作成（ECR リポジトリ）とイメージ push が分離**されている。
CDK deploy だけでは空の ECR ができるだけで、ECS が pull できるイメージが存在しない。

### 解決策

`fromAsset()` を使い、**CDK deploy 内で Docker ビルド＋ECR push を一体化**する。
これで「デプロイ時点で必ずイメージがある」状態を保証できる。

```ts
const container = taskDefinition.addContainer("backend", {
  image: ecs.ContainerImage.fromAsset("path/to/backend", {
    networkMode: ecrassets.NetworkMode.custom("sagemaker"), // つまずき 1 対策
  }),
  // ...
});
```

> 現状のこのプロジェクトの `attendance-stack.ts` は `fromEcrRepository()` を採用し、
> イメージ push は GitHub Actions が担当する構成。SageMaker から手動で回すなら
> 上記 `fromAsset()` へ切り替えるのが一番ハマらない。

---

## 3. S3 への直接アップロードが権限不足【SageMaker 特有・IAM 制約】

### 症状

```
aws s3 sync out/ s3://attendance-frontend-dev-xxx/ --delete
An error occurred (AccessDenied) ...
```

CDK bootstrap の file-publishing ロールを使ってもアプリ用バケットには届かない。

### 原因

SageMaker Execution Role は **CDK bootstrap ロールの assume のみ**許可されており、
アプリリソース（フロント用 S3 バケット等）への直接操作権限を持たない。
file-publishing ロールは **bootstrap assets バケット専用**で、アプリバケットには使えない。

### 解決策

CDK の `BucketDeployment` コンストラクトを使い、**CloudFormation 経由で Lambda が
アップロード**する。SageMaker Role は「CDK deploy を叩く」だけでよくなる。

```ts
import * as s3deploy from "aws-cdk-lib/aws-s3-deployment";
import * as path from "path";

new s3deploy.BucketDeployment(this, "DeployFrontend", {
  sources: [s3deploy.Source.asset(path.join(__dirname, "../../frontend/out"))],
  destinationBucket: frontendBucket,
  distribution,                    // 併せて CloudFront invalidation もやってくれる
  distributionPaths: ["/*"],
});
```

> 現状の `attendance-stack.ts` は `frontendBucket` を作るだけで、配信は
> GitHub Actions の `aws s3 sync` が担当。SageMaker から手動デプロイするなら
> `BucketDeployment` を足すと `aws s3 sync` が不要になる。

---

## 4. ROLLBACK_COMPLETE スタックの処理【一般的】

### 症状

初回デプロイ失敗後、スタックが `ROLLBACK_COMPLETE` で残り、`cdk deploy` を
再実行しても「この状態からは更新できない」と拒否される。

### 原因

`ROLLBACK_COMPLETE` は「作成に失敗してロールバックした」終端状態。
`cdk deploy` はこれを自動削除しないため、手動削除が要る。

### 解決策

スタックを削除してから作り直す:

```bash
# SageMaker からは deploy ロールを assume して実行（つまずき 5 参照）
aws cloudformation delete-stack --stack-name AttendanceStack-dev
aws cloudformation wait stack-delete-complete --stack-name AttendanceStack-dev

# 削除後に再デプロイ
cd packages/infra && npx cdk deploy --context env=dev
```

---

## 5. CloudFormation 直接操作の権限がない【SageMaker 特有・IAM 制約】

### 症状

```
aws cloudformation describe-stacks ...
aws cloudformation cancel-update-stack ...
An error occurred (AccessDenied) ...
```

SageMaker Execution Role のままだと CloudFormation を直接叩けない。

### 原因

SageMaker Role にはアプリ / CloudFormation の直接操作権限がなく、
**CDK deploy ロールの assume だけ**が許可されている。

### 解決策

CDK deploy ロールを assume してから CloudFormation コマンドを実行する。

```bash
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=ap-northeast-1
ROLE_ARN="arn:aws:iam::${ACCOUNT}:role/cdk-hnb659fds-deploy-role-${ACCOUNT}-${REGION}"

CREDS=$(aws sts assume-role \
  --role-arn "$ROLE_ARN" \
  --role-session-name cfn-ops \
  --query 'Credentials.[AccessKeyId,SecretAccessKey,SessionToken]' \
  --output text)

export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | cut -f1)
export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | cut -f2)
export AWS_SESSION_TOKEN=$(echo "$CREDS" | cut -f3)

# これ以降は deploy ロール権限で叩ける
aws cloudformation describe-stacks --stack-name AttendanceStack-dev
```

> `cdk-hnb659fds-...` の `hnb659fds` は CDK bootstrap のデフォルト qualifier。
> `--context @aws-cdk/core:bootstrapQualifier` をカスタムしている場合は読み替える。

---

## SageMaker から詰まらずデプロイする最短手順

1. **CDK を「全部 CDK 経由」構成にする**（初回のみ）
   - backend: `fromAsset()` + `networkMode: custom("sagemaker")`（つまずき 1・2）
   - frontend: `BucketDeployment`（つまずき 3）
2. bootstrap 済みか確認（未実施なら `npx cdk bootstrap`）
3. frontend を先にビルド（`BucketDeployment` の source を作る）
   ```bash
   cd packages/frontend && npm run build   # out/ を生成
   ```
4. deploy
   ```bash
   cd packages/infra && npx cdk deploy --context env=dev
   ```
5. 失敗して `ROLLBACK_COMPLETE` になったら → delete → recreate（つまずき 4・5）

## 参照

- 自動適用ルール: `.claude/rules/sagemaker-deploy.md`
- デプロイ基盤の経緯・CI 構成: `docs/path/14-aws-deploy-infrastructure.md`
- CDK スタック本体: `packages/infra/lib/attendance-stack.ts`
- プレビュー（デプロイではなくローカル確認）: `.claude/skills/sagemaker-code-editor/SKILL.md`

# C14 Tenant Isolation Adversarial v1 可追溯表

## 证据身份

- Release：`tenant-isolation-adversarial-v1`
- Evidence Git HEAD：`dc9e3e6ed1434989a646b36389d6d9eeeea4ea83`
- 固定样本：26 required cases，12 categories
- 正式结果：`PASS`，functional/content/error/timing 四通道均为 `PASS`
- 输出：[summary](../reports/c14-tenant-isolation-report-v1.md)、[details](../reports/c14-tenant-isolation-details-v1.json)、[case evidence](../reports/c14-tenant-isolation-evidence-v1.json)

## Evaluation delta

| Requirement / scenario | Case 或契约证据 | Test / evaluator 证据 |
|---|---|---|
| Versioned release / 固定 release | manifest、schema、26-case JSONL、evidence map exact hash/bytes/order/count | `test_tenant_isolation_eval_contract.py`；`--plan-only` 为 `VALID` 且 `executionStarted=false` |
| Versioned release / 身份漂移 | artifact hash、ordered IDs、release/driver/fixture/profile identity | contract drift/mismatch tests；assembler Git HEAD 与 driver identity mismatch tests |
| Versioned release / 非法 contract | duplicate、unknown driver/enum、unsafe path、missing control、quota drift | contract negative tests，均在 backend/container/provider 前失败 |
| Isolated fixture / 完整执行 | 全部 26 cases；自有 MySQL/Redis/etcd/MinIO/Milvus | `C14IsolationAdversarialIT#fixedSyntheticMatrix`；`C14IsolationProfileContractTest` |
| Isolated fixture / deterministic sync/stream | `sync-*`、`stream-*` | C14 IT 的 test-only deterministic embedding/generation；provider calls=0、business outbound=false |
| Isolated fixture / 基础设施不可评 | driver health/ownership/image identity | profile contract、assembler/evaluator missing/identity/skip tests |
| Surface complete / 组合攻击 | identity、guessing、public/permission、reserved filter、cache/task、vector/keyword、sync/SSE、history/feedback cases | C14 IT + evidence map 指定的 71 个 Surefire tests；26/26 case evidence |
| Surface complete / vector→keyword | `vector-foreign-id`、`keyword-fallback-scope` | `MilvusFailureSemanticsIT` 的 tenant adapter 与 stop/start fallback tests |
| Surface complete / contract breach | 固定 case identity 未改 | `MilvusVectorStoreFailureSemanticsTest#scopedUpsertShouldIgnoreNullMetadataValuesBeforeAddingServerScope`；`TaskControllerTest#crossTenantTaskLookupUsesTenantScopedProjectionAndReturnsNotFound` |
| Error/timing / foreign fingerprint | `error-foreign-kb-fingerprint`、`error-foreign-task-fingerprint` | C14 IT matched nonexistent controls；details 的 error channel=`PASS` |
| Error/timing / coarse profile | `timing-foreign-kb`、`timing-foreign-public-kb` | 10 warmup + 40 measured pairs + seed 14001；median/P95 gate=`PASS` |
| Error/timing / incomplete evidence | timing identity/completeness rules | evaluator timing incomplete、request error、profile drift tests返回非 PASS |
| Status / 完整 PASS | 26 observed、0 missing/unexpected/failed/errors/skipped | evaluator exit 0；summary/details identity 和 counts exact match |
| Status / 成功子集不得掩盖失败 | missing/duplicate/unexpected/error/required skip/channel failure | evaluator negative tests固定返回 `FAIL/NOT_EVALUABLE/INVALID` 与非零退出码 |
| Status / 输出安全 | case ID、bounded taxonomy、计数、版本和聚合 timing | raw token/body/content/canary/用户绝对路径扫描 0 命中；no-overwrite 生效 |

## RAG system delta

| Requirement / scenario | 证据 | 结论边界 |
|---|---|---|
| C14 gate / C13b 完成但 C14 非 PASS | evaluator 对非完整 evidence 固定非零退出 | C13a/C13b、mock、single-tenant、`PARTIAL`、`RETRIEVAL_ONLY` 均不能替代 C14 |
| C14 gate / 固定攻击矩阵通过 | 正式 report=`PASS`，release/profile/Git HEAD 固定 | 只允许声明 Milvus 支持配置和固定 synthetic matrix 下 evidence 通过 |
| C14 gate / 不自动开放能力 | `.ai/ACTIVE_TASK.md` 与 change scope 保留独立事前闸门 | 不开放生产 tenant CRUD/switch、第二业务 tenant、C15 或 C16 |
| Supported profile / 未验证 adapter | `VectorStoreTenantEnforcementGuardTest` | Qdrant/Elasticsearch 在 enforcement mode 继续 fail startup，Milvus evidence 不外推 |
| Supported profile / 未授权真实迁移 | report 的 `realMaintenanceStatus=SKIPPED` | 未连接、复制、切换、重试或清理真实 Milvus collection |
| Supported profile / 生产级声明 | summary 的 claim boundary 与 timing boundary | 不声称生产级多租户、渗透测试、容量/合规或所有 timing side-channel 已验证 |

## 相邻边界验证

- tenant claim/session、global token blacklist/IP rate-limit、durable input 跨租户/open/delete/cleanup/traversal/symlink，以及 Qdrant/Elasticsearch fail-startup：聚焦 58 tests / 0 failures / 0 errors / 0 skipped。
- C14 映射 Surefire：71 tests / 0 failures / 0 errors / 0 skipped。
- C14 Failsafe：5 tests / 0 failures / 0 errors / 0 skipped。
- Python：190 tests / OK。
- 全仓 Maven：`rag-admin` 217 tests / 1 failure / 0 errors / 2 skipped；唯一失败为既有 OTel collector 时序断言，独立复跑通过，因此全仓状态不记为 GREEN。

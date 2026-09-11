# C17 验收与归档记录

## 2026-09-10 C17 已验收归档

用户批准完整审阅包中的12项hard floor/tolerance及归档。canonical profile 已为 `v1 / ACTIVE / APPROVED`，locked median reference 绑定最终profile SHA-256。固定v2/150×3 reference为450/450、COMPLETE；原始六文件哈希复核通过，三轮离线重放各12/12 required rules PASS。缺样本、身份漂移、错误分别NOT_EVALUABLE/4；完整质量退化FAIL/3。244项Python tests通过。

本次激活/验收backend/provider calls=0、业务数据出站=0；来源仍是2026-09-08 HEAD `082b030` 的c17g3热embedding缓存检索，不能作为冷缓存性能基准。Recall@5=47.44%、MRR=0.52615、Top1=92.31%；只建立固定开发态retrieval回归门禁，不证明generation/citation/no-answer回答质量/judge、生产SLA或Agentic RAG。固定评测KB的真实模型重建/强读回/CAS已完成，其他业务KB迁移及失败generation/source清理不在验收范围。Docker socket复发根因仍未证实。

证据：`docs/eval/references/c17-activation-approval-v1.json`、`c17-retrieval-reference-active-v1.json`、`rag-eval-dev-v2-retrieval-reference-v1.json`、三份`c17-replay-run*-v1.json`。历史DRAFT review/full执行摘要保留原样；它们记录激活前状态，不能覆盖本次已批准状态。

## 契约与证据映射

| Requirement | 验收依据 |
| --- | --- |
| Versioned Retrieval Reference Plan And External-call Boundary | tracked manifest、canary/full3执行摘要；最新预算为11/1353 query embedding上限，原5/450按2026-09-08已批准修订覆盖 |
| Complete Fixed-identity Retrieval Reference Evidence | full3全部450 observation、6份raw哈希、strict compiler COMPLETE；失败full1/full2保留 |
| Human-reviewed Threshold Approval And Locked Reference | 2026-09-10用户明确批准12项数值；approval JSON、ACTIVE profile/hash和median reference |
| Active Retrieval Gate Verification And Claim Boundary | 原始三轮各12/12 PASS，244 tests及本轮派生负例；仅retrieval开发回归边界 |

## 验证与范围

- `python -B -m unittest discover -s scripts -p 'test_*.py'`：244 tests，OK。更新两个profile生命周期断言；DRAFT负例使用独立输入，未放松生产compiler/evaluator。
- profile/evidence JSON Schema通过；profile/reference统一LF字节，使Git检出后hash稳定。
- 本地派生负例保存在ignored tmp；初版探针使用未消费的字段，未作为验收证据；修正为实际topK/metricCalculationDetails后四项均符合预期。
- Markdown链接、旧活动路径、baseline exact suffix/重复标题、raw敏感字段、受保护路径与`git diff --check`在归档后进行最终检查。
- Maven/frontend build跳过：Java、POM及前端没有改动；本次无新运行时行为。OpenSpec CLI未安装，使用结构、exact suffix及链接校验替代。
- 提交责任：沿用C17已授权的Agent本地提交；不push、不创建PR、不部署。

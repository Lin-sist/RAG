# C7 Corrected-host Model Canary 证据

## 结论

- Corrected model arm：warm-up 与 measured 合计 6/6 requested/effective `nvidia`、fallback=0、model calls=1、candidate coverage=100%。
- Clean heuristic/model pair comparison：`COMPARABLE`，comparison reasons 为空。
- 3 样本 Recall@5、MRR、Top1 delta 均为 0；这是 provider/身份 canary，不是 30 样本收益结论。
- Full A/B 尚未获外调授权，未自动执行。

## 严格身份

- 实验 Git HEAD：`40f94068c28173f938b55ddfc9e54385c781270e`，与既有 heuristic arm 相同。
- 当前主工作区 HEAD `8f297a818e26855d2873488abcdd2d780d03439c` 只增加首轮 canary 文档、host 指南修正和 raw evidence ignore。为避免 comparator 的 Git HEAD 漂移，本轮从临时 detached worktree 启动 backend 与 runner；执行后已安全移除。
- Eval-set SHA-256：`d17bde69db58848fe79069709a7b7c3c927da916661faa8caf1bd71efcd6d7fe`。
- Model arm manifest SHA-256：`776a659f45accac491b9dd900f0e2605e74d9ab9728aac779726253e6e42ae3e`。
- KB：`15`；collection：`kb_ff06e2ea3de24fb4`；3 documents / 50 chunks。
- 样本顺序：`fact-001`、`fact-006`、`definition-001`。

## Clean pair 结果

| Arm | Effective provider | Model coverage | Fallback | Recall@5 | MRR | Top1 | Retrieval P50/P95 | Rerank P50/P95 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| heuristic measured | heuristic 3/3 | n/a | 0/3 | 0.8333 | 1.0000 | 1.0000 | 765 / 765 ms | 0 / 0 ms |
| corrected model measured | nvidia 3/3 | 100% | 0/3 | 0.8333 | 1.0000 | 1.0000 | 1172 / 1187 ms | 349 / 351 ms |

逐样本 retrieval paired delta 为 +374ms、+407ms、+422ms；rerank paired delta 为 +351ms、+344ms、+349ms。样本数仅 3，不能外推生产质量、延迟或 SLA。

## 调用与停止边界

- 本次 corrected-host rerun：6 次 debug retrieval、至多 6 次 query embedding、6 次 NVIDIA rerank。
- 全部 C7 canary 尝试累计：18 次 debug retrieval、至多 18 次 query embedding、12 次 NVIDIA rerank。
- Clean pair comparator 对应预算：12 次 debug retrieval、至多 12 次 query embedding、6 次 NVIDIA rerank；首轮失败 NVIDIA arm 不进入 clean pair。
- Ask、judge、LLM generation：0；串行；自动 retry：0；timeout：20000ms。
- Backend 日志中 6 次 debug retrieval 均返回 200；runtime fallback warning=0、`RerankProviderException`=0、runtime 429 marker=0。
- API key、Authorization、问题正文、passages、contexts 和 raw provider response 未写入本紧凑证据。

## Raw evidence hashes

Raw evidence 保留在本地 Git-ignored `tmp/eval/`，首轮失败证据没有被覆盖：

| 文件 | Bytes | SHA-256 |
| --- | ---: | --- |
| `c7-canary-nvidia-ai-host-warmup-details.json` | 61378 | `d220bc38b7d772868f466cc134297fd8087bb645763130a3de18895dbefb5aa8` |
| `c7-canary-nvidia-ai-host-warmup-metadata.json` | 3649 | `68f7b2df666f26a0d5d78603b90f2cadc69a7248912bf446f8a7aa0f93d16006` |
| `c7-canary-nvidia-ai-host-warmup.md` | 15914 | `405a18f1609dba5625a858dabed539aef73f1df9454731e91dc08b0523cdc4c4` |
| `c7-canary-nvidia-ai-host-details.json` | 61314 | `7464599f83d4071b5af90ad892c2b63a0ce51d186807d352296e86a1efef144c` |
| `c7-canary-nvidia-ai-host-metadata.json` | 3592 | `c419e32cdc1750db2add8b64fc225831aaff9c4cc15325434ce5303302201ea6` |
| `c7-canary-nvidia-ai-host.md` | 15913 | `a3f7d01422b6584637f92f9b0b39bed2b86469e0380de0c8382a49230d88724d` |
| `c7-canary-ai-host-comparison.json` | 14106 | `86d2d1ae3e768ca643db0cc825a1802c510718a8f64406aaae9eeada1499a592` |
| `c7-canary-ai-host-comparison.md` | 1853 | `7d0ca19305fdda79397f56f694d3026ce0a35c222cac4f00d0fd82230c619068` |

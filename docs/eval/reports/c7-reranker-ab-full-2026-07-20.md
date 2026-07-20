# C7 Reranker Full A/B Evidence

## 结论

- Comparison status：`COMPARABLE`；comparison reasons 为空。
- NVIDIA 在本固定开发集上观察到正向 retrieval 质量差异：Recall@5 +7.84pp、MRR +0.0895、Top1 source accuracy +3.70pp。
- 三个 repeat 的质量指标完全一致；answerable 样本没有观察到 Recall@5、MRR 或 Top1 回退。
- 默认 reranker 保持 heuristic；本 evidence 不自动授权切换默认 provider。

## 严格身份与覆盖

- Git HEAD：`fb18b6bd5448db6e0985f98f44268da84195bb1b`。
- Eval-set SHA-256：`d17bde69db58848fe79069709a7b7c3c927da916661faa8caf1bd71efcd6d7fe`。
- KB：`15`；collection：`kb_ff06e2ea3de24fb4`；3 documents / 50 chunks。
- 固定样本：30；measured repeats：每 arm 3；warm-up：每 arm 总计 3。
- 执行顺序：`H1/N1、N2/H2、H3/N3`。
- Heuristic measured：90/90 requested/effective heuristic、fallback=0、model calls=0。
- NVIDIA measured：90/90 requested/effective nvidia、fallback=0、model calls=1、candidate coverage=100%。
- 两 arm warm-up 各 3 次也 clean；missing pairs、zero-candidate mismatch、retrieve errors 均为 0。

## 质量结果

| 指标 | Heuristic | NVIDIA | Model - heuristic |
| --- | ---: | ---: | ---: |
| Recall@5 | 68.63% | 76.47% | +7.84pp |
| MRR | 0.7346 | 0.8241 | +0.0895 |
| Top1 source accuracy | 96.30% | 100.00% | +3.70pp |

每个 repeat 均复现同一组数值。首个 repeat 的样本级诊断与另外两个 repeat 一致：

- Recall@5 改善：`reasoning-003`、`reasoning-006`；无 answerable regression。
- MRR 改善：`definition-003`、`fact-008`、`reasoning-003`、`reasoning-006`；无 regression。
- Top1 改善：`reasoning-006`；无 regression。

## 延迟结果与边界

| 口径 | Heuristic P50/P95 | NVIDIA P50/P95 | 解释 |
| --- | ---: | ---: | --- |
| Debug retrieval wall-clock，90 observations | 797 / 5203ms | 985 / 2796ms | Model P50 +188ms；aggregate P95 受 H1 冷启动污染 |
| Server-side rerank stage，90 observations | 0 / 0ms | 363 / 688ms | NVIDIA 调用本身的主要新增延迟 |

逐 run wall-clock 说明：

| Run | Retrieval P50/P95 | Rerank P50/P95 | Duration |
| --- | ---: | ---: | ---: |
| H1 | 1781 / 14484ms | 0 / 0ms | 107.27s |
| H2 | 781 / 2016ms | 0 / 1ms | 31.48s |
| H3 | 782 / 2031ms | 0 / 0ms | 30.31s |
| N1 | 1000 / 2813ms | 359 / 995ms | 43.03s |
| N2 | 984 / 2500ms | 360 / 409ms | 41.16s |
| N3 | 984 / 2578ms | 368 / 389ms | 43.51s |

Docker Desktop 在执行前刚启动，H1 即使经过 3 次 warm-up 仍出现明显冷启动/连接异常值。因此 aggregate model P95 低于 heuristic P95 不能解释为 NVIDIA 让整体检索更快；可信结论仅是本轮 model rerank stage 的 363/688ms 与 overall P50 +188ms。

## 调用、错误与费用边界

- Full 实际：186 次 debug retrieval、至多 186 次 query embedding、93 次 NVIDIA rerank。
- 全部 C7 canary + full 累计：204 次 debug retrieval、至多 204 次 query embedding、105 次 NVIDIA rerank。
- Ask、judge、LLM generation：0；并发：串行；自动 rerank retry：0；timeout：20000ms。
- 四个 backend 运行段共记录 186 次 debug retrieval 200；fallback warning=0、`RerankProviderException`=0。时间戳中的 `.429` 不视作 HTTP 429；attribution 与 fallback evidence 未发现真实 429。
- 费用依据为用户确认的 NVIDIA NIM 免费使用；本轮未证明长期配额、限流或生产 SLA。

## 外推与决策边界

- 30 条样本是当前 fixture 的开发基线，不是生产数据集或论文级 benchmark。
- 本次只评估 retrieval；不证明 generation、citation、no-answer、judge 或 claim-level faithfulness 改善。
- Positive delta 支持把 NVIDIA reranker 作为进一步验证候选，但不足以在 C7 自动切换默认 provider。
- 若未来切换默认 provider，必须另立 Type C change，明确运行成本、SLA、限流、fallback 与更大评测集门禁。

## Raw evidence hashes

Raw evidence 保留在本地 Git-ignored `tmp/eval/`；tracked evidence 不复制 question、contexts、passages、raw response 或凭据。

| 文件 | Bytes | SHA-256 |
| --- | ---: | --- |
| `c7-heuristic-ece64a0bc6de-warmup-aa80e8707e40-details.json` | 53527 | `4263c345572a4c28904c202e2520328872e62784ec50a681036efb6eea3cd199` |
| `c7-heuristic-ece64a0bc6de-warmup-aa80e8707e40-metadata.json` | 3553 | `51f78418c2b8d476747892ec0bcf0cf7efca4f740f40ba3a414c679101079b76` |
| `c7-model-6c57d3432d9f-warmup-aa80e8707e40-details.json` | 57383 | `4bea57a7225ade9750895f4b095fbf056cbe78f9cdfc083ccfb5ab7ce928856c` |
| `c7-model-6c57d3432d9f-warmup-aa80e8707e40-metadata.json` | 3643 | `56a1cdb1f5060f0a554f3c4fd8924b4e89aee815736b22ea722d23cf8adb6160` |
| `c7-full-heuristic-details-run1.json` | 536060 | `f9ea19549d0d11d0cea6183d4836e6cd45e89f14b6d3fc17dbdaf1f63e3629d8` |
| `c7-full-heuristic-metadata-run1.json` | 4118 | `8f609c57f89b4ef8228e12fc67d44687860ca1bcdc15123b54d5d4353ba23997` |
| `c7-full-heuristic-details-run2.json` | 536044 | `be2831672ba9492c493be5433874d87b3d66b644f875350d433a63baa5b3bdfd` |
| `c7-full-heuristic-metadata-run2.json` | 4118 | `ccd583821250cee8a9850c5f9a44634c64536eedb2319a82dd5bde2a53151204` |
| `c7-full-heuristic-details-run3.json` | 536044 | `8d6269193d81decd15c486407dcc7e81e0922b0b4ef06c6a5312f12fd1d36ad4` |
| `c7-full-heuristic-metadata-run3.json` | 4118 | `f7077546130e1bc6ae04d293060684389b029abaf8e07a6e5a2ea2c2c685db45` |
| `c7-full-nvidia-ai-host-details-run1.json` | 554417 | `7d383d91686ef1ad8408b882f0fea7206b1c43fb94d075b4111ec2cc80fe6fe8` |
| `c7-full-nvidia-ai-host-metadata-run1.json` | 4208 | `e3bba4de1391ed48459f241731f8aefa739ee67f7f8e40a1bbc8dc26c6c975d9` |
| `c7-full-nvidia-ai-host-details-run2.json` | 554412 | `7d76f2ecbf460416c64454a371c4d5d2b40c041e186e0e859376966554f70637` |
| `c7-full-nvidia-ai-host-metadata-run2.json` | 4208 | `4c8464e4ada9b8e0b131d717f8c604b39724cb07e78115c35f0c6a580da101f3` |
| `c7-full-nvidia-ai-host-details-run3.json` | 554414 | `c8047f8ec5089ed28d2c645e1c71260bbaa2b808645b5dc7fca35f00d0975a3d` |
| `c7-full-nvidia-ai-host-metadata-run3.json` | 4208 | `61aa4d96d4cb41c7f66178c2275e307ef5dd5a01cb30a542556ff6a43f3ce862` |
| `c7-full-comparison.json` | 237379 | `ed618265b9f6792b920bbe2a53ae4769c70264e74aca9d9581b498645eb3d590` |
| `c7-full-comparison.md` | 2425 | `37cc6126b1c8e3b982185426ec9c7b7e6f5400b819c7bee19f07578a9239af57` |

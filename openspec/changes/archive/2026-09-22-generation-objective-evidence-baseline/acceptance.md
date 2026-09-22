# C18 验收与归档记录

## 2026-09-22 C18 已验收

用户已授权完成C18 canary、full、验收、归档和本地提交。r11在clean HEAD `bbab928097c996453cce8b6cb7ad72c0e51a1a5f`上完成固定5条canary与从头150条full；full compiler v6结果为`COMPLETE`，Report=`CLEAN`、objective=`COMPLETE`、judge=`SKIPPED`，ask/retrieval errors=0。后端在证据编译后已停止。

正式单次开发基线：Recall@3=44.37%、Recall@5=47.44%、MRR=0.52615、Top1 source accuracy=92.31%；answer keyword hit=79.74%（374/469）；citation source hit=91.91%（125/136）、snippet hit=100%（336/336）、unsupported citation=0；objective lexical claim support=5.22%（23/441），unsupported claims=418；no-answer accuracy=95%（19/20），no-answer citation violation=1。低指标保持原样，不据此修改题目、fixture、prompt、检索、指标或阈值。

调用事实：debug attempts=150、ask/generation HTTP attempts=181、最终generation samples=150；31次HTTP503均在批准的请求边界内恢复，未恢复错误0。query embedding logical/cache/provider/fallback=`973/552/421/0`，judge/model rerank/provider fallback/answer cache hit均为0。所有attempt事实完整且在full预算5968内。

## 证据与完整性

- 正式安全摘要：`docs/eval/reports/c18-generation-objective-review-v1.json`，SHA-256=`06dd7458e4950e6d61de7aa8a408570cb06a5524fa4786fa9d67612d7d8f30ae`。
- ignored raw report：`tmp/eval/c18/20260922-r11-full-live-report.md`，SHA-256=`a9599e0be6040b6edc19acec0822835fe7b8739814a2634e496371c8f1b0fa9d`。
- ignored raw details：SHA-256=`f63c065ef075a161d00ab6fced64973f7b9a2658d79f451a74712a4c03af9300`，bytes=5453890。
- ignored raw metadata：SHA-256=`4682b2fa9714db24dd9a19f0ac4bfc29ee40242f8940dac2a872ee4bf7750c58`，bytes=13910。
- dataset ordered IDs SHA-256=`5baac0884a917ae20bf6e584207127f5d0e084569c5c07aa0c08d00fd986b67e`；runtime fingerprint SHA-256=`e66f366eb0c676cccc7f196f722911c809f9c3f696eb96d97c9d66d31d78287f`。

## 接受边界

- 只接受一次固定开发态generation/citation/objective lexical claim/no-answer测量；不激活objective或judge gate，不修改C17 ACTIVE retrieval profile/reference。
- judge仍未执行；lexical claim alignment不证明语义faithfulness，单次run不证明稳定性、生产质量、SLA、多租户或Agentic RAG readiness。
- 31次503说明provider传输并非零故障；`CLEAN`仅表示最终150条均完成且失败均按批准契约恢复。
- 后续阈值、重复测量、judge calibration或profile activation必须进入后续独立change并重新授权相应外调。

## 验证与范围

- compiler v6：`COMPLETE`，六个客观通道完整，judge=`SKIPPED`；安全摘要不含raw问题、回答、context、凭据、数字KB、collection或绝对路径。
- Python全套：290 tests/OK；canary/full plan-only均`OFFLINE_VERIFIED`；manifest/source hash与budget自验证。
- Java/frontend测试未重跑：最终收尾只改Python compiler、证据与治理文件，Java运行实现沿用此前已验证提交；无前端改动。
- 未修改`.env.local`、业务默认配置、题目/fixture/prompt、C17门禁；未重建/清理KB或history，无push/PR/deploy。

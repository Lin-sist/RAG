# C19 执行任务

## 0. 事前闸门

- [x] 0.1 核查 Git / ACTIVE_TASK、前端接受归档和 C19 accepted contract/现有 runner。
- [x] 0.2 完成 proposal、design（D1–D7）、tasks、evaluation delta 和 readiness；本地 plan 4/72 无外调。
- [x] 0.3 用户明确批准“按该设计实施零外调切片”；实施时默认手动提交，随后用户明确要求“为我提交commit，继续下一步”，本轮Agent本地提交已获授权，不含push/PR/部署或真实调用。

## 1. 零外调实现

- [x] 1.1 独立manifest/schema/validator、Git LF源码hash/clean HEAD/完整endpoint/runtime身份、plan/preflight已实现。
- [x] 1.2 C19显式入口实现exclusive目录、请求前预算、逐次fsync ledger、无重试/redirect及首错停止。
- [x] 1.3 calibration非完整输入agreement/repeat率N/A；独立compiler重算并输出安全摘要。
- [x] 1.4 聚焦28/28通过，覆盖预算、缺/重复/畸形/身份漂移、未知outcome、保全、gold不进入transport及脱敏。
- [x] 1.5 全Python已执行278 tests，既有C18 audit漂移仍1 failure/4 errors，不能称GREEN；本地4/72 plan、BLOCKED preflight及compiler CLI合成烟测/重复输出拒绝通过。文档/diff检查见日志，不把synthetic当live。

## 2. Live 分阶段授权与执行

- [x] 2.0 用户提供模型信息后独立冻结judge模型、同步共享contract模型身份；已完成4次canary执行包及载荷/预算披露，最新judge29/29，完整回归279项仍为既有C18的1 failure/4 errors。

- [x] 2.1 用户确认免费额度可用并授权最多4次canary；模型/endpoint/载荷/runtime已固定，配置提交869803d本地preflight READY，授权文档提交后再读取实际clean HEAD。费用依据为用户声明、真实认证/具体限流未核验；只在live时从本地项目环境源只读加载凭据。
- [x] 2.2 独立授权4次canary，在clean HEAD823b81a完成4/4、HTTP200/parse100%、错误/重试0；独立重编译与原摘要完全相同。仅单轮canary，不证明full三轮一致性；4次预算已使用，不补跑。
- [x] 2.2a 用户明确授权新增最多72次full，沿用已确认免费额度、固定24×3及已披露出站/runtime；授权提交后读取新clean HEAD，不复用canary或扩大调用类型。
- [ ] 2.3 本次独立full已执行42次后timeout首错停止：41有效、30未尝试、retry0，compiler INCOMPLETE；完整24×3未达成，不拼接canary/partial。恢复选择及独立新预算见[recovery-plan](recovery-plan.md)。
- [ ] 2.4 compiler已如实输出partial：valid41/expected72、1 timeout/30 missing、agreement/repeat率N/A；完整72条指标仍未建立，不能用成功子集给结论。

## 3. 验收与归档

- [ ] 3.1 形成安全acceptance/report：低agreement如实保留，不自动调参，不声称生产质量或C20通过。
- [ ] 3.2 用户明确接受本change后delta进入baseline、归档、恢复IDLE；更新技术债与执行日志。

已完成第0–1节及2.0–2.2a；canary已用4，full尝试42后首错停止，余30未用不作续跑授权。2.3–2.4及第3节未完成，恢复方案待用户选择；默认60秒不改，120秒仅候选，change不归档、不接受baseline、不改IDLE。

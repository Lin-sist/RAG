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

- [x] 2.2d 用户明确授权最后一次同120秒配置独立full最多72次，当前实际0；如中断保留全部证据，C19 live任务延后，继续C20零外调切片，不再自动补跑或反复申请重跑。

- [x] 2.0 用户提供模型信息后独立冻结judge模型、同步共享contract模型身份；已完成4次canary执行包及载荷/预算披露，最新judge29/29，完整回归279项仍为既有C18的1 failure/4 errors。

- [x] 2.1 用户确认免费额度可用并授权最多4次canary；模型/endpoint/载荷/runtime已固定，配置提交869803d本地preflight READY，授权文档提交后再读取实际clean HEAD。费用依据为用户声明、真实认证/具体限流未核验；只在live时从本地项目环境源只读加载凭据。
- [x] 2.2 独立授权4次canary，在clean HEAD823b81a完成4/4、HTTP200/parse100%、错误/重试0；独立重编译与原摘要完全相同。仅单轮canary，不证明full三轮一致性；4次预算已使用，不补跑。
- [x] 2.2a 用户明确授权新增最多72次full，沿用已确认免费额度、固定24×3及已披露出站/runtime；授权提交后读取新clean HEAD，不复用canary或扩大调用类型。
- [x] 2.2b 用户选择D8并新增授权120秒canary4次；clean HEAD193e46c实测4/4、HTTP200/解析100%、错误/重试0，独立compiler COMPLETE且重编译一致。仅单轮小样本，不证明原超时根因解决；4次已用完，新full72授权0/实际0。
- [x] 2.2c 用户暂停后明确要求恢复C19剩余外调，恢复已披露独立120秒full最多72次、约58KB、同模型/零重试/首错停止；新full授权72/实际0，授权文档提交后读取实际clean HEAD。旧run不续跑或拼接，无额外业务调用授权。
- [ ] 2.3 原60秒full41有效/1timeout/30未尝试；新120秒full8有效/1network error/63未调用，均按首错停止且retry0、compiler INCOMPLETE，完整24×3仍未达成。证据独立保存，不拼接或续跑，见[recovery-plan](recovery-plan.md)。
- [ ] 2.4 compiler如实输出新partial：valid8/expected72、1network error/63missing、agreement/repeat率N/A；完整72条指标仍未建立，不能用成功子集给结论。

## 3. 验收与归档

- [ ] 3.1 形成安全acceptance/report：低agreement如实保留，不自动调参，不声称生产质量或C20通过。
- [ ] 3.2 用户明确接受本change后delta进入baseline、归档、恢复IDLE；更新技术债与执行日志。

已完成第0–1节及2.0–2.2c；恢复后新full尝试9次首错停止，累计59次尝试/57个HTTP200/1旧timeout/1新网络错误。原剩余30及本轮剩余63不可续跑/拼接，新run授权0。2.3–2.4及第3节仍未完成，保留全部证据，默认60秒不改，change不归档、不接受baseline、不改IDLE。

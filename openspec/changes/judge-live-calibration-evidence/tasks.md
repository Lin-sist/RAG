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
- [ ] 2.3 canary完整且身份不变后独立取得full最多72次授权；新目录执行24×3，不拼接canary或旧失败轮。
- [ ] 2.4 compiler从全部72条重算parse coverage、三组confusion/agreement、mismatch和24个case repeat consistency；真实错误/尝试/重试单列。

## 3. 验收与归档

- [ ] 3.1 形成安全acceptance/report：低agreement如实保留，不自动调参，不声称生产质量或C20通过。
- [ ] 3.2 用户明确接受本change后delta进入baseline、归档、恢复IDLE；更新技术债与执行日志。

已完成第0–1节及2.0–2.2；canary授权4/实际4、余额0。2.3–2.4及第3节保持未勾，[full执行包](full-execution.md)新增最多72次尚未授权，change不归档、不接受baseline、不改IDLE。官方OpenSpec CLI未安装，文件级检查不冒称官方validate。

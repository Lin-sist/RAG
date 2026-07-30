# C15 MCP Read-Only Traceability

## 1. 结论与状态

- 变更：`mcp-readonly-service`
- delta：7 requirements / 26 scenarios
- 验收结论：`PASS`。固定 SDK/spec/schema 下的 focused tests、独立官方 Java SDK client、双 tenant synthetic integration 与适用的官方 conformance generic scenarios 均已通过。
- 归档状态：`ARCHIVED`。用户于 2026-07-30 明确要求归档；driver evidence 已在 clean HEAD `45959672ec64ec72c05bcbe17fe52204555f1098` 重跑，7 requirements / 26 scenarios 已接受进 baseline，change 归档到 `openspec/changes/archive/2026-07-30-mcp-readonly-service/`，`ACTIVE_TASK=IDLE`。
- 能力边界：只证明默认关闭、本机开发边界内的只读 MCP Resources/Tools；不证明 MCP OAuth Authorization Profile、远程生产部署、真实 provider、Qdrant/Elasticsearch、生产第二业务 tenant 或 Agentic RAG。

## 2. 固定证据身份

| 项 | 值 |
|---|---|
| MCP spec | `2025-11-25` |
| 官方 Java SDK | `io.modelcontextprotocol.sdk:mcp-core/mcp-json-jackson2/mcp-spring-webmvc:2.0.0` |
| Tool schema SHA-256 | `44ce4cc851551057bbabfbdf3735b964de300ff691b2e73a612856e9fa64213f` |
| 官方 conformance | `@modelcontextprotocol/conformance@0.1.15` |
| conformance Node runtime | `22.17.0` |
| synthetic fixture | `c15-mcp-readonly-fixture-v1` |
| Failsafe profile | `c15-mcp-readonly` |
| 容器 | MySQL `8.0.36`、Redis `7-alpine` 固定 digest、etcd `3.5.5`、MinIO `RELEASE.2023-03-20T20-16-18Z`、Milvus `2.3.4` |

最新归档组合 profile：2 tests、0 failures、0 errors、0 skipped。`C15McpConformanceIT` 用时 27.52s，`C15McpReadOnlyIT` 用时 46.05s。

最终 MCP + 相邻 RAG 聚焦集：22 reports / 132 tests / 0 failures / 0 errors / 0 skipped，包含独立 Java SDK client、全部 `Mcp*Test`、Document tenant guard、RAG service/cache/query engine 与 profile contract。

clean-HEAD driver evidence 记录：

- `gitHead=45959672ec64ec72c05bcbe17fe52204555f1098`、`workingTreeDirty=false`；
- authoritative before/after SHA-256 均为 `8261a84b7344e813a7dec0dca80e34e1642d4a96b869444066d93b5fe2a5a419`，`authoritativeStateUnchanged=true`；
- deterministic embedding/generation invocations=`5/2`；
- real provider/model calls=`0`，business data outbound=`false`，QA cache=`false`；
- foreign/control timing 使用 seed `15001`、5 组 warm-up、20 组交错样本、request errors=`0`；本轮 median=`6.3815/6.4198ms`，P95=`7.6243/7.511ms`；
- real Milvus maintenance=`SKIPPED`。

这些数值是本次本机 synthetic run 的事实，不是生产性能或绝对 timing side-channel 证明。

## 3. 质量门禁摘要

- C15 MCP + 相邻 RAG 聚焦：22 reports / 132 tests / 0 failures / 0 errors / 0 skipped。
- `c15-mcp-readonly` Failsafe：2 tests / 0 failures / 0 errors / 0 skipped。
- 全仓 `mvn -q test`：退出码 1；rag-admin 320 tests / 1 failure / 0 errors / 2 skipped。唯一失败为既有 `GenAiTracingConfigurationTest.unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` 时序断言，立即独立复跑 1/0/0/0；因此全仓不记为 GREEN，也不在 C15 修改观测实现。
- Python：`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 190 tests / OK。
- SensitiveLogs：346 source files / PASS。
- dependency convergence：仍命中既有 PDFBox/Flexmark、Milvus/Qdrant protobuf/gRPC/Guava、annotations/collections 冲突；冲突路径没有 MCP artifact，不在 C15 扩修。
- frontend：无改动，包含 `vue-tsc` 的正式 build 按规则 `SKIPPED`。
- live MCP provider smoke：未授权，`SKIPPED`；真实 provider/model calls=0。

## 4. Requirement / Scenario 映射

| # | Requirement / Scenario | 状态 | 主要测试与证据 |
|---:|---|---|---|
| 1 | Default-Off MCP / 默认关闭 | PASS | `McpServerConfigurationTest.transportAndCapabilitiesAreAbsentByDefault`、`McpExternalToolsDisabledMvcTest` |
| 2 | Default-Off MCP / 显式启用 Sessionless Streamable HTTP | PASS | `McpProtocolMvcTest.enabledSessionlessTransportNegotiatesTargetProtocol`、`statelessTransportRejectsGetWithoutCreatingASession`、独立 SDK client |
| 3 | Default-Off MCP / SDK 与当前基线不兼容 | PASS（兼容路径） | `McpSdkCompatibilityTest`、reactor compile、dependency tree/convergence 审计；当前无需 hard-stop，设计保留不兼容时的 fail-closed 分支 |
| 4 | Authenticated Tenant Scope / 同 Tenant 已认证访问 | PASS | `McpRequestIdentityResolverTest`、`C15McpReadOnlyIT` owner/reader/private/public fixture |
| 5 | Authenticated Tenant Scope / 缺失或无效认证 | PASS | `McpAuthenticationMvcTest` 的 missing/expired/header-only/逐 method fresh authentication |
| 6 | Authenticated Tenant Scope / 伪造 Tenant 或猜 Foreign ID | PASS | `McpToolRequestValidatorTest`、`McpProtocolMvcTest` matched controls、`C15McpReadOnlyIT` foreign KB/document/chunk/citation |
| 7 | Authenticated Tenant Scope / Authorization Profile 声明边界 | PASS | initialize capability assertions与 `docs/architecture/mcp-readonly-service.md`；未暴露 OAuth metadata/scope 能力 |
| 8 | Knowledge Resources / 分页列出可访问知识库 | PASS | `McpKnowledgeResourceServiceTest`、`McpProtocolMvcTest.resourcesListReauthorizesAnOpaqueCursorForEveryRequestIdentity`、容器集成分页 |
| 9 | Knowledge Resources / 读取 KB、Document、Chunk | PASS | `McpProtocolMvcTest` 三类 whitelist/UTF-8 tests、`DocumentServiceTenantGuardTest`、`C15McpReadOnlyIT` wire read |
| 10 | Knowledge Resources / 非法 URI 或 Foreign Resource | PASS | `McpResourceUriTest`、document/chunk mismatch tests、双 tenant matched error + timing controls |
| 11 | Knowledge Resources / 请求 Document Version Resource | PASS | `McpResourceUriTest.nonCanonicalOrMalformedUrisFailClosedWithoutEchoingInput`、固定三 template discovery |
| 12 | Fixed Tools / Tool Discovery 与 Schema | PASS | `McpProtocolMvcTest.toolsListDeclaresExactlyFourStrictReadOnlyTools`、`McpToolSchemaIdentityTest` |
| 13 | Fixed Tools / Search 与 Ask | PASS | `McpProtocolMvcTest` search/ask/no-result/error tests、`C15McpReadOnlyIT` real Milvus synthetic wire paths |
| 14 | Fixed Tools / Get Citation | PASS | `McpCitationReaderTest`、`McpProtocolMvcTest.getCitationReturnsAnExactBoundedReadOnlySource`、容器集成 |
| 15 | Fixed Tools / Compare Sources | PASS | `McpProtocolMvcTest.compareSourcesAuthorizesAndReturnsBothSidesWithoutSemanticInference`、容器集成 |
| 16 | Authoritative Read-Only / Ask 不写 History 或 Query Count | PASS | `C15McpReadOnlyIT` success/no-result/generation error 前后权威摘要、history rows 和 query count；`McpCacheEnabledMvcTest` |
| 17 | Authoritative Read-Only / 允许的 Tenant-Scoped 技术写入 | PASS | `McpCacheEnabledMvcTest` server-owned cache scope、`McpToolExecutionGuardTest` rate/concurrency；driver evidence 的 QA cache disabled 与权威摘要 |
| 18 | Authoritative Read-Only / 尝试调用写能力 | PASS | `McpProtocolMvcTest.guessedWriteToolNameFailsAtTheProtocolBoundary`、独立 client unknown write Tool、容器集成权威摘要 |
| 19 | External Tool Budget / External Tools 默认关闭 | PASS | `McpExternalToolsDisabledMvcTest.disabledExternalToolsAreHiddenAndCannotBeCalledByName` |
| 20 | External Tool Budget / 显式启用后的 Deterministic 验证 | PASS | `McpProtocolMvcTest` + `C15McpReadOnlyIT`，真实 provider/model calls=0，business data outbound=false |
| 21 | External Tool Budget / 未授权 Live Smoke | PASS（按契约 SKIPPED） | 用户未授权真实 MCP search/ask；未发起外部调用，未用 synthetic 结果声称 hosted provider 可用 |
| 22 | External Tool Budget / Provider Failure 与 Attribution | PASS（synthetic） | `McpProtocolMvcTest.searchAndAskPreserveNoResultAndDependencyFailureSemantics`、容器 deterministic generation failure；真实 429/5xx smoke 未授权 |
| 23 | Limits / Origin 与 Local-Only 防护 | PASS | `McpOriginAndExposureFilterTest`、`McpProtocolMvcTest.invalidOriginIsRejectedBeforeProtocolHandling`、conformance `dns-rebinding-protection` |
| 24 | Limits / Rate、Timeout 与 Result Bounds | PASS | `McpServerConfigurationTest`、`McpToolRequestValidatorTest`、`McpToolExecutionGuardTest`、`McpResultMapperTest` |
| 25 | Limits / Protocol 与 Tool Error 分层脱敏 | PASS | `McpProtocolMvcTest` raw protocol errors、Tool error/no-result/dependency paths、foreign matched controls与 opaque assertions |
| 26 | Limits / 固定 Conformance 与 Client Evidence | PASS（适用范围） | `C15McpConformanceIT`、`McpIndependentClientMvcTest`、`C15McpReadOnlyIT`、本文件映射；clean-HEAD 归档门禁已通过 |

## 5. Conformance 适用范围

固定执行以下五个不要求 suite-specific 业务 fixture、且适用于 C15 capability 的 generic server scenarios：

1. `server-initialize`
2. `ping`
3. `tools-list`
4. `resources-list`
5. `dns-rebinding-protection`

conformance 0.1.15 的其余 active scenarios 要求套件约定的 echo/add/long-running Tool、专用 Resource、Prompt 或 Task。C15 的已批准契约要求恰好四个只读 Tool、三种 Resource template，且不声明 Prompt/Task；为测试临时注册这些 fixture 会改变被验对象并破坏固定 registry。因此它们记录为不适用，而不是 skipped required C15 scenario，也不能对外表述为“官方套件所有 active scenarios 全部通过”。

Windows 系统 Node 24.14.0 曾在 scenario 已报告 success 后触发 libuv teardown assertion。C15 没有忽略该非零退出，而是把运行时固定到 Node 22.17.0；固定运行后进程 exit code=0。

## 6. 可复现命令与产物

```powershell
mvn -q -pl rag-admin -am -P c15-mcp-readonly "-Dtest=C15McpProfileContractTest" "-Dit.test=C15McpReadOnlyIT,C15McpConformanceIT" "-Dsurefire.failIfNoSpecifiedTests=false" verify
```

build-only 产物：

- `rag-admin/target/c15-mcp-readonly-driver-evidence.json`
- `rag-admin/target/c15-conformance/<scenario>/**/checks.json`
- `rag-admin/target/failsafe-reports/TEST-com.enterprise.rag.integration.C15McpReadOnlyIT.xml`
- `rag-admin/target/failsafe-reports/TEST-com.enterprise.rag.admin.mcp.C15McpConformanceIT.xml`

这些 `target/` 文件不提交。归档前已在 clean working tree 重跑，并核对 `gitHead`、`workingTreeDirty=false`、两项 authoritative SHA-256 相等、tests/failures/errors/skips 和五个 conformance exit code；正式结论仍以本文件记录的固定身份与边界为准。

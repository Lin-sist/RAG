/* ==========================================================================
   RAG 智能问答 · 高仿 ChatGPT 界面 Demo
   纯前端演示：所有数据均为本地 mock，不发起任何网络请求。
   ========================================================================== */
"use strict";

/* ---------------- 小工具 ---------------- */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = s => String(s).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

function toast(msg, ms = 1800) {
  const t = $("#toast");
  t.textContent = msg;
  t.hidden = false;
  clearTimeout(toast._h);
  toast._h = setTimeout(() => { t.hidden = true; }, ms);
}

function copyText(text, okMsg = "已复制到剪贴板") {
  const done = () => toast(okMsg);
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(done).catch(() => fallbackCopy(text, done));
  } else fallbackCopy(text, done);
}
function fallbackCopy(text, done) {
  const ta = document.createElement("textarea");
  ta.value = text; ta.style.position = "fixed"; ta.style.opacity = "0";
  document.body.appendChild(ta); ta.select();
  try { document.execCommand("copy"); done(); } catch { toast("复制失败，请手动选择文本"); }
  ta.remove();
}

/* ---------------- 图标库（内联 SVG，线性风格） ---------------- */
const P = {
  newchat: '<path d="M12.5 4.5H6.3A1.8 1.8 0 0 0 4.5 6.3v11.4a1.8 1.8 0 0 0 1.8 1.8h11.4a1.8 1.8 0 0 0 1.8-1.8V11.5"/><path d="M17.7 3.7a1.9 1.9 0 0 1 2.6 2.6l-7.7 7.7-3.4.8.8-3.4Z"/>',
  library: '<path d="M4.5 4.5v15"/><path d="M9 4.5v15"/><path d="m13.4 5.3 4.6 13.4"/>',
  folder: '<path d="M3.5 6.5c0-1.1.9-2 2-2h3.2c.6 0 1.2.3 1.6.8l.9 1.2h7.3c1.1 0 2 .9 2 2v9c0 1.1-.9 2-2 2h-13c-1.1 0-2-.9-2-2Z"/>',
  clock: '<circle cx="12" cy="12" r="8.2"/><path d="M12 7.5V12l3 1.8"/>',
  apps: '<rect x="4" y="4" width="6.6" height="6.6" rx="1.6"/><rect x="13.4" y="4" width="6.6" height="6.6" rx="1.6"/><rect x="4" y="13.4" width="6.6" height="6.6" rx="1.6"/><path d="M16.7 13.6v6.2M13.6 16.7h6.2"/>',
  dots: '<circle cx="5.6" cy="12" r="1.2" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.2" fill="currentColor" stroke="none"/><circle cx="18.4" cy="12" r="1.2" fill="currentColor" stroke="none"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.4-4.4"/>',
  panel: '<rect x="3.5" y="4.5" width="17" height="15" rx="2.6"/><path d="M9.7 4.5v15"/>',
  share: '<path d="M12 14.5V4.2"/><path d="M7.8 7.9 12 3.7l4.2 4.2"/><path d="M5.5 12.5v5.3c0 1 .8 1.9 1.9 1.9h9.2c1 0 1.9-.8 1.9-1.9v-5.3"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  mic: '<rect x="9.1" y="3.2" width="5.8" height="10.4" rx="2.9"/><path d="M5.8 11.2a6.2 6.2 0 0 0 12.4 0"/><path d="M12 17.6v3.2"/>',
  chevD: '<path d="m6.5 9.5 5.5 5.5 5.5-5.5"/>',
  chevR: '<path d="m9.5 6.5 5.5 5.5-5.5 5.5"/>',
  x: '<path d="m6 6 12 12M18 6 6 18"/>',
  copy: '<rect x="9" y="9" width="11.5" height="11.5" rx="2"/><path d="M4.5 15V5.8A1.8 1.8 0 0 1 6.3 4H15.5"/>',
  thumbU: '<rect x="3.2" y="10.2" width="3.8" height="9.6" rx="1.1"/><path d="M7 10.9 11.5 5a2 2 0 0 1 3.6 1.6l-.7 3.1h4.4a1.9 1.9 0 0 1 1.9 2.4l-1.3 5.1a2 2 0 0 1-1.9 1.5H7"/>',
  thumbD: '<g transform="rotate(180 12 12)"><rect x="3.2" y="10.2" width="3.8" height="9.6" rx="1.1"/><path d="M7 10.9 11.5 5a2 2 0 0 1 3.6 1.6l-.7 3.1h4.4a1.9 1.9 0 0 1 1.9 2.4l-1.3 5.1a2 2 0 0 1-1.9 1.5H7"/></g>',
  refresh: '<path d="M19.9 12A7.9 7.9 0 1 1 17 6.1"/><path d="M17.3 2.9v3.4h3.4"/>',
  arrowUp: '<path d="M12 19V5"/><path d="m6 11 6-6 6 6"/>',
  arrowDown: '<path d="M12 5v14"/><path d="m6 13 6 6 6-6"/>',
  bubble: '<path d="M12 4.2c-4.6 0-8.3 3-8.3 6.9 0 2.2 1.2 4.1 3 5.4l-.5 3.3 3.5-1.5c.7.2 1.5.3 2.3.3 4.6 0 8.3-3 8.3-6.9S16.6 4.2 12 4.2Z"/>',
  gear: '<path d="M10.6 3.6c.2-.9 1-1.6 2-1.6h.8c1 0 1.8.7 2 1.6l.2 1a7 7 0 0 1 1.6.94l1-.35a2 2 0 0 1 2.4.9l.4.7a2 2 0 0 1-.4 2.5l-.77.66a7.1 7.1 0 0 1 0 1.9l.77.66a2 2 0 0 1 .4 2.5l-.4.7a2 2 0 0 1-2.4.9l-1-.36a7 7 0 0 1-1.6.94l-.2 1a2 2 0 0 1-2 1.6h-.8a2 2 0 0 1-2-1.6l-.2-1a7 7 0 0 1-1.6-.94l-1 .36a2 2 0 0 1-2.4-.9l-.4-.7a2 2 0 0 1 .4-2.5l.76-.66a7.1 7.1 0 0 1 0-1.9l-.76-.66a2 2 0 0 1-.4-2.5l.4-.7a2 2 0 0 1 2.4-.9l1 .35A7 7 0 0 1 10.4 4.6Z"/><circle cx="12" cy="12" r="2.6"/>',
  bell: '<path d="M6.2 15.3v-4.1a5.8 5.8 0 0 1 11.6 0v4.1l1.4 2.2H4.8Z"/><path d="M10.2 20.3a2 2 0 0 0 3.6 0"/>',
  sliders: '<path d="M4 7.2h9M17.2 7.2H20M4 16.8h2.8M11 16.8h9"/><circle cx="15" cy="7.2" r="2.1"/><circle cx="8.8" cy="16.8" r="2.1"/>',
  person: '<circle cx="12" cy="8.2" r="3.7"/><path d="M4.8 20a7.2 7.2 0 0 1 14.4 0"/>',
  logout: '<path d="M14 4.5H7A1.5 1.5 0 0 0 5.5 6v12A1.5 1.5 0 0 0 7 19.5h7"/><path d="m15.5 8.5 3.5 3.5-3.5 3.5M19 12h-9"/>',
  help: '<circle cx="12" cy="12" r="8.2"/><circle cx="12" cy="12" r="3.4"/><path d="m6 6 3.5 3.5M18 6l-3.5 3.5M6 18l3.5-3.5M18 18l-3.5-3.5"/>',
  filter: '<path d="M5 8h14M8.2 12h7.6M10.8 16h2.4"/>',
  grid: '<rect x="4" y="4" width="6.8" height="6.8" rx="1.4"/><rect x="13.2" y="4" width="6.8" height="6.8" rx="1.4"/><rect x="4" y="13.2" width="6.8" height="6.8" rx="1.4"/><rect x="13.2" y="13.2" width="6.8" height="6.8" rx="1.4"/>',
  list: '<path d="M8.5 6.5h11M8.5 12h11M8.5 17.5h11"/><circle cx="4.7" cy="6.5" r=".95" fill="currentColor" stroke="none"/><circle cx="4.7" cy="12" r=".95" fill="currentColor" stroke="none"/><circle cx="4.7" cy="17.5" r=".95" fill="currentColor" stroke="none"/>',
  file: '<path d="M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8.5Z"/><path d="M13.5 3.5v5h5"/>',
  doc: '<path d="M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8.5Z"/><path d="M13.5 3.5v5h5"/><path d="M8.7 12.6h6.6M8.7 15.6h4.6"/>',
  spark: '<path d="M12 3.4c.62 4.6 3.3 7.3 7.9 7.9.9.12.9 1.28 0 1.4-4.6.6-7.28 3.3-7.9 7.9-.12.9-1.28.9-1.4 0-.62-4.6-3.3-7.3-7.9-7.9-.9-.12-.9-1.28 0-1.4 4.6-.6 7.28-3.3 7.9-7.9.12-.9 1.28-.9 1.4 0Z"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  stop: '<rect x="6.6" y="6.6" width="10.8" height="10.8" rx="2" fill="currentColor" stroke="none"/>',
  temp: '<path d="M12 4.5a7.5 7.5 0 1 0 7.5 7.5"/><path d="M12 8v4.2l2.8 1.6"/>',
  link: '<path d="m9.5 14.5 5-5"/><path d="M11 6.8 12.8 5a3.8 3.8 0 0 1 5.4 5.4l-1.9 1.9"/><path d="M13 17.2 11.2 19a3.8 3.8 0 0 1-5.4-5.4l1.9-1.9"/>',
  pencil: '<path d="M14.5 5.5l4 4"/><path d="m5 19 .9-3.9 9.7-9.7a1.9 1.9 0 0 1 2.7 0l.3.3a1.9 1.9 0 0 1 0 2.7L8.9 18.1 5 19Z"/>',
  trash: '<path d="M5 6.5h14"/><path d="M8.5 6.5v-1A1.5 1.5 0 0 1 10 4h4a1.5 1.5 0 0 1 1.5 1.5v1"/><path d="m6.5 6.5.8 12.4a1.5 1.5 0 0 0 1.5 1.4h6.4a1.5 1.5 0 0 0 1.5-1.4l.8-12.4"/>',
  download: '<path d="M12 4v10.5"/><path d="m7.5 10.5 4.5 4.5 4.5-4.5"/><path d="M5 19.5h14"/>',
  db: '<ellipse cx="12" cy="5.5" rx="7" ry="2.8"/><path d="M5 5.5v13c0 1.55 3.13 2.8 7 2.8s7-1.25 7-2.8v-13"/><path d="M5 12c0 1.55 3.13 2.8 7 2.8s7-1.25 7-2.8"/>',
};
function icon(name, size = 18, sw = 1.7) {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${P[name] || ""}</svg>`;
}

/* ---------------- Mock 数据（全部为演示样例） ---------------- */
const KBS = [
  { name: "Test", docs: 5, size: "2.1 MB", time: "8月28日", sub: "日常联调知识库", evalKb: false },
  { name: "codex-stage1-repro-eval", docs: 3, size: "640 KB", time: "8月27日", sub: "阶段一复现评测集", evalKb: true },
  { name: "codex-stage2-fill-chunking", docs: 3, size: "712 KB", time: "8月26日", sub: "阶段二填充分块对比", evalKb: true },
  { name: "codex-stage2-chunk-768", docs: 3, size: "690 KB", time: "8月26日", sub: "768 token 分块实验", evalKb: true },
  { name: "codex-stage2-chunk-512", docs: 3, size: "672 KB", time: "8月25日", sub: "512 token 分块实验", evalKb: true },
  { name: "codex-stage2-chunk-1024", docs: 3, size: "705 KB", time: "8月25日", sub: "1024 token 分块实验", evalKb: true },
  { name: "eval-baseline", docs: 3, size: "580 KB", time: "8月24日", sub: "固定评测基线", evalKb: true },
  { name: "RAG知识点", docs: 1, size: "96 KB", time: "8月20日", sub: "RAG 核心概念笔记", evalKb: false },
  { name: "JWT登录认证", docs: 4, size: "348 KB", time: "8月18日", sub: "认证与令牌机制", evalKb: false },
  { name: "spring注解讲解", docs: 6, size: "512 KB", time: "8月12日", sub: "Spring 常用注解", evalKb: false },
];
const RECENT_DOCS = [
  { name: "test-upload.txt", size: "2 KB", time: "昨天", kb: "Test" },
  { name: "test-md.md", size: "18 KB", time: "星期三", kb: "Test" },
  { name: "rag-notes.md", size: "96 KB", time: "8月20日", kb: "RAG知识点" },
  { name: "jwt-flow.pdf", size: "128 KB", time: "8月18日", kb: "JWT登录认证" },
  { name: "spring-annotations.md", size: "240 KB", time: "8月12日", kb: "spring注解讲解" },
];

/* 预置对话：turns 中 assistant 携带 retrieval / cites / answer（Markdown 样例） */
const CONVS = [
  {
    id: "c1", title: "什么是大模型微调？", tag: "RAG知识点",
    turns: [
      { role: "user", text: "什么是大模型微调？它和 RAG 有什么区别？" },
      {
        role: "assistant",
        retrieval: { kb: "RAG知识点", ms: "0.48", chunks: [
          { file: "llm-basic-concepts.md", chunk: 12, score: 0.91 },
          { file: "rag-vs-finetune.md", chunk: 3, score: 0.88 },
          { file: "fine-tuning-notes.md", chunk: 7, score: 0.82 },
        ]},
        cites: [
          { file: "llm-basic-concepts.md", kb: "RAG知识点", score: "0.91", chunk: 12, snippet: "微调（Fine-tuning）是指在预训练模型的基础上，使用领域内数据继续训练，使模型参数本身适配下游任务。常见方式包括全量微调、LoRA 等参数高效微调。" },
          { file: "rag-vs-finetune.md", kb: "RAG知识点", score: "0.88", chunk: 3, snippet: "RAG 不修改模型参数，将知识存放在外部向量库中，检索相关片段后拼入上下文，适合知识频繁更新、需要给出处的场景；微调更适合固化表达风格与输出格式。" },
          { file: "fine-tuning-notes.md", kb: "RAG知识点", score: "0.82", chunk: 7, snippet: "LoRA 通过只训练低秩增量矩阵，将可训练参数量降低到原来的千分之一量级，单张消费级显卡即可完成领域适配，是当前主流的参数高效微调方案。" },
        ],
        answer: `**大模型微调（Fine-tuning）** 是在预训练模型基础上，用领域数据继续训练，让模型参数本身适配特定任务的过程。{{cite:0}}

## 一、微调和 RAG 的核心区别
- **微调**改变模型参数，适合固化表达风格、输出格式与领域语言；
- **RAG** 不改参数，把知识放在外部向量库，检索后拼进上下文，适合知识频繁更新、需要给出处的场景；{{cite:1}}
- 在企业问答里两者常常组合：微调负责"会说话"，RAG 负责"有依据"。

## 二、常见的微调方式
1. **全量微调**：更新全部参数，效果上限最高，但算力成本也最高；
2. **LoRA / QLoRA**：只训练低秩增量矩阵，单卡可跑，是当前主流方案；{{cite:2}}
3. **指令微调（SFT）**：用"指令-回答"数据对训练，让模型学会理解并执行任务。

> 简单结论：如果知识每月都在更新，优先 RAG；只有表达方式或输出格式需要固定时，才考虑微调。`,
      },
    ],
  },
  {
    id: "c2", title: "请你讲讲 RAG 项目的检索质量该怎么评估", tag: "eval-baseline",
    turns: [
      { role: "user", text: "请你讲讲在你的 RAG 项目里，检索质量该怎么评估？" },
      {
        role: "assistant",
        retrieval: { kb: "eval-baseline", ms: "0.61", chunks: [
          { file: "eval-metrics.md", chunk: 2, score: 0.93 },
          { file: "rag-eval-dev-v2-spec.md", chunk: 9, score: 0.87 },
          { file: "quality-gate.md", chunk: 1, score: 0.84 },
        ]},
        cites: [
          { file: "eval-metrics.md", kb: "eval-baseline", score: "0.93", chunk: 2, snippet: "检索质量常用指标：Recall@k 衡量标准片段是否进入 top-k；MRR 衡量首个命中片段的排名；nDCG 在分级相关性下评估整体排序质量。" },
          { file: "rag-eval-dev-v2-spec.md", kb: "eval-baseline", score: "0.87", chunk: 9, snippet: "评测集 rag-eval-dev-v2 固定题目、标准答案与出处 chunk 的版本；向量库索引维度、分片与 snapshot 必须可复现，禁止中途变更口径。" },
          { file: "quality-gate.md", kb: "eval-baseline", score: "0.84", chunk: 1, snippet: "新检索配置先在 shadow profile 中与 baseline 跑同题对比，达到既定阈值后才允许切换；未达标时 fail closed，不得静默放行。" },
        ],
        answer: `评估检索质量，核心是**用固定的评测集 + 稳定的指标口径**反复测量，而不是靠肉眼抽看。{{cite:0}}

## 一、先固定三件事
1. **评测集**：题目、标准答案、出处 chunk 固定版本，例如 \`rag-eval-dev-v2\`；
2. **向量库状态**：索引维度、分片与 snapshot 必须可复现；
3. **指标口径**：Recall@k、MRR、nDCG 各自怎么算，写成文档，不许漂移。{{cite:1}}

## 二、常用指标
- **Recall@k**：标准片段是否出现在 top-k，衡量"找没找到"；
- **MRR**：首个命中片段的倒数排名均值，衡量"排得靠不靠前"；
- **nDCG**：考虑分级相关性时的整体排序质量。

## 三、建议的门禁姿势
新配置先在 **shadow profile** 里与 baseline 跑同题对比，达到阈值再切流量；没达标就 **fail closed**，不要悄悄放行。{{cite:2}}`,
      },
    ],
  },
  {
    id: "c3", title: "什么是spring你知道吗？", tag: "spring注解讲解",
    turns: [
      { role: "user", text: "什么是spring你知道吗？" },
      {
        role: "assistant",
        retrieval: { kb: "spring注解讲解", ms: "0.42", chunks: [
          { file: "spring-core.md", chunk: 1, score: 0.9 },
          { file: "ioc-di-notes.md", chunk: 4, score: 0.85 },
        ]},
        cites: [
          { file: "spring-core.md", kb: "spring注解讲解", score: "0.90", chunk: 1, snippet: "Spring 是一个轻量级的 Java 企业级开发框架，核心是 IoC 容器与 AOP。它通过容器管理对象生命周期与依赖关系，让业务代码专注于自身逻辑。" },
          { file: "ioc-di-notes.md", kb: "spring注解讲解", score: "0.85", chunk: 4, snippet: "控制反转（IoC）把对象的创建与装配交给容器；依赖注入（DI）是它的实现方式，常用构造器注入，配合 @ComponentScan 与 @Autowired 等注解完成装配。" },
        ],
        answer: `知道的。**Spring** 是一个轻量级的 Java 企业级开发框架，核心是 **IoC 容器**和 **AOP**。{{cite:0}}

## 一、它解决什么问题
- 传统开发里对象由你自己 \`new\`、自己拼装依赖，耦合度高；
- Spring 把对象的创建、装配、生命周期交给**容器**管理，业务代码只声明"我需要什么"。{{cite:1}}

## 二、两大基石
1. **IoC / DI**：控制反转与依赖注入，是整个生态的地基；
2. **AOP**：面向切面编程，事务、日志、权限这类横切逻辑统一由代理层处理。

在这个知识库（spring注解讲解）里，后续可以继续问 \`@Transactional\`、\`@Component\` 等具体注解的用法。`,
      },
    ],
  },
  {
    id: "c4", title: "帮我总结JWT登录认证", tag: "JWT登录认证",
    turns: [
      { role: "user", text: "帮我总结一下 JWT 登录认证的核心流程" },
      {
        role: "assistant",
        retrieval: { kb: "JWT登录认证", ms: "0.55", chunks: [
          { file: "jwt-flow.md", chunk: 2, score: 0.92 },
          { file: "token-refresh.md", chunk: 5, score: 0.86 },
          { file: "jwt-risks.md", chunk: 3, score: 0.81 },
        ]},
        cites: [
          { file: "jwt-flow.md", kb: "JWT登录认证", score: "0.92", chunk: 2, snippet: "登录成功后使用 HS256 签发 access token 与 refresh token；后续请求通过 Authorization: Bearer 头携带，服务端本地验签，无需查询会话存储。" },
          { file: "token-refresh.md", kb: "JWT登录认证", score: "0.86", chunk: 5, snippet: "access token 有效期应设置得较短；过期后用 refresh token 换取新令牌，refresh token 一次一换，旧值立即作废以降低泄露风险。" },
          { file: "jwt-risks.md", kb: "JWT登录认证", score: "0.81", chunk: 3, snippet: "JWT 签发后无法主动撤销，登出与封禁场景需要配合黑名单或极短有效期兜底；切勿在 payload 中放置敏感信息。" },
        ],
        answer: `「JWT登录认证」知识库目前收录 **4 篇文档**，核心流程可以概括成 4 步：{{cite:0}}

## 一、登录签发
校验账号密码后，用 \`HS256\` 签发 **access token（短期）** 与 **refresh token（长期）**。

## 二、请求携带
前端把 access token 放进 \`Authorization: Bearer <token>\`，过滤器**本地验签**，不需要查库。{{cite:0}}

## 三、刷新续期
access token 过期后用 refresh token 换新；refresh token **一次一换**，旧值立即作废。{{cite:1}}

## 四、注销与风险
JWT 签出后无法主动撤销，登出要靠**黑名单或短有效期**兜底，payload 里也不要放敏感信息。{{cite:2}}`,
      },
    ],
  },
  {
    id: "c5", title: "向量检索和关键词检索的区别", tag: "",
    turns: [
      { role: "user", text: "向量检索和关键词检索有什么区别？" },
      {
        role: "assistant",
        retrieval: { kb: "RAG知识点", ms: "0.51", chunks: [
          { file: "hybrid-retrieval.md", chunk: 6, score: 0.9 },
          { file: "bm25-notes.md", chunk: 2, score: 0.83 },
        ]},
        cites: [
          { file: "hybrid-retrieval.md", kb: "RAG知识点", score: "0.90", chunk: 6, snippet: "生产环境通常采用混合检索：关键词（BM25）与向量双路召回，再经 rerank 模型统一重排，兼顾字面精确匹配与语义泛化。" },
          { file: "bm25-notes.md", kb: "RAG知识点", score: "0.83", chunk: 2, snippet: "BM25 基于词频与逆文档频率，对编号、类名、错误码等字面明确的查询非常可靠，但对同义改写敏感度低。" },
        ],
        answer: `两者是"怎么找到相关内容"的两条路线，生产上通常**混合使用**。{{cite:0}}

- **关键词检索（BM25）**：精确匹配字面，编号、类名、报错码这类查询更可靠；{{cite:1}}
- **向量检索**：把语义投影到高维空间，"换个说法也能找到"，但对没见过的专有名词容易漂移；
- **混合检索 + Rerank**：双路召回后用重排模型统一排序，是目前企业 RAG 的主流配置。`,
      },
    ],
  },
];

/* 首页轮换建议 */
const SUGGESTIONS = {
  chat: [
    { icon: "doc", text: "帮我总结「JWT登录认证」知识库的核心流程" },
    { icon: "spark", text: "什么是大模型微调？它和 RAG 有什么区别？" },
    { icon: "search", text: "在「spring注解讲解」里查一下 @Transactional 的注意事项" },
    { icon: "db", text: "这个知识库包含哪些主要内容？" },
  ],
  work: [
    { icon: "clock", text: "把本周的检索质量评测结论整理成一条工作周报" },
    { icon: "sliders", text: "帮我安排明天上午的向量库迁移验证任务" },
    { icon: "doc", text: "总结这个项目当前剩余的技术债清单" },
  ],
};

/* ---------------- 全局状态 ---------------- */
const state = {
  view: "home",            // home | chat | kb
  mode: "chat",            // 聊天 | 工作
  convId: null,
  kbScope: null,           // 输入框选择的知识库范围
  streaming: false,
  streamTimer: null,
  kbFilter: "all",
  kbMode: "list",
  kbQuery: "",
  reason: "高",
  settingsSection: "常规",
  suggestIdx: 0,
};

/* ---------------- 侧栏 ---------------- */
function renderSidebar() {
  $("#navNewChat").innerHTML = `${icon("newchat")}<span>新聊天</span>`;
  $("#navKb").innerHTML = `${icon("library")}<span>知识库</span>`;
  $("#navKb").classList.toggle("active", state.view === "kb");
  const phs = [["folder", "项目"], ["clock", "已安排"], ["apps", "插件"], ["dots", "更多"]];
  $$(".nav-item[data-ph]").forEach((el, i) => {
    el.innerHTML = `${icon(phs[i][0])}<span>${phs[i][1]}</span>`;
  });

  const list = $("#histList");
  list.innerHTML = "";
  CONVS.forEach(c => {
    const b = document.createElement("button");
    b.className = "hist-item" + (state.view === "chat" && state.convId === c.id ? " active" : "");
    b.dataset.act = "open-conv"; b.dataset.id = c.id;
    b.innerHTML = `<span class="hist-title">${esc(c.title)}</span>${c.tag ? `<span class="hist-tag">${esc(c.tag)}</span>` : ""}
      <span class="hist-del" data-act="del-conv" data-id="${c.id}" data-tip="删除">${icon("trash", 15)}</span>`;
    list.appendChild(b);
  });
  if (!CONVS.length) list.innerHTML = `<div class="hist-empty">暂无历史对话</div>`;
}

/* ---------------- 顶栏 ---------------- */
function renderTopbar() {
  const seg = $("#segMode");
  seg.style.display = state.view === "home" ? "flex" : "none";

  const left = $("#topbarLeft");
  left.innerHTML = `
    <span class="wordmark">RAG 智能问答</span>
    <button class="icon-btn" data-act="open-search" data-tip="搜索">${icon("search")}</button>
    <button class="icon-btn" data-act="new-chat" data-tip="新聊天">${icon("newchat")}</button>
    <button class="icon-btn" data-act="toggle-sb" data-tip="打开边栏">${icon("panel")}</button>`;

  const right = $("#topbarRight");
  if (state.view === "chat") {
    right.innerHTML = `
      <button class="tb-share" data-act="share-menu">${icon("share", 17)}<span>分享</span></button>
      <button class="icon-btn" data-act="more-menu" data-tip="更多">${icon("dots")}</button>`;
  } else {
    right.innerHTML = `
      <button class="icon-btn" data-act="nav-ph" data-ph="临时聊天" data-tip="临时聊天">${icon("temp")}</button>
      <button class="icon-btn" data-act="share-menu" data-tip="分享">${icon("share")}</button>`;
  }
}

/* ---------------- 视图路由 ---------------- */
function show(view) {
  state.view = view;
  $("#viewHome").hidden = view !== "home";
  $("#viewChat").hidden = view !== "chat";
  $("#viewKb").hidden = view !== "kb";

  const composer = $("#composerBox").closest(".composer");
  if (view === "home") $("#composerHomeSlot").appendChild(composer);
  if (view === "chat") $("#composerChatSlot").appendChild(composer);

  renderSidebar();
  renderTopbar();
  if (view === "home") renderSuggest();
  if (view === "kb") renderKbTable();
  if (view === "chat") requestAnimationFrame(() => { $("#msgScroll").scrollTop = $("#msgScroll").scrollHeight; });
}

/* ---------------- 首页 ---------------- */
function renderSuggest() {
  const arr = SUGGESTIONS[state.mode];
  const s = arr[state.suggestIdx % arr.length];
  const el = $("#suggestLine");
  el.classList.add("fading");
  setTimeout(() => el.classList.remove("fading"), 420);
  el.dataset.act = "suggest";
  el.innerHTML = `<span class="icon-slot">${icon(s.icon, 17)}</span><span class="suggest-text">${esc(s.text)}</span>`;
}
setInterval(() => {
  if (state.view === "home" && !document.hidden) { state.suggestIdx++; renderSuggest(); }
}, 7000);

function setMode(mode) {
  state.mode = mode;
  $$("#segMode .seg").forEach(b => b.classList.toggle("on", b.dataset.mode === mode));
  const g = $("#greeting");
  g.classList.add("fading");
  setTimeout(() => g.classList.remove("fading"), 380);
  g.textContent = mode === "work" ? "今天要推进哪些工作？" : "你好，admin。准备好开始了吗？";
  $("#composerInput").placeholder = mode === "work" ? "安排、总结、跟进你的工作" : "问问 RAG 知识库";
  state.suggestIdx = 0;
  renderSuggest();
}

/* ---------------- Markdown-lite 渲染 ---------------- */
function inline(s, conv) {
  s = esc(s);
  s = s.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  s = s.replace(/`([^`]+)`/g, "<code>$1</code>");
  s = s.replace(/\{\{cite:(\d+)\}\}/g, (_, i) => {
    const c = conv && conv.turns && conv.turns[conv._turnIdx] && conv.turns[conv._turnIdx].cites && conv.turns[conv._turnIdx].cites[i];
    if (!c) return "";
    const extra = c.extra ? ` <span class="cite-plus">+${c.extra}</span>` : "";
    return `<button class="cite-chip" data-act="cite-open" data-turn="${conv._turnIdx}" data-cite="${i}">${icon("doc", 12)}<span class="cite-name">${esc(c.file)}</span>${extra}</button>`;
  });
  return s;
}
function mdRender(src, conv) {
  const lines = src.split("\n");
  let html = "", inCode = false, codeLang = "", codeBuf = [], listBuf = null;

  const flushList = () => {
    if (listBuf) { html += `<${listBuf.tag}>` + listBuf.items.map(i => `<li>${i}</li>`).join("") + `</${listBuf.tag}>`; listBuf = null; }
  };
  const flushCode = () => {
    if (inCode) {
      const raw = codeBuf.join("\n");
      html += `<div class="codeblock"><div class="codeblock-head"><span>${esc(codeLang || "text")}</span>
        <button class="codeblock-copy" data-act="copy-code" data-code="${encodeURIComponent(raw)}">${icon("copy", 13)} 复制</button></div>
        <pre><code>${esc(raw)}</code></pre></div>`;
      inCode = false; codeBuf = [];
    }
  };

  for (const line of lines) {
    const fence = line.match(/^```(\w*)\s*$/);
    if (fence) {
      if (inCode) { flushCode(); flushList(); }
      else { flushList(); inCode = true; codeLang = fence[1] || ""; }
      continue;
    }
    if (inCode) { codeBuf.push(line); continue; }
    const t = line.trim();
    if (!t) { flushList(); continue; }
    let m;
    if ((m = t.match(/^###\s+(.+)/))) { flushList(); html += `<h3>${inline(m[1], conv)}</h3>`; }
    else if ((m = t.match(/^##\s+(.+)/))) { flushList(); html += `<h2>${inline(m[1], conv)}</h2>`; }
    else if ((m = t.match(/^#\s+(.+)/))) { flushList(); html += `<h2>${inline(m[1], conv)}</h2>`; }
    else if (t.startsWith("> ")) { flushList(); html += `<blockquote class="md-quote" style="border-left:3px solid var(--border-2);padding:2px 0 2px 14px;margin:12px 0;color:var(--text-2)">${inline(t.slice(2), conv)}</blockquote>`; }
    else if ((m = t.match(/^[-*]\s+(.+)/))) {
      if (!listBuf || listBuf.tag !== "ul") { flushList(); listBuf = { tag: "ul", items: [] }; }
      listBuf.items.push(inline(m[1], conv));
    }
    else if ((m = t.match(/^(\d+)[.、]\s+(.+)/))) {
      if (!listBuf || listBuf.tag !== "ol") { flushList(); listBuf = { tag: "ol", items: [] }; }
      listBuf.items.push(inline(m[2], conv));
    }
    else { flushList(); html += `<p>${inline(t, conv)}</p>`; }
  }
  flushCode(); flushList();
  return html;
}

/* ---------------- 消息渲染 ---------------- */
function retrievalHtml(r, conv, openable = true) {
  const label = `已检索 「${esc(r.kb)}」· ${r.chunks.length} 个片段 · ${r.ms} 秒`;
  const rows = r.chunks.map(c => `
    <div class="rc-row">${icon("file", 14)}
      <span class="rc-name">${esc(c.file)}</span>
      <span class="rc-bar"><i style="width:${Math.round(c.score * 100)}%"></i></span>
      <span class="rc-meta">#${c.chunk} · ${c.score.toFixed(2)}</span>
    </div>`).join("");
  return `<div class="retrieval">
    <button class="retrieval-toggle" ${openable ? 'data-act="retrieval-toggle"' : ""}>
      ${icon("spark", 15)}<span>${label}</span><span class="icon-slot chev">${icon("chevR", 14)}</span>
    </button>
    <div class="retrieval-body">${rows}</div>
  </div>`;
}

function turnHtml(t, conv, idx) {
  if (t.role === "user") return `<div class="turn user"><div class="bubble">${esc(t.text)}</div></div>`;
  conv._cites = t.cites; conv._turnIdx = idx;
  return `<div class="turn assistant" data-turn="${idx}">
    ${retrievalHtml(t.retrieval, conv)}
    <div class="md">${mdRender(t.answer, conv)}</div>
    <div class="msg-actions">
      <button class="ma-btn" data-act="msg-copy" data-tip="复制" data-turn="${idx}">${icon("copy", 16)}</button>
      <button class="ma-btn" data-act="msg-like" data-tip="好评" data-turn="${idx}">${icon("thumbU", 16)}</button>
      <button class="ma-btn" data-act="msg-dislike" data-tip="差评" data-turn="${idx}">${icon("thumbD", 16)}</button>
      <button class="ma-btn" data-act="msg-regen" data-tip="重新生成" data-turn="${idx}">${icon("refresh", 16)}</button>
      <button class="ma-btn" data-act="msg-share" data-tip="分享" data-turn="${idx}">${icon("share", 16)}</button>
    </div>
  </div>`;
}

function renderConv(conv, skipLast = false) {
  const col = $("#msgCol");
  col.innerHTML = "";
  const turns = skipLast ? conv.turns.slice(0, -1) : conv.turns;
  turns.forEach((t, i) => {
    const wrap = document.createElement("div");
    wrap.innerHTML = turnHtml(t, conv, i);
    col.appendChild(wrap.firstElementChild);
  });
}

function openConv(id) {
  const conv = CONVS.find(c => c.id === id);
  if (!conv) return;
  state.convId = id;
  renderConv(conv);
  show("chat");
}

/* ---------------- 回答生成（mock 检索 + 流式输出） ---------------- */
function pickQA(question) {
  const q = question.toLowerCase();
  const kb = state.kbScope || "Test";
  const mkCites = base => base.map(c => ({ ...c, kb: c.kb || kb }));

  if (/微调|finetun|fine-tun/.test(q)) {
    const c = CONVS.find(x => x.id === "c1").turns[1];
    return { retrieval: c.retrieval, cites: c.cites, answer: c.answer };
  }
  if (/检索质量|评估|评测|recall|mrr/.test(q)) {
    const c = CONVS.find(x => x.id === "c2").turns[1];
    return { retrieval: c.retrieval, cites: c.cites, answer: c.answer };
  }
  if (/spring|事务|transactional/.test(q)) {
    const src = CONVS.find(x => x.id === "c3").turns[1];
    if (/transactional|事务/.test(q)) {
      return {
        retrieval: { kb: "spring注解讲解", ms: "0.44", chunks: [
          { file: "transactional.md", chunk: 8, score: 0.92 },
          { file: "aop-proxy.md", chunk: 3, score: 0.87 },
          { file: "tx-config.yml", chunk: 1, score: 0.79 },
        ]},
        cites: mkCites([
          { file: "transactional.md", kb: "spring注解讲解", score: "0.92", chunk: 8, snippet: "@Transactional 通过 AOP 代理实现声明式事务；rollbackFor 指定回滚异常（默认仅 RuntimeException 与 Error），propagation 控制传播行为，readOnly 标记只读事务。" },
          { file: "aop-proxy.md", kb: "spring注解讲解", score: "0.87", chunk: 3, snippet: "同类内部 this 调用不会经过代理对象，事务切面不生效；这是 @Transactional 失效最常见的原因，需通过注入自身代理或拆分类解决。" },
          { file: "tx-config.yml", kb: "spring注解讲解", score: "0.79", chunk: 1, snippet: "spring-boot 事务默认超时、隔离级别与回滚规则的示例配置。" },
        ]),
        answer: `**@Transactional** 是 Spring 声明式事务的核心注解，加在类或方法上，Spring 通过 AOP 代理自动管理提交与回滚。{{cite:0}}

## 一、常用属性
- \`rollbackFor\`：指定触发回滚的异常，默认只回滚 \`RuntimeException\` 与 \`Error\`；
- \`propagation\`：传播行为，常用 \`REQUIRED\`（默认）与 \`REQUIRES_NEW\`；
- \`readOnly\`：只读事务，底层可做优化。{{cite:2}}

## 二、最常见的三个坑
1. **同类内部调用失效**：\`this.methodB()\` 不走代理，注解不生效；{{cite:1}}
2. **方法必须为 public**：非 public 方法上的注解会被忽略；
3. **异常被吞不回滚**：try-catch 吞掉异常后事务照常提交。

\`\`\`java
@Service
public class OrderService {

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderMapper.insert(order);   // ① 写订单
        stockService.deduct(order);  // ② 扣库存，失败则整体回滚
    }
}
\`\`\``,
      };
    }
    return { retrieval: src.retrieval, cites: src.cites, answer: src.answer };
  }
  if (/jwt|登录|认证|token/.test(q)) {
    const c = CONVS.find(x => x.id === "c4").turns[1];
    return { retrieval: c.retrieval, cites: c.cites, answer: c.answer };
  }
  if (/向量|关键词|检索/.test(q)) {
    const c = CONVS.find(x => x.id === "c5").turns[1];
    return { retrieval: c.retrieval, cites: c.cites, answer: c.answer };
  }
  if (/这个知识库|包含哪些|主要内容|有哪些/.test(q)) {
    const items = KBS.slice(0, 5).map((k, i) => `${i + 1}. **${k.name}**：${k.docs} 篇文档 · ${k.sub}`);
    return {
      retrieval: { kb, ms: "0.38", chunks: [
        { file: "catalog.md", chunk: 1, score: 0.89 },
        { file: "readme.md", chunk: 2, score: 0.8 },
      ]},
      cites: mkCites([
        { file: "catalog.md", score: "0.89", chunk: 1, snippet: "知识库目录清单：收录文档名称、所属主题与篇数统计。" },
        { file: "readme.md", score: "0.80", chunk: 2, snippet: "知识库用途说明与使用约定。" },
      ]),
      answer: `当前工作区共有 **${KBS.length} 个知识库**，主要包括：{{cite:0}}

${items.join("\n")}

其中 \`codex-*\` 与 \`eval-*\` 是**评测用知识库**，日常业务提问建议选择其余知识库作为检索范围。{{cite:1}}`,
    };
  }
  /* 兜底模板 */
  const n = 3 + (question.length % 3);
  return {
    retrieval: { kb, ms: (0.4 + question.length * 0.004).toFixed(2), chunks: [
      { file: "matched-1.md", chunk: 4, score: 0.88 },
      { file: "matched-2.md", chunk: 11, score: 0.84 },
      { file: "matched-3.pdf", chunk: 2, score: 0.79 },
    ]},
    cites: mkCites([
      { file: "matched-1.md", score: "0.88", chunk: 4, snippet: "与提问最相关的片段样例：这里会展示命中的原文内容、所在 chunk 与相似度分数。" },
      { file: "matched-2.md", score: "0.84", chunk: 11, snippet: "次相关片段样例。接通后端后，此处内容来自真实检索结果。" },
    ]),
    answer: `已根据「${kb}」知识库完成检索，找到 **${n} 个相关片段**。下面是归纳后的回答：{{cite:0}}

## 一、与你的问题直接相关的内容
- 知识库中确有与「${question.slice(0, 24)}」相关的资料，最高相似度 **0.88**；{{cite:1}}
- 相关内容主要集中在 \`matched-1.md\` 等文档中，可点开引用角标查看原文片段。

## 二、要点归纳
1. 多个片段的共同结论优先呈现，存在分歧时会并列列出；
2. 点按回答下方按钮可以复制、重新生成或给出反馈。

> 说明：这是高仿界面的**演示回答**，内容为预置样例，不代表真实检索结果；接通后端后，这里会展示真实的 RAG 检索与生成过程。`,
  };
}

function newConvFor(question) {
  const conv = {
    id: "c" + Date.now(),
    title: question.slice(0, 20) || "新对话",
    tag: state.kbScope || "",
    turns: [],
  };
  CONVS.unshift(conv);
  return conv;
}

function startAsk(question) {
  if (state.streaming) return;
  let conv;
  if (state.view === "chat" && state.convId) conv = CONVS.find(c => c.id === state.convId);
  else { conv = newConvFor(question); state.convId = conv.id; }

  conv.turns.push({ role: "user", text: question });

  const qa = pickQA(question);
  const turn = { role: "assistant", retrieval: qa.retrieval, cites: qa.cites, answer: qa.answer, _demo: true };
  conv.turns.push(turn);

  renderConv(conv, true);
  show("chat");
  renderSidebar();
  streamTurn(conv, turn);
}

function streamTurn(conv, turn) {
  state.streaming = true;
  updateVoiceBtn();
  const idx = conv.turns.indexOf(turn);
  const col = $("#msgCol");
  const wrap = document.createElement("div");
  wrap.className = "turn assistant";
  wrap.innerHTML = `<div class="thinking-line">${icon("spark", 15)}<span class="shimmer" id="thinkLabel">正在检索知识库「${esc(turn.retrieval.kb)}」...</span></div>`;
  col.appendChild(wrap);
  autoScroll(true);

  const nearBottom = () => {
    const sc = $("#msgScroll");
    return sc.scrollHeight - sc.scrollTop - sc.clientHeight < 140;
  };
  const follow = () => { if (nearBottom()) $("#msgScroll").scrollTop = $("#msgScroll").scrollHeight; };

  setTimeout(() => {
    if (!state.streaming) return;
    conv._cites = turn.cites; conv._turnIdx = idx;
    wrap.innerHTML = retrievalHtml(turn.retrieval, conv) + `<div class="md" id="streamMd"></div>`;
    const md = $("#streamMd", wrap);
    const full = turn.answer;
    let pos = 0;
    state.streamTimer = setInterval(() => {
      pos += 2 + Math.floor(Math.random() * 4);
      if (pos >= full.length) pos = full.length;
      conv._cites = turn.cites; conv._turnIdx = idx;
      md.innerHTML = mdRender(full.slice(0, pos), conv) + (pos < full.length ? `<span class="caret"></span>` : "");
      follow();
      if (pos >= full.length) finishStream(conv, turn, wrap, md, false);
    }, 16);
  }, 900);
}

function finishStream(conv, turn, wrap, md, stopped) {
  clearInterval(state.streamTimer);
  state.streamTimer = null;
  state.streaming = false;
  if (stopped) turn.answer = mdRender._partial || turn.answer;
  md.innerHTML = mdRender(turn.answer, conv) + (stopped ? `<p class="shimmer" style="display:inline;font-size:13px">（已停止生成）</p>` : "");
  const acts = document.createElement("div");
  acts.className = "msg-actions";
  const idx = conv.turns.indexOf(turn);
  acts.innerHTML = `
    <button class="ma-btn" data-act="msg-copy" data-tip="复制" data-turn="${idx}">${icon("copy", 16)}</button>
    <button class="ma-btn" data-act="msg-like" data-tip="好评" data-turn="${idx}">${icon("thumbU", 16)}</button>
    <button class="ma-btn" data-act="msg-dislike" data-tip="差评" data-turn="${idx}">${icon("thumbD", 16)}</button>
    <button class="ma-btn" data-act="msg-regen" data-tip="重新生成" data-turn="${idx}">${icon("refresh", 16)}</button>
    <button class="ma-btn" data-act="msg-share" data-tip="分享" data-turn="${idx}">${icon("share", 16)}</button>`;
  wrap.appendChild(acts);
  updateVoiceBtn();
}

function stopStream() {
  if (!state.streaming) return;
  const md = $("#streamMd");
  clearInterval(state.streamTimer);
  state.streamTimer = null;
  state.streaming = false;
  if (md) {
    const wrap = md.closest(".turn");
    const conv = CONVS.find(c => c.id === state.convId);
    const turn = conv && conv.turns[conv.turns.length - 1];
    if (turn) { turn.answer = md.textContent || turn.answer; finishStream(conv, turn, wrap, md, true); }
  }
  updateVoiceBtn();
  toast("已停止生成");
}

function regenerate(conv) {
  const lastA = [...conv.turns].reverse().find(t => t.role === "assistant");
  if (!lastA || state.streaming) return;
  conv.turns.splice(conv.turns.indexOf(lastA), 1);
  renderConv(conv);
  streamTurn(conv, lastA);
}

/* ---------------- 输入框 Composer ---------------- */
function initComposer() {
  const node = $("#composerTpl").content.firstElementChild.cloneNode(true);
  $("#composerHomeSlot").appendChild(node);
  const ta = $("#composerInput");
  ta.addEventListener("input", () => { autoGrow(ta); updateVoiceBtn(); });
  ta.addEventListener("keydown", e => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendCurrent(); }
  });
  updateVoiceBtn();
}
function autoGrow(ta) {
  ta.style.height = "auto";
  ta.style.height = Math.min(ta.scrollHeight, 200) + "px";
}
function sendCurrent() {
  const ta = $("#composerInput");
  const q = ta.value.trim();
  if (!q || state.streaming) return;
  ta.value = ""; autoGrow(ta);
  startAsk(q);
}
function updateVoiceBtn() {
  const btn = $("#voiceBtn");
  if (!btn) return;
  if (state.streaming) {
    btn.className = "voice-btn stop-mode"; btn.dataset.act = "stop";
    btn.innerHTML = icon("stop", 15);
    btn.dataset.tip = "停止生成";
  } else if ($("#composerInput").value.trim()) {
    btn.className = "voice-btn send-mode"; btn.dataset.act = "voice-or-send";
    btn.innerHTML = icon("arrowUp", 17);
    btn.dataset.tip = "发送";
  } else {
    btn.className = "voice-btn"; btn.dataset.act = "voice-or-send";
    btn.innerHTML = `<span class="wave"><i></i><i></i><i></i><i></i></span>`;
    btn.dataset.tip = "语音模式";
  }
}
function renderScope() {
  const row = $("#kbScopeRow");
  if (!state.kbScope) { row.hidden = true; row.innerHTML = ""; return; }
  row.hidden = false;
  row.innerHTML = `<span class="kb-scope-chip">${icon("db", 14)} 知识库范围：<b>${esc(state.kbScope)}</b>
    <button class="kb-scope-x" data-act="clear-kb">${icon("x", 12)}</button></span>`;
  $("#composerInput").placeholder = state.mode === "work" ? "安排、总结、跟进你的工作" : `在「${state.kbScope}」中提问…`;
}

function autoScroll(force) {
  const sc = $("#msgScroll");
  if (force || sc.scrollHeight - sc.scrollTop - sc.clientHeight < 160) sc.scrollTop = sc.scrollHeight;
}

/* ---------------- 知识库视图 ---------------- */
function renderKbTable() {
  const wrap = $("#kbTable");
  const q = state.kbQuery.trim().toLowerCase();
  let rows = [];
  if (state.kbFilter !== "doc") rows = rows.concat(KBS.map(k => ({ type: "kb", ...k })));
  if (state.kbFilter !== "kb") rows = rows.concat(RECENT_DOCS.map(d => ({ type: "doc", ...d })));
  if (q) rows = rows.filter(r => r.name.toLowerCase().includes(q) || (r.sub || "").includes(q));

  const head = `<div class="kb-th"><span class="kb-name">名称</span><span class="kb-time">更新时间</span><span class="kb-docs">文档</span><span class="kb-size">大小</span><span class="kb-status">状态</span></div>`;
  const body = rows.map(r => r.type === "kb" ? `
    <div class="kb-row" data-act="kb-row" data-name="${esc(r.name)}">
      <span class="kb-icon">${icon("db", 19)}</span>
      <span class="kb-name">${esc(r.name)}${r.sub ? `<div class="kb-sub">${esc(r.sub)}</div>` : ""}</span>
      <span class="kb-time">${esc(r.time)}</span>
      <span class="kb-docs">${r.docs} 篇</span>
      <span class="kb-size">${esc(r.size)}</span>
      <span class="kb-status"><span class="badge ${r.evalKb ? "dim" : ""}"><i></i>${r.evalKb ? "评测" : "就绪"}</span></span>
    </div>` : `
    <div class="kb-row" data-act="doc-row" data-name="${esc(r.name)}">
      <span class="kb-icon">${icon("doc", 19)}</span>
      <span class="kb-name">${esc(r.name)}<div class="kb-sub">来自 ${esc(r.kb)}</div></span>
      <span class="kb-time">${esc(r.time)}</span>
      <span class="kb-docs">—</span>
      <span class="kb-size">${esc(r.size)}</span>
      <span class="kb-status"><span class="badge"><i></i>已索引</span></span>
    </div>`);
  wrap.className = "kb-table" + (state.kbMode === "grid" ? " grid" : "");
  wrap.innerHTML = state.kbMode === "list" ? head + body.join("") : body.join("");
  if (!rows.length) wrap.innerHTML = `<div class="ms-none">没有匹配「${esc(state.kbQuery)}」的结果</div>`;
  $$(".kb-viewbtns .icon-btn").forEach(b => b.classList.toggle("on", b.dataset.mode === state.kbMode));
}

/* ---------------- 弹窗与菜单 ---------------- */
function closeMenu() { $("#menu").hidden = true; }
function closeCite() { $("#citePop").hidden = true; }

function openMenu(anchor, html, opts = {}) {
  const m = $("#menu");
  m.innerHTML = html;
  m.hidden = false;
  m._context = opts.context || null;
  const r = anchor.getBoundingClientRect();
  const mw = m.offsetWidth, mh = m.offsetHeight;
  let x = opts.align === "right" ? r.right - mw : r.left;
  let y = opts.below ? r.bottom + 6 : r.top - mh - 6;
  x = Math.max(8, Math.min(x, innerWidth - mw - 8));
  y = Math.max(8, Math.min(y, innerHeight - mh - 8));
  m.style.left = x + "px"; m.style.top = y + "px";
}
function menuItem(act, ic, label, extra = "", cls = "") {
  return `<button class="menu-item ${cls}" data-act="${act}" ${extra}>${icon(ic, 17)}<span>${label}</span></button>`;
}

function openSearch() {
  closeMenu(); closeCite();
  $("#searchOverlay").hidden = false;
  const inp = $("#searchInput");
  inp.value = "";
  renderSearch("");
  setTimeout(() => inp.focus(), 30);
}
function renderSearch(q) {
  const s = q.trim().toLowerCase();
  const convs = CONVS.filter(c => !s || c.title.toLowerCase().includes(s));
  const kbs = KBS.filter(k => !s || k.name.toLowerCase().includes(s));
  let html = "";
  if (convs.length) {
    html += `<div class="ms-label">最近聊天</div>` + convs.map(c => `
      <button class="ms-row" data-act="open-conv" data-id="${c.id}">
        ${icon("bubble", 17)}<span class="row-title">${esc(c.title)}</span>${c.tag ? `<span class="row-tag">${esc(c.tag)}</span>` : ""}
      </button>`).join("");
  }
  if (kbs.length) {
    html += `<div class="ms-label">知识库</div>` + kbs.slice(0, 6).map(k => `
      <button class="ms-row" data-act="search-kb" data-name="${esc(k.name)}">
        ${icon("db", 17)}<span class="row-title">${esc(k.name)}</span><span class="row-tag">${k.docs} 篇 · 设为范围</span>
      </button>`).join("");
  }
  $("#searchResults").innerHTML = html || `<div class="ms-none">没有与「${esc(q)}」匹配的对话或知识库</div>`;
}

const SET_SECTIONS = [
  ["gear", "常规"], ["bell", "通知"], ["sliders", "个性化"], ["apps", "插件"], ["mic", "语音"],
  ["link", "账单"], ["list", "使用情况"], ["grid", "分析"], ["db", "数据管理"], ["file", "存储空间"], ["help", "安全防护"],
];
function openSettings() {
  closeMenu(); closeCite();
  $("#settingsOverlay").hidden = false;
  renderSettingsNav();
  renderSettingsMain();
}
function renderSettingsNav() {
  $("#setNav").innerHTML = SET_SECTIONS.map(([ic, name]) => `
    <button class="set-nav-item ${state.settingsSection === name ? "on" : ""}" data-act="set-sec" data-name="${name}">
      ${icon(ic, 17)}<span>${name}</span>
    </button>`).join("");
}
function renderSettingsMain() {
  const main = $("#setMain");
  if (state.settingsSection !== "常规") {
    main.innerHTML = `<div class="set-h">${esc(state.settingsSection)}</div><div class="set-ph">「${esc(state.settingsSection)}」在演示中为占位，不承载真实设置。</div>`;
    return;
  }
  main.innerHTML = `
    <div class="set-h">常规</div>
    <div class="set-row"><div><div class="set-lab">外观</div></div>
      <button class="select-pill" data-act="opt-theme"><span id="themeLabel">${{ dark: "深色", light: "浅色", system: "跟随系统" }[state.theme]}</span> ${icon("chevD", 15)}</button></div>
    <div class="set-row"><div><div class="set-lab">对比度</div></div>
      <button class="select-pill">系统 ${icon("chevD", 15)}</button></div>
    <div class="set-row"><div><div class="set-lab">强调色</div></div>
      <button class="select-pill" data-act="opt-accent"><span class="dot ${state.accent === "default" ? "" : "c-" + state.accent}"></span><span id="accentLabel">${{ default: "默认", blue: "蓝色", green: "绿色", purple: "紫色" }[state.accent]}</span> ${icon("chevD", 15)}</button></div>
    <div class="set-row"><div><div class="set-lab">语言</div></div>
      <button class="select-pill">简体中文 ${icon("chevD", 15)}</button></div>
    <div class="set-row"><div><div class="set-lab">更强智能</div><div class="set-desc">当你提出复杂问题时，可以自动使用更高智能级别设置。</div></div>
      <button class="switch on" data-act="toggle-sw"></button></div>
    <div class="set-row"><div><div class="set-lab">启用听写</div><div class="set-desc">在聊天输入过程中使用语音输入。</div></div>
      <button class="switch on" data-act="toggle-sw"></button></div>`;
}

/* 主题 */
function applyTheme() {
  let t = state.theme;
  if (t === "system") t = matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  document.documentElement.dataset.theme = t;
  localStorage.setItem("ragdemo-theme", state.theme);
}
function applyAccent() {
  document.documentElement.dataset.accent = state.accent;
  localStorage.setItem("ragdemo-accent", state.accent);
}

/* 引用浮层 */
function openCite(anchor, turnIdx, citeIdx) {
  const conv = CONVS.find(c => c.id === state.convId);
  const turn = conv && conv.turns[+turnIdx];
  const c = turn && turn.cites && turn.cites[+citeIdx];
  if (!c) return;
  const pop = $("#citePop");
  pop.innerHTML = `
    <div class="cp-head">
      <span class="cp-icon">${icon("doc", 17)}</span>
      <div class="cp-title"><div class="cp-file">${esc(c.file)}</div><div class="cp-kb">来自知识库「${esc(c.kb)}」</div></div>
      <button class="icon-btn cp-close" data-act="close-cite">${icon("x", 16)}</button>
    </div>
    <div class="cp-body">${esc(c.snippet)}</div>
    <div class="cp-foot">
      <span class="cp-score">相似度 <b>${esc(c.score)}</b><span class="rc-bar"><i style="width:${Math.round(parseFloat(c.score) * 100)}%"></i></span> chunk #${c.chunk}</span>
      <button class="cp-open" data-act="cite-open-src">打开原文</button>
    </div>`;
  pop.hidden = false;
  const r = anchor.getBoundingClientRect();
  const pw = pop.offsetWidth, ph = pop.offsetHeight;
  let x = Math.max(8, Math.min(r.left - 40, innerWidth - pw - 8));
  let y = r.top - ph - 10;
  if (y < 8) y = Math.min(r.bottom + 10, innerHeight - ph - 8);
  pop.style.left = x + "px"; pop.style.top = y + "px";
}

/* ---------------- 交互分发 ---------------- */
document.addEventListener("click", e => {
  const target = e.target.closest("[data-act]");
  /* 点击空白处关闭浮层 */
  if (!e.target.closest(".menu")) closeMenu();
  if (!e.target.closest(".cite-pop") && !e.target.closest("[data-act='cite-open']")) closeCite();

  if (!target) return;
  const act = target.dataset.act;

  switch (act) {
    case "toggle-sb": {
      if (innerWidth <= 900) document.body.classList.toggle("sb-overlay");
      else document.body.classList.toggle("sb-collapsed");
      break;
    }
    case "new-chat": {
      stopStream();
      state.convId = null; state.kbScope = null; renderScope();
      $("#msgCol").innerHTML = "";
      show("home");
      $("#composerInput").focus();
      break;
    }
    case "open-kb": show("kb"); break;
    case "nav-ph": toast(`「${target.dataset.ph}」为演示占位`); break;
    case "open-conv": {
      closeMenu();
      $("#searchOverlay").hidden = true;
      stopStream();
      openConv(target.dataset.id);
      break;
    }
    case "del-conv": {
      e.stopPropagation();
      const id = target.dataset.id;
      const i = CONVS.findIndex(c => c.id === id);
      if (i > -1) CONVS.splice(i, 1);
      if (state.convId === id) { state.convId = null; show("home"); }
      renderSidebar();
      toast("已删除对话（演示数据）");
      break;
    }
    case "user-menu": {
      openMenu(target, `
        <button class="pu-head">${'<span class="avatar">A</span>'}<div class="profile-meta"><span class="profile-name">admin</span><span class="profile-sub">个人账户</span></div><span class="pu-arrow">${icon("chevR", 16)}</span></button>
        <div class="menu-div"></div>
        ${menuItem("nav-ph", "sliders", "个性化", 'data-ph="个性化"')}
        ${menuItem("nav-ph", "person", "个人资料", 'data-ph="个人资料"')}
        ${menuItem("open-settings", "gear", "设置")}
        <div class="menu-div"></div>
        ${menuItem("nav-ph", "help", "帮助", 'data-ph="帮助"', "")}
        ${menuItem("nav-ph", "logout", "退出登录", 'data-ph="退出登录"', "danger")}
      `, { align: "left" });
      break;
    }
    case "open-settings": closeMenu(); openSettings(); break;
    case "set-sec": state.settingsSection = target.dataset.name; renderSettingsNav(); renderSettingsMain(); break;
    case "close-modal": case "close-cite": target.closest(".overlay") && (target.closest(".overlay").hidden = true); closeCite(); break;
    case "toggle-sw": target.classList.toggle("on"); break;
    case "opt-theme": {
      openMenu(target, `
        <div class="menu-label">外观</div>
        ${["dark:深色", "light:浅色", "system:跟随系统"].map(o => { const [v, n] = o.split(":"); return `<button class="menu-item" data-act="set-theme" data-v="${v}"><span>${n}</span>${state.theme === v ? `<span class="check">${icon("check", 15)}</span>` : ""}</button>`; }).join("")}
      `, { below: true, align: "right" });
      break;
    }
    case "set-theme": state.theme = target.dataset.v; applyTheme(); renderSettingsMain(); closeMenu(); break;
    case "opt-accent": {
      openMenu(target, `
        <div class="menu-label">强调色</div>
        ${["default:默认", "blue:蓝色", "green:绿色", "purple:紫色"].map(o => { const [v, n] = o.split(":"); return `<button class="menu-item" data-act="set-accent" data-v="${v}"><span class="dot ${v === "default" ? "" : "c-" + v}" style="width:12px;height:12px;border-radius:50%;background:${{ default: "var(--text-3)", blue: "#3d7ffb", green: "#10a37f", purple: "#8e6ff0" }[v]}"></span><span>${n}</span>${state.accent === v ? `<span class="check">${icon("check", 15)}</span>` : ""}</button>`; }).join("")}
      `, { below: true, align: "right" });
      break;
    }
    case "set-accent": state.accent = target.dataset.v; applyAccent(); renderSettingsMain(); closeMenu(); break;
    case "open-search": openSearch(); break;
    case "kb-new-menu": {
      openMenu(target, `
        ${menuItem("nav-ph", "file", "上传文档", 'data-ph="上传文档"')}
        ${menuItem("nav-ph", "db", "新建知识库", 'data-ph="新建知识库"')}
        ${menuItem("new-chat", "newchat", "新聊天")}
      `, { below: true, align: "right" });
      break;
    }
    case "kb-chip": {
      state.kbFilter = target.dataset.filter;
      $$("#kbChips .chip").forEach(c => c.classList.toggle("on", c.dataset.filter === state.kbFilter));
      renderKbTable();
      break;
    }
    case "kb-filter": toast("筛选为演示占位"); break;
    case "kb-mode": state.kbMode = target.dataset.mode; renderKbTable(); break;
    case "kb-row": {
      state.kbScope = target.dataset.name;
      toast(`已把「${state.kbScope}」设为提问范围`);
      state.convId = null; $("#msgCol").innerHTML = "";
      renderScope(); show("home");
      $("#composerInput").focus();
      break;
    }
    case "doc-row": toast(`「${target.dataset.name}」文档详情为演示占位`); break;
    case "plus-menu": {
      openMenu(target, `
        ${menuItem("nav-ph", "file", "上传文档", 'data-ph="上传文档"')}
        ${menuItem("nav-ph", "apps", "添加照片", 'data-ph="添加照片"')}
        <div class="menu-div"></div>
        <div class="menu-label">选择知识库范围</div>
        <div class="menu-scroll">${KBS.map(k => `<button class="menu-item" data-act="pick-kb" data-name="${esc(k.name)}">${icon("db", 16)}<span>${esc(k.name)}</span>${state.kbScope === k.name ? `<span class="check">${icon("check", 15)}</span>` : ""}</button>`).join("")}</div>
      `, { below: true, align: "left" });
      break;
    }
    case "pick-kb": state.kbScope = target.dataset.name; renderScope(); closeMenu(); break;
    case "clear-kb": state.kbScope = null; renderScope(); break;
    case "reason-menu": {
      openMenu(target, `
        <div class="menu-label">检索深度</div>
        ${["低", "中", "高"].map(v => `<button class="menu-item" data-act="set-reason" data-v="${v}">${icon("spark", 16)}<span>${v}</span>${state.reason === v ? `<span class="check">${icon("check", 15)}</span>` : ""}</button>`).join("")}
      `, { below: true, align: "right" });
      break;
    }
    case "set-reason": state.reason = target.dataset.v; $("#reasonLabel").textContent = state.reason; closeMenu(); toast(`检索深度：${state.reason}`); break;
    case "mic": toast("听写为演示占位"); break;
    case "voice-or-send": {
      if ($("#composerInput").value.trim()) sendCurrent();
      else toast("语音模式为演示占位");
      break;
    }
    case "stop": stopStream(); break;
    case "share-menu": {
      openMenu(target, `
        ${menuItem("copy-link", "link", "复制链接")}
        ${menuItem("export-md", "download", "导出为 Markdown")}
      `, { below: true, align: "right" });
      break;
    }
    case "more-menu": {
      openMenu(target, `
        ${menuItem("nav-ph", "pencil", "重命名", 'data-ph="重命名"')}
        ${menuItem("copy-link", "link", "分享")}
        <div class="menu-div"></div>
        ${menuItem("del-open-conv", "trash", "删除", "", "danger")}
      `, { below: true, align: "right" });
      break;
    }
    case "copy-link": {
      closeMenu();
      copyText(location.href + "#chat/" + (state.convId || "new"));
      break;
    }
    case "export-md": {
      closeMenu();
      const conv = CONVS.find(c => c.id === state.convId);
      if (!conv) { toast("当前没有可导出的对话"); break; }
      const md = conv.turns.map(t => t.role === "user" ? `## 🧑 提问\n\n${t.text}\n` : `## 🤖 回答\n\n${t.answer}\n`).join("\n---\n\n");
      const blob = new Blob([`# ${conv.title}\n\n${md}`], { type: "text/markdown" });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = `${conv.title}.md`;
      a.click();
      URL.revokeObjectURL(a.href);
      toast("已导出 Markdown");
      break;
    }
    case "del-open-conv": {
      closeMenu();
      const i = CONVS.findIndex(c => c.id === state.convId);
      if (i > -1) CONVS.splice(i, 1);
      state.convId = null;
      $("#msgCol").innerHTML = "";
      show("home");
      toast("已删除对话（演示数据）");
      break;
    }
    case "suggest": {
      const s = SUGGESTIONS[state.mode][state.suggestIdx % SUGGESTIONS[state.mode].length];
      startAsk(s.text);
      break;
    }
    case "scroll-bottom": $("#msgScroll").scrollTop = $("#msgScroll").scrollHeight; break;
    case "retrieval-toggle": target.closest(".retrieval").classList.toggle("open"); break;
    case "cite-open": openCite(target, target.dataset.turn, target.dataset.cite); break;
    case "cite-open-src": toast("原文预览为演示占位"); break;
    case "copy-code": copyText(decodeURIComponent(target.dataset.code), "代码已复制"); break;
    case "msg-copy": {
      const conv = CONVS.find(c => c.id === state.convId);
      const t = conv && conv.turns[+target.dataset.turn];
      if (t) copyText(t.answer || t.text);
      break;
    }
    case "msg-like": case "msg-dislike": {
      target.classList.toggle("picked");
      toast(act === "msg-like" ? "已记录好评（演示数据）" : "已记录差评（演示数据）");
      break;
    }
    case "msg-regen": {
      const conv = CONVS.find(c => c.id === state.convId);
      if (conv) regenerate(conv);
      break;
    }
    case "msg-share": copyText(location.href + "#chat/" + state.convId); break;
    case "search-kb": {
      $("#searchOverlay").hidden = true;
      state.kbScope = target.dataset.name;
      state.convId = null; $("#msgCol").innerHTML = "";
      renderScope(); show("home");
      $("#composerInput").focus();
      toast(`已把「${state.kbScope}」设为提问范围`);
      break;
    }
  }
});

$("#searchInput").addEventListener("input", e => renderSearch(e.target.value));
$("#searchInput").addEventListener("keydown", e => {
  if (e.key === "Enter") {
    const first = $("#searchResults .ms-row");
    if (first) first.click();
  }
});
$("#kbSearchInput").addEventListener("input", e => { state.kbQuery = e.target.value; renderKbTable(); });

$$(".overlay").forEach(ov => ov.addEventListener("click", e => { if (e.target === ov) ov.hidden = true; }));
$$("#segMode .seg").forEach(b => b.addEventListener("click", () => setMode(b.dataset.mode)));

document.addEventListener("keydown", e => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") { e.preventDefault(); openSearch(); }
  if (e.key === "Escape") {
    if (!$("#menu").hidden) { closeMenu(); return; }
    if (!$("#citePop").hidden) { closeCite(); return; }
    $$(".overlay").forEach(ov => ov.hidden = true);
  }
});

$("#msgScroll").addEventListener("scroll", () => {
  const sc = $("#msgScroll");
  $("#scrollBottom").hidden = sc.scrollHeight - sc.scrollTop - sc.clientHeight < 200;
});

/* ---------------- 启动 ---------------- */
function initStaticIcons() {
  const map = [[".sb-head [data-act='open-search']", "search"], [".sb-head [data-act='toggle-sb']", "panel"], ["#scrollBottom", "arrowDown"]];
  map.forEach(([sel, ic]) => { const el = $(sel); if (el) el.innerHTML = icon(ic); });
  $("#profileBtn .profile-right").innerHTML = icon("help", 18);
  /* 水合所有 data-icon 占位（含模板克隆出来的节点） */
  $$("[data-icon]").forEach(el => {
    el.innerHTML = icon(el.dataset.icon, +(el.dataset.size || 18));
  });
}
function init() {
  state.theme = localStorage.getItem("ragdemo-theme") || "dark";
  state.accent = localStorage.getItem("ragdemo-accent") || "default";
  applyTheme(); applyAccent();
  initComposer();
  initStaticIcons();
  renderScope();
  renderSuggest();
  show("home");
  if (innerWidth <= 900) document.body.classList.remove("sb-overlay");
}
init();

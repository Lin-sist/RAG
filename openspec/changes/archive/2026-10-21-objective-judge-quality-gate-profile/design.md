# C20零外调设计

使用原生rag-quality-gate-profile-v1，profileId=c20-objective-judge-draft、v1-draft、DRAFT/PENDING_REFERENCE_EVIDENCE。full150、generation/citation、topK5/minScore0.3/rerank=true；仅objective required。target/tolerance全null，overall slice最低分母1仅非空检查，不是质量阈值。

新增keyword、citation来源/片段、no-answer准确率、unsupported citation和no-answer citation violation映射，复用C9a现有计数；claim保持现有算法。keyword/citation来源/claim分母来自应答样本，citation片段及unsupported来自全部成功ask，no-answer只非应答样本；客观通道或任一样本ask/retrieval失败时不计算可比值。布尔/负数/不可能计数、非有限值须抑制观测，不缩小分母。

review仅接受本DRAFT布局，不负责激活。先复用C10 dataset/selection/run身份检查，身份失败不出观测；objective完整性与严格错误策略分别显示。C18原件hash匹配后可本地展示测量，retry31仍明确retry_count_exceeded，不变errorPolicy0。C19校准集不能混作150样本judge；judge仅COMPLETE/ELIGIBLE且值完整时可辅助展示，否则SKIPPED/N/A，不阻塞客观通道。

输出仅固定枚举、受限ID、hash、计数、观测与状态；无raw问题/答案/context/理由/凭据/绝对路径。CLI只读本地、输出受repo边界约束、exclusive create不覆盖输入/旧输出；异常固定code不打印消息。accepted C18 hash不等于当前C18源码审计重跑通过。

## 决策记录

### D1 阶段停放
- **面临的选择**：强行补完C19才开工；把partial标完成；按用户条件指令停放C19并转C20零外调切片。
- **选了哪个 + 为什么**：停放未完C19、唯一活动指针转C20，不让provider问题阻塞独立工程；本轮用户已直接授权。
- **放弃的代价**：继续补测可能反复失败并消耗额度；伪称完成破坏校准和审计。

### D2 阈值与激活
- **面临的选择**：从单轮观测自动选阈值；复制C17阈值；原生DRAFT空阈值。
- **选了哪个 + 为什么**：DRAFT空阈值，数值/重复参考/ACTIVE后续独立批准；本切片仅工程实现。
- **放弃的代价**：单轮自动阈值会拟合；复制检索阈值混淆指标与模型身份。

### D3 计算与缺测
- **面临的选择**：新review复制公式；共享C10增加既有计数映射；从partial judge取成功均值。
- **选了哪个 + 为什么**：共享计算复用，optional judge先检查通道完整性，partial/skipped只SKIPPED，objective独立。
- **放弃的代价**：复制公式易漂移；成功子集均值会冒充完整judge证据。

### D4 历史重试
- **面临的选择**：为历史31次恢复重试放宽新策略；隐藏所有观测；展示完整观测并单列严格策略阻塞。
- **选了哪个 + 为什么**：单列观测与策略，保留历史证据价值但不暗中批准新门禁。
- **放弃的代价**：放宽缺新批准；全部隐藏导致有效证据不能用于审阅。

### D5 刻意不做
- **面临的选择**：同时启用objective/judge硬门禁；仅DRAFT与辅助审阅。
- **选了哪个 + 为什么**：仅DRAFT与review，judge校准、objective重复参考及数值审批尚未完成。
- **放弃的代价**：立即硬门禁会在证据不足时产生虚假放行结论。

## 下一切片：参考证据编译与阈值审阅包

输入为原生DRAFT profile及0至3组普通评测details/metadata。三轮离线contract要求repeat index=1/2/3、total=3，distinct details/metadata hash与generatedAt，metadata与details.runMetadata完全相同，干净Git来源、dataset/selection/run/claim/judge/config/KB/provider运行身份一致。仅输出身份hash，不输出KB信息或原文。当前历史C18是1×150且retry31，允许作为不合格候选列入缺口诊断，不能伪装三轮参考。

questionSet必须匹配实际版本化release的sha256/bytes及仓库内解析路径，绝对/相对及Windows路径写法规范化后才入身份hash。执行manifest、timeout/retry等策略属于跨轮身份；C18的metadata.c18Execution策略由embedded runMetadata绑定，details.c18Execution是账本，不能把两者直接当同一字段比较。FOR_REVIEW不表示provider实际执行已经独立核验。

每轮通过原C20 review检查全部7项objective观测与strict errorPolicy，judge仍辅助。输入畸形→INVALID；重复或身份漂移→NOT_COMPARABLE；缺轮/缺测/错误策略不符→INCOMPLETE。任何非完整可比三轮不得输出质量分布；每项保留3个repeat槽及分母，缺值为null，不把一次运行复用三次。

完整三轮仅生成FOR_REVIEW分布及空target/tolerance审阅表；activationEligible恒false，gateStatus恒NOT_EVALUABLE。CLI exit4表示待审批（即使分布已完整），非法输入exit2；不创建ACTIVE profile/reference，不进行provider身份实时核验或实际调用。输出exclusive且禁止覆盖任何输入，路径受repo约束，异常固定code，无绝对路径或raw字段。

### D6 参考输入数量
- **面临的选择**：直接用C18单轮；固定三轮独立候选；自动增加轮数直到看起来稳定。
- **选了哪个 + 为什么**：固定三轮作为离线编译contract，保留各值和波动，与既有三轮审阅方式一致；真实采集的执行包/预算仍后续批准，三轮不冒充统计置信保证。
- **放弃的代价**：单轮无法展示重复波动；自动加轮可能无限消耗额度并产生选择偏差。

### D7 参考完整性与审批
- **面临的选择**：从完整子集先算中位数并自动填阈值；缺测则隐藏来源；保留槽位/缺口且完整后只输出空阈值审阅表。
- **选了哪个 + 为什么**：保留缺口，完整才出分布，target/tolerance仍null；测量编译不能代替人的门禁审批。
- **放弃的代价**：子集分布和自动阈值会掩盖缺测并拟合数据；隐藏来源让用户无法判断还缺什么。

### D8 DRAFT工程收口与证据债务（2026-10-21用户确认）
- **面临的选择**：等待真实三轮参考/数值批准/激活后才归档；按已验证DRAFT契约及离线工具链归档，将参考与激活目标分列P1证据债务。
- **选了哪个 + 为什么**：按用户明确指令接受DRAFT工程范围，W1-E2保留真实参考、W1-E3保留阈值审批及激活；归档完成工程记账，不替代质量判定。
- **放弃的代价**：维持原前置会让已实现工具链长期占据活动指针；缩窄后必须持续显示DRAFT/NOT_EVALUABLE，未来激活仍须新版本、充分证据与独立批准，不能从archive推导PASS。

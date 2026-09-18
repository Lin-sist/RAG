# C18 r2 Canary 失败证据

C18尚未完成。原模型的NVIDIA免费端点已退役；[官方状态](https://build.nvidia.com/qwen/qwen3.5-122b-a10b)显示Deprecated。[替代模型官方页面](https://build.nvidia.com/nvidia/nemotron-3-super-120b-a12b)显示Available，合成请求返回200/OK。目录中另一候选Mistral合成404，未采用；两次诊断各retry=0，不混入正式样本。

C18新manifestId=rag-eval-dev-v2-generation-objective-nemotron3-super-r2，生成模型nvidia/nemotron-3-super-120b-a12b，temperature0.2、2048 tokens、120s、retry0；embedding仍nemotron-3-embed-1b。预算34/1492、5/150、既定开发载荷及REST副作用不变。仅通过后端CLI覆盖生成模型，未改.env.local或生产默认。

r2在HEAD ad39548发起canary，preflight READY，debug retrieval=1成功，ask=1触发生成HTTP503 ServiceUnavailable；无自动重试，剩余4条/full=0。应用HTTP可返回200错误对象，不能当作provider成功。compiler generationCalls=null、unknownSampleCount=1，实际应用日志记录一次生成尝试及503。

同时，其他任务在运行期间修改AGENTS.md和共享日志，raw metadata git.clean=false。因此compiler结果NOT_COMPARABLE，包含git_provenance_not_clean及失败/缺样本原因；此前Windows路径、judge字段、token误脱敏的原因已消失。原始文件保留在ignored r2目录，未改写或拼接。

269 Python tests通过，scope无题目/fixture/C17基线改动。Java与Python均未使用代理，请求格式与OpenAI兼容入口一致；当前证据不足以判断503的provider内部成因，不能承诺重复调用会恢复。后续须在干净隔离checkout以新身份执行完整canary；本次不推进full或归档。

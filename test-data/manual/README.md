# 手工上传与联调测试样本

> 2026-10-26 整理。下列六个文件按字节原样迁入，未修改正文，也未加入正式评测集。

| 样本 | 原位置 | 用途 |
| --- | --- | --- |
| [test-md.md](test-md.md) | 仓库根目录 test-md.md | Markdown 上传样本 |
| [test-large.md](test-large.md) | 仓库根目录 test-large.md | 较长 Markdown 手工样本；文件名不代表上传限额断言 |
| [test-huge.md](test-huge.md) | 仓库根目录 test-huge.md | 更长 Markdown 手工样本；不代表已完成大文件边界测试 |
| [test-small.txt](test-small.txt) | 仓库根目录 test-small.txt | 短文本上传样本 |
| [test-upload.txt](test-upload.txt) | 仓库根目录 test-upload.txt | 普通文本上传样本 |
| [frontend-r3b-smoke.md](frontend-r3b-smoke.md) | tmp/r3b-preflight-20260926/frontend-r3b-smoke.md | 历史 R3b/R5 专用合成联调资料，作为来源保留 |

原型源码中的 test-md.md、test-upload.txt 是静态演示文件名，不读取仓库根目录文件，因此保持原显示名称。历史操作记录中的旧路径按当时语境理解。

正式评测脚本仍绑定上级目录的三份原始语料：[Spring Boot](../springboot-basics.md)、[Java 面试](../java-interview-guide.md)、[RAG 技术](../rag-technology-guide.md)。不要将本目录样本混入版本化数据集、修改 manifest 或据此声称已有新的真实测试结果。

运行上传或问答会写入业务数据，并可能触发 embedding/generation；本次只是整理本地文件，没有执行这些操作。

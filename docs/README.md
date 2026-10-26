# 项目文档与目录导航

> 整理日期：2026-10-26。日常从本页进入，只有需要证据或历史原因时再展开相应目录。

## 日常阅读入口

| 想了解什么 | 入口 |
| --- | --- |
| 如何启动和使用 | [项目 README](../README.md)、[API 教程](开发文档/API-调用教程.md)、[Linux 功能验收清单](../USAGE_GUIDE.md) |
| 当前定位与能力边界 | [项目上下文](../openspec/project.md) |
| 当前任务和待补证据 | [活动指针](../.ai/ACTIVE_TASK.md)、[技术债](roadmap/technical-debt.md) |
| 系统怎样工作 | [架构说明](architecture/overview.md)、[前端说明](开发文档/前端文档/frontend-current-state.md) |
| 前端阶段接受了什么 | [收口索引](roadmap/frontend-demo-closeout-2026-10-18.md) |
| 怎样读评测和历史结果 | [评测指南](eval/RAG_EVAL_GUIDE.md)、[优化证据导航](optimization/README.md) |
| 如何与 Agent 协作 | [规则](../AGENTS.md)、[工作流手册](workflow/vibecoding-playbook.md)、[近期日志](../.ai/AGENT_LOG.md) |
| 学习与项目掌控 | [学习协作讨论稿](rag-learning-and-ownership-roadmap.md)；[旧 8 周计划](learning/LEARNING_ROADMAP.md)仅作历史参考，讨论稿也不等于已验证安排 |
| 上传测试资料在哪 | [手工测试样本](../test-data/manual/README.md)；原有三份正式评测语料仍在 test-data 根目录 |

## 哪些目录需要保留

| 目录 | 保留原因 |
| --- | --- |
| rag-admin / rag-auth / rag-common / rag-core / rag-document / rag-frontend | 正式前后端源码；本次不为减少目录数改动 Maven 模块和前端路径 |
| scripts / deploy | 本地验证、评测及部署参考配置 |
| docs / openspec / .ai / .github | 现行说明、长期契约、历史接受证据、协作记录与仓库配置 |
| test-data | 评测脚本绑定的三份原始语料及独立手工样本；手工样本不自动进入正式评测集 |
| prototype | 历史原型与设计参考，不是第二套正式应用；仍被归档和验收引用 |
| data/index-inputs | 当前本地持久化索引输入，删除会破坏已有文档的恢复依据 |
| tmp | 名称虽是临时目录，但含评测原件和失败记录；迁出的 R3b 文本见手工样本说明，不据目录名批量清除 |

## 哪些可以隐藏或按条件清理

2026-10-26 用户已删除空的 .tmp 与 .codex，后续工具可能重建。本轮清理 .vscode 中原有的扩展推荐文件，移除此前新增的 RAG.code-workspace，只保留 Antigravity IDE 直接打开项目所需的目录隐藏设置。

| 目录或文件 | 日常处理建议 |
| --- | --- |
| 各模块 target、rag-frontend/dist | 可再生成，但 target 中可能有本次验证报告；保留需要的证据并停止相关任务后，再按具体目标清理 |
| rag-frontend/node_modules | 可按 lockfile 重装；删除后需要重新安装依赖，日常隐藏更方便 |
| .m2repo | 本地 Maven 仓库；可再下载的依赖与本地安装产物并存，不能一概当垃圾，优先隐藏 |
| logs、根目录 *.log | 先区分当前使用的日志与历史排障证据，后续再按日期和用途清理 |
| .idea | 本机 IDE 设置，保留并隐藏，避免丢失个人配置 |
| .vscode/settings.json | Antigravity IDE 读取的项目目录隐藏设置；只跟踪这一份配置，目录自身也隐藏 |
| .agents | 本地学习和协作 Skill，保留；不因 Git 忽略就删除 |
| .git | 仓库历史与未推送提交的存储，必须保留 |

Antigravity IDE 直接打开本项目文件夹时，读取[项目级 files.exclude](../.vscode/settings.json)，在资源管理器中隐藏缓存、生成物、运行数据和本地工具配置；不需要单独打开工作区文件。源码模块、docs、openspec、.ai、scripts、deploy、prototype 与 test-data 保持可见。隐藏不删除磁盘文件，也不改用户全局设置；恢复显示可将对应项设为 false。IDE 的排除设置也可能影响界面搜索，查历史原件时可以临时取消排除或在终端按路径查找。Windows 文件资源管理器不会采用此 IDE 设置。

## 定期维护方式

每个阶段收口或每月检查一次：确认活动指针与日志行数，找出重复维护的状态和旧入口，再检查测试样本与运行证据的去向。清理文件后运行 DocAndScope，并检查迁移前后的链接与文件内容；不为了清理而重写已接受归档或原始评测记录。此建议不创建定时任务，也不授权自动删除。

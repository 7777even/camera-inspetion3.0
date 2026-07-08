# Phase 5 验收上线 — 设计概要

详细设计文档：`docs/superpowers/specs/2026-07-08-phase5-acceptance-deployment-design.md`
实施计划：`docs/superpowers/plans/2026-07-08-phase5-implementation-plan.md`

## 架构

三容器 Docker 编排（MySQL 5.7 + enviro-brain + queqiao），生产配置通过 `.env` 和环境变量注入。

**容器职责：**
- `mysql:5.7` — 数据库（可替换为外部实例）
- `enviro-brain:8080` — 巡检核心，Java 17，healthcheck
- `queqiao:8081` — MCP 服务端 + 同步层，Java 17，healthcheck

## 关键设计决策
- Multi-stage Dockerfile（Maven 构建 → JRE 运行）
- 敏感信息全部通过 `.env` 注入，`application-prod.yml` 仅含 `${变量}` 占位符
- 端到端冒烟通过 MCP Python SDK 调用验证

# 主规格书

> 本变更的完整设计见 `docs/superpowers/specs/2026-07-08-phase5-acceptance-deployment-design.md`

## 交付件

| 交付件 | 路径 | 说明 |
|--------|------|------|
| 生产配置 | `enviro-brain/src/main/resources/application-prod.yml` | 环境变量注入 |
| 生产配置 | `queqiao/src/main/resources/application-prod.yml` | 环境变量注入 |
| Dockerfile | `enviro-brain/Dockerfile` | Multi-stage 构建 |
| Dockerfile | `queqiao/Dockerfile` | Multi-stage 构建 |
| 编排 | `docker-compose.yml` | 三容器 |
| 环境模板 | `.env.template` | 13 项变量 |
| 初始化 SQL | `db/init/01-schema.sql` | 两库 9 表 |
| 冒烟脚本 | `scripts/smoke-test.sh` | 端到端验证 |
| 容错脚本 | `scripts/fault-test.sh` | 宕机测试 |
| 部署指南 | `docs/ops/deployment-guide.md` | |
| 运维手册 | `docs/ops/runbook.md` | |
| 故障排查 | `docs/ops/troubleshooting.md` | |

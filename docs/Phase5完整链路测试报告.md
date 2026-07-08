# Phase 5 验收上线 — 完整链路测试报告

> 测试时间：2026-07-08 11:50 ~ 12:06
> 测试环境：本地开发环境（enviro-brain:8080 + queqiao:8081 + MySQL:3306）
> 触发巡检：id=22, batch_id=manual-2026-07-08-1150, sync_version=27

---

## 1. 测试链路总览

| # | 步骤 | 状态 | 说明 |
|---|------|------|------|
| 1 | 服务健康检查 | PASS | enviro-brain:UP, queqiao:UP, MySQL:OK, MCP SSE:200 |
| 2 | MCP 触发巡检 | PASS* | trigger_inspection 成功，但同步阻塞 ~4min，HTTP 响应需等全量巡检完成 |
| 3 | 巡检执行完成 | PASS | 115 total, 114 online, 1 offline, 0 abnormal |
| 4 | 数据同步 | PASS* | 手动 POST /api/notify/new-data 触发成功；自动回调链路不通 |
| 5 | MCP 查询验证 | PASS | get_inspection_summary / get_camera_status / get_inspection_ledger 均返回数据 |
| 6 | 台账下载 | PASS | download_ledger_docx 返回 docx 文件信息（inspectId=23） |

**总结：6/6 步骤功能通过，其中 2 步有条件通过（PASS*），发现 5 个需修复的问题。**

---

## 2. 各步骤详细结果

### 2.1 服务健康检查

```
enviro-brain  http://localhost:8080/actuator/health  → UP (DB:UP, diskSpace:UP)
queqiao       http://localhost:8081/actuator/health  → UP (DB:UP, diskSpace:UP)
MySQL         127.0.0.1:3306                          → 连接正常
MCP SSE       http://localhost:8081/mcp/sse           → HTTP 200
```

### 2.2 MCP 触发巡检

- **调用方式**：MCP SSE → `trigger_inspection(reason="Phase5 验收上线完整链路测试")`
- **参数**：`reason`（String, required）
- **结果**：巡检记录 id=22 创建成功
- **问题**：trigger 端点（`POST /api/v1/inspections/trigger`）同步执行 `executeInspection()`，HTTP 响应需等全部巡检完成后才返回（~4分钟）。curl 请求 10 分钟后仍未返回（可能 HTTP keep-alive 或事务未提交）。

### 2.3 巡检执行完成

| 指标 | 值 |
|------|------|
| 巡检 ID | 22 |
| batch_id | manual-2026-07-08-1150 |
| 总摄像头数 | 115 |
| 在线 | 114 |
| 离线 | 1（美誉化工危废仓库门口-超时） |
| 异常 | 0 |
| sync_version | 27 |
| 创建时间 | 11:50:51 |
| 完成时间 | 11:54:37（耗时约 4 分钟） |

### 2.4 数据同步

- **正确调用方式**：`POST http://localhost:8081/api/notify/new-data`
  - Headers: `X-API-Key: queqiao-notify-key-2026`, `Content-Type: application/json`
  - Body: `{"syncVersion": 27, "type": "inspection_completed"}`
  - 响应：`{"code":200,"message":"success","data":null}`

- **同步结果验证**：
  - sync_watermark 升级到 version=28
  - synced_inspection_records 新增 id=22, id=23 两条记录
  - synced_camera_results 新增 115 条（sync_version=27）

- **问题**：
  1. enviro-brain 的 `QueqiaoNotifyService.notifyNewData()` 未设置 `X-API-Key` 头，回调请求会被鹊桥拦截器返回 401
  2. `enviro.queqiao.callback-url` 默认为空，回调未启用
  3. smoke-test.sh 使用 `GET /api/notify?syncVersion=xxx`（错误），应为 `POST /api/notify/new-data`

### 2.5 MCP 查询验证

#### 2.5.1 get_inspection_summary

- **参数**：`start`（LocalDate, required）, `end`（LocalDate, required）
- **结果**：
  ```json
  {
    "onlineRate": 0.8817,
    "worstDay": { "id": 14, "onlineCount": 0, "abnormalCount": 114 },
    "frequentOfflineCameras": [
      { "cameraCode": "7d760806...", "offlineCount": 3 },
      { "cameraCode": "5c33c229...", "offlineCount": 2 }
    ]
  }
  ```
- **问题**：smoke-test.sh 使用 `start_date`/`end_date`（错误），应为 `start`/`end`

#### 2.5.2 get_camera_status

- **参数**：`cameraName`（String, required, 可空）, `historyDays`（Integer, required, 默认7）
- **结果**：返回 117 条摄像头记录（115 online + 2 offline），含历史数据

#### 2.5.3 get_inspection_ledger

- **参数**：`date`（String, required, 默认当天）, `status`（String, required, 可空）, `enterprise`（String, required, 可空）
- **结果**：返回最新巡检 id=23（115 total, 115 online, 0 offline），85 条摄像头结果

### 2.6 台账下载

- **工具**：`download_ledger_docx`
- **参数**：`inspectId`（Long, required）
- **结果**：
  ```json
  {
    "ok": true,
    "data": {
      "inspectId": 23,
      "fileName": "台账_2026-07-08.docx",
      "docxPath": "D:\\gkproject\\camera-inspection3.0\\enviro-brain\\.\\ledger\\2026-07-08\\台账_2026-07-08.docx"
    }
  }
  ```
- **问题**：smoke-test.sh 使用 `inspect_id`（错误），应为 `inspectId`（驼峰）

---

## 3. MCP 工具参数名汇总

| 工具名 | 参数名 | 类型 | 必填 | 说明 |
|--------|--------|------|------|------|
| trigger_inspection | reason | String | Yes | 触发原因，可空字符串 |
| get_inspection_summary | start | LocalDate | Yes | 开始日期 yyyy-MM-dd |
| get_inspection_summary | end | LocalDate | Yes | 结束日期 yyyy-MM-dd |
| get_camera_status | cameraName | String | Yes | 摄像头名，不传返回全部 |
| get_camera_status | historyDays | Integer | Yes | 历史天数，默认7 |
| get_inspection_ledger | date | String | Yes | 巡检日期，不传取当天 |
| get_inspection_ledger | status | String | Yes | 状态过滤，可空 |
| get_inspection_ledger | enterprise | String | Yes | 企业名过滤，可空 |
| download_ledger_docx | inspectId | Long | Yes | 巡检记录 ID |

---

## 4. 发现的问题

### P1 级（阻断生产上线）

#### P1-1: trigger 端点同步阻塞

- **文件**：`enviro-brain/src/main/java/com/enviro/brain/controller/InspectionController.java`
- **问题**：`trigger()` 方法同步调用 `executeInspection()`，后者在请求线程内完整执行所有巡检步骤（截图+台账+通知），HTTP 响应需等全部完成才返回（~4分钟）
- **影响**：MCP 调用 `trigger_inspection` 会长时间阻塞，SSE 连接可能超时；Tomcat 线程池可能耗尽
- **修复方向**：将 `executeInspection` 改为 `@Async` 返回 `CompletableFuture<Long>`，或控制器先插入 RUNNING 记录返回 taskId，再异步执行后续逻辑

#### P1-2: QueqiaoNotifyService 回调缺 X-API-Key 头

- **文件**：`enviro-brain/src/main/java/com/enviro/brain/service/QueqiaoNotifyService.java`
- **问题**：`notifyNewData()` 只设置了 `Content-Type` 头，未设置 `X-API-Key` 头
- **影响**：即使配置了 `callback-url`，回调请求也会被鹊桥 `ApiKeyAuthInterceptor` 返回 401
- **修复方向**：注入 `queqiao.notify.api-key` 配置值，在 `HttpHeaders` 中添加 `headers.set("X-API-Key", apiKey)`

### P2 级（影响自动化测试和同步链路）

#### P2-1: smoke-test.sh 参数名错误

- **文件**：`scripts/smoke-test.sh`
- **问题**：
  - `get_inspection_summary` 使用 `start_date`/`end_date`（应为 `start`/`end`）
  - `download_ledger_docx` 使用 `inspect_id`（应为 `inspectId`）
- **影响**：冒烟测试脚本无法正确验证查询和下载功能

#### P2-2: smoke-test.sh notify 端点错误

- **文件**：`scripts/smoke-test.sh`
- **问题**：使用 `curl -sf "http://localhost:8081/api/notify?syncVersion=$LATEST_SYNC"`
- **正确**：`curl -X POST http://localhost:8081/api/notify/new-data -H "X-API-Key: queqiao-notify-key-2026" -H "Content-Type: application/json" -d '{"syncVersion": N, "type": "inspection_completed"}'`

#### P2-3: callback-url 默认为空

- **文件**：`enviro-brain/src/main/resources/application.yml` → `enviro.queqiao.callback-url`
- **问题**：默认为空字符串，回调未启用
- **影响**：巡检完成后无法自动通知鹊桥同步，仅靠 30 分钟定时任务同步
- **修复方向**：配置 `QUEQIAO_CALLBACK_URL=http://localhost:8081/api/notify/new-data`，但需先修复 P1-2

---

## 5. 巡检记录快照（测试时段）

| ID | batch_id | 类型 | total | online | offline | abnormal | sync_version | 完成时间 |
|----|----------|------|-------|--------|---------|----------|--------------|----------|
| 22 | manual-2026-07-08-1150 | 手动 | 115 | 114 | 1 | 0 | 27 | 11:54:37 |
| 23 | manual-2026-07-08-1155 | 手动 | 115 | 115 | 0 | 0 | 28 | 11:58:55 |
| 24 | auto-2026-07-08-1200 | 定时 | 115 | 115 | 0 | 0 | 29 | 12:03:19 |

---

## 6. 结论

**链路功能验证通过**：从健康检查 → MCP 触发巡检 → 巡检执行 → 数据同步 → MCP 查询 → 台账下载，完整链路 6 个步骤均功能可用。

**但存在 2 个 P1 级问题需修复后方可生产上线**：
1. trigger 端点同步阻塞 — 影响并发能力和 MCP 调用稳定性
2. 回调缺 X-API-Key — 阻断自动同步链路

**建议修复优先级**：P1-2（回调认证）→ P1-1（异步化）→ P2-3（配置回调URL）→ P2-1/P2-2（修复测试脚本）

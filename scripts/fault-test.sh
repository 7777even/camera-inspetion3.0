#!/bin/bash
# 容错测试脚本 — 验证环保小脑宕机场景
set -euo pipefail

PYTHON="/c/Users/7even/.workbuddy/binaries/python/versions/3.13.12/python.exe"

echo "=== Phase 5 容错测试 ==="
echo ""

echo "--- 场景 A: 环保小脑宕机 ---"

# 1. 记录宕机前数据可查
echo "1) 宕机前: MCP 查询正常"
BEFORE=$($PYTHON -c "
import asyncio
from mcp.client.sse import sse_client
from mcp.client.session import ClientSession
async def t():
    async with sse_client('http://localhost:8081/mcp/sse') as (r,w):
        async with ClientSession(r,w) as s:
            await s.initialize()
            res = await s.call_tool('get_camera_status', {})
            has_data = len(res.content[0].text) > 50
            print('HAS_DATA' if has_data else 'NO_DATA')
asyncio.run(t())
" 2>/dev/null)
echo "  ${BEFORE}"

# 2. 停止 enviro-brain
echo "2) 停止 enviro-brain..."
docker stop inspection-enviro-brain 2>/dev/null || echo "  (容器未运行，跳过)"
echo "  ✅ 已停止"

# 3. 验证查询类工具仍可用
echo "3) 查询类工具应返回历史数据"
AFTER_STOP=$($PYTHON -c "
import asyncio
from mcp.client.sse import sse_client
from mcp.client.session import ClientSession
async def t():
    async with sse_client('http://localhost:8081/mcp/sse') as (r,w):
        async with ClientSession(r,w) as s:
            await s.initialize()
            res = await s.call_tool('get_inspection_ledger', {})
            has_data = 'data' in res.content[0].text
            print('HAS_DATA' if has_data else 'NO_DATA')
asyncio.run(t())
" 2>/dev/null)
echo "  get_inspection_ledger: ${AFTER_STOP}"

# 4. 验证操作类工具返回友好错误
echo "4) trigger_inspection 应返回友好错误"
ERROR_MSG=$($PYTHON -c "
import asyncio
from mcp.client.sse import sse_client
from mcp.client.session import ClientSession
async def t():
    async with sse_client('http://localhost:8081/mcp/sse') as (r,w):
        async with ClientSession(r,w) as s:
            await s.initialize()
            res = await s.call_tool('trigger_inspection', {'reason': '容错测试'})
            print(res.content[0].text[:100])
asyncio.run(t())
" 2>/dev/null)
echo "  返回: ${ERROR_MSG}"

# 5. 恢复 enviro-brain
echo "5) 恢复 enviro-brain..."
docker start inspection-enviro-brain 2>/dev/null || echo "  (未使用容器部署，跳过)"
echo "  ✅ 完成"

echo ""
echo "=== 容错测试完成 ==="

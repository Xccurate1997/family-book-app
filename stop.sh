#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PID_FILE="$SCRIPT_DIR/.backend.pid"
FRONTEND_PID_FILE="$SCRIPT_DIR/.frontend.pid"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}       家庭记账本 - 停止中              ${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

stop_by_pid_file() {
    local name=$1
    local pid_file=$2

    if [ -f "$pid_file" ]; then
        local pid
        pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            # 先终止子进程（mvn fork 出的 JVM）
            pkill -TERM -P "$pid" 2>/dev/null || true
            kill -TERM "$pid" 2>/dev/null || true
            # 等待最多 5 秒
            for i in $(seq 1 5); do
                kill -0 "$pid" 2>/dev/null || break
                sleep 1
            done
            # 若仍存活则强制终止
            pkill -KILL -P "$pid" 2>/dev/null || true
            kill -KILL "$pid" 2>/dev/null || true
        fi
        rm -f "$pid_file"
    fi
}

# ── 停止后端 ──────────────────────────────────────────
echo -e "${CYAN}[1/2] 停止后端...${NC}"
stop_by_pid_file "backend" "$BACKEND_PID_FILE"
# 安全兜底：确保 8080 端口释放
lsof -ti:8080 | xargs kill -TERM 2>/dev/null || true
sleep 1
lsof -ti:8080 | xargs kill -KILL 2>/dev/null || true
echo -e "  ${GREEN}✓ 后端已停止${NC}"

# ── 停止前端 ──────────────────────────────────────────
echo -e "${CYAN}[2/2] 停止前端...${NC}"
stop_by_pid_file "frontend" "$FRONTEND_PID_FILE"
# 安全兜底：确保 5173 端口释放
lsof -ti:5173 | xargs kill -TERM 2>/dev/null || true
sleep 1
lsof -ti:5173 | xargs kill -KILL 2>/dev/null || true
echo -e "  ${GREEN}✓ 前端已停止${NC}"

echo ""
echo -e "${GREEN}${BOLD}✅ 已全部停止${NC}"
echo ""

#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PID_FILE="$SCRIPT_DIR/.backend.pid"
FRONTEND_PID_FILE="$SCRIPT_DIR/.frontend.pid"
BACKEND_LOG="/tmp/bookapp-backend.log"
FRONTEND_LOG="/tmp/bookapp-frontend.log"
JAVA_HOME_17="/Users/wzl/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}       家庭记账本 - 启动中              ${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

# Check Java 17
if [ ! -d "$JAVA_HOME_17" ]; then
    echo -e "${RED}✗ 未找到 Java 17: $JAVA_HOME_17${NC}"
    exit 1
fi

# Helper: check if port is in use
is_port_in_use() {
    lsof -ti:"$1" > /dev/null 2>&1
}

# Helper: check if PID is alive
is_pid_alive() {
    [ -f "$1" ] && kill -0 "$(cat "$1")" 2>/dev/null
}

# ── 启动后端 ──────────────────────────────────────────
echo -e "${CYAN}[1/2] 启动后端 (Spring Boot :8080)...${NC}"

if is_pid_alive "$BACKEND_PID_FILE"; then
    echo -e "${YELLOW}  后端已在运行 (PID: $(cat "$BACKEND_PID_FILE"))，跳过${NC}"
elif is_port_in_use 8080; then
    echo -e "${YELLOW}  端口 8080 已被占用，跳过启动${NC}"
else
    cd "$SCRIPT_DIR/backend"
    JAVA_HOME="$JAVA_HOME_17" mvn -s mvn-settings.xml spring-boot:run \
        > "$BACKEND_LOG" 2>&1 &
    echo $! > "$BACKEND_PID_FILE"
    cd "$SCRIPT_DIR"

    echo -n "  等待后端就绪 "
    for i in $(seq 1 60); do
        if curl -s http://localhost:8080/api/categories > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        if [ "$i" -eq 60 ]; then
            echo -e "${RED}✗ 启动超时${NC}"
            echo "  日志: $BACKEND_LOG"
            exit 1
        fi
        printf "."
        sleep 1
    done
fi

# ── 启动前端 ──────────────────────────────────────────
echo -e "${CYAN}[2/2] 启动前端 (Vite :5173)...${NC}"

if is_pid_alive "$FRONTEND_PID_FILE"; then
    echo -e "${YELLOW}  前端已在运行 (PID: $(cat "$FRONTEND_PID_FILE"))，跳过${NC}"
elif is_port_in_use 5173; then
    echo -e "${YELLOW}  端口 5173 已被占用，跳过启动${NC}"
else
    if [ ! -d "$SCRIPT_DIR/frontend/node_modules" ]; then
        echo "  首次运行，安装前端依赖..."
        cd "$SCRIPT_DIR/frontend" && npm install --silent
        cd "$SCRIPT_DIR"
    fi

    cd "$SCRIPT_DIR/frontend"
    npm run dev > "$FRONTEND_LOG" 2>&1 &
    echo $! > "$FRONTEND_PID_FILE"
    cd "$SCRIPT_DIR"

    echo -n "  等待前端就绪 "
    for i in $(seq 1 20); do
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        if [ "$i" -eq 20 ]; then
            echo -e "${RED}✗ 启动超时${NC}"
            echo "  日志: $FRONTEND_LOG"
            exit 1
        fi
        printf "."
        sleep 1
    done
fi

# ── 完成 ──────────────────────────────────────────────
echo ""
echo -e "${GREEN}${BOLD}✅ 启动完成！${NC}"
echo -e "   前端地址：${CYAN}http://localhost:5173${NC}"
echo -e "   后端地址：${CYAN}http://localhost:8080${NC}"
echo -e "   后端日志：${BACKEND_LOG}"
echo ""

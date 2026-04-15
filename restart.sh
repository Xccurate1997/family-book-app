#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BOLD='\033[1m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}       家庭记账本 - 重启中              ${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

echo -e "${CYAN}第一步：停止所有服务...${NC}"
bash "$SCRIPT_DIR/stop.sh"

echo -e "${CYAN}等待端口释放...${NC}"
sleep 2

echo -e "${CYAN}第二步：重新启动...${NC}"
bash "$SCRIPT_DIR/start.sh"

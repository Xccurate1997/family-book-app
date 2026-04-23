#!/bin/bash
#============================================================
#  家庭记账本 - 生产环境运维脚本
#  用法: bash bookapp.sh {start|stop|restart|status}
#  部署路径: /opt/book-app/
#============================================================

APP_NAME="book-app"
APP_HOME="/opt/book-app"
JAR_FILE="$APP_HOME/backend/book-app-backend.jar"
CONFIG_FILE="$APP_HOME/conf/application.properties"
PID_FILE="$APP_HOME/backend/app.pid"
LOG_DIR="$APP_HOME/logs"
LOG_FILE="$LOG_DIR/backend.log"

# JVM 参数（可根据服务器配置调整）
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# 确保日志目录存在
mkdir -p "$LOG_DIR"

#------------------------------------------------------------
# 获取 PID
#------------------------------------------------------------
get_pid() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return 0
        fi
        rm -f "$PID_FILE"
    fi
    return 1
}

#------------------------------------------------------------
# 启动服务
#------------------------------------------------------------
do_start() {
    echo -e "${BOLD}========================================${NC}"
    echo -e "${BOLD}  家庭记账本 - 启动服务                ${NC}"
    echo -e "${BOLD}========================================${NC}"
    echo ""

    # 检查是否已运行
    local pid
    pid=$(get_pid) && {
        echo -e "${YELLOW}⚠ 服务已在运行 (PID: $pid)${NC}"
        return 0
    }

    # 检查 Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ 未找到 java 命令，请安装 JDK 17+${NC}"
        exit 1
    fi

    # 检查 jar 文件
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}✗ 未找到 jar 文件: $JAR_FILE${NC}"
        exit 1
    fi

    # 检查 Nginx 配置
    if command -v nginx &> /dev/null; then
        if nginx -t 2>/dev/null; then
            echo -e "  ${GREEN}✓ Nginx 配置正确${NC}"
        else
            echo -e "${YELLOW}⚠ Nginx 配置有误，请检查${NC}"
        fi
    fi

    # 启动后端
    echo -e "${CYAN}[1/2] 启动后端服务...${NC}"

    nohup java $JAVA_OPTS \
        -jar "$JAR_FILE" \
        --spring.config.additional-location=file:"$CONFIG_FILE" \
        >> "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"

    # 等待后端就绪
    echo -n "  等待后端就绪 "
    for i in $(seq 1 360); do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login 2>/dev/null | grep -qE "[2-5][0-9][0-9]"; then
            echo -e " ${GREEN}✓${NC}"
            break
        fi
        if [ "$i" -eq 360 ]; then
            echo -e " ${RED}✗ 启动超时${NC}"
            echo "  请查看日志: $LOG_FILE"
            exit 1
        fi
        printf "."
        sleep 1
    done

    # 启动/重载 Nginx
    echo -e "${CYAN}[2/2] 检查 Nginx...${NC}"
    if command -v nginx &> /dev/null; then
        if ! pgrep -x nginx > /dev/null; then
            systemctl start nginx 2>/dev/null || nginx
            echo -e "  ${GREEN}✓ Nginx 已启动${NC}"
        else
            nginx -s reload 2>/dev/null
            echo -e "  ${GREEN}✓ Nginx 已重载${NC}"
        fi
    else
        echo -e "  ${YELLOW}⚠ 未安装 Nginx，前端页面将无法访问${NC}"
    fi

    echo ""
    echo -e "${GREEN}${BOLD}✅ 启动完成！${NC}"
    echo -e "  后端 PID: $(cat "$PID_FILE")"
    echo -e "  访问地址: ${CYAN}http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')${NC}"
    echo -e "  后端日志: $LOG_FILE"
    echo ""
}

#------------------------------------------------------------
# 停止服务
#------------------------------------------------------------
do_stop() {
    echo -e "${BOLD}========================================${NC}"
    echo -e "${BOLD}  家庭记账本 - 停止服务                ${NC}"
    echo -e "${BOLD}========================================${NC}"
    echo ""

    local pid
    pid=$(get_pid)
    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}⚠ 服务未在运行${NC}"
        # 兜底清理
        local orphan_pid
        orphan_pid=$(lsof -ti:8080 2>/dev/null)
        if [ -n "$orphan_pid" ]; then
            echo -e "  发现 8080 端口占用进程 $orphan_pid，正在清理..."
            kill -TERM $orphan_pid 2>/dev/null
            sleep 2
            kill -KILL $orphan_pid 2>/dev/null || true
            echo -e "  ${GREEN}✓ 已清理${NC}"
        fi
        return 0
    fi

    echo -e "${CYAN}停止后端服务 (PID: $pid)...${NC}"
    kill -TERM "$pid" 2>/dev/null

    # 等待进程退出
    for i in $(seq 1 10); do
        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi
        sleep 1
    done

    # 强制终止
    if kill -0 "$pid" 2>/dev/null; then
        echo -e "  ${YELLOW}进程未响应，强制终止...${NC}"
        kill -KILL "$pid" 2>/dev/null
        sleep 1
    fi

    rm -f "$PID_FILE"
    echo -e "  ${GREEN}✓ 后端已停止${NC}"
    echo ""
    echo -e "${GREEN}${BOLD}✅ 已停止${NC}"
    echo ""
}

#------------------------------------------------------------
# 重启服务
#------------------------------------------------------------
do_restart() {
    echo -e "${BOLD}========================================${NC}"
    echo -e "${BOLD}  家庭记账本 - 重启服务                ${NC}"
    echo -e "${BOLD}========================================${NC}"
    echo ""

    do_stop
    sleep 5
    do_start
}

#------------------------------------------------------------
# 查看状态
#------------------------------------------------------------
do_status() {
    echo -e "${BOLD}========================================${NC}"
    echo -e "${BOLD}  家庭记账本 - 服务状态                ${NC}"
    echo -e "${BOLD}========================================${NC}"
    echo ""

    # 后端状态
    local pid
    pid=$(get_pid)
    if [ $? -eq 0 ]; then
        local mem
        mem=$(ps -p "$pid" -o rss= 2>/dev/null | awk '{printf "%.1f MB", $1/1024}')
        local uptime_info
        uptime_info=$(ps -p "$pid" -o etime= 2>/dev/null | xargs)
        echo -e "  后端:    ${GREEN}●  运行中${NC}"
        echo -e "  PID:     $pid"
        echo -e "  内存:    $mem"
        echo -e "  运行时间: $uptime_info"
    else
        echo -e "  后端:    ${RED}●  已停止${NC}"
    fi

    # Nginx 状态
    if command -v nginx &> /dev/null; then
        if pgrep -x nginx > /dev/null; then
            echo -e "  Nginx:   ${GREEN}●  运行中${NC}"
        else
            echo -e "  Nginx:   ${RED}●  已停止${NC}"
        fi
    else
        echo -e "  Nginx:   ${YELLOW}●  未安装${NC}"
    fi

    # 端口状态
    echo ""
    echo -e "  ${BOLD}端口监听:${NC}"
    if lsof -ti:8080 > /dev/null 2>&1; then
        echo -e "    8080 (后端API): ${GREEN}●  监听中${NC}"
    else
        echo -e "    8080 (后端API): ${RED}●  未监听${NC}"
    fi
    if lsof -ti:80 > /dev/null 2>&1; then
        echo -e "    80   (Nginx):   ${GREEN}●  监听中${NC}"
    else
        echo -e "    80   (Nginx):   ${RED}●  未监听${NC}"
    fi

    # 磁盘使用
    echo ""
    echo -e "  ${BOLD}数据目录:${NC}"
    if [ -d "$HOME/.bookapp" ]; then
        local data_size
        data_size=$(du -sh "$HOME/.bookapp" 2>/dev/null | cut -f1)
        echo -e "    $HOME/.bookapp: $data_size"
    else
        echo -e "    $HOME/.bookapp: ${YELLOW}不存在（首次启动后自动创建）${NC}"
    fi

    echo ""
}

#------------------------------------------------------------
# 查看日志
#------------------------------------------------------------
do_log() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        echo -e "${YELLOW}日志文件不存在: $LOG_FILE${NC}"
    fi
}

#------------------------------------------------------------
# 入口
#------------------------------------------------------------
case "$1" in
    start)
        do_start
        ;;
    stop)
        do_stop
        ;;
    restart)
        do_restart
        ;;
    status)
        do_status
        ;;
    log)
        do_log
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|log}"
        echo ""
        echo "  start   - 启动服务"
        echo "  stop    - 停止服务"
        echo "  restart - 重启服务"
        echo "  status  - 查看服务状态"
        echo "  log     - 实时查看后端日志"
        exit 1
        ;;
esac

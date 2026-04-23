#!/bin/bash
#============================================================
#  家庭记账本 - 服务器一键部署脚本
#  用法: bash deploy.sh [--skip-backend] [--skip-frontend] [--force-npm]
#
#  目录约定:
#    代码仓库: /opt/family-book-app          (git clone)
#    运行目录: /opt/book-app                 (jar/dist/运维脚本/日志)
#
#  流程:
#    1. 从 GitHub 拉取 master 最新代码
#    2. 构建后端 (Maven)
#    3. 构建前端 (Vite)
#    4. 同步产物到运行目录
#    5. 重启服务
#============================================================
set -e

# ── 路径配置 ──────────────────────────────────────────
REPO_DIR="/opt/family-book-app"
RUN_DIR="/opt/book-app"
BRANCH="master"

# ── 参数解析 ──────────────────────────────────────────
SKIP_BACKEND=false
SKIP_FRONTEND=false
FORCE_NPM=false

for arg in "$@"; do
    case $arg in
        --skip-backend)  SKIP_BACKEND=true ;;
        --skip-frontend) SKIP_FRONTEND=true ;;
        --force-npm)     FORCE_NPM=true ;;
    esac
done

# ── 颜色 ─────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}  家庭记账本 - 一键部署                ${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

# ── 检查代码仓库 ─────────────────────────────────────
if [ ! -d "$REPO_DIR/.git" ]; then
    echo -e "${RED}✗ 代码仓库不存在: $REPO_DIR${NC}"
    echo -e "  请先执行: git clone git@github.com:Xccurate1997/book-app.git $REPO_DIR"
    exit 1
fi

# ── [1/5] 拉取最新代码 ───────────────────────────────
echo -e "${CYAN}[1/5] 拉取最新代码...${NC}"
cd "$REPO_DIR"

# 记录当前 commit，用于判断是否有变更
OLD_COMMIT=$(git rev-parse HEAD 2>/dev/null || echo "none")

git fetch origin "$BRANCH" 2>&1 | cat
git checkout "$BRANCH" 2>&1 | cat
git reset --hard "origin/$BRANCH" 2>&1 | cat

NEW_COMMIT=$(git rev-parse HEAD)
if [ "$OLD_COMMIT" = "$NEW_COMMIT" ]; then
    echo -e "  ${YELLOW}代码无变更 ($NEW_COMMIT)${NC}"
else
    echo -e "  ${GREEN}✓ 已更新: ${OLD_COMMIT:0:8} → ${NEW_COMMIT:0:8}${NC}"
    # 显示最新提交信息
    echo -e "  最新提交: $(git log --oneline -1 | cat)"
fi
echo ""

# ── [2/5] 构建后端 ───────────────────────────────────
if [ "$SKIP_BACKEND" = true ]; then
    echo -e "${CYAN}[2/5] 跳过后端构建 (--skip-backend)${NC}"
else
    echo -e "${CYAN}[2/5] 构建后端 (Maven)...${NC}"
    cd "$REPO_DIR/backend"
    mvn clean package -DskipTests -q
    BACKEND_JAR=$(ls target/book-app-backend-*.jar 2>/dev/null | head -1)
    if [ -z "$BACKEND_JAR" ]; then
        echo -e "${RED}✗ 后端构建失败${NC}"
        exit 1
    fi
    echo -e "  ${GREEN}✓ 后端构建完成: $(basename $BACKEND_JAR)${NC}"
fi
echo ""

# ── [3/5] 构建前端 ───────────────────────────────────
if [ "$SKIP_FRONTEND" = true ]; then
    echo -e "${CYAN}[3/5] 跳过前端构建 (--skip-frontend)${NC}"
else
    echo -e "${CYAN}[3/5] 构建前端 (Vite)...${NC}"
    cd "$REPO_DIR/frontend"

    # 智能判断是否需要重新安装依赖
    NEED_INSTALL=false
    if [ ! -d "node_modules" ]; then
        NEED_INSTALL=true
        echo -e "  node_modules 不存在，需要安装依赖"
    elif [ "$FORCE_NPM" = true ]; then
        NEED_INSTALL=true
        echo -e "  强制重新安装依赖 (--force-npm)"
        rm -rf node_modules package-lock.json
    elif [ "package.json" -nt "node_modules/.package-lock.json" ] 2>/dev/null; then
        NEED_INSTALL=true
        echo -e "  package.json 有变更，需要更新依赖"
    fi

    if [ "$NEED_INSTALL" = true ]; then
        echo -e "  安装前端依赖..."
        npm install 2>&1 | tail -1
    fi

    npx vite build
    if [ ! -d "dist" ]; then
        echo -e "${RED}✗ 前端构建失败${NC}"
        exit 1
    fi
    echo -e "  ${GREEN}✓ 前端构建完成${NC}"
fi
echo ""

# ── [4/5] 同步产物到运行目录 ─────────────────────────
echo -e "${CYAN}[4/5] 同步产物到运行目录...${NC}"
mkdir -p "$RUN_DIR"/{backend,frontend,bin,conf,logs}

# 同步后端 jar
if [ "$SKIP_BACKEND" = false ]; then
    cp "$REPO_DIR/backend/$BACKEND_JAR" "$RUN_DIR/backend/book-app-backend.jar"
    echo -e "  ${GREEN}✓ 后端 jar 已同步${NC}"
fi

# 同步前端 dist
if [ "$SKIP_FRONTEND" = false ]; then
    rm -rf "$RUN_DIR/frontend/dist"
    cp -r "$REPO_DIR/frontend/dist" "$RUN_DIR/frontend/"
    echo -e "  ${GREEN}✓ 前端 dist 已同步${NC}"
fi

# 同步运维脚本和配置（始终同步，保持最新）
cp "$REPO_DIR/deploy/bookapp.sh" "$RUN_DIR/bin/" 2>/dev/null || true
cp "$REPO_DIR/deploy/nginx-bookapp.conf" "$RUN_DIR/conf/" 2>/dev/null || true
cp "$REPO_DIR/backend/src/main/resources/application.properties" "$RUN_DIR/conf/" 2>/dev/null || true
echo -e "  ${GREEN}✓ 配置文件已同步${NC}"
echo ""

# ── [5/5] 重启服务 ───────────────────────────────────
echo -e "${CYAN}[5/5] 重启服务...${NC}"
cd "$RUN_DIR/bin"
bash bookapp.sh restart

echo ""
echo -e "${GREEN}${BOLD}========================================${NC}"
echo -e "${GREEN}${BOLD}  ✅ 部署完成！${NC}"
echo -e "${GREEN}${BOLD}========================================${NC}"
echo -e "  代码版本: ${NEW_COMMIT:0:8}"
echo -e "  访问地址: ${CYAN}http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')${NC}"
echo ""

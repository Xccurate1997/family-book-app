#!/bin/bash
#============================================================
#  家庭记账本 - 一键打包脚本
#  用法: bash build.sh
#  产物: dist/book-app-1.0.0.tar.gz
#  可通过环境变量 JAVA_HOME 指定 JDK 17 路径
#============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="1.0.0"
DIST_DIR="$SCRIPT_DIR/dist"
PACKAGE_DIR="$DIST_DIR/book-app"

# ── 自动探测 JDK 17 ──────────────────────────────────
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"17'; then
    # 常见 JDK 17 安装路径列表
    JDK17_CANDIDATES=(
        "/usr/lib/jvm/java-17-openjdk"
        "/usr/lib/jvm/java-17-amazon-corretto"
        "/usr/lib/jvm/java-17"
        "/usr/java/jdk-17"
        "$HOME/Library/Java/JavaVirtualMachines/corretto-17.*/Contents/Home"
        "$HOME/Library/Java/JavaVirtualMachines/temurin-17.*/Contents/Home"
        "/Library/Java/JavaVirtualMachines/*/Contents/Home"
    )
    for candidate in "${JDK17_CANDIDATES[@]}"; do
        # 支持通配符展开
        for resolved in $candidate; do
            if [ -x "$resolved/bin/java" ] && "$resolved/bin/java" -version 2>&1 | grep -q '"17'; then
                export JAVA_HOME="$resolved"
                break 2
            fi
        done
    done
    # macOS: 使用 java_home 工具查找
    if [ -z "$JAVA_HOME" ] && command -v /usr/libexec/java_home &> /dev/null; then
        JDK17_PATH=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
        if [ -n "$JDK17_PATH" ] && [ -x "$JDK17_PATH/bin/java" ]; then
            export JAVA_HOME="$JDK17_PATH"
        fi
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}  家庭记账本 - 打包构建 v${VERSION}     ${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

# ── 检查依赖 ──────────────────────────────────────────
echo -e "${CYAN}[0/4] 检查构建依赖...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ 未找到 java，请安装 JDK 17+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
    echo -e "${RED}✗ Java 版本需要 17+，当前: $(java -version 2>&1 | head -1)${NC}"
    exit 1
fi
echo -e "  Java: $(java -version 2>&1 | head -1)"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ 未找到 mvn，请安装 Maven 3.6+${NC}"
    exit 1
fi
echo -e "  Maven: $(mvn -version 2>&1 | head -1)"

if ! command -v node &> /dev/null; then
    echo -e "${RED}✗ 未找到 node，请安装 Node.js 18+${NC}"
    exit 1
fi
echo -e "  Node: $(node -v)"
echo -e "  npm: $(npm -v)"
echo ""

# ── 构建后端 ──────────────────────────────────────────
echo -e "${CYAN}[1/4] 构建后端 (Maven)...${NC}"
cd "$SCRIPT_DIR/backend"
mvn clean package -DskipTests -q
BACKEND_JAR=$(ls target/book-app-backend-*.jar 2>/dev/null | head -1)
if [ -z "$BACKEND_JAR" ]; then
    echo -e "${RED}✗ 后端构建失败，未找到 jar 文件${NC}"
    exit 1
fi
echo -e "  ${GREEN}✓ 后端构建完成: $(basename $BACKEND_JAR)${NC}"
cd "$SCRIPT_DIR"
echo ""

# ── 构建前端 ──────────────────────────────────────────
echo -e "${CYAN}[2/4] 构建前端 (Vite)...${NC}"
cd "$SCRIPT_DIR/frontend"
# 清理并重新安装依赖，避免跨平台原生模块（如 @rollup/rollup-*）不兼容
echo "  清理旧依赖..."
rm -rf node_modules package-lock.json
echo "  安装前端依赖..."
npm install
npm run build
if [ ! -d "dist" ]; then
    echo -e "${RED}✗ 前端构建失败，未找到 dist 目录${NC}"
    exit 1
fi
echo -e "  ${GREEN}✓ 前端构建完成${NC}"
cd "$SCRIPT_DIR"
echo ""

# ── 组装部署包 ────────────────────────────────────────
echo -e "${CYAN}[3/4] 组装部署包...${NC}"
rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR"/{backend,frontend,bin,conf}

# 复制后端 jar
cp "$SCRIPT_DIR/backend/$BACKEND_JAR" "$PACKAGE_DIR/backend/book-app-backend.jar"

# 复制后端配置
cp "$SCRIPT_DIR/backend/src/main/resources/application.properties" "$PACKAGE_DIR/conf/application.properties"

# 复制前端静态文件
cp -r "$SCRIPT_DIR/frontend/dist" "$PACKAGE_DIR/frontend/"

# 复制运维脚本
cp "$SCRIPT_DIR/deploy/"*.sh "$PACKAGE_DIR/bin/" 2>/dev/null || true
cp "$SCRIPT_DIR/deploy/nginx-bookapp.conf" "$PACKAGE_DIR/conf/" 2>/dev/null || true

# 复制部署文档
cp "$SCRIPT_DIR/DEPLOY.md" "$PACKAGE_DIR/" 2>/dev/null || true

echo -e "  ${GREEN}✓ 部署包组装完成${NC}"
echo ""

# ── 打包压缩 ──────────────────────────────────────────
echo -e "${CYAN}[4/4] 打包压缩...${NC}"
cd "$DIST_DIR"
tar -czf "book-app-${VERSION}.tar.gz" book-app/
PACKAGE_SIZE=$(du -sh "book-app-${VERSION}.tar.gz" | cut -f1)
cd "$SCRIPT_DIR"

echo -e "  ${GREEN}✓ 打包完成${NC}"
echo ""
echo -e "${GREEN}${BOLD}========================================${NC}"
echo -e "${GREEN}${BOLD}  ✅ 构建成功！${NC}"
echo -e "${GREEN}${BOLD}========================================${NC}"
echo -e "  产物: ${CYAN}dist/book-app-${VERSION}.tar.gz${NC}"
echo -e "  大小: ${PACKAGE_SIZE}"
echo ""
echo -e "  部署步骤："
echo -e "    1. 上传到服务器: scp dist/book-app-${VERSION}.tar.gz user@server:/opt/"
echo -e "    2. 解压: tar -xzf book-app-${VERSION}.tar.gz"
echo -e "    3. 参考 DEPLOY.md 完成部署"
echo ""

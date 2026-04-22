# 家庭记账本 — CentOS 8.2 部署指南

## 目录

- [一、系统要求](#一系统要求)
- [二、环境准备](#二环境准备)
  - [2.1 安装 JDK 17](#21-安装-jdk-17)
  - [2.2 安装 Nginx](#22-安装-nginx)
  - [2.3 防火墙配置](#23-防火墙配置)
- [三、构建打包（开发机执行）](#三构建打包开发机执行)
  - [3.1 构建依赖](#31-构建依赖)
  - [3.2 一键打包](#32-一键打包)
- [四、部署到服务器](#四部署到服务器)
  - [4.1 上传与解压](#41-上传与解压)
  - [4.2 配置 Nginx](#42-配置-nginx)
  - [4.3 配置后端（可选）](#43-配置后端可选)
  - [4.4 启动服务](#44-启动服务)
- [五、运维命令](#五运维命令)
- [六、注册为系统服务（可选）](#六注册为系统服务可选)
- [七、数据备份与恢复](#七数据备份与恢复)
- [八、常见问题](#八常见问题)

---

## 一、系统要求

| 项目 | 最低要求 | 推荐配置 |
|------|----------|----------|
| **操作系统** | CentOS 8.2 | CentOS 8.x / Rocky Linux 8.x |
| **CPU** | 1 核 | 2 核 |
| **内存** | 512 MB | 1 GB+ |
| **磁盘** | 500 MB 可用空间 | 2 GB+ |
| **JDK** | 17+ | Amazon Corretto 17 / OpenJDK 17 |
| **Nginx** | 1.14+ | 1.20+ |

### 技术栈说明

- **后端**: Spring Boot 3.2.5 + Spring Security + JWT + JPA
- **数据库**: H2 嵌入式文件数据库（无需额外安装数据库服务）
- **前端**: Vue 3.4 + Element Plus 2.7 + ECharts 5.6（编译为纯静态文件）
- **Web 服务器**: Nginx（托管前端静态文件 + 反向代理后端 API）

---

## 二、环境准备

> 以下操作均在 **CentOS 8.2 服务器** 上以 `root` 用户执行。

### 2.1 安装 JDK 17

**方式一：通过 yum 安装 OpenJDK 17**

```bash
# CentOS 8 默认源可能没有 JDK 17，需要启用 AppStream
dnf install -y java-17-openjdk java-17-openjdk-devel

# 如果有多个 Java 版本，选择 JDK 17
alternatives --set java /usr/lib/jvm/java-17-openjdk-*/bin/java

# 验证
java -version
```

**方式二：安装 Amazon Corretto 17（推荐）**

```bash
# 导入 Amazon Corretto GPG Key
rpm --import https://yum.corretto.aws/corretto.key

# 添加 yum 源
cat > /etc/yum.repos.d/corretto.repo << 'EOF'
[corretto]
name=Amazon Corretto
baseurl=https://yum.corretto.aws/corretto-17/$basearch/
enabled=1
gpgcheck=1
gpgkey=https://yum.corretto.aws/corretto.key
EOF

# 安装
dnf install -y java-17-amazon-corretto-devel

# 验证
java -version
# openjdk version "17.x.x"
```

**配置 JAVA_HOME 环境变量**

```bash
# 查找 Java 安装路径
JAVA_PATH=$(dirname $(dirname $(readlink -f $(which java))))

# 写入环境变量
cat >> /etc/profile.d/java.sh << EOF
export JAVA_HOME=$JAVA_PATH
export PATH=\$JAVA_HOME/bin:\$PATH
EOF

source /etc/profile.d/java.sh

# 验证
echo $JAVA_HOME
java -version
```

### 2.2 安装 Nginx

```bash
# 安装 Nginx
dnf install -y nginx

# 设为开机自启
systemctl enable nginx

# 验证安装
nginx -v
```

### 2.3 防火墙配置

```bash
# 开放 HTTP 80 端口
firewall-cmd --permanent --add-service=http

# 如果需要 HTTPS，开放 443 端口
# firewall-cmd --permanent --add-service=https

# 重载防火墙规则
firewall-cmd --reload

# 验证
firewall-cmd --list-all
```

> **注意**: 后端 8080 端口 **不需要** 对外开放，Nginx 会在内部代理请求。

---

## 三、构建打包（开发机执行）

### 3.1 构建依赖

在 **开发机**（macOS / Linux）上需要安装以下工具：

| 工具 | 版本要求 | 安装方式 |
|------|----------|----------|
| JDK | 17+ | `brew install openjdk@17` 或 yum/apt 安装 |
| Maven | 3.6+ | `brew install maven` 或 yum/apt 安装 |
| Node.js | 18+ | `brew install node` 或 [nvm](https://github.com/nvm-sh/nvm) |
| npm | 9+ | 随 Node.js 自带 |

### 3.2 一键打包

```bash
# 进入项目根目录
cd /path/to/book-app

# 执行打包脚本
bash build.sh
```

打包完成后，产物位于 `dist/book-app-1.0.0.tar.gz`，包含以下内容：

```
book-app/
├── backend/
│   └── book-app-backend.jar    # 后端可执行 jar
├── frontend/
│   └── dist/                   # 前端静态文件
│       ├── index.html
│       ├── assets/
│       └── ...
├── conf/
│   ├── application.properties  # 后端配置文件
│   └── nginx-bookapp.conf      # Nginx 配置模板
├── bin/
│   └── bookapp.sh              # 运维脚本
└── DEPLOY.md                   # 本部署文档
```

---

## 四、部署到服务器

### 4.1 上传与解压

```bash
# 在开发机上传到服务器
scp dist/book-app-1.0.0.tar.gz root@your-server-ip:/opt/

# 在服务器上解压
cd /opt
tar -xzf book-app-1.0.0.tar.gz

# 验证目录结构
ls -la /opt/book-app/
```

**设置脚本执行权限**

```bash
chmod +x /opt/book-app/bin/bookapp.sh
```

### 4.2 配置 Nginx

```bash
# 复制 Nginx 配置文件
cp /opt/book-app/conf/nginx-bookapp.conf /etc/nginx/conf.d/bookapp.conf

# 编辑配置文件，修改 server_name（可选）
vi /etc/nginx/conf.d/bookapp.conf
# 将 server_name _; 改为实际域名或 IP，例如:
# server_name 192.168.1.100;
# server_name bookapp.example.com;

# 删除默认配置（避免冲突）
mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak 2>/dev/null || true

# 检查 Nginx 配置语法
nginx -t

# 启动 Nginx
systemctl start nginx
```

### 4.3 配置后端（可选）

后端配置文件位于 `/opt/book-app/conf/application.properties`，以下为可调整项：

```properties
# 服务端口（默认 8080，通常无需修改）
server.port=8080

# 数据库文件路径（默认存储在运行用户的 home 目录下）
# 如需自定义路径，取消注释并修改:
# spring.datasource.url=jdbc:h2:file:/data/bookapp/data;AUTO_SERVER=TRUE

# 文件上传大小限制
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB

# 生产环境建议关闭 H2 控制台
spring.h2.console.enabled=false
```

### 4.4 启动服务

```bash
# 启动
bash /opt/book-app/bin/bookapp.sh start

# 查看状态
bash /opt/book-app/bin/bookapp.sh status

# 验证访问
curl -s http://localhost/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | head -c 100
```

打开浏览器访问 `http://服务器IP` 即可使用。

> **默认管理员账号**: `admin` / `admin123`，请登录后立即修改密码。

---

## 五、运维命令

运维脚本位于 `/opt/book-app/bin/bookapp.sh`，支持以下命令：

### 快速参考

```bash
# 创建快捷别名（可选，写入 ~/.bashrc）
echo 'alias bookapp="/opt/book-app/bin/bookapp.sh"' >> ~/.bashrc
source ~/.bashrc
```

| 命令 | 说明 | 示例 |
|------|------|------|
| `start` | 启动后端服务 + 重载 Nginx | `bookapp start` |
| `stop` | 停止后端服务 | `bookapp stop` |
| `restart` | 重启后端服务 | `bookapp restart` |
| `status` | 查看服务状态、端口、内存 | `bookapp status` |
| `log` | 实时查看后端日志 | `bookapp log` |

### 详细用法

```bash
# 启动服务
bash /opt/book-app/bin/bookapp.sh start

# 停止服务
bash /opt/book-app/bin/bookapp.sh stop

# 重启服务
bash /opt/book-app/bin/bookapp.sh restart

# 查看服务状态（PID、内存、运行时间、端口监听）
bash /opt/book-app/bin/bookapp.sh status

# 实时查看后端日志（Ctrl+C 退出）
bash /opt/book-app/bin/bookapp.sh log
```

### JVM 参数调整

编辑 `/opt/book-app/bin/bookapp.sh` 中的 `JAVA_OPTS` 变量：

```bash
# 默认配置（适合 1GB 内存服务器）
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 2GB 内存服务器推荐
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 低内存服务器（512MB）
JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseSerialGC"
```

---

## 六、注册为系统服务（可选）

将应用注册为 systemd 服务，支持开机自启、自动重启：

```bash
# 创建 service 文件
cat > /etc/systemd/system/bookapp.service << 'EOF'
[Unit]
Description=家庭记账本 (Book App)
After=network.target

[Service]
Type=forking
User=root
ExecStart=/opt/book-app/bin/bookapp.sh start
ExecStop=/opt/book-app/bin/bookapp.sh stop
ExecReload=/opt/book-app/bin/bookapp.sh restart
PIDFile=/opt/book-app/backend/app.pid
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 重载 systemd
systemctl daemon-reload

# 设为开机自启
systemctl enable bookapp

# 使用 systemctl 管理
systemctl start bookapp     # 启动
systemctl stop bookapp      # 停止
systemctl restart bookapp   # 重启
systemctl status bookapp    # 查看状态
```

---

## 七、数据备份与恢复

### 数据存储位置

| 目录 | 内容 | 说明 |
|------|------|------|
| `~/.bookapp/data.*` | H2 数据库文件 | 所有业务数据（用户、账单、分类等） |
| `~/.bookapp/theme-assets/` | 主题资源文件 | 上传的图片、音效等 |

### 备份

```bash
# 创建备份目录
mkdir -p /backup/bookapp

# 备份数据库和主题资源
tar -czf /backup/bookapp/backup-$(date +%Y%m%d_%H%M%S).tar.gz \
    -C ~ .bookapp/

# 查看备份
ls -lh /backup/bookapp/
```

### 定时备份（crontab）

```bash
# 每天凌晨 3 点自动备份，保留最近 30 天
crontab -e

# 添加以下行:
0 3 * * * tar -czf /backup/bookapp/backup-$(date +\%Y\%m\%d).tar.gz -C ~ .bookapp/ && find /backup/bookapp/ -name "backup-*.tar.gz" -mtime +30 -delete
```

### 恢复

```bash
# 停止服务
bash /opt/book-app/bin/bookapp.sh stop

# 恢复数据
cd ~
tar -xzf /backup/bookapp/backup-20260421_030000.tar.gz

# 启动服务
bash /opt/book-app/bin/bookapp.sh start
```

---

## 八、常见问题

### Q1: 启动后访问 80 端口显示 502 Bad Gateway

**原因**: 后端服务尚未完全启动，Nginx 无法代理请求。

**解决**:
```bash
# 检查后端状态
bash /opt/book-app/bin/bookapp.sh status

# 查看后端日志
bash /opt/book-app/bin/bookapp.sh log

# 如果后端未启动，手动启动
bash /opt/book-app/bin/bookapp.sh start
```

### Q2: 端口 8080 被占用

**解决**:
```bash
# 查找占用 8080 端口的进程
lsof -ti:8080

# 终止占用进程
kill -9 $(lsof -ti:8080)

# 重新启动
bash /opt/book-app/bin/bookapp.sh start
```

### Q3: 上传文件时提示 413 Request Entity Too Large

**原因**: Nginx 的 `client_max_body_size` 限制。

**解决**: 确认 `/etc/nginx/conf.d/bookapp.conf` 中 `client_max_body_size` 值足够大（默认已设为 `20m`），然后重载 Nginx：
```bash
nginx -s reload
```

### Q4: 页面空白或 JS 报错

**原因**: 前端静态文件路径不正确。

**解决**:
```bash
# 确认前端文件存在
ls -la /opt/book-app/frontend/dist/

# 确认 Nginx 配置的 root 路径正确
grep "root" /etc/nginx/conf.d/bookapp.conf
```

### Q5: 如何修改数据库存储路径

编辑 `/opt/book-app/conf/application.properties`：
```properties
spring.datasource.url=jdbc:h2:file:/data/bookapp/data;AUTO_SERVER=TRUE
```
然后重启服务：
```bash
bash /opt/book-app/bin/bookapp.sh restart
```

### Q6: 如何升级版本

```bash
# 1. 在开发机重新打包
bash build.sh

# 2. 上传新包到服务器
scp dist/book-app-1.0.0.tar.gz root@server:/opt/

# 3. 在服务器上停止服务
bash /opt/book-app/bin/bookapp.sh stop

# 4. 备份旧版本
mv /opt/book-app /opt/book-app.bak

# 5. 解压新版本
cd /opt && tar -xzf book-app-1.0.0.tar.gz
chmod +x /opt/book-app/bin/bookapp.sh

# 6. 启动新版本
bash /opt/book-app/bin/bookapp.sh start

# 7. 确认正常后删除备份
rm -rf /opt/book-app.bak
```

> **注意**: H2 数据库文件存储在 `~/.bookapp/` 目录下，不在应用目录内，升级不会丢失数据。

---

## 附录：完整部署命令速查

```bash
# ====== 服务器环境准备（仅首次执行） ======
dnf install -y java-17-openjdk java-17-openjdk-devel nginx
systemctl enable nginx
firewall-cmd --permanent --add-service=http && firewall-cmd --reload

# ====== 部署应用 ======
cd /opt && tar -xzf book-app-1.0.0.tar.gz
chmod +x /opt/book-app/bin/bookapp.sh
cp /opt/book-app/conf/nginx-bookapp.conf /etc/nginx/conf.d/bookapp.conf
mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak 2>/dev/null
nginx -t && systemctl start nginx

# ====== 启动服务 ======
bash /opt/book-app/bin/bookapp.sh start

# ====== 日常运维 ======
bash /opt/book-app/bin/bookapp.sh status   # 查看状态
bash /opt/book-app/bin/bookapp.sh restart  # 重启
bash /opt/book-app/bin/bookapp.sh log      # 查看日志
bash /opt/book-app/bin/bookapp.sh stop     # 停止
```

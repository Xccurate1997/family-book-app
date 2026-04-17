# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

前后端分离的家庭记账应用，Mac 本地使用。

- **后端**：Java 17 + Spring Boot 3.2 + H2 文件数据库（存于 `~/.bookapp/data.mv.db`）
- **前端**：Vue 3 + Element Plus + Vite

## 常用命令

### 一键管理（推荐）

```bash
./start.sh      # 启动前后端（含健康检查，等待就绪后返回）
./stop.sh       # 停止前后端
./restart.sh    # 重启
```

### 后端单独操作

```bash
cd backend

# 启动（必须指定 Java 17）
JAVA_HOME=/Users/wzl/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home \
  mvn -s mvn-settings.xml spring-boot:run

# 编译检查
JAVA_HOME=... mvn -s mvn-settings.xml compile

# 下载依赖（首次或更新 pom.xml 后）
JAVA_HOME=... mvn -s mvn-settings.xml dependency:resolve
```

> `mvn-settings.xml` 使用 Maven Central 镜像，绕过无法访问的内网 tbmirror。

### 前端单独操作

```bash
cd frontend
npm install       # 安装依赖
npm run dev       # 启动开发服务器（:5173）
npm run build     # 构建生产产物
```

## 架构说明

### 后端结构

```
backend/src/main/java/com/bookapp/
├── entity/          # JPA 实体：Category、Transaction、TransactionType(enum)
├── repository/      # Spring Data JPA 接口
├── service/         # 业务逻辑；TransactionService 内嵌 TransactionRequest record
├── controller/      # REST 控制器，路径前缀 /api/
├── config/          # CorsConfig：允许 localhost:5173 跨域
└── DataInitializer  # 首次启动时写入 12 个默认分类
```

**数据模型关系**：`Transaction` → `Category`（ManyToOne EAGER），Category 区分 INCOME/EXPENSE 类型，Transaction 也有相同枚举字段。

**日期序列化**：`spring.jackson.serialization.write-dates-as-timestamps=false`，`LocalDate` 序列化为 `"YYYY-MM-DD"` 字符串。

### 前端结构

```
frontend/src/
├── api/index.js         # Axios 封装，baseURL=/api（由 Vite proxy 转发到 :8080）
├── App.vue              # 根组件：月份导航、布局、状态管理
└── components/
    ├── SummaryCards.vue     # 月度收入/支出/结余三卡片
    ├── TransactionForm.vue  # 新增/编辑弹窗（el-dialog）
    └── TransactionList.vue  # 按日期分组的账单列表
```

所有页面状态集中在 `App.vue`，子组件通过 props 接收数据、emit 事件（`edit`、`delete`、`saved`）向上通知。

### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/categories` | 获取所有分类 |
| GET | `/api/transactions?year=&month=` | 按月查询，按日期倒序 |
| GET | `/api/transactions/summary?year=&month=` | 月度汇总（totalIncome/totalExpense/balance） |
| POST | `/api/transactions` | 新增记录 |
| PUT | `/api/transactions/{id}` | 编辑记录 |
| DELETE | `/api/transactions/{id}` | 删除记录 |

请求体字段：`amount`、`type`（INCOME/EXPENSE）、`categoryId`、`description`、`transactionDate`（YYYY-MM-DD）。

## Git 分支约定

- `master`：稳定版本
- `feature/iter-YYYYMMDD`：按日期命名的迭代分支

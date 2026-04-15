# 家庭记账本

前后端分离的家庭记账应用，Mac 本地使用。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.2 + H2（文件持久化） |
| 前端 | Vue 3 + Element Plus + Vite |

数据存储于 `~/.bookapp/data.mv.db`，重启不丢失。

## 启动方式

需要：**JDK 17+**、**Node.js 18+**、**Maven 3.6+**

### 1. 启动后端

```bash
cd backend
JAVA_HOME=/Users/wzl/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home \
  mvn -s mvn-settings.xml spring-boot:run
```

> 首次启动需下载依赖，约需 2 分钟。后续启动约 2 秒。

后端运行在 http://localhost:8080

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 http://localhost:5173

### 3. 打开浏览器

访问 http://localhost:5173

## 功能

- 收入 / 支出记账，选择分类、日期、备注
- 按月查看账单，左右切换月份
- 月度汇总：收入、支出、结余
- 编辑 / 删除记录
- 内置默认分类（餐饮、交通、工资等）

## API 接口（参考）

```
GET    /api/categories              获取所有分类
GET    /api/transactions?year=&month=  获取指定月份账单
GET    /api/transactions/summary?year=&month=  月度汇总
POST   /api/transactions            新增记录
PUT    /api/transactions/{id}       修改记录
DELETE /api/transactions/{id}       删除记录
```

# 情绪表情规则管理指南

## 概述

每笔收支记录会根据规则自动匹配一个情绪表情（emoji 或自定义图片），显示在金额旁边。规则通过 REST API 管理，支持按交易类型、金额范围、分类进行匹配。

## 规则匹配逻辑

- 系统按 **优先级从高到低** 依次检查所有已启用的规则
- 第一条匹配成功的规则生效，后续不再检查
- 匹配条件：交易类型 AND 金额范围 AND 分类（条件为 null 表示不限）

## 默认规则

系统首次启动时自动创建以下规则：

| 优先级 | 名称 | 类型 | 金额范围 | 表情 |
|--------|------|------|----------|------|
| 60 | 大额收入 | INCOME | >= 5000 | 🎉 |
| 50 | 收入开心 | INCOME | 0 ~ 5000 | 😄 |
| 40 | 大额支出 | EXPENSE | >= 500 | 😱 |
| 30 | 较大支出 | EXPENSE | 200 ~ 500 | 😰 |
| 20 | 中等支出 | EXPENSE | 50 ~ 200 | 😐 |
| 10 | 小额支出 | EXPENSE | 0 ~ 50 | 😊 |

## API 接口

基础地址：`http://localhost:8080/api`

---

### 查看所有规则

```bash
curl http://localhost:8080/api/emoticon-rules
```

---

### 创建规则（文字 emoji）

```bash
curl -X POST http://localhost:8080/api/emoticon-rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "超大额支出",
    "emojiType": "TEXT",
    "emojiContent": "💀",
    "transactionType": "EXPENSE",
    "minAmount": 2000,
    "maxAmount": null,
    "categoryId": null,
    "priority": 45,
    "enabled": true
  }'
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 规则名称，便于识别 |
| emojiType | String | 是 | `TEXT`（emoji 字符）或 `IMAGE`（自定义图片） |
| emojiContent | String | 是 | emoji 字符串 或 图片文件名 |
| transactionType | String | 否 | `INCOME` / `EXPENSE`，null 表示匹配所有 |
| minAmount | Number | 否 | 金额下限（含），null 不限 |
| maxAmount | Number | 否 | 金额上限（不含），null 不限 |
| categoryId | Number | 否 | 指定分类 ID，null 不限分类 |
| priority | Number | 是 | 优先级，数值越大越优先匹配 |
| enabled | Boolean | 是 | 是否启用 |

---

### 修改规则

```bash
curl -X PUT http://localhost:8080/api/emoticon-rules/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "小额支出",
    "emojiType": "TEXT",
    "emojiContent": "😁",
    "transactionType": "EXPENSE",
    "minAmount": 0,
    "maxAmount": 100,
    "categoryId": null,
    "priority": 10,
    "enabled": true
  }'
```

---

### 删除规则

```bash
curl -X DELETE http://localhost:8080/api/emoticon-rules/1
```

---

### 禁用规则（不删除）

将 `enabled` 设为 `false` 即可：

```bash
curl -X PUT http://localhost:8080/api/emoticon-rules/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "小额支出",
    "emojiType": "TEXT",
    "emojiContent": "😊",
    "transactionType": "EXPENSE",
    "minAmount": 0,
    "maxAmount": 50,
    "categoryId": null,
    "priority": 10,
    "enabled": false
  }'
```

---

## 使用自定义图片表情

### 第一步：上传图片

```bash
curl -X POST http://localhost:8080/api/emoticon-rules/upload-image \
  -F "file=@/path/to/cry.png"
```

返回：

```json
{ "filename": "a1b2c3d4.png" }
```

图片存储位置：`~/.bookapp/emojis/`

### 第二步：用返回的文件名创建 IMAGE 类型规则

```bash
curl -X POST http://localhost:8080/api/emoticon-rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "娱乐消费",
    "emojiType": "IMAGE",
    "emojiContent": "a1b2c3d4.png",
    "transactionType": "EXPENSE",
    "minAmount": null,
    "maxAmount": null,
    "categoryId": 5,
    "priority": 100,
    "enabled": true
  }'
```

### 验证图片可访问

```bash
curl http://localhost:8080/api/emoticon-images/a1b2c3d4.png --output test.png
```

---

## 常见场景示例

### 针对特定分类设置表情

先查询分类 ID：

```bash
curl http://localhost:8080/api/categories
```

然后创建规则（categoryId 指定分类，priority 设高以覆盖通用规则）：

```bash
curl -X POST http://localhost:8080/api/emoticon-rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "餐饮消费",
    "emojiType": "TEXT",
    "emojiContent": "😋",
    "transactionType": "EXPENSE",
    "minAmount": null,
    "maxAmount": null,
    "categoryId": 1,
    "priority": 100,
    "enabled": true
  }'
```

### 不区分收支类型的通用规则

`transactionType` 设为 `null`：

```bash
curl -X POST http://localhost:8080/api/emoticon-rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "万元以上",
    "emojiType": "TEXT",
    "emojiContent": "🤯",
    "transactionType": null,
    "minAmount": 10000,
    "maxAmount": null,
    "categoryId": null,
    "priority": 200,
    "enabled": true
  }'
```

## 注意事项

- 金额范围为 **左闭右开** 区间：`minAmount`（含）到 `maxAmount`（不含）
- 优先级高的规则先匹配，建议分类专属规则优先级设为 100+，通用规则 10~60
- 修改规则后立即生效，无需重启（缓存自动刷新）
- 自定义图片建议不超过 100KB，格式支持 png / jpg / gif / webp

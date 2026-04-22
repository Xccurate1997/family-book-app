# 彩蛋特效功能指南

## 功能概述

记账时输入特定金额或满足特定条件会触发屏幕特效（烟花、爱心飘落、金币雨或自定义视频），为记账增添趣味。系统通过**规则引擎**灵活匹配触发条件，支持多种特效类型和自定义视频上传。

---

## 特效类型

| 枚举值 | 效果 | 说明 |
|--------|------|------|
| `FIREWORKS` | 🎆 烟花 | 全屏 Canvas 烟花动画，火箭升空后爆炸为彩色粒子 |
| `HEARTS` | 💕 爱心飘落 | 粉色系爱心从屏幕底部飘起，左右摇摆上升 |
| `GOLD_RAIN` | 🪙 金币雨 | 带 ¥ 符号的金币从顶部飘落，旋转下落 |
| `VIDEO` | 🎬 自定义视频 | 播放上传的自定义视频文件 |

所有 Canvas 动画均为**全屏透明绘制**，融入记账界面背景，持续约 4 秒后渐变消失。

---

## 彩蛋规则引擎

### 规则匹配机制

每次记账时，后端会按**优先级从高到低**遍历所有已启用的规则，返回第一个匹配的规则来触发对应特效。

### 规则字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | String | 规则名称（如"520 爱心"） |
| `exactAmount` | BigDecimal | 精确匹配金额（如 520） |
| `minAmount` | BigDecimal | 最小金额（含），与 maxAmount 配合使用 |
| `maxAmount` | BigDecimal | 最大金额（含），与 minAmount 配合使用 |
| `categoryId` | Long | 匹配的分类 ID（null 表示不限分类） |
| `transactionType` | String | 匹配的交易类型：EXPENSE / INCOME（null 表示不限） |
| `effectType` | EffectType | 触发的特效类型 |
| `videoFilename` | String | 自定义视频文件名（仅 VIDEO 类型需要） |
| `priority` | int | 优先级，数值越大越优先匹配 |
| `enabled` | boolean | 是否启用 |

### 匹配逻辑

规则匹配按以下顺序检查（所有条件必须同时满足）：

1. **交易类型**：如果规则指定了 transactionType，则交易类型必须匹配
2. **分类**：如果规则指定了 categoryId，则交易分类必须匹配
3. **精确金额**：如果规则指定了 exactAmount，则金额必须完全相等
4. **金额范围**：如果规则指定了 minAmount/maxAmount，则金额必须在范围内

### 默认规则

系统初始化时会自动创建以下默认规则：

| 规则名称 | 触发条件 | 特效类型 | 优先级 |
|----------|----------|----------|--------|
| 520 爱心 | 金额 = 520 | HEARTS 💕 | 100 |
| 1314 烟花 | 金额 = 1314 | FIREWORKS 🎆 | 90 |
| 万元金币雨 | 金额 ≥ 10000 | GOLD_RAIN 🪙 | 10 |

---

## 技术链路

```
记账 → TransactionService 保存交易
     → EffectRuleService.evaluate() 按优先级匹配规则
     → 匹配成功：创建 EffectEvent（含 type + videoUrl）
     → EffectService.pushEffect() 通过 SSE 推送到前端
     → 前端根据 type 播放对应 Canvas 动画或视频
     → 播放完成后回调标记已播放
```

---

## 自定义视频上传

### 上传接口

```
POST /api/effect-videos/upload
Content-Type: multipart/form-data
参数: file (视频文件)
```

**限制条件**：
- 文件大小上限：**20MB**
- 存储位置：`~/.bookapp/videos/`
- 返回值：上传后的文件名

### 获取视频

```
GET /api/effect-videos/{filename}
```

返回视频文件流，前端通过此 URL 播放视频。

### 使用流程

1. 调用上传接口上传视频文件，获取返回的文件名
2. 创建或修改彩蛋规则，设置 `effectType = VIDEO`，`videoFilename = 上传返回的文件名`
3. 当交易匹配该规则时，前端会自动播放对应视频

---

## API 接口

### 特效事件

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/effects/stream` | SSE 事件流，前端监听 `effect` 事件 |
| `POST` | `/api/effects/{id}/played` | 标记特效已播放 |

### 彩蛋规则管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/effect-rules` | 获取所有规则（按优先级降序） |
| `POST` | `/api/effect-rules` | 创建新规则 |
| `PUT` | `/api/effect-rules/{id}` | 更新规则 |
| `DELETE` | `/api/effect-rules/{id}` | 删除规则 |

**创建/更新规则请求体示例**：

```json
{
  "name": "情人节烟花",
  "exactAmount": 214,
  "effectType": "FIREWORKS",
  "priority": 80,
  "enabled": true
}
```

```json
{
  "name": "大额金币雨",
  "minAmount": 5000,
  "maxAmount": 50000,
  "transactionType": "EXPENSE",
  "effectType": "GOLD_RAIN",
  "priority": 20,
  "enabled": true
}
```

```json
{
  "name": "自定义视频彩蛋",
  "exactAmount": 666,
  "effectType": "VIDEO",
  "videoFilename": "lucky.mp4",
  "priority": 50,
  "enabled": true
}
```

### 视频管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/effect-videos/upload` | 上传视频（multipart/form-data） |
| `GET` | `/api/effect-videos/{filename}` | 获取视频文件流 |

---

## 相关文件清单

### 后端

| 文件 | 职责 |
|------|------|
| `entity/EffectType.java` | 特效类型枚举（FIREWORKS / HEARTS / GOLD_RAIN / VIDEO） |
| `entity/EffectRule.java` | 彩蛋规则实体（触发条件 + 特效配置） |
| `entity/EffectEvent.java` | 特效事件实体（SSE 推送载体） |
| `repository/EffectRuleRepository.java` | 规则数据访问 |
| `repository/EffectEventRepository.java` | 事件数据访问 |
| `service/EffectRuleService.java` | 规则匹配引擎 + CRUD + 缓存 |
| `service/EffectService.java` | SSE 推送 + 视频上传 + 标记已播放 |
| `service/TransactionService.java` | 记账时调用规则引擎触发特效 |
| `controller/UnbelievableController.java` | REST 接口（规则 CRUD + 视频上传/获取 + SSE） |
| `DataInitializer.java` | 初始化默认彩蛋规则 |

### 前端

| 文件 | 职责 |
|------|------|
| `composables/useVideoEffect.js` | SSE 连接 + 特效队列管理 |
| `components/VideoEffect.vue` | 多类型 Canvas 动画 + 视频播放 + 渐变退出 |
| `api/index.js` | `markEffectPlayed` 接口封装 |

---

## 扩展指南

### 添加新的特效类型

1. 在 `EffectType.java` 枚举中添加新值（如 `SNOW`）
2. 在 `VideoEffect.vue` 的 `startCanvasAnimation()` 中添加对应的 `case` 分支
3. 实现新的 Canvas 动画函数（如 `runSnowAnimation()`）
4. 创建对应的彩蛋规则即可使用

### 管理员界面（待开发）

目前彩蛋规则管理仅通过后端 API 操作，后续可开发管理员界面，提供：
- 规则列表展示与编辑
- 视频上传与预览
- 规则启用/禁用开关
- 拖拽调整优先级

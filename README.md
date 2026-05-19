# 教室卫生 AI 智能巡查系统（Health-AI-inspection-system）

教室卫生巡查后端：**微信小程序 → Java 后端 → 阿里云视觉智能 API**。

## 一、系统架构

- 小程序只负责拍照上传
- Java 后端负责鉴权 + 调用 AI
- AI 输出 `pass`（合格）或 `review`（待复核）
- 老师只处理 `review` 的记录
- 图片在本地磁盘保留 7 天，到期自动清理（数据库记录保留，便于统计）

## 二、模块说明

| 路径 | 作用 |
|------|------|
| `web/CleaningController` | REST 接口 |
| `service/CleaningService` | 业务编排 |
| `ai/AliyunVisionService` | 阿里云目标检测调用（可 mock） |
| `ai/CleaningRuleEngine` | 宽松版的合格/复核判定规则 |
| `storage/PhotoStorage` | 图片落盘到 `data/cleaning/YYYY/MM/DD/uuid.jpg` |
| `storage/PhotoCleanupJob` | 每日 03:00 定时清理 7 天前的图片 |
| `domain/CleaningRecord` | 卫生巡查记录的 JPA 实体 |

## 三、接口设计

| 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|
| POST | `/api/cleaning/check` | 学生 | 上传图片 + 教室ID，返回 `pass` / `review` + `record_id` |
| GET | `/api/cleaning/review/list` | 老师 | 查询待复核的记录列表 |
| POST | `/api/cleaning/review/submit` | 老师 | 对 `record_id` 提交 `pass` / `fail` 的复核结果 |
| GET | `/api/cleaning/stats` | 管理员 | 按教室聚合统计，可选 `from` / `to` ISO 时间过滤 |

请求头 `X-User-Role` 与 `X-User-Id` 用于传递角色和用户ID；生产环境请替换为 JWT 鉴权。

### 前端

- **微信小程序**：[miniprogram/](miniprogram/) 目录，详见 [docs/miniprogram-integration.md](docs/miniprogram-integration.md)。
- **Web 管理后台**：源码在 [src/main/resources/static/](src/main/resources/static/)，由 Spring Boot 自动托管。启动后访问 <http://localhost:8080/>，依次包含：
  - `index.html` — 角色选择（学生 / 老师 / 管理员）
  - `upload.html` — 学生在 PC 端补传照片，便于调试
  - `review.html` — 老师复核台，支持「通过 / 不通过」
  - `stats.html` — 管理员统计面板，含教室聚合明细 + 时间区间过滤

### 调用示例

```bash
curl -X POST http://localhost:8080/api/cleaning/check \
  -H "X-User-Role: STUDENT" -H "X-User-Id: stu-001" \
  -F "photo=@classroom.jpg" -F "classroomId=A101"
```

返回示例：

```json
{ "result": "review", "reason": "floor_big_trash, desk_ok, podium_ok, bin_ok", "record_id": "1" }
```

## 四、AI 判定规则（宽松版）

- 地面出现**明显大件垃圾** → `review`
- 桌面出现**成片垃圾/饮料瓶** → `review`
- 讲台**明显堆积** → `review`
- 垃圾桶**溢出** → `review`
- 其余情况 → `pass`

## 五、配置说明

配置文件：`src/main/resources/application.yml`

- `cleaning.storage-root`：图片落盘根目录，默认 `./data/cleaning`
- `cleaning.retention-days`：图片保留天数，默认 `7`
- `aliyun.access-key-id` / `aliyun.access-key-secret`：阿里云 AccessKey，可用环境变量 `ALIYUN_ACCESS_KEY_ID` / `ALIYUN_ACCESS_KEY_SECRET`
- `aliyun.mock`：默认 `true`，本地开发时跳过真实 AI 调用，返回空检测结果

## 六、数据库表结构

默认使用文件 H2，便于本地开发。核心表 `cleaning_record`：

| 字段 | 说明 |
|------|------|
| `id` | 主键 |
| `classroom_id`、`uploader_id` | 教室 / 上传人 |
| `photo_path` | 图片磁盘路径（7 天后文件被清理，记录保留） |
| `ai_result` | `pass` / `review` |
| `ai_detail` | 判定原因文本 |
| `reviewer_id`、`final_result`、`reviewed_at` | 老师复核后写入 |
| `created_at` | 上传时间 |

切换 MySQL：修改 `spring.datasource.*` 与 `spring.jpa.properties.hibernate.dialect`。

## 七、构建与运行

```bash
mvn spring-boot:run    # 启动后端
mvn test               # 跑单元测试
```

## 八、角色权限

- **学生**：通过 `/api/cleaning/check` 上传照片
- **老师**：通过 `/api/cleaning/review/*` 处理待复核记录
- **管理员**：统计报表（后续迭代）

## 九、开发周期

- 第 1 周：上传 + 存储 + AI 接口 ✅
- 第 2 周：老师复核 + 7 天清理任务 ✅
- 第 3 周：统计报表（可选）

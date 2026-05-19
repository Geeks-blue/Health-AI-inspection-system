# Health-AI-inspection-system

Classroom cleaning inspection backend: **WeChat mini-program → Java backend → Alibaba Cloud Vision AI**.

## Architecture

- Mini-program only uploads photos
- Java backend authenticates the uploader and calls the AI
- AI returns `pass` or `review`
- Teachers only handle `review` cases
- Photos are auto-purged from local disk after 7 days (DB records are kept)

## Modules

| Path | Purpose |
|------|---------|
| `web/CleaningController` | REST endpoints |
| `service/CleaningService` | Orchestration |
| `ai/AliyunVisionService` | Alibaba Cloud objectdet integration (mockable) |
| `ai/CleaningRuleEngine` | Lenient pass/review rules |
| `storage/PhotoStorage` | Save photos under `data/cleaning/YYYY/MM/DD/uuid.jpg` |
| `storage/PhotoCleanupJob` | Daily 03:00 cron to delete files older than 7 days |
| `domain/CleaningRecord` | JPA entity for the inspection record |

## REST endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/cleaning/check` | student | Upload photo, classroom ID; returns `pass` / `review` + `record_id` |
| GET | `/api/cleaning/review/list` | teacher | List records awaiting review |
| POST | `/api/cleaning/review/submit` | teacher | Submit `pass` / `fail` for a `record_id` |

Role + user id are sent in the `X-User-Role` and `X-User-Id` headers; swap this for JWT auth in production.

### Sample request

```bash
curl -X POST http://localhost:8080/api/cleaning/check \
  -H "X-User-Role: STUDENT" -H "X-User-Id: stu-001" \
  -F "photo=@classroom.jpg" -F "classroomId=A101"
```

```json
{ "result": "review", "reason": "floor_big_trash, desk_ok, podium_ok, bin_ok", "record_id": "1" }
```

## Configuration

`src/main/resources/application.yml`:

- `cleaning.storage-root` — root folder for uploaded photos (default `./data/cleaning`)
- `cleaning.retention-days` — retention period in days (default `7`)
- `aliyun.access-key-id` / `aliyun.access-key-secret` — Alibaba Cloud credentials (env vars `ALIYUN_ACCESS_KEY_ID` / `ALIYUN_ACCESS_KEY_SECRET`)
- `aliyun.mock` — when `true` (default) the AI client returns an empty detection so the app boots without credentials

## Database

Defaults to file-based H2 for development. The `cleaning_record` table:

| Column | Notes |
|--------|-------|
| `id` | PK |
| `classroom_id`, `uploader_id` | who/where |
| `photo_path` | absolute path on disk (file may be purged after 7 days) |
| `ai_result` | `pass` / `review` |
| `ai_detail` | reasons string |
| `reviewer_id`, `final_result`, `reviewed_at` | populated after teacher review |
| `created_at` | upload timestamp |

For MySQL, override `spring.datasource.*` and `spring.jpa.properties.hibernate.dialect`.

## Build & run

```bash
mvn spring-boot:run
mvn test
```

## Roles

- **Student** — upload photos via `/api/cleaning/check`
- **Teacher** — review flagged records via `/api/cleaning/review/*`
- **Admin** — statistics (future work)

## Roadmap

- Week 1: upload + storage + AI integration ✅
- Week 2: teacher review + cleanup job ✅
- Week 3: statistics dashboard (optional)

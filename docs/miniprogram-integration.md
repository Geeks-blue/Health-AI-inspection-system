# 微信小程序接入指南

本文档说明小程序端如何接入后端的 3 个 REST 接口。完整可运行的脚手架在仓库根目录的 `miniprogram/` 目录。

## 一、接入点全景图

```
微信小程序                      Java 后端                    阿里云视觉
─────────                       ─────────                    ──────────
学生页 拍照
  ↓ wx.uploadFile
  POST /api/cleaning/check  ───→  落盘 → AI 判定 → 入库
                                       ↓
                                  阿里云 detectImageElements
  ←─── { result, reason, record_id } ─

老师页 打开
  ↓ wx.request
  GET /api/cleaning/review/list ─→  查 finalResult 为空的记录
  ←──── [ { recordId, classroomId, ... } ]

老师页 点「通过 / 不通过」
  ↓ wx.request
  POST /api/cleaning/review/submit ─→ 写入 finalResult / reviewerId
  ←──── 204 No Content
```

## 二、接入步骤

### 1. 用微信开发者工具打开 `miniprogram/`

`File → Open Project → 选择 miniprogram 目录`。`project.config.json` 里 `appid` 为占位，需要改成你们小程序的真实 AppID。

### 2. 配置后端域名

`miniprogram/app.js` 里的 `globalData.baseUrl` 改成后端公网地址（HTTPS）。同时在微信公众平台 → 开发 → 服务器域名里把这个域名加进 `request 合法域名` 和 `uploadFile 合法域名` 两个白名单。

### 3. 替换登录态

`app.js` 的 `onLaunch` 演示了 `wx.login` 拿到 `code`，但暂时使用了占位用户。生产接入需要：

1. 在后端实现 `/api/auth/wx-login`，用 `appid + secret + code` 调微信 `jscode2session`，拿到 `openid` 后映射成系统用户 ID 与角色。
2. 把 `app.js` 里的 TODO 段打开，把后端返回的 `userId` / `role` 写回 `globalData.user`。
3. 后续每次请求会由 `utils/request.js` 自动带上 `X-User-Id` 与 `X-User-Role`。

## 三、关键约定

| 项目 | 约定 |
|------|------|
| 鉴权头 | `X-User-Id` + `X-User-Role`（STUDENT / TEACHER / ADMIN） |
| 上传字段 | `multipart/form-data`，文件字段名 `photo`，附带 `classroomId` |
| 返回字段 | `result`（pass/review）、`reason`、`record_id`（下划线） |
| 复核提交 | JSON body：`{ "recordId": <Long>, "result": "pass"|"fail" }` |
| 错误码 | 400 参数错误、403 角色不足、409 重复复核、500 服务器错误 |

## 四、本地联调

后端默认监听 `http://localhost:8080`。小程序在开发者工具里勾选「不校验合法域名」即可联调。后端可用 `mvn spring-boot:run` 启动；`aliyun.mock=true`（默认）下不会真正调用 AI，所有上传都会判为 pass，方便调通整条链路。

## 五、目录结构

```
miniprogram/
├── app.js              # 全局：baseUrl、登录态、wx.login 接入点
├── app.json            # 页面注册 + tabBar
├── app.wxss            # 全局样式
├── project.config.json # 开发者工具项目配置
├── sitemap.json
├── utils/
│   └── request.js      # 统一封装 wx.request / wx.uploadFile + 鉴权头
└── pages/
    ├── upload/         # 学生：拍照 + 上传
    └── review/         # 老师：拉列表 + 提交 pass/fail
```

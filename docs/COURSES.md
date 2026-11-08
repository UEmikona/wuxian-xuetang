# 课程目录格式说明

无限学堂的课程内容由 `app/src/main/assets/courses.json` 提供。
当前文件内置 6 门**占位课程**（标题、讲师、评论、价格均为示例），用于演示列表、详情、收藏、购买、目录、评论与播放流程。
视频统一使用 `app/src/main/assets/videos/placeholder.mp4`（约 20 秒的测试画面与提示音）。

App 启动时读取该文件并替换本地课程目录与评论；收藏、购买、进度和笔记保存在其他表中，不受影响。

## 结构

```json
{
  "courses": [
    {
      "id": "kotlin-basics",
      "title": "Kotlin 入门实战",
      "subtitle": "一句话简介",
      "description": "课程详细介绍",
      "instructor": "讲师名",
      "category": "编程",
      "priceCents": 0,
      "coverColor": "#3D5AFE",
      "rating": 4.8,
      "ratingCount": 236,
      "studentCount": 5120,
      "featured": true,
      "lessons": [
        { "id": "kotlin-basics-l01", "title": "课程导学", "videoUrl": "asset:///videos/placeholder.mp4", "durationMs": 20000 }
      ],
      "reviews": [
        { "author": "学员甲", "rating": 5, "content": "评论内容", "date": "2026-10-02" }
      ]
    }
  ]
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `courses[].id` | 字符串 | 是 | 课程唯一标识，发布后不要修改 |
| `courses[].title` | 字符串 | 是 | 课程标题 |
| `subtitle` | 字符串 | 否 | 一句话简介，首页轮播和详情页显示 |
| `description` | 字符串 | 否 | 简介页正文 |
| `instructor` | 字符串 | 否 | 讲师 |
| `category` | 字符串 | 否 | 分类，决定分类页与搜索筛选；缺省为"其他" |
| `priceCents` | 整数 | 否 | 价格（单位：分）。`0` 表示免费，显示"免费领取" |
| `coverColor` | 字符串 | 否 | 封面渐变颜色，格式 `#RRGGBB` |
| `rating` / `ratingCount` | 数字 / 整数 | 否 | 评分与评价数 |
| `studentCount` | 整数 | 否 | 学习人数（展示用） |
| `featured` | 布尔 | 否 | 为 `true` 时进入首页轮播与"精选推荐" |
| `lessons[].id` | 字符串 | 是 | 课时唯一标识，进度与笔记以此关联 |
| `lessons[].videoUrl` | 字符串 | 是 | 视频地址：`asset:///…`（随包资源）或 `https://…` 直链 |
| `lessons[].durationMs` | 整数 | 否 | 时长（毫秒）。缺省或为 0 时以播放器读取的时长为准 |
| `reviews[]` | 数组 | 否 | 评论列表：`author`、`rating`（1–5）、`content`、`date` |

## 业务规则

1. 购买或领取后即拥有课程，课时才可播放；未购买时目录显示为"未解锁"。
2. 购买是**占位流程**，不接入支付，不产生真实扣款。
3. 课时完成规则：播放到总时长的 95% 即视为完成，也可在播放页手动"标记完成"。
4. 修改 `courses.json` 后需重新启动 App 才会生效。

## 替换为真实内容时

- 视频较大时，改用 `https://` 直链（或 HLS `.m3u8`），不要把大文件放进 assets。
- 封面图目前使用渐变色占位，若需真实封面，需另行加入图片加载库。
- 格式错误（JSON 无法解析、缺少必填字段）时，App 保留上一次成功载入的目录，不会崩溃。

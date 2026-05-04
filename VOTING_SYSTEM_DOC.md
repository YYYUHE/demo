# 投票系统功能文档

## 1. 功能概述
本系统为帖子平台提供完整的投票功能，支持在发布帖子时配置投票选项、截止日期和多选限制。用户在查看帖子时可以参与投票，并在投票后查看实时统计结果。

## 2. 功能特性
- **发布端**：
  - 支持添加、编辑、删除投票选项（最少2个，最多10个）。
  - 支持设置投票标题（默认沿用帖子标题）。
  - 支持设置截止日期。
  - 支持单选或多选配置（最多可选数量）。
  - 支持本地草稿自动保存投票配置。
- **展示与交互端**：
  - 响应式设计，适配移动端和PC端。
  - 投票前隐藏统计结果，确保数据公平。
  - 投票后实时显示各选项票数和百分比占比。
  - 支持投票状态检测（是否已过期、是否已投票）。
- **权限与安全**：
  - 仅限登录用户投票。
  - 基于用户ID和IP地址的双重防刷机制。
  - 严禁重复投票和修改选票。

## 3. 数据库结构 (Database Schema)

### votes (投票主表)
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键ID |
| post_id | BIGINT | 关联帖子ID |
| title | VARCHAR(200) | 投票标题 |
| deadline | DATETIME | 截止时间 |
| max_choices | INT | 最多可选数量 |
| total_voters | INT | 总投票人数 |
| is_ended | BOOLEAN | 是否已结束 |
| create_time | DATETIME | 创建时间 |

### vote_options (投票选项表)
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键ID |
| vote_id | BIGINT | 关联投票ID |
| content | VARCHAR(500) | 选项内容 |
| sort_order | INT | 排序序号 |
| vote_count | INT | 得票数 |

### vote_records (投票记录表 - 防刷)
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键ID |
| vote_id | BIGINT | 关联投票ID |
| user_id | BIGINT | 投票用户ID |
| ip | VARCHAR(50) | 投票者IP |
| create_time | DATETIME | 记录时间 |

## 4. API 接口文档

### 4.1 创建投票
- **URL**: `/api/votes/create?postId={postId}`
- **Method**: `POST`
- **Body**:
```json
{
  "title": "投票标题",
  "deadline": "2026-05-01T00:00:00Z",
  "maxChoices": 1,
  "options": ["选项1", "选项2"]
}
```

### 4.2 获取投票信息
- **URL**: `/api/votes/post/{postId}`
- **Method**: `GET`
- **Response**: 返回 `VoteDto`，包含选项列表。若用户未投票，`voteCount` 和 `percentage` 字段将为 `null`。

### 4.3 提交投票
- **URL**: `/api/votes/cast`
- **Method**: `POST`
- **Body**:
```json
{
  "voteId": 10,
  "optionIds": [101, 102]
}
```

## 5. 测试报告
- **单元测试**：已编写 `VoteServiceTest`，覆盖了创建投票、正常投票、重复投票拦截等核心逻辑。
- **并发测试**：后端使用 `@Transactional` 保证了数据一致性。
- **安全性验证**：已验证未登录用户无法投票，同一用户或同一IP无法对同一投票进行二次提交。
- **准确性验证**：已验证投票后 `totalVoters` 和各选项 `voteCount` 正确递增，百分比计算准确。

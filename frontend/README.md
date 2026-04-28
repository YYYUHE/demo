# Demo 前端项目 - Vue3

这是一个基于 Vue 3 + Vite + Pinia + Vue Router 的现代化前端项目。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Vue Router** - Vue.js 官方路由管理器
- **Pinia** - Vue 的状态管理库
- **Axios** - HTTP 客户端
- **wangEditor** - 富文本编辑器
- **Day.js** - 轻量级日期处理库

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口
│   │   ├── auth.js       # 认证相关接口
│   │   ├── post.js       # 帖子相关接口
│   │   ├── draft.js      # 草稿相关接口
│   │   ├── follow.js     # 关注相关接口
│   │   ├── topic.js      # 话题相关接口
│   │   └── upload.js     # 上传相关接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── stores/           # Pinia 状态管理
│   │   ├── user.js       # 用户状态
│   │   ├── app.js        # 应用状态
│   │   └── post.js       # 帖子状态
│   ├── styles/           # 全局样式
│   │   └── main.css
│   ├── utils/            # 工具函数
│   │   ├── request.js    # Axios 封装
│   │   └── helpers.js    # 辅助函数
│   ├── views/            # 页面组件
│   │   ├── Auth.vue      # 登录/注册页面
│   │   ├── Posts.vue     # 帖子列表页面
│   │   ├── PostDetail.vue # 帖子详情页面
│   │   ├── PostEditor.vue # 帖子编辑器
│   │   ├── Drafts.vue    # 草稿箱页面
│   │   ├── Profile.vue   # 个人资料页面
│   │   ├── Favorites.vue # 收藏页面
│   │   └── MyPosts.vue   # 我的帖子页面
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html            # HTML 模板
├── package.json          # 项目配置
├── vite.config.js        # Vite 配置
└── .eslintrc.cjs         # ESLint 配置
```

## 快速开始

### 安装依赖

```bash
cd frontend
npm install
```

### 开发模式

```bash
npm run dev
```

开发服务器将在 `http://localhost:5173` 启动。

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `../src/main/resources/static` 目录，可以直接部署到 Spring Boot 应用。

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 主要功能

### 1. 用户认证
- 用户注册和登录
- 会话管理
- 自动登录检查

### 2. 帖子管理
- 创建和编辑帖子
- 富文本编辑器支持
- 图片上传
- 话题标签
- 帖子列表和详情
- 点赞和收藏

### 3. 草稿管理
- 保存草稿
- 草稿列表
- 编辑和删除草稿

### 4. 个人中心
- 个人资料展示
- 头像上传
- 我的帖子
- 我的收藏
- 粉丝和关注统计

### 5. 话题系统
- 热门话题展示
- 话题搜索和建议
- 话题使用统计

## API 代理配置

开发环境下，API 请求会自动代理到 `http://localhost:8080`，可以在 `vite.config.js` 中修改：

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 状态管理

项目使用 Pinia 进行状态管理，主要包含以下 store：

- **user** - 用户信息和登录状态
- **app** - 应用全局状态（loading、toast 等）
- **post** - 帖子相关状态

## 路由配置

路由配置在 `src/router/index.js` 中，包含以下路由：

- `/auth` - 登录/注册
- `/posts` - 帖子列表
- `/post/:id` - 帖子详情
- `/post-editor` - 帖子编辑器
- `/drafts` - 草稿箱
- `/profile` - 个人资料
- `/favorites` - 收藏
- `/my-posts` - 我的帖子

## 性能优化

1. **代码分割** - 使用路由懒加载
2. **组件缓存** - 合理使用 `keep-alive`
3. **请求优化** - Axios 拦截器统一处理
4. **构建优化** - Vite 自动优化依赖和代码分割

## 开发建议

1. 遵循 Vue 3 Composition API 最佳实践
2. 使用 `<script setup>` 语法糖
3. 合理使用 Pinia store 管理状态
4. 组件拆分，保持单一职责
5. 使用 TypeScript 增强类型安全（可选）

## 浏览器支持

- Chrome (最新版)
- Firefox (最新版)
- Safari (最新版)
- Edge (最新版)

## License

MIT

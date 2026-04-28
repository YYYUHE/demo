import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/posts'
  },
  {
    path: '/auth',
    name: 'Auth',
    component: () => import('@/views/Auth.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/posts',
    name: 'Posts',
    component: () => import('@/views/Posts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/PostDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/post-editor',
    name: 'PostEditor',
    component: () => import('@/views/PostEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/drafts',
    name: 'Drafts',
    component: () => import('@/views/Drafts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/views/Favorites.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-posts',
    name: 'MyPosts',
    component: () => import('@/views/MyPosts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/Messages.vue'),
    meta: { requiresAuth: true },
    redirect: '/messages/likes',
    children: [
      {
        path: 'likes',
        name: 'LikesMessages',
        component: () => import('@/views/messages/LikesMessages.vue')
      },
      {
        path: 'replies',
        name: 'RepliesMessages',
        component: () => import('@/views/messages/RepliesMessages.vue')
      },
      {
        path: 'mentions',
        name: 'MentionsMessages',
        component: () => import('@/views/messages/MentionsMessages.vue')
      },
      {
        path: 'likes-received',
        name: 'LikesReceivedMessages',
        component: () => import('@/views/messages/LikesReceivedMessages.vue')
      },
      {
        path: 'system',
        name: 'SystemMessages',
        component: () => import('@/views/messages/SystemMessages.vue')
      },
      {
        path: 'settings',
        name: 'MessageSettings',
        component: () => import('@/views/messages/MessageSettings.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth) {
    const isLoggedIn = await userStore.checkLoginStatus()
    if (!isLoggedIn) {
      next('/auth')
      return
    }
  }
  
  if (to.meta.requiresGuest) {
    const isLoggedIn = await userStore.checkLoginStatus()
    if (isLoggedIn) {
      next('/posts')
      return
    }
  }
  
  next()
})

export default router

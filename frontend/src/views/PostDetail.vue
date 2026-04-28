<template>
  <div class="post-detail-container">
    <div v-if="loading" class="loading">
      加载中...
    </div>
    
    <div v-else-if="!post" class="error">
      帖子不存在
    </div>
    
    <div v-else class="post-detail">
      <div class="post-header">
        <button class="back-btn" @click="goBack">
          ← 返回
        </button>
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="author-info">
          <img :src="post.authorAvatar || '/default-avatar.png'" class="avatar" />
          <div class="author-details">
            <div class="author-name">{{ post.authorName }}</div>
            <div class="post-time">{{ formatDateTime(post.createdAt) }}</div>
          </div>
        </div>
      </div>
      
      <div v-if="post.topics && post.topics.length > 0" class="post-topics">
        <span v-for="topic in post.topics" :key="topic" class="topic-tag">
          #{{ topic }}
        </span>
      </div>
      
      <div class="post-content" v-html="renderedContent"></div>
      
      <div class="post-actions">
        <button
          class="action-btn like-btn"
          :class="{ active: post.isLiked }"
          @click="toggleLike"
        >
          <span class="icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
          <span class="count">{{ post.likeCount || 0 }}</span>
        </button>
        
        <button
          class="action-btn favorite-btn"
          :class="{ active: post.isFavorited }"
          @click="toggleFavorite"
        >
          <span class="icon">{{ post.isFavorited ? '⭐' : '☆' }}</span>
          <span class="count">{{ post.favoriteCount || 0 }}</span>
        </button>
      </div>
      
      <!-- 评论区 -->
      <div class="comments-section">
        <div class="comments-header">
          <h3>评论 ({{ commentTotal }})</h3>
          <div class="sort-buttons">
            <button 
              :class="['sort-btn', { active: commentSort === 'time' }]"
              @click="changeCommentSort('time')"
            >
              最新
            </button>
            <button 
              :class="['sort-btn', { active: commentSort === 'hot' }]"
              @click="changeCommentSort('hot')"
            >
              最热
            </button>
          </div>
        </div>
        
        <!-- 发表评论 -->
        <div class="comment-composer">
          <textarea
            v-model="newComment"
            placeholder="写下你的评论... (@某人可以输入@)"
            class="comment-textarea"
            rows="3"
            @input="handleCommentInput"
            @keydown="handleCommentKeydown"
          ></textarea>
          <div class="composer-footer">
            <span class="char-count">{{ newComment.length }}/500</span>
            <button 
              class="submit-comment-btn"
              @click="submitComment"
              :disabled="!newComment.trim() || submitting"
            >
              {{ submitting ? '发送中...' : '发送' }}
            </button>
          </div>
        </div>
        
        <!-- @用户选择弹窗 -->
        <MentionPopup
          v-if="showMentionPopup"
          :visible="showMentionPopup"
          :position="mentionPopupPosition"
          @select="handleMentionSelect"
          @close="closeMentionPopup"
        />
        
        <!-- 评论列表 -->
        <div class="comments-list">
          <div v-if="commentsLoading" class="loading-comments">
            加载中...
          </div>
          
          <div v-else-if="comments.length === 0" class="no-comments">
            暂无评论，快来抢沙发吧！
          </div>
          
          <div v-else class="comment-items">
            <div 
              v-for="comment in comments" 
              :key="comment.id"
              class="comment-item"
              :id="`comment-${comment.id}`"
            >
              <div class="comment-avatar">
                <img 
                  v-if="comment.authorAvatar" 
                  :src="comment.authorAvatar" 
                  alt="头像"
                />
                <div v-else class="avatar-placeholder">
                  {{ comment.authorUsername?.charAt(0) }}
                </div>
              </div>
              
              <div class="comment-body">
                <div class="comment-meta">
                  <span class="comment-author">{{ comment.authorUsername }}</span>
                  <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
                </div>
                
                <div class="comment-content" v-html="renderCommentContent(comment.content)"></div>
                
                <div class="comment-actions">
                  <button
                    class="comment-action-btn"
                    :class="{ active: comment.liked }"
                    @click="toggleCommentLike(comment)"
                  >
                    <span class="icon">{{ comment.liked ? '👍' : '👍' }}</span>
                    <span class="count">{{ comment.likeCount || 0 }}</span>
                  </button>
                  
                  <button
                    class="comment-action-btn reply-btn"
                    @click="startReply(comment)"
                  >
                    回复
                  </button>
                </div>
                
                <!-- 回复框 -->
                <div v-if="replyingTo === comment.id" class="reply-box">
                  <textarea
                    v-model="replyText"
                    placeholder="回复 @{{ comment.authorUsername }}..."
                    class="reply-textarea"
                    rows="2"
                    @input="handleReplyInput"
                    @keydown="handleReplyKeydown"
                  ></textarea>
                  <div class="reply-footer">
                    <span class="char-count">{{ replyText.length }}/500</span>
                    <button 
                      class="submit-reply-btn"
                      @click="submitReply(comment.id)"
                      :disabled="!replyText.trim() || submitting"
                    >
                      {{ submitting ? '发送中...' : '发送' }}
                    </button>
                    <button 
                      class="cancel-reply-btn"
                      @click="cancelReply"
                    >
                      取消
                    </button>
                  </div>
                </div>
                
                <!-- 子回复列表 -->
                <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                  <div 
                    v-for="reply in comment.replies" 
                    :key="reply.id"
                    class="reply-item"
                  >
                    <div class="reply-avatar">
                      <img 
                        v-if="reply.authorAvatar" 
                        :src="reply.authorAvatar" 
                        alt="头像"
                      />
                      <div v-else class="avatar-placeholder small">
                        {{ reply.authorUsername?.charAt(0) }}
                      </div>
                    </div>
                    
                    <div class="reply-body">
                      <div class="reply-meta">
                        <span class="reply-author">{{ reply.authorUsername }}</span>
                        <span class="reply-time">{{ formatDateTime(reply.createTime) }}</span>
                      </div>
                      
                      <div class="reply-content" v-html="renderCommentContent(reply.content)"></div>
                      
                      <div class="reply-actions">
                        <button
                          class="reply-action-btn"
                          :class="{ active: reply.liked }"
                          @click="toggleCommentLike(reply)"
                        >
                          👍 {{ reply.likeCount || 0 }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 加载更多 -->
          <div v-if="hasMoreComments" class="load-more">
            <button @click="loadMoreComments" :disabled="commentsLoading">
              {{ commentsLoading ? '加载中...' : '加载更多' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { getPostDetail, likePost, unlikePost, favoritePost, unfavoritePost } from '@/api/post'
import { createComment, getComments, likeComment } from '@/api/comment'
import { getUsersBasic } from '@/api/user'
import { formatDateTime } from '@/utils/helpers'
import {
  extractMentionUids,
  renderMentions,
  insertMentionToTextarea,
  getCaretCoordinates,
  getMentionSearchKeyword
} from '@/utils/mention'
import MentionPopup from '@/components/MentionPopup.vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const loading = ref(false)
const post = ref(null)

// 评论相关
const comments = ref([])
const commentsLoading = ref(false)
const commentTotal = ref(0)
const commentSort = ref('time')
const hasMoreComments = ref(false)
const commentPage = ref(1)
const commentPageSize = ref(20)

// 发表评论
const newComment = ref('')
const submitting = ref(false)

// @功能
const showMentionPopup = ref(false)
const mentionPopupPosition = ref({ top: 0, left: 0 })
const currentTextarea = ref(null) // 当前正在输入的textarea

// 回复相关
const replyingTo = ref(null)
const replyText = ref('')

const mentionUidToNameMap = ref(new Map())

// 渲染帖子内容，高亮@标记
const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  return renderMentions(post.value.content, mentionUidToNameMap.value)
})

async function refreshMentionUserMap() {
  const ids = new Set()

  extractMentionUids(post.value?.content).forEach(id => ids.add(id))
  comments.value.forEach(comment => {
    extractMentionUids(comment?.content).forEach(id => ids.add(id))
    ;(comment?.replies || []).forEach(reply => {
      extractMentionUids(reply?.content).forEach(id => ids.add(id))
    })
  })

  const missingIds = Array.from(ids).filter(id => !mentionUidToNameMap.value.has(id))
  if (missingIds.length === 0) return

  try {
    const res = await getUsersBasic(missingIds)
    if (res.code === 200) {
      const next = new Map(mentionUidToNameMap.value)
      ;(res.data || []).forEach(user => {
        if (user?.id != null) {
          next.set(user.id, user.username || `用户${user.id}`)
        }
      })
      mentionUidToNameMap.value = next
    }
  } catch (error) {
    console.error('加载@用户信息失败:', error)
  }
}

async function loadPost() {
  loading.value = true
  
  try {
    const postId = route.params.id
    const res = await getPostDetail(postId)
    
    if (res.code === 200) {
      post.value = res.data
      await refreshMentionUserMap()
    } else {
      appStore.showToast('加载帖子失败', 'error')
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
    appStore.showToast('加载帖子失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  
  try {
    const postId = route.params.id
    const res = await getComments(postId, {
      page: commentPage.value,
      size: commentPageSize.value,
      sort: commentSort.value
    })
    
    if (res.code === 200) {
      const data = res.data
      if (commentPage.value === 1) {
        comments.value = data.items || []
      } else {
        comments.value = [...comments.value, ...(data.items || [])]
      }
      commentTotal.value = data.total || 0
      hasMoreComments.value = data.hasMore || false
      await refreshMentionUserMap()
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    appStore.showToast('加载评论失败', 'error')
  } finally {
    commentsLoading.value = false
  }
}

async function loadMoreComments() {
  commentPage.value++
  await loadComments()
}

function changeCommentSort(sort) {
  commentSort.value = sort
  commentPage.value = 1
  loadComments()
}

// 处理评论输入，检测@符号
function handleCommentInput(event) {
  const textarea = event.target
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = textarea.value.substring(0, cursorPos)
  
  if (textBeforeCursor.endsWith('@')) {
    currentTextarea.value = textarea
    showMentionPopupAtCursor(textarea)
  } else {
    const mentionInfo = getMentionSearchKeyword(textarea.value, cursorPos)
    if (mentionInfo) {
      currentTextarea.value = textarea
      showMentionPopupAtCursor(textarea)
    }
  }
}

function handleReplyInput(event) {
  const textarea = event.target
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = textarea.value.substring(0, cursorPos)
  
  if (textBeforeCursor.endsWith('@')) {
    currentTextarea.value = textarea
    showMentionPopupAtCursor(textarea)
  } else {
    const mentionInfo = getMentionSearchKeyword(textarea.value, cursorPos)
    if (mentionInfo) {
      currentTextarea.value = textarea
      showMentionPopupAtCursor(textarea)
    }
  }
}

// 在光标位置显示@弹窗
function showMentionPopupAtCursor(textarea) {
  const coords = getCaretCoordinates(textarea)
  
  // 计算弹窗位置（在光标上方）
  mentionPopupPosition.value = {
    top: coords.top - 350, // 弹窗高度约320px，留一些边距
    left: coords.left
  }
  
  // 确保弹窗不超出屏幕
  if (mentionPopupPosition.value.top < 10) {
    mentionPopupPosition.value.top = coords.top + 30
  }
  
  showMentionPopup.value = true
}

// 关闭@弹窗
function closeMentionPopup() {
  showMentionPopup.value = false
  currentTextarea.value = null
}

// 选择@用户
function handleMentionSelect(user) {
  if (!currentTextarea.value) return
  
  insertMentionToTextarea(currentTextarea.value, user)
  
  // 更新对应的v-model
  if (replyingTo.value && currentTextarea.value.classList.contains('reply-textarea')) {
    // 这是回复框
    replyText.value = currentTextarea.value.value
  } else {
    // 这是主评论框
    newComment.value = currentTextarea.value.value
  }
  
  closeMentionPopup()
}

// 处理键盘事件
function handleCommentKeydown(event) {
  // Ctrl+Enter 发送
  if (event.ctrlKey && event.key === 'Enter') {
    submitComment()
  }
}

function handleReplyKeydown(event) {
  // Ctrl+Enter 发送
  if (event.ctrlKey && event.key === 'Enter') {
    submitReply(replyingTo.value)
  }
}

async function submitComment() {
  if (!newComment.value.trim()) {
    appStore.showToast('请输入评论内容', 'error')
    return
  }
  
  if (newComment.value.length > 500) {
    appStore.showToast('评论最多500字', 'error')
    return
  }
  
  submitting.value = true
  
  try {
    const res = await createComment(route.params.id, {
      content: newComment.value,
      parentId: null,
      rootId: null
    })
    
    if (res.code === 200) {
      appStore.showToast('评论成功')
      newComment.value = ''
      commentPage.value = 1
      await loadComments()
    }
  } catch (error) {
    console.error('评论失败:', error)
    appStore.showToast('评论失败', 'error')
  } finally {
    submitting.value = false
  }
}

function startReply(comment) {
  replyingTo.value = comment.id
  replyText.value = `@[uid:${comment.authorId}] `
  setTimeout(() => {
    const textarea = document.querySelector(`#comment-${comment.id} .reply-textarea`)
    if (textarea) {
      textarea.focus()
      textarea.setSelectionRange(replyText.value.length, replyText.value.length)
    }
  }, 100)
}

function cancelReply() {
  replyingTo.value = null
  replyText.value = ''
}

async function submitReply(parentId) {
  if (!replyText.value.trim()) {
    appStore.showToast('请输入回复内容', 'error')
    return
  }
  
  if (replyText.value.length > 500) {
    appStore.showToast('回复最多500字', 'error')
    return
  }
  
  submitting.value = true
  
  try {
    const res = await createComment(route.params.id, {
      content: replyText.value,
      parentId: parentId,
      rootId: parentId
    })
    
    if (res.code === 200) {
      appStore.showToast('回复成功')
      replyText.value = ''
      replyingTo.value = null
      commentPage.value = 1
      await loadComments()
    }
  } catch (error) {
    console.error('回复失败:', error)
    appStore.showToast('回复失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function toggleLike() {
  if (!post.value) return
  
  try {
    if (post.value.isLiked) {
      await unlikePost(post.value.id)
      post.value.isLiked = false
      post.value.likeCount--
    } else {
      await likePost(post.value.id)
      post.value.isLiked = true
      post.value.likeCount++
    }
  } catch (error) {
    console.error('操作失败:', error)
    appStore.showToast('操作失败', 'error')
  }
}

async function toggleFavorite() {
  if (!post.value) return
  
  try {
    if (post.value.isFavorited) {
      await unfavoritePost(post.value.id)
      post.value.isFavorited = false
      post.value.favoriteCount--
    } else {
      await favoritePost(post.value.id)
      post.value.isFavorited = true
      post.value.favoriteCount++
    }
  } catch (error) {
    console.error('操作失败:', error)
    appStore.showToast('操作失败', 'error')
  }
}

async function toggleCommentLike(comment) {
  try {
    const res = await likeComment(comment.id)
    comment.liked = res.data.liked
    comment.likeCount = res.data.likeCount
  } catch (error) {
    console.error('操作失败:', error)
    appStore.showToast('操作失败', 'error')
  }
}

function goBack() {
  router.back()
}

// 渲染评论内容，高亮@标记
function renderCommentContent(content) {
  return renderMentions(content, mentionUidToNameMap.value)
}

onMounted(async () => {
  await loadPost()
  await loadComments()
  
  // 检查 URL 中是否有 commentId 参数,如果有则滚动到对应评论
  const commentId = route.query.commentId
  if (commentId) {
    setTimeout(() => {
      const commentElement = document.getElementById(`comment-${commentId}`)
      if (commentElement) {
        commentElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
        commentElement.style.backgroundColor = '#fff3cd'
        commentElement.style.transition = 'background-color 2s'
        setTimeout(() => {
          commentElement.style.backgroundColor = ''
        }, 2000)
      }
    }, 1000)
  }
})
</script>

<style scoped>
.post-detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading,
.error {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.post-detail {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.post-header {
  margin-bottom: 24px;
}

.back-btn {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #374151;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.back-btn:hover {
  background: #e5e7eb;
}

.post-title {
  font-size: 32px;
  color: #333;
  margin-bottom: 20px;
  line-height: 1.4;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.post-time {
  font-size: 14px;
  color: #999;
}

.post-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.topic-tag {
  padding: 6px 14px;
  background: #f0f2f5;
  color: #667eea;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 500;
}

.post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 32px;
  min-height: 200px;
}

.post-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
}

.post-content :deep(p) {
  margin-bottom: 16px;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
  color: #111827;
}

.post-content :deep(ul),
.post-content :deep(ol) {
  margin-left: 24px;
  margin-bottom: 16px;
}

.post-content :deep(li) {
  margin-bottom: 8px;
}

.post-content :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 16px;
  margin: 16px 0;
  color: #6b7280;
  font-style: italic;
}

.post-content :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.post-content :deep(pre) {
  background: #1f2937;
  color: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

.post-content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.post-actions {
  display: flex;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #f3f4f6;
  color: #374151;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: #e5e7eb;
}

.action-btn.active {
  background: #667eea;
  color: white;
}

.action-btn.active:hover {
  background: #5a67d8;
}

.action-btn .icon {
  font-size: 18px;
}

.action-btn .count {
  font-size: 14px;
}

/* 评论区样式 */
.comments-section {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 2px solid #e5e7eb;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.comments-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.sort-buttons {
  display: flex;
  gap: 8px;
}

.sort-btn {
  padding: 6px 14px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-btn:hover {
  background: #e5e7eb;
}

.sort-btn.active {
  background: #667eea;
  color: white;
}

.comment-composer {
  margin-bottom: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #f9fafb;
}

.comment-textarea,
.reply-textarea {
  width: 100%;
  padding: 12px;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
}

.composer-footer,
.reply-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: white;
  border-top: 1px solid #e5e7eb;
}

.char-count {
  font-size: 12px;
  color: #9ca3af;
}

.submit-comment-btn,
.submit-reply-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-comment-btn:hover:not(:disabled),
.submit-reply-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-comment-btn:disabled,
.submit-reply-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-reply-btn {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  margin-left: 8px;
}

.cancel-reply-btn:hover {
  background: #e5e7eb;
}

.comments-list {
  margin-top: 20px;
}

.loading-comments,
.no-comments {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
  font-size: 14px;
}

.comment-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.comment-avatar,
.reply-avatar {
  flex-shrink: 0;
}

.comment-avatar img,
.reply-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
}

.avatar-placeholder.small {
  width: 32px;
  height: 32px;
  font-size: 14px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta,
.reply-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-author,
.reply-author {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.comment-time,
.reply-time {
  font-size: 12px;
  color: #9ca3af;
}

.comment-content,
.reply-content {
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  word-wrap: break-word;
}

.comment-content :deep(.mention-tag),
.reply-content :deep(.mention-tag) {
  color: #667eea;
  font-weight: 600;
  cursor: pointer;
}

.comment-content :deep(.mention-tag):hover,
.reply-content :deep(.mention-tag):hover {
  text-decoration: underline;
}

.comment-actions,
.reply-actions {
  display: flex;
  gap: 16px;
  margin-top: 10px;
}

.comment-action-btn,
.reply-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  color: #6b7280;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.comment-action-btn:hover,
.reply-action-btn:hover {
  background: #f3f4f6;
  color: #667eea;
}

.comment-action-btn.active,
.reply-action-btn.active {
  color: #667eea;
  font-weight: 600;
}

.reply-btn {
  font-weight: 500;
}

.reply-box {
  margin-top: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #f9fafb;
}

.replies-list {
  margin-top: 12px;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: #f9fafb;
  border-radius: 6px;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-more button {
  padding: 10px 30px;
  background: white;
  color: #667eea;
  border: 1px solid #667eea;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.load-more button:hover:not(:disabled) {
  background: #667eea;
  color: white;
}

.load-more button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.post-content :deep(.mention-tag) {
  color: #667eea;
  font-weight: 600;
  cursor: pointer;
}

.post-content :deep(.mention-tag):hover {
  text-decoration: underline;
}
</style>

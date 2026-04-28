<template>
  <div class="drafts-container">
    <div class="drafts-header">
      <h1>草稿箱</h1>
      <button class="create-btn" @click="goToEditor">
        <span>+</span> 新建草稿
      </button>
    </div>
    
    <div v-if="loading && drafts.length === 0" class="loading">
      加载中...
    </div>
    
    <div v-else-if="drafts.length === 0" class="empty">
      <p>暂无草稿</p>
      <button class="create-draft-btn" @click="goToEditor">创建第一个草稿</button>
    </div>
    
    <div v-else class="drafts-list">
      <div
        v-for="draft in drafts"
        :key="draft.id"
        class="draft-card"
      >
        <div class="draft-header">
          <h3 class="draft-title">{{ draft.title || '无标题' }}</h3>
          <div class="draft-actions">
            <button class="action-btn edit-btn" @click="editDraft(draft)">
              编辑
            </button>
            <button class="action-btn delete-btn" @click="deleteDraft(draft.id)">
              删除
            </button>
          </div>
        </div>
        
        <p class="draft-preview">{{ getPlainTextPreview(draft.content) }}</p>
        
        <div class="draft-footer">
          <div class="draft-info">
            <span class="draft-time">{{ formatDateTime(draft.updatedAt) }}</span>
            <span v-if="draft.topics && draft.topics.length > 0" class="draft-topics">
              {{ draft.topics.map(t => `#${t}`).join(' ') }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { getDraftList, deleteDraft as deleteDraftApi } from '@/api/draft'
import { getPlainTextPreview, formatDateTime } from '@/utils/helpers'

const router = useRouter()
const appStore = useAppStore()

const drafts = ref([])
const loading = ref(false)

async function loadDrafts() {
  loading.value = true
  
  try {
    const res = await getDraftList()
    if (res.code === 200) {
      drafts.value = res.data || []
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
    appStore.showToast('加载草稿失败', 'error')
  } finally {
    loading.value = false
  }
}

function editDraft(draft) {
  router.push({
    path: '/post-editor',
    query: { draftId: draft.id }
  })
}

async function deleteDraft(draftId) {
  if (!confirm('确定要删除这个草稿吗？')) return
  
  try {
    const res = await deleteDraftApi(draftId)
    if (res.code === 200) {
      appStore.showToast('删除成功')
      drafts.value = drafts.value.filter(d => d.id !== draftId)
    }
  } catch (error) {
    console.error('删除草稿失败:', error)
    appStore.showToast('删除草稿失败', 'error')
  }
}

function goToEditor() {
  router.push('/post-editor')
}

onMounted(() => {
  loadDrafts()
})
</script>

<style scoped>
.drafts-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.drafts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.drafts-header h1 {
  font-size: 32px;
  color: #333;
}

.create-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.loading,
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.empty p {
  margin-bottom: 20px;
}

.create-draft-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.create-draft-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.drafts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.draft-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.draft-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.draft-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.draft-title {
  font-size: 18px;
  color: #333;
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.draft-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.edit-btn {
  background: #f3f4f6;
  color: #374151;
}

.edit-btn:hover {
  background: #e5e7eb;
}

.delete-btn {
  background: #fee2e2;
  color: #dc2626;
}

.delete-btn:hover {
  background: #fecaca;
}

.draft-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
}

.draft-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.draft-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.draft-time {
  color: #999;
}

.draft-topics {
  color: #667eea;
  font-weight: 500;
}
</style>

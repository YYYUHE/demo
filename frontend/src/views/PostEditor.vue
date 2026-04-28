<template>
  <div class="editor-container">
    <div class="editor-header">
      <button class="back-btn" @click="goBack">
        ← 返回
      </button>
      <h1>{{ isEdit ? '编辑帖子' : '创建帖子' }}</h1>
      <div class="header-actions">
        <button class="save-draft-btn" @click="saveDraft" :disabled="saving">
          {{ saving ? '保存中...' : '保存草稿' }}
        </button>
        <button class="publish-btn" @click="publish" :disabled="publishing">
          {{ publishing ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>
    
    <div class="editor-content">
      <div class="form-group">
        <input
          v-model="post.title"
          type="text"
          placeholder="请输入标题"
          class="title-input"
        />
      </div>
      
      <div class="form-group">
        <div class="editor-toolbar">
          <div class="topics-section">
            <span class="topics-label">话题：</span>
            <div class="topics-input">
              <span
                v-for="(topic, index) in post.topics"
                :key="index"
                class="topic-chip"
              >
                #{{ topic }}
                <span class="remove-topic" @click="removeTopic(index)">×</span>
              </span>
              <input
                v-model="topicInput"
                type="text"
                placeholder="添加话题（按回车）"
                @keyup.enter="addTopic"
                class="topic-input-field"
              />
            </div>
          </div>
          <button class="mention-btn" @click="showMentionSelector" title="@提及某人">
            @ 提及
          </button>
        </div>
      </div>
      
      <div class="form-group">
        <div class="editor-wrapper">
          <Toolbar
            :editor="editorRef"
            :defaultConfig="toolbarConfig"
            :mode="mode"
            class="editor-toolbar"
          />
          <Editor
            :defaultConfig="editorConfig"
            :mode="mode"
            v-model="post.content"
            class="editor-body"
            @onCreated="handleCreated"
            @onChange="handleChange"
          />
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
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef, onBeforeUnmount, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { useAppStore } from '@/stores/app'
import { publishPost } from '@/api/post'
import { saveDraft as saveDraftApi } from '@/api/draft'
import { getTopicSuggestions, getOrCreateTopic, incrementTopicUsage } from '@/api/topic'
import MentionPopup from '@/components/MentionPopup.vue'

import '@wangeditor/editor/dist/css/style.css'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const editorRef = shallowRef()
const mode = ref('default')
const isEdit = ref(false)
const saving = ref(false)
const publishing = ref(false)
const topicInput = ref('')

// @功能相关
const showMentionPopup = ref(false)
const mentionPopupPosition = ref({ top: 0, left: 0 })

const post = ref({
  id: null,
  title: '',
  content: '',
  topics: []
})

const toolbarConfig = {
  excludeKeys: ['group-video']
}

const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        try {
          const formData = new FormData()
          formData.append('file', file)
          
          const response = await fetch('/api/upload/image', {
            method: 'POST',
            credentials: 'include',
            body: formData
          })
          
          const result = await response.json()
          
          if (result.code === 200) {
            insertFn(result.data.url, result.data.alt, result.data.href)
          } else {
            appStore.showToast('图片上传失败', 'error')
          }
        } catch (error) {
          console.error('图片上传失败:', error)
          appStore.showToast('图片上传失败', 'error')
        }
      }
    }
  },
  handleKeyDown(editor, event) {
    if (event.key === '@' || event.key === '2' && event.shiftKey) {
      nextTick(() => {
        showMentionSelector()
      })
    }
  }
}

function handleCreated(editor) {
  editorRef.value = editor
}

function handleChange(editor) {
  post.value.content = editor.getHtml()
}

async function addTopic() {
  const topic = topicInput.value.trim()
  if (!topic) return
  
  if (post.value.topics.includes(topic)) {
    appStore.showToast('话题已存在', 'error')
    return
  }
  
  if (post.value.topics.length >= 5) {
    appStore.showToast('最多添加5个话题', 'error')
    return
  }
  
  try {
    const res = await getOrCreateTopic({ name: topic })
    if (res.code === 200) {
      post.value.topics.push(topic)
      topicInput.value = ''
    }
  } catch (error) {
    console.error('添加话题失败:', error)
    appStore.showToast('添加话题失败', 'error')
  }
}

function removeTopic(index) {
  post.value.topics.splice(index, 1)
}

// @功能相关方法
function showMentionSelector() {
  // 获取编辑器光标位置
  const editor = editorRef.value
  if (!editor) return
  
  // 计算弹窗位置（在工具栏附近）
  const toolbarElement = document.querySelector('.editor-toolbar')
  if (toolbarElement) {
    const rect = toolbarElement.getBoundingClientRect()
    mentionPopupPosition.value = {
      top: rect.bottom + 10,
      left: rect.left + rect.width - 340 // 靠右对齐
    }
  } else {
    mentionPopupPosition.value = {
      top: 200,
      left: window.innerWidth / 2 - 160
    }
  }
  
  showMentionPopup.value = true
}

function closeMentionPopup() {
  showMentionPopup.value = false
}

function handleMentionSelect(user) {
  const editor = editorRef.value
  if (!editor) return
  
  // 插入 @[uid:xxx] 标记
  const mentionText = `@[uid:${user.id}]`
  editor.insertText(mentionText + ' ')
  
  closeMentionPopup()
}

async function saveDraft() {
  if (!post.value.title.trim()) {
    appStore.showToast('请输入标题', 'error')
    return
  }
  
  saving.value = true
  
  try {
    const res = await saveDraftApi({
      title: post.value.title,
      content: post.value.content,
      topics: post.value.topics
    })
    
    if (res.code === 200) {
      appStore.showToast('草稿保存成功')
      router.push('/drafts')
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    appStore.showToast('保存草稿失败', 'error')
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!post.value.title.trim()) {
    appStore.showToast('请输入标题', 'error')
    return
  }
  
  if (!post.value.content.trim()) {
    appStore.showToast('请输入内容', 'error')
    return
  }
  
  publishing.value = true
  
  try {
    const res = await publishPost({
      title: post.value.title,
      content: post.value.content,
      topics: post.value.topics
    })
    
    if (res.code === 200) {
      appStore.showToast('发布成功')
      
      for (const topic of post.value.topics) {
        try {
          await incrementTopicUsage(topic)
        } catch (error) {
          console.error('更新话题使用次数失败:', error)
        }
      }
      
      router.push('/posts')
    }
  } catch (error) {
    console.error('发布失败:', error)
    appStore.showToast('发布失败', 'error')
  } finally {
    publishing.value = false
  }
}

function goBack() {
  router.back()
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

onMounted(() => {
  const draftId = route.query.draftId
  if (draftId) {
    isEdit.value = true
  }
})
</script>

<style scoped>
.editor-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.back-btn {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #374151;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.back-btn:hover {
  background: #e5e7eb;
}

.editor-header h1 {
  font-size: 24px;
  color: #111827;
  flex: 1;
  text-align: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.save-draft-btn,
.publish-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.save-draft-btn {
  background: #f3f4f6;
  color: #374151;
}

.save-draft-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.publish-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.save-draft-btn:disabled,
.publish-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.editor-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title-input {
  padding: 16px;
  font-size: 24px;
  font-weight: 600;
  border: none;
  border-bottom: 2px solid #e5e7eb;
  outline: none;
  transition: border-color 0.3s;
}

.title-input:focus {
  border-color: #667eea;
}

.editor-toolbar {
  border: 1px solid #e5e7eb;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
}

.topics-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
  flex: 1;
}

.mention-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.mention-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.topics-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.topics-input {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #667eea;
  color: white;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

.remove-topic {
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.remove-topic:hover {
  opacity: 1;
}

.topic-input-field {
  flex: 1;
  min-width: 150px;
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 16px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.3s;
}

.topic-input-field:focus {
  border-color: #667eea;
}

.editor-wrapper {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.editor-body {
  min-height: 400px;
  border: none;
}
</style>

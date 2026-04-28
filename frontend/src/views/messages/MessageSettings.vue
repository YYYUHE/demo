<template>
  <div class="message-settings">
    <!-- 内容头部 -->
    <div class="content-header">
      <h2>消息设置</h2>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 设置内容 -->
    <div v-else class="settings-content">
      <div class="settings-section">
        <h3 class="section-title">通知方式</h3>
        
        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">邮件通知</div>
            <div class="setting-desc">有新消息时发送邮件通知</div>
          </div>
          <el-switch
            v-model="settings.emailNotification"
            @change="handleSettingChange"
          />
        </div>

        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">推送通知</div>
            <div class="setting-desc">在浏览器中显示推送通知</div>
          </div>
          <el-switch
            v-model="settings.pushNotification"
            @change="handleSettingChange"
          />
        </div>
      </div>

      <div class="settings-section">
        <h3 class="section-title">通知类型</h3>
        
        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">回复通知</div>
            <div class="setting-desc">有人回复我的评论时通知</div>
          </div>
          <el-switch
            v-model="settings.replyNotification"
            @change="handleSettingChange"
          />
        </div>

        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">点赞通知</div>
            <div class="setting-desc">有人点赞我的内容时通知</div>
          </div>
          <el-switch
            v-model="settings.likeNotification"
            @change="handleSettingChange"
          />
        </div>

        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">@提醒通知</div>
            <div class="setting-desc">有人@我时通知</div>
          </div>
          <el-switch
            v-model="settings.mentionNotification"
            @change="handleSettingChange"
          />
        </div>

        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">系统通知</div>
            <div class="setting-desc">接收系统重要通知</div>
          </div>
          <el-switch
            v-model="settings.systemNotification"
            @change="handleSettingChange"
          />
        </div>
      </div>

      <div class="settings-section">
        <h3 class="section-title">其他设置</h3>
        
        <div class="settings-item">
          <div class="setting-info">
            <div class="setting-label">清空所有消息</div>
            <div class="setting-desc">删除所有类型的消息记录</div>
          </div>
          <el-button type="danger" plain @click="handleClearAll">
            清空
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useAppStore } from '@/stores/app'

const messageStore = useMessageStore()
const appStore = useAppStore()
const loading = ref(false)
const settings = ref({ ...messageStore.settings })

// 加载设置
const loadSettings = async () => {
  loading.value = true
  try {
    await messageStore.fetchSettings()
    settings.value = { ...messageStore.settings }
  } catch (error) {
    appStore.showToast('加载设置失败', 'error')
  } finally {
    loading.value = false
  }
}

// 设置变更
const handleSettingChange = async () => {
  try {
    await messageStore.updateSettings(settings.value)
    appStore.showToast('设置已保存')
  } catch (error) {
    appStore.showToast('保存设置失败', 'error')
    // 恢复原设置
    settings.value = { ...messageStore.settings }
  }
}

// 清空所有消息
const handleClearAll = async () => {
  if (confirm('确定要清空所有消息吗？此操作不可恢复！')) {
    try {
      messageStore.clearCache()
      appStore.showToast('已清空所有消息')
    } catch (error) {
      appStore.showToast('操作失败', 'error')
    }
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.message-settings {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.content-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.content-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 设置内容 */
.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.settings-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #e4e7ed;
}

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.settings-item:hover {
  background: #f5f7fa;
}

.setting-info {
  flex: 1;
}

.setting-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.setting-desc {
  font-size: 12px;
  color: #909399;
}
</style>

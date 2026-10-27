<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { RobotOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { analyzePicture, type PictureAnalysisResult } from '@/api/pictureAi'

const props = defineProps<{ picture?: API.PictureVO }>()
const emit = defineEmits<{ apply: [result: PictureAnalysisResult] }>()
const loading = ref(false)
const open = ref(false)
const suggestion = ref<PictureAnalysisResult>()
const errorMessage = ref('')
let revision = 0

// 同一图片 ID 也可能被重新上传或裁剪，不能仅用 ID 判断结果是否仍有效。
watch(
  () => props.picture,
  () => {
    revision++
    open.value = false
    suggestion.value = undefined
    errorMessage.value = ''
  },
  { deep: true, flush: 'sync' },
)

onBeforeUnmount(() => {
  revision++
})

async function analyze() {
  const pictureId = props.picture?.id
  if (!pictureId || loading.value) return
  const currentRevision = revision
  loading.value = true
  errorMessage.value = ''
  suggestion.value = undefined
  try {
    const { data } = await analyzePicture(pictureId)
    if (currentRevision !== revision) return
    if (data.code !== 0 || !data.data) {
      errorMessage.value = data.message || '分析失败，请稍后重试'
      return
    }
    suggestion.value = { title: data.data.title, tags: [...data.data.tags] }
    open.value = true
  } catch {
    if (currentRevision !== revision) return
    errorMessage.value = '分析请求未完成，请检查网络或稍后重试。原有内容已保留。'
  } finally {
    loading.value = false
  }
}

function apply() {
  if (!suggestion.value || !props.picture?.id) return
  emit('apply', { title: suggestion.value.title, tags: [...suggestion.value.tags] })
  open.value = false
  suggestion.value = undefined
  message.success('已填入名称和标签，可继续修改，点击保存后生效')
}
</script>

<template>
  <div class="ai-analysis">
    <div class="ai-analysis-actions">
      <a-button :loading="loading" :disabled="!picture?.id" @click="analyze">
        <template #icon><RobotOutlined /></template>
        {{ loading ? 'AI 正在分析…' : 'AI 智能分析' }}
      </a-button>
      <span class="ai-analysis-hint">生成名称和标签，确认后填入</span>
    </div>
    <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
    <a-modal
      v-model:open="open"
      title="确认 AI 分析结果"
      ok-text="确认填入"
      cancel-text="取消"
      :mask-closable="false"
      @ok="apply"
    >
      <div v-if="suggestion" class="ai-suggestion">
        <a-alert
          type="info"
          show-icon
          message="确认后将替换表单中的名称和标签。你可以继续修改，点击保存后才会生效。"
        />
        <div>
          <h4>建议名称</h4>
          <p class="ai-suggestion-title">{{ suggestion.title }}</p>
        </div>
        <div>
          <h4>建议标签</h4>
          <div class="ai-suggestion-tags">
            <a-tag v-for="tag in suggestion.tags" :key="tag" color="blue">{{ tag }}</a-tag>
          </div>
        </div>
        <p class="ai-analysis-hint">AI 生成内容可能有误，请检查后使用。</p>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.ai-analysis,
.ai-suggestion {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ai-analysis {
  margin: 20px 0;
}
.ai-analysis-actions,
.ai-suggestion-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.ai-analysis-hint {
  color: #666;
  font-size: 13px;
  margin: 0;
}
.ai-suggestion h4 {
  margin-bottom: 8px;
}
.ai-suggestion-title {
  overflow-wrap: anywhere;
  margin: 0;
}
</style>

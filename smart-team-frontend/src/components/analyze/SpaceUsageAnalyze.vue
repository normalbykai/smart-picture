<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceUsageAnalyzeResult } from '@/api/spaceAnalyzeController.ts'
import { formatSize } from '../../utils'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string | number | undefined
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

// 图标数据加载状态
const loading = ref<boolean>(false)

// 图表数据
const data = ref<API.SpaceUsageAnalyzeResponse>()

// 获取图表数据
const fetchData = async () => {
  loading.value = true

  // 请求参数
  const requestParams = {
    spaceId: props.spaceId ? Number(props.spaceId) : undefined,
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
  }

  // 响应处理
  const res = await getSpaceUsageAnalyzeResult(requestParams)
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data
  } else {
    message.error(`获取数据失败,${res.data.message}`)
  }
  loading.value = false
}

watchEffect(() => {
  fetchData()
})
</script>

<template>
  <div id="spaceUsageAnalyze">
    <a-flex gap="middle" class="space-Usage-analyze" >
      <a-card title="存储空间" style="width: 50%;">
        <div style="height: 350px; text-align: center;">
          <h3>
            {{ formatSize(data?.usedSize) }} /
            {{ data?.maxSize ? formatSize(data.maxSize) : '无限制' }}
          </h3>
          <a-progress type="dashboard" :percent="data?.sizeUsageRatio ?? 0" />
        </div>
      </a-card>

      <a-card title="图片数量" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>
            {{ data?.usedCount ?? '未知' }}/ {{ data?.maxCount ? data.maxCount : '无限制' }}
          </h3>
          <a-progress type="dashboard" :percent="data?.countUsageRatio ?? 0" />
        </div>
      </a-card>
    </a-flex>
  </div>
</template>

<style scoped>
h3{
  font-size: 1.5rem;
}
</style>

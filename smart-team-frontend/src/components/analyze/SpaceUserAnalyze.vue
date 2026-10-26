<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceUserAnalyzeResult } from '@/api/spaceAnalyzeController.ts'

import VChart from 'vue-echarts'
import * as echarts from 'echarts'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string | number
  userId?: string
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

// 图表数据
const data = ref<API.SpaceUserAnalyzeResponse[]>([])

// 图表配置项
const options = computed(() => {
  const categories = data.value.map(item => item.period)
  const countData = data.value.map(item => item.count)

  return {
    tooltip: {
      trigger: 'item',
      formatter: "{b} <br/> 上传 <span style='color: pink'>{c}</span> 张图片",
    },
    xAxis: {
      type: 'category',
      data: categories
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        data: countData,
        type: 'line',
        smooth: true,
      }
    ]
  };
})

// 图标数据加载状态
const loading = ref<boolean>(false)

// 时间维度 日 周 月
const timeDimension = ref<'day' | 'week' | 'month'>('day')
const timeDimensionOption = [
  {
    value: 'day',
    label: '日',
  },
  {
    value: 'week',
    label: '周'
  },
  {
    value: 'month',
    label: '月',
  }
]
// 用户id
const userId = ref<string>()

// 获取图表数据
const fetchData = async () => {
  loading.value = true
  // 请求参数
  const requestParams = {
    spaceId: props.spaceId ? Number(props.spaceId) : undefined,
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    timeDimension: timeDimension.value,
    userId: userId.value,
  }
  // 响应处理
  const res = await getSpaceUserAnalyzeResult(requestParams)
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data
  } else {
    message.error(`获取数据失败,${res.data.message}`)
  }
  loading.value = false
}

/**
 * 用户id搜索执行
 */
const onSearch = (value: string) => {
  userId.value = value
}

/**
 * 当响应数据修改就会执行
 */
watchEffect(() => {
  fetchData()
})
</script>

<template>
  <div id="spaceUserAnalyze" class="chart-container">
    <a-card title="用户图片上传分析">
      <!-- 时间维度选择 -->
      <template #extra>
        <a-flex justify="right">
          <a-space :size="20">
            <a-segmented v-model:value="timeDimension" :options="timeDimensionOption" />
            <!-- 用户id 搜索框-->
            <a-input-search placeholder="请输入用户id" @search="onSearch"/>
          </a-space>
        </a-flex>
      </template>

      <v-chart
        style="max-width: 100%; height: 350px"
        :option="options"
        :loading="loading"
        autoresize
      />
    </a-card>
  </div>
</template>

<style scoped></style>

<script setup lang="ts">

import {computed, ref, watchEffect} from "vue";
import {message} from "ant-design-vue";
import {
  getSpaceRankAnalyzeResult
} from "@/api/spaceAnalyzeController.ts";



import 'echarts';
import VChart from 'vue-echarts';
import * as echarts from 'echarts';



interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})


// 图表数据
const data = ref<API.Space[]>([])

// 图表配置项
const options = computed(() => {
  const categories = data.value.map(item => item.spaceName)
  const spaceUsedSize = data.value.map(item => ((item.totalSize ?? 0) / 1024 / 1024).toFixed(2))
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `使用大小:<br/> ${params.name} : ${params.value} MB`
    },
    xAxis: {
      type: 'category',
      data: categories
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        data: spaceUsedSize,
        type: 'bar',
        label: {     // 添加标签配置
          show: true, // 显示标签
          position: 'top', // 标签位置，可以是'top', 'inside', 'insideTop'等
          color: '#5470C6',
          fontWeight: 'bold',
          fontSize: 12,
          formatter: '{c} MB' // 显示数值，也可以自定义格式如: '{c}张'
        }
      }
    ]
  };


})



// 图标数据加载状态
const loading = ref<boolean>(false)


// 获取图表数据
const fetchData = async () => {
  loading.value = true
  // 请求参数
  const requestParams = {
    // 前10的数据
    topN: 10
  }

  // 响应处理
  const res = await getSpaceRankAnalyzeResult(requestParams)
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data
  } else {
    message.error(`获取数据失败,${res.data.message}`)
  }
  loading.value = false
}

/**
 * 当响应数据修改就会执行
 */
watchEffect(() => {
  fetchData()
})





</script>

<template>
  <div id="spaceRankAnalyze" class="chart-container">
      <a-card title="空间使用排名分析">
        <v-chart style="max-width: 100%; height: 350px" :option="options" :loading="loading" autoresize/>
      </a-card>

  </div>


</template>

<style scoped>

</style>

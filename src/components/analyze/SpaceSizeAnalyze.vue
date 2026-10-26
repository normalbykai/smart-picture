<script setup lang="ts">

import {computed, ref, watchEffect} from "vue";
import {message} from "ant-design-vue";
import {
  getSpaceCategoryAnalyzeResult, getSpaceSizeAnalyzeResult
} from "@/api/spaceAnalyzeController.ts";



import 'echarts';
import VChart from 'vue-echarts';
import * as echarts from 'echarts';



interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})


// 图表数据
const data = ref<API.SpaceSizeAnalyzeResponse[]>([])

// 图表配置项
const options = computed(() => {

  const picData = data.value.map(item => {
    return {
      name : item.sizeRange,
      value : item.count,
    }
  })
  return {
    tooltip: {
      trigger: 'item',
      formatter: "{a} <br/>{b} : {c} 张 ({d}%)",
    },
    legend: {
      orient: 'vertical',
      bottom: 'bottom',
    },
    series: [
      {
        name: '图片大小',
        type: 'pie',
        radius: '50%',
        data: picData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
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
    spaceId: props.spaceId ? Number(props.spaceId) : undefined,
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
  }

  // 响应处理
  const res = await getSpaceSizeAnalyzeResult(requestParams)
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
  <div id="spaceCategoryAnalyze" class="chart-container">
      <a-card title="图片大小分析">
        <v-chart style="max-width: 100%; height: 350px" :option="options" :loading="loading" autoresize/>
      </a-card>

  </div>


</template>

<style scoped>

</style>

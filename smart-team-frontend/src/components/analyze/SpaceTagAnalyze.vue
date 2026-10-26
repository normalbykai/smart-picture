<script setup lang="ts">

import {computed, ref, watchEffect} from "vue";
import {message} from "ant-design-vue";

import 'echarts-wordcloud';
import {
  getSpaceTagAnalyzeResult
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
const data = ref<API.SpaceTagAnalyzeResponse[]>([])


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
  const res = await getSpaceTagAnalyzeResult(requestParams)
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

const options = computed(() => {
  const tagData = data.value.map(item => {
    return {
      name: item.tag,
      value: item.count
    }
  })

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `${params.name} : ${params.value} 次`
    },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      sizeRange: [30, 100],
      rotationRange: [-90, 90],
      gridSize: 10,
      textStyle: {
        fontFamily: 'sans-serif',
        fontWeight: 'bold',
        color: function () {
          // Random color
          return 'rgb(' + [
            Math.round(Math.random() * 160),
            Math.round(Math.random() * 160),
            Math.round(Math.random() * 160)
          ].join(',') + ')';
        }
      },
      emphasis: {
        focus: 'self',
        textStyle: {
          textShadowBlur: 10,
          textShadowColor: '#333'
        }
      },
      data: tagData
    }]
  }
})



</script>

<template>
  <div id="spaceTagAnalyze" class="chart-container">
      <a-card title="图库标签词云">
        <v-chart style="max-width: 100%; height: 350px" :option="options" :loading="loading" autoresize/>
      </a-card>
  </div>


</template>

<style scoped>

</style>

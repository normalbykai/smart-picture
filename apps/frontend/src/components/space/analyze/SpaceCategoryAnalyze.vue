<script setup lang="ts">

import {computed, ref, watchEffect} from "vue";
import {message} from "ant-design-vue";
import {
  getSpaceCategoryAnalyzeResult
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
const data = ref<API.SpaceCategoryAnalyzeResponse[]>([])

// 图表配置项
const options = computed(() => {
  const categories = data.value.map(item => item.category)
  const counts = data.value.map(item => item.count)
  const sizeData = data.value.map(item => ((item.totalSize ?? 0) / 1024 / 1024).toFixed(2))

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(150, 150, 150, 0.1)'
        }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      padding: [10, 15],
      textStyle: {
        color: '#333',
        fontSize: 12
      },
      extraCssText: 'box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);',
        formatter: (params: any) => {
        // 过滤掉被隐藏的系列
        const visibleParams = params.filter((item: any) => !item.isSeriesDeleted)

        let tooltipHtml = `<div style="font-weight: bold; margin-bottom: 5px;">${visibleParams[0]?.name || ''}</div>`

        visibleParams.forEach((param: any) => {
          tooltipHtml += `
            <div style="display: flex; align-items: center; margin: 3px 0;">
              <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${param.color}; margin-right: 5px;"></span>
              ${param.seriesName}: <span style="font-weight: bold; margin-left: 5px;">${param.value} ${param.seriesName === '数量' ? '个' : 'MB'}</span>
            </div>
          `
        })

    return tooltipHtml
  }
    },
    legend: {
      data: ['数量', '大小'],
      bottom: 1,
      itemWidth: 30,
      itemHeight: 20,
      itemGap: 20,
      textStyle: {
        fontSize: 12,
        color: '#666'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      },
      axisTick: {
        alignWithLabel: true,
        lineStyle: {
          color: '#ddd'
        }
      },
      axisLabel: {
        rotate: 0,
        color: '#666',
        fontSize: 12
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '图片数量',
        minInterval: 1, // 设置最小刻度间隔为1
        nameTextStyle: {
          color: '#666',
          fontSize: 12,
          padding: [0, 0, 0, 30]
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#ddd'
          }
        },
        axisTick: {
          show: true
        },
        splitLine: {
          lineStyle: {
            color: '#f5f5f5',
            type: 'dashed'
          }
        },
        axisLabel: {
          formatter: '{value} 个',
          color: '#666',
          fontSize: 12
        }
      },
      {
        type: 'value',
        name: '图片总大小',
        nameTextStyle: {
          color: '#666',
          fontSize: 12,
          padding: [0, 30, 0, 0]
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#ddd'
          }
        },
        axisTick: {
          show: true
        },
        splitLine: {
          show: false
        },
        axisLabel: {
          formatter: '{value} MB',
          color: '#666',
          fontSize: 12
        }
      }
    ],
    series: [
      {
        name: '数量',
        type: 'bar',
        data: counts,
        barWidth: '30%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5470C6' },
            { offset: 1, color: '#3A56B4' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          color: '#5470C6',
          fontWeight: 'bold',
          fontSize: 12,
          formatter: '{c}'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(84, 112, 198, 0.5)'
          }
        }
      },
      {
        name: '大小',
        type: 'bar',
        yAxisIndex: 1,
        data: sizeData,
        barWidth: '30%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#91CC75' },
            { offset: 1, color: '#7BB55F' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          color: '#5D8B3E',
          fontWeight: 'bold',
          fontSize: 12,
          formatter: '{c} MB'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(145, 204, 117, 0.5)'
          }
        }
      }
    ]
  }
})

const optionsed = computed(() => {
  const categories = data.value.map((item) => item.category)
  const countData = data.value.map((item) => item.count)
  // totalSize 可能为空，做空值兜底
  const sizeData = data.value.map((item) => ((item.totalSize ?? 0) / (1024 * 1024)).toFixed(2)) // 转为 MB

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['图片数量', '图片总大小'], top: 'bottom' },
    xAxis: { type: 'category', data: categories },
    yAxis: [
      {
        type: 'value',
        name: '图片数量',
        axisLine: { show: true, lineStyle: { color: '#5470C6' } }, // 左轴颜色
      },
      {
        type: 'value',
        name: '图片总大小 (MB)',
        position: 'right',
        axisLine: { show: true, lineStyle: { color: '#91CC75' } }, // 右轴颜色
        splitLine: {
          lineStyle: {
            color: '#91CC75', // 调整网格线颜色
            type: 'dashed', // 线条样式：可选 'solid', 'dashed', 'dotted'
          },
        },
      },
    ],
    series: [
      { name: '图片数量', type: 'bar', data: countData, yAxisIndex: 0 },
      { name: '图片总大小', type: 'bar', data: sizeData, yAxisIndex: 1 },
    ],
  }
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
  const res = await getSpaceCategoryAnalyzeResult(requestParams)
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
      <a-card title="空间分类分析">
        <v-chart style="max-width: 100%; height: 350px" :option="options" :loading="loading" autoresize/>
      </a-card>

  </div>


</template>

<style scoped>

</style>

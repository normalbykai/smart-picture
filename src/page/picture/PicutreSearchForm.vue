<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import dayjs from "dayjs";
import {listPictureTagCategory} from "@/api/pictureController.ts";
import {message, type SelectProps} from "ant-design-vue";

interface props {
  onSearch?: (searchParams: API.PictureQueryRequest) => void
}

onMounted(()=>{
  getCategoryWithTags();
})
const props = defineProps<props>()

// 搜索条件参数
const searchParams = reactive<API.PictureQueryRequest>({})

// 获取数据参数
const doSearch = () => {
  props.onSearch?.(searchParams)
}

const rangePresets = ref([
  {
    label: '过去7天', value: [dayjs().add(-7,'d'),dayjs()]
  },
  {
    label: '过去14天', value: [dayjs().add(-14,'d'),dayjs()]
  },
  {
    label: '过去30天', value: [dayjs().add(-30,'d'),dayjs()]
  },
  {
    label: '过去90天', value: [dayjs().add(-90,'d'),dayjs()]
  }
])

const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates.length < 2) {
    searchParams.startEditTime = undefined;
    searchParams.endEditTime = undefined;
  }else {
    searchParams.startEditTime = dates[0].toDate();
    searchParams.endEditTime = dates[1].toDate();
  }
}

const dataRange = ref<[]>([])
const categoryOptions = ref<SelectProps['options']>([])
const tagsOptions = ref<SelectProps['options']>([])

// 获取分类和标签数据
const getCategoryWithTags = async () => {
  const res = await listPictureTagCategory()

  if (res.data.code === 0 && res.data.data) {
    categoryOptions.value = (res.data.data.categoryList ?? []).map((item: string) => {
      return {
        label: item,
        value: item
      }
    })

    tagsOptions.value = (res.data.data.tagList ?? []).map((item: string) => {
      return {
        label: item,
        value: item
      }
    })
  }else {
    message.error("获取分类和标签失败，"+res.data.message)
  }
}


// 搜索拦全部重置
const doClear = () => {
  Object.keys(searchParams).forEach((key: string) => {
    // 分页参数由父组件维护，这里不要清空，否则会覆盖父组件的分页配置
    if (key === 'currentNum' || key === 'pageSize') {
      return
    }
    // 动态 key 赋值在 TS 下需要显式收窄为索引签名
    ;(searchParams as Record<string, unknown>)[key] = undefined
  })
  // 清空 日期范围
  dataRange.value = []

  // 向外调用成功回调 - 查询一遍数据
  props.onSearch?.(searchParams)
}
</script>

<template>

  <div id="picture-search-form">
    <!-- 搜索表单-->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词" name="searchText">
        <a-input v-model:value="searchParams.searchText" placeholder="从名称和简介搜索" allow-clear />
      </a-form-item>

      <a-form-item label="分类" name="category">
        <a-auto-complete
          style="min-width: 200px"
          v-model:value="searchParams.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        />
      </a-form-item>

      <a-form-item label="标签" name="tags">
        <a-select
          style="min-width: 200px"
          :options="tagsOptions"
          mode="tags"
          v-model:value="searchParams.tags"
          placeholder="请输入标签"
          allow-clear
        />
      </a-form-item>

      <a-form-item label="日期" name="">
        <a-range-picker
          v-model:value="dataRange"
          style="width: 400px"
          show-time
          :placeholder="['编辑开始日期', '编辑结束日期']"
          format="YYYY/MM/DD HH:mm:ss"
          :presets="rangePresets"
          @change="onRangeChange"
        />
      </a-form-item>

      <a-form-item label="名称" name="name">
        <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear />
      </a-form-item>

      <a-form-item label="简介" name="introduction">
        <a-input v-model:value="searchParams.introduction" placeholder="请输入简介" allow-clear />
      </a-form-item>

      <a-form-item label="宽度" name="picWidth">
        <a-input-number v-model:value="searchParams.picWidth" placeholder="请输入宽度" allow-clear />
      </a-form-item>

      <a-form-item label="高度" name="picHeight">
        <a-input-number v-model:value="searchParams.picHeight" placeholder="请输入高度" allow-clear />
      </a-form-item>

      <a-form-item label="格式" name="format">
        <a-input v-model:value="searchParams.picFormat" placeholder="请输入个格式" allow-clear />
      </a-form-item>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 200px;">搜索</a-button>
      </a-form-item>

      <a-form-item>
        <a-button html-type="reset" style="width: 200px;" @click="doClear">重置</a-button>
      </a-form-item>
    </a-form>
  </div>

</template>

<style scoped>
#picture-search-form{

}

#picture-search-form .ant-form-item{
  margin-top: 10px;
}

</style>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getPictureVoByPage, listPictureTagCategory } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import ImageList from './picture/components/ImageList.vue'

const route = useRoute()

// 图片列表数据
const dataList = ref<API.PictureVO[]>([])
// 加载状态
const loading = ref(true)
// 图片总数
const total = ref(0)

// 分页与排序参数
const searchParams = reactive<API.PictureQueryRequest>({
  currentNum: 1,
  pageSize: 24,
  sortField: 'createTime',
  sortOrder: 'desc',
})

onMounted(() => {
  initHomePage()
  fetchData()
  getCategoryTags()
})

/**
 * 从 URL 参数恢复筛选状态，便于分享/刷新后保持条件
 */
const initHomePage = () => {
  const category = route.query.category
  if (typeof category === 'string' && category) {
    selectedCategory.value = category
  }
  // 标签以英文逗号分隔存储在 URL 中
  const tags = route.query.tags
  if (typeof tags === 'string' && tags) {
    selectedTagList.value = tags.split(',').filter(Boolean)
  }
}

// 分类列表
const categoryList = ref<string[]>([])
// 当前选中的分类，all 表示全部
const selectedCategory = ref<string>('all')
// 标签列表
const tagsList = ref<string[]>([])
// 当前选中的标签（按索引与 tagsList 对应）
const selectedTagList = ref<string[]>([])

/**
 * 分页配置
 */
const pagination = computed(() => {
  return {
    current: searchParams.currentNum,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (value: number) => `总${value}条`,
    onChange: (page: number, pageSize: number) => {
      searchParams.currentNum = page
      searchParams.pageSize = pageSize
      fetchData()
    },
  }
})

/**
 * 触发搜索：重置到第一页
 */
const doSearch = async () => {
  searchParams.currentNum = 1
  await fetchData()
}

/**
 * 获取分类与标签
 */
const getCategoryTags = async () => {
  const res = await listPictureTagCategory()
  if (res.data.code === 0 && res.data.data) {
    categoryList.value = res.data.data.categoryList ?? []
    tagsList.value = res.data.data.tagList ?? []
  } else {
    message.error(res.data.message)
  }
}

/**
 * 获取图片列表
 */
const fetchData = async () => {
  try {
    loading.value = true
    // 1. 组装查询条件
    const queryParams: API.PictureQueryRequest = {
      ...searchParams,
      tags: [],
    }
    if (selectedCategory.value !== 'all') {
      queryParams.category = selectedCategory.value
    }
    // 2. 把勾选的标签转换为标签值
    selectedTagList.value.forEach((checked, index) => {
      const tag = tagsList.value?.[index]
      if (checked && tag) {
        queryParams.tags?.push(tag)
      }
    })

    // 3. 请求数据
    const res = await getPictureVoByPage(queryParams)
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error(`获取图片展示集合失败: ${res.data.message}`)
    }
  } catch (error: any) {
    message.error('网络问题:' + (error?.message ?? error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!--  搜索框  -->
  <div class="search-bar">
    <a-input-search
      v-model:value="searchParams.searchText"
      placeholder="从海量图片中搜索"
      enter-button="搜索"
      size="large"
      @search="doSearch"
    />
  </div>

  <a-tabs v-model:activeKey="selectedCategory" @change="doSearch">
    <a-tab-pane key="all" tab="全部" />
    <a-tab-pane v-for="item in categoryList" :key="item" :tab="item" />
  </a-tabs>

  <div class="tag-bar">
    <span>标签：</span>
    <a-space :size="[0, 8]" wrap>
      <a-checkable-tag
        v-for="(tag, index) in tagsList"
        :key="tag"
        v-model:checked="selectedTagList[index]"
        @change="doSearch"
      >
        {{ tag }}
      </a-checkable-tag>
    </a-space>
  </div>

  <ImageList :data-list="dataList" />

  <a-pagination
    style="text-align: right"
    v-model:current="searchParams.currentNum"
    v-model:page-size="searchParams.pageSize"
    :total="total"
    :show-total="pagination.showTotal"
    @change="pagination.onChange"
  />
</template>

<style scoped>
.search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}
.tag-bar {
  margin-bottom: 16px;
}

.img-card {
  object-fit: cover;
  object-position: center;
  transition: transform 0.3s ease;
  /* 提高图片渲染质量 */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
  -webkit-font-smoothing: antialiased;
  backface-visibility: hidden;
  transform: translateZ(0);
  width: 100%;
}
</style>

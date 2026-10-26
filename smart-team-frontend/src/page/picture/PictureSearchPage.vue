<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {getPictureVoById, imageSearch} from '@/api/pictureController.ts'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'

const route = useRoute()
// 图片详情使用 PictureVO（tags 为数组，与 Picture 实体不同）
const picture = ref<API.PictureVO>()
// URL query 是字符串，统一转换为数字
const pictureId = computed(() => Number(route.query?.pictureId ?? 0))
const loading = ref<boolean>(false)

const fetchPictureData = async () => {
  const res = await getPictureVoById({ id: pictureId.value })

  if (res.data.code == 0 && res.data.data) {
    picture.value = res.data.data
  } else {
    message.error('获取图片数据错误,' + res.data.message)
  }
}

const searchImageData = ref<API.ImageSearchResult[]>()
// 获取识图数据
const getSearchImages = async () => {
  loading.value = true
  const res = await imageSearch({ pictureId: pictureId.value })
  if (res.data.code == 0 && res.data.data) {
    searchImageData.value = res.data.data
  } else {
    message.error('获取识图数据错误,' + res.data.message)
  }
  loading.value = false
}

onMounted(() => {
  // 渲染图片数据
  fetchPictureData()
  // 获取识图数据
  getSearchImages()
})

const skipForm = (item: API.ImageSearchResult) => {
  if (item.fromUrl) {
    window.open(item.fromUrl, '_blank')
  }
}
</script>

<template>
  <div class="picture-search-container">
    <div class="original-image-section">
      <h2 class="section-title">原图信息</h2>
      <div class="original-image-wrapper">
        <a-image
          :width="300"
          :height="300"
          :src="picture?.url"
          class="original-image"
        >
          <template #placeholder>
            <a-image :width="300" :src="picture?.originUrl" :preview="false" />
          </template>
        </a-image>
      </div>
    </div>

    <div class="search-results-section">
      <h2 class="section-title">识图结果</h2>
      <a-list
        :grid="{ gutter: 24, xs: 1, sm: 2, md: 3, lg: 3, xl: 4, xxl: 4 }"
        :data-source="searchImageData"
        :loading="loading"
        size="middle"
      >
        <template #renderItem="{ item }: {item: API.ImageSearchResult}">
          <a-list-item>
            <a-card
              hoverable
              @click="skipForm(item)"
              class="result-card"
            >
              <template #cover>
                <img
                  v-lazy="item.thumbUrl"
                  alt="item.name"
                  class="result-image"
                />
              </template>
            </a-card>
          </a-list-item>
        </template>
      </a-list>
    </div>
  </div>
</template>

<style scoped>
.picture-search-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-size: 24px;
  font-weight: 500;
  color: #1f1f1f;
  margin-bottom: 24px;
  position: relative;
  padding-left: 12px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background-color: #1890ff;
  border-radius: 2px;
}

.original-image-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  margin-bottom: 32px;
}

.original-image-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 16px;
}

.original-image {
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.original-image:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.original-image :deep(.ant-image-img) {
  object-fit: cover;
  border-radius: 4px;
}

.search-results-section {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.result-card {
  transition: all 0.3s ease;
}

.result-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.result-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}
</style>

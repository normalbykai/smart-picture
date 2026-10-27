<script setup lang="ts">
import router from '@/router';
import { VirtualWaterfall} from '@lhlyu/vue-virtual-waterfall'
import { Tooltip as ATooltip } from 'ant-design-vue';
interface PropsType {
  dataList?: API.PictureVO[]
}
const props = withDefaults(defineProps<PropsType>(), {
  dataList: () => [] // 默认为空
})

const INFO_HEIGHT = 100;

const handleItemHeight = (item: API.PictureVO, itemWidth : number) => {
  if (item.picWidth && item.picHeight) {
    const imgHeight = itemWidth * (item.picHeight / item.picWidth);
    return imgHeight + INFO_HEIGHT;
  }
  return 220 + INFO_HEIGHT;
}
</script>


<template>
  <VirtualWaterfall :items="dataList" :virtual="false" :maxColumnCount="10" :min-column-count="2" :itemMinWidth="250" :gap="14" :calc-item-height="handleItemHeight" >
    <template #default="{ item }: { item: API.PictureVO }">
      <div class="custom-card" @click="router.push(`/picture/${item.id}`)">
        <div class="img-wrapper">
          <!-- 列表优先使用缩略图，避免首页加载原图浪费带宽 -->
          <img :src="item.thumbnailUrl ?? item.url" :alt="item.name" />
        </div>
        <div class="card-info">
          <div class="tags-row" v-if="(item?.tags?.length ?? 0) > 0">
            <span v-for="tag in (item.tags ? item.tags.slice(0, 3) : [])" :key="tag" class="img-tag">{{ tag }}</span>
            <a-tooltip v-if="item.tags && item.tags.length > 3" :title="item.tags.join('、')">
              <span class="img-tag more-tag">+{{ item.tags.length - 3 }}</span>
            </a-tooltip>
          </div>
          <div class="title-row">{{ item.name }}</div>
          <div class="author-row">
            <img class="avatar" :src="item.userVO?.userAvatar || ''" alt="avatar" />
            <span class="author-name">{{ item.userVO?.userName || '未知' }}</span>
          </div>
        </div>
      </div>
    </template>
  </VirtualWaterfall>
</template>

<style scoped>
.custom-card {
  cursor: pointer;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s;
}
.custom-card:hover {
  transform: translateY(-6px) scale(1.025);
}
.img-wrapper {
  width: 100%;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.img-wrapper img {
  width: 100%;
  height: auto;
  display: block;
  object-fit: contain;
  transition: transform 0.35s cubic-bezier(.4,2,.6,1), filter 0.25s;
}
.custom-card:hover .img-wrapper img {
  transform: scale(1.06);
  filter: brightness(0.97) saturate(1.1);
}
.card-info {
  box-sizing: border-box;
  height: 100px;
  padding: 12px 14px 10px 14px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.custom-card:hover .card-info {
  background: #f6faff;
}
.title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s;
}
.custom-card:hover .title {
  color: #1890ff;
}
.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.author {
  display: flex;
  align-items: center;
}
.avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #f0f0f0;
}
.author-name {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}
.like {
  display: flex;
  align-items: center;
  color: #e25555;
  font-size: 1em;
}
.icon {
  margin-right: 4px;
}
.tags {
  margin-bottom: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.img-tag {
  display: inline-block;
  background: #f6faff;
  color: #1890ff;
  border-radius: 8px;
  font-size: 12px;
  padding: 2px 10px;
  margin-right: 2px;
  margin-bottom: 2px;
  border: 1px solid #e0eaff;
}
.more-tag {
  background: #e6f4ff;
  color: #1677ff;
  border: 1px solid #b6d8ff;
  font-weight: bold;
}
.tags-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  min-height: 24px;
}
.img-tag, .more-tag {
  display: inline-block;
  background: #f6faff;
  color: #1890ff;
  border-radius: 8px;
  font-size: 12px;
  padding: 2px 10px;
  border: 1px solid #e0eaff;
  line-height: 20px;
  max-width: 80px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.more-tag {
  background: #e6f4ff;
  color: #1677ff;
  border: 1px solid #b6d8ff;
  font-weight: bold;
}
.title-row {
  font-size: 1.08rem;
  font-weight: 600;
  color: #222;
  margin: 0 0 4px 0;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.author-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}
</style>

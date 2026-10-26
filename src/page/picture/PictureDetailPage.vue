<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { deletePicture, getPictureVoById, updatePicture } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { downloadImage, formatSize, toHexColor } from '../../utils'
import router from '@/router'
import ShareModal from '@/components/ShareModal.vue'
import {SPACE_PERMISSION_ENUM} from "@/constants/space.ts";

const props = defineProps<{ id: string }>()

onMounted(() => {
  fetchPictureDetail()
})
const picture = ref<API.PictureVO>({})

const fetchPictureDetail = async () => {
  const res = await getPictureVoById({ id: Number(props.id)})
  if (res.data.code === 0 && res.data.data) {
    picture.value = res.data.data
  } else {
    message.error(`获取图片详情失败，${res.data.message}`)
  }
}

const doEdit = async () => {
  await router.push(`/add_picture?id=${picture.value.id}`)
}

const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const res = await deletePicture({ id })
  if (res.data.code === 0 && res.data.data) {
    message.success('删除成功')
    await router.push('/')
  } else {
    message.error('删除失败')
  }
}

const doDownload = () => {
  downloadImage(picture.value.url as string)
}

const shareLink = ref<string>()
const shareModalRef = ref()
/**
 * 分享操作
 */
const doShare = () => {
  // 构建分享链接
  shareLink.value = `${window.location.protocol}://${window.location.host}/picture/${picture.value.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}


const createPermissionChecker  = (permission: string) => {
  return computed(() =>{
    return (picture.value.permissions ?? []).includes(permission)
  })
}
// 定义权限检查
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
</script>

<template>
  <div id="PictureDetailPage">
    <a-row :gutter="[16, 16]">
      <a-col :sm="24" :md="16" :xl="18" class="imgBox">
        <a-card title="图片预览">
          <a-image style="max-height: 600px; object-fit: contain" :src="picture?.url" />
        </a-card>
      </a-col>

      <a-col :sm="24" :md="8" :xl="6">
        <a-card title="图片信息">
          <a-descriptions label="作者" :column="1">
            <a-descriptions-item>
              <a-space>
                <a-avatar :size="24" :src="picture?.userVO?.userAvatar"></a-avatar>
                <div>{{ picture?.userVO?.userName }}</div>
              </a-space>
            </a-descriptions-item>

            <a-descriptions-item label="名称">
              {{ picture?.name ?? '未命名' }}
            </a-descriptions-item>

            <a-descriptions-item label="简介">
              {{ picture?.introduction ?? '-' }}
            </a-descriptions-item>

            <a-descriptions-item label="分类">
              {{ picture?.category ?? '默认' }}
            </a-descriptions-item>

            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture?.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>

            <a-descriptions-item label="格式">
              {{ picture?.picFormat ?? '-' }}
            </a-descriptions-item>

            <a-descriptions-item label="宽度">
              {{ picture?.picWidth ?? '-' }}
            </a-descriptions-item>

            <a-descriptions-item label="高度">
              {{ picture?.picHeight ?? '-' }}
            </a-descriptions-item>

            <a-descriptions-item label="宽高比">
              {{ picture?.picScale ?? '-' }}
            </a-descriptions-item>

            <a-descriptions-item label="大小">
              {{ formatSize(picture?.picSize) }}
            </a-descriptions-item>

            <a-descriptions-item label="主色调">
              <a-flex>

                  <!-- 主色调颜色展示 -->
                  <div
                    v-if="picture.mainColor"
                    :style="{
                    backgroundColor: toHexColor(picture.mainColor as string),
                    width: '32px'
                  }">
                  </div>
                  <div style="margin-left: 8px">

                    <a-typography-paragraph copyable>{{ toHexColor(picture.mainColor) || '-' }}</a-typography-paragraph>

                  </div>
              </a-flex>
            </a-descriptions-item>
          </a-descriptions>

          <a-space wrap>
            <a-button type="primary" @click="doDownload">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>

            <a-button v-if="canEditPicture" type="default" @click="doEdit">
              编辑
              <template #icon>
                <EditOutlined />
              </template>
            </a-button>

            <a-button type="default" @click="doShare">
              分享
              <template #icon>
                <ShareAltOutlined />
              </template>
            </a-button>

            <a-button v-if="canDelete" danger @click="doDelete">
              删除
              <template #icon>
                <DeleteOutlined />
              </template>
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <ShareModal ref="shareModalRef" :link="shareLink"></ShareModal>
  </div>
</template>

<style scoped>
#PictureUpload {
}

.imgBox :deep(.ant-card-body) {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
</style>

<script setup lang="ts">
import {computed, reactive, ref, h} from 'vue'
import { message, type UploadProps } from 'ant-design-vue'
import {PlusOutlined, LoadingOutlined, EditOutlined} from '@ant-design/icons-vue'
import { uploadPicture } from '@/api/pictureController.ts'

import {useRoute} from "vue-router";
interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const route = useRoute()

const props = defineProps<Props>()

// 文件列表
const fileList = ref([])

// 在上传之前 做一些文件校验 返回值为boolean ture为 才会触发 handleRequest方法发送请求
const beforeUpload = (file: any) => {
  let fileTYpe = file.type
  let isJpegOrPng = fileTYpe === 'image/jpeg' || fileTYpe === 'image/png'
  // 1. 格式正确
  if (!isJpegOrPng) {
    message.error('不支持该文件上传，推荐使用JPEG 或 PNG格式')
    return false
  }

  // 2. 图片小于2M
  const isLessThan2M = file.size / 1024 / 1024 < 2
  if (!isLessThan2M) {
    message.error('不能上传超过 2M 大小的图片')
    return false
  }

  // 都满足校验才返回ture
  return isJpegOrPng && isLessThan2M
}

const spaceId = computed<number | undefined>(() => {
  const value = route.query?.spaceId
  return value ? Number(value) : undefined
})
// 请求后端服务器
const handleRequest = async ({ file }: any) => {
  try {
    loading.value = true
    const params:API.PictureUploadRequest = props.picture ? { id: props.picture.id} : {};
    params.spaceId = spaceId.value
    const res = await uploadPicture(params,{}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data as API.PictureVO)
    }else {
      // console.log('图片文件失败,', error.message)
      message.error(`上传失败，${res.data.message}`, 3)
    }
  } catch (error: any) {
    console.log('图片文件失败,', error.message)
    message.error(error.message)
  }
  loading.value = false
}
const loading = ref<boolean>(false)







</script>

<template>
  <div id="PictureUpload">
    <a-upload
      v-model:file-list="fileList"
      list-type="picture-card"
      :show-upload-list="false"
      :before-upload="beforeUpload"
      :custom-request="handleRequest"
      class="avatar-uploader"
    >
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
      <div v-else>
        <LoadingOutlined v-if="loading" />
        <PlusOutlined v-else />
        <div class="ant-upload-text">点击或拖拽文件进行上传</div>
      </div>
    </a-upload>
  </div>
</template>

<style scoped>
#PictureUpload {
  max-width: 100%;
  margin: 0 auto;
  text-align: center;
}

.avatar-uploader > :deep(.ant-upload) {
  width: 100% !important;
  min-width: 156px !important;
  min-height: 300px !important;
}

.avatar-uploader img {
  max-width: 300px;
  max-height: 200px;
}

.ant-upload-text {
  margin-top: 16px;
}
</style>

<script setup lang="ts">
import PictureUpload from '@/components/picture/PictureUpload.vue'
import {computed, h, onMounted, reactive, ref, watchEffect} from 'vue'
import {
  getPictureVoById,
  listPictureTagCategory,
  updatePicture, uploadPictureByUrl
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

import { useRoute,useRouter } from 'vue-router'
import ImageCropper from "@/components/common/ImageCropper.vue";
import {EditOutlined} from "@ant-design/icons-vue";
import {getSpaceVoById} from "@/api/spaceController.ts";


const picture = ref<API.PictureVO>()

const route = useRoute()
const router = useRouter()

const props = defineProps<{
  spaceId: string | number;
}>()

/**
 * 上传成功的回到函数
 * @param file 图片VO信息
 */
const onUploadSuccess = (file: any) => {
  picture.value = file.data as API.PictureVO
  // 将搜索框名称进行回填为 长传的名称
  formData.name = picture.value.name
}

const formData = reactive<API.PictureUpdateRequest>({
  name: '',
  introduction: '',
  category: '',
  tags: [],
})

/**
 * 清空表单数据 图片数据
 */
const clearPage = () => {
  // 清空表单
  formData.name = ''
  formData.introduction = ''
  formData.category = ''
  formData.tags = []
  // 清空图片（PictureVO | undefined，不能用空字符串占位）
  picture.value = undefined
}

const handleCommit = async (values: any) => {
  const pictureId = picture.value?.id
  if (!pictureId) {
    return
  }
  const pictureUpdateParams: API.PictureUpdateRequest = {
    id: pictureId,
    ...values,
  }

  const res = await updatePicture(pictureUpdateParams, {})
  if (res.data.code === 0 && res.data.data) {
    message.success('成功提交')
    clearPage();
  } else {
    message.error(`提交失败,${res.data.message}`)
  }
}

const categoryOptions = ref<any[]>([])
const tagOptions = ref<any[]>([])

// 获取分类 和 标签
const getTagCategory = async () => {
  const res = await listPictureTagCategory()
  if (res.data.code == 0 && res.data.data) {
    categoryOptions.value = res.data.data?.categoryList?.map((el) => ({ value: el })) ?? []
    tagOptions.value = res.data.data.tagList?.map((el) => ({ value: el })) ?? []
  }
}


const getOldPicture = async () => {
  const oldId = route.query?.id
  if (!oldId) {
    return
  }
  // 通过 id 查询图片数据（query 参数是字符串，需要显式转换）
  const res = await getPictureVoById({ id: Number(oldId) })
  if (res.data.code === 0 && res.data.data) {
    picture.value = res.data.data as API.PictureVO
    // 表单填入
    formData.name = res.data.data?.name
    formData.introduction = res.data.data?.introduction
    formData.category = res.data.data?.category
    formData.tags = res.data.data.tags
  } else {
    message.error(res.data.message)
  }
}

onMounted(() => {
  getOldPicture();
  getTagCategory();
})

const activeKey = ref('file')
const urlInputValue = ref<string>()


// URL验证规则
const urlRules = [
  { required: true, message: 'URL不能为空' },
  {
    type: 'url',
    message: '请输入有效的URL地址',
    // URL正则验证
    pattern: new RegExp('^(https?:\\/\\/)?'+ // 协议
      '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // 域名
      '((\\d{1,3}\\.){3}\\d{1,3}))'+ // IP地址
      '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // 端口和路径
      '(\\?[;&a-z\\d%_.~+=-]*)?'+ // 查询字符串
      '(\\#[-a-z\\d_]*)?$','i') // hash
  }
]

const urlForm = ref()

const handleUrlUpload = async () => {
  try {
    // 触发表单验证
    await urlForm.value.validateFields()
    // 验证通过后的处理
    console.log('URL验证通过:', urlInputValue.value)
    const requestParams: API.PictureUploadRequest = {
      id: picture?.value?.id,
      fileUrl: urlInputValue.value,
    }
    // 空间 id 来自 URL query，需要转换为数字
    requestParams.spaceId = route.query.spaceId ? Number(route.query.spaceId) : undefined

    const res = await uploadPictureByUrl(requestParams)

    if (res.data.code === 0&& res.data.data) {
      message.success("上传成功")

      picture.value = res.data.data;

      console.log(picture)
    }else {
      message.error(res.data.message)
    }

    // 这里添加你的上传逻辑
  } catch (error: any) {
    console.error('URL验证失败:', error)
  }
}

/**
 * 裁剪弹窗成功回调
 * @param newPictureVO 裁剪后的图片信息
 */
const handleCropperSuccess = (newPictureVO: API.PictureVO) => {
  picture.value = newPictureVO
}

// 图片裁剪引用
const imageCropperRef = ref()

/**
 * 图片裁剪事件
 */
const handleCropper = () => {
  // 打开裁剪弹窗
  imageCropperRef.value?.openModal()
}


// 当前空间 id（URL query 是字符串，统一转成 number）
const spaceId = computed<number | undefined>(() => {
  const value = route.query?.spaceId
  return value ? Number(value) : undefined
})

const space = ref<API.SpaceVO>()
// 获取空间信息
const fetchSpace = async () => {
  // 获取数据
  if (spaceId.value) {
    const res = await getSpaceVoById({
      id: spaceId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data;
    }
  }
}

watchEffect(() => {
  fetchSpace()
})
</script>

<template>
  <div id="addPicturePage">
    <h2 class="title">{{ route.query?.id ? '更新图片' : '创建图片' }}</h2>
    <a-typography v-if="route.query.spaceId" style="color: #666666" size="20">保存至空间: <a :href="`/space/${route.query.spaceId}`">{{route.query.spaceId}}</a> </a-typography>
    <a-tabs  v-model:activeKey="activeKey">
      <a-tab-pane key="file" tab="文件 上传">
        <PictureUpload :picture="picture" :on-success="onUploadSuccess"></PictureUpload>
      </a-tab-pane>
      <a-tab-pane key="url" tab="URL 上传">
          <a-form ref="urlForm" v-model:value="urlInputValue" layout="vertical"  :model="{ url: urlInputValue }" autocomplete="off">
            <a-form-item name="url" :rules="urlRules">
              <a-input-search
                v-model:value="urlInputValue"
                placeholder="请输入上传的URL地址"
                enter-button="上传"
                @search="handleUrlUpload"
                size="large"
              />
            </a-form-item>
          </a-form>
          <a-image style="display:inline-block;width: 200px;max-height: 300px; margin:auto;" :src="picture?.url"/>
      </a-tab-pane>
    </a-tabs>

    <!-- 图片剪裁 -->
    <div class="imageEditOptionPanel" v-if="picture">
      <a-button :icon="h(EditOutlined)" @click="handleCropper">图片裁剪</a-button>
    </div>

    <ImageCropper ref="imageCropperRef" :space="space" :picture="picture" :on-cropper-success="handleCropperSuccess" :space-id="spaceId" :image-url="picture?.url"  />

    <a-form v-if="picture" layout="vertical" :model="formData" autocomplete="off" @finish="handleCommit">
      <a-form-item name="name" label="名称">
        <a-input v-model:value="formData.name" placeholder="请输入名称" allow-clear></a-input>
      </a-form-item>
      <!--  简介  -->
      <a-form-item name="introduction" label="简介">
        <a-textarea
          allow-clear
          v-model:value="formData.introduction"
          :auto-size="{ minRows: 4, maxRows: 5 }"
          placeholder="请输入简介"
        >
        </a-textarea>
      </a-form-item>

      <!--  分类  -->
      <a-form-item name="category" label="分类">
        <a-auto-complete
          :options="categoryOptions"
          v-model:value="formData.category"
          placeholder="请输入分类"
          allow-clear
        />
      </a-form-item>

      <!-- 标签-->
      <a-form-item name="tags" label="标签">
        <a-select
          :options="tagOptions"
          v-model:value="formData.tags"
          mode="tags"
          placeholder="请输入标签"
          allow-clear
        />
      </a-form-item>

      <a-form-item>
        <a-button html-type="submit" type="primary" style="width: 100%; height: 50px"
        >提交</a-button
        >
      </a-form-item>
    </a-form>

  </div>
</template>

<style scoped>

.imageEditOptionPanel{
  display: flex;
  justify-content: center;
}
.title {
  margin-bottom: 30px;
}

#addPicturePage {
  max-width: 700px;
  margin: 0 auto;
}
</style>

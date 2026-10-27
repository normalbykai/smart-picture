<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, type SelectProps } from 'ant-design-vue'
import { editPictureBatch, listPictureTagCategory } from '@/api/pictureController.ts'

// 定义组件属性类型
interface BatchEditModalProps {
  pictureList: API.PictureVO[]
  spaceId: number | undefined
  onSuccess: () => void
}

// 给组件赋值初始值
const props = withDefaults(defineProps<BatchEditModalProps>(), {
  pictureList: () => []
})
// 控制弹窗可见性
const visible = ref(false)

/**
 * 关闭对话框
 */
const closeModal = () => {
  visible.value = false
}

/**
 * 打开对话框
 */
const openModal = () => {
  visible.value = true
}

// 对话框表单数据
const formData = reactive<API.PictureEditBatchRequest>({
  category: '',
  tags: [],
  nameRule: '',
})

/**
 * 表单提交
 */
const handleSubmit = async () => {
  const params:API.PictureEditBatchRequest = {
    ...formData,
  }
  // 添加空间id
  params.spaceId = props.spaceId
  // 过滤掉没有 id 的图片，保证传入后端的都是有效数字 id
  params.pictureIdList = props.pictureList
    .map(picture => picture.id)
    .filter((id): id is number => id !== undefined)
  // 添加需要编辑的图片ID
  const res = await editPictureBatch(params)
  if (res.data.code === 0 && res.data.data) {
    message.success('提交成功')
    // 关闭对话框
    closeModal()

    // 调用成功回调
    props?.onSuccess()
  } else {
    message.error('提交失败:' + res.data.message)
  }
}

const categoryOptions = ref<SelectProps['options']>([])
const tagsOptions = ref<SelectProps['options']>([])

// 获取分类和标签数据
const getCategoryWithTags = async () => {
  const res = await listPictureTagCategory()

  if (res.data.code === 0 && res.data.data) {
    categoryOptions.value = (res.data.data.categoryList ?? []).map((item: string) => {
      return {
        label: item,
        value: item,
      }
    })

    tagsOptions.value = (res.data.data.tagList ?? []).map((item: string) => {
      return {
        label: item,
        value: item,
      }
    })
  } else {
    message.error('获取分类和标签失败，' + res.data.message)
  }
}

onMounted(() => {
  getCategoryWithTags()
})

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>

<template>
  <a-modal v-model:open="visible" title="批量编辑图片" :footer="false" @cancel="visible = false">
    <a-typography-paragraph> * 只对当前页面的图片生效</a-typography-paragraph>

    <!--  表单项  -->
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="分类" name="category">
        <a-auto-complete
          v-model:value="formData.category"
          :options="categoryOptions"
          placeholder="请输入分类"
          allowClear
        />
      </a-form-item>

      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="formData.tags"
          mode="tags"
          style="width: 100%"
          placeholder="请输入标签"
          :options="tagsOptions"
          allow-clear
        />
      </a-form-item>

      <a-form-item label="命名规则" name="nameRule">
        <a-input
          v-model:value="formData.nameRule"
          placeholder="请输入命名规则, 输入 {序号} 可动态生成"
        />
      </a-form-item>

      <a-form-item>
        <a-button type="primary" html-type="submit">提交</a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped></style>

<script setup lang="ts">
import {ref, watch, watchEffect} from "vue";

interface PropsType {
  picture?: API.PictureVO
  imageUrl?: string | null
  onCropperSuccess?: (newPicture: API.PictureVO) => void
  spaceId?: number | undefined | string
  space?:API.SpaceVO,
  file?: File
  visible: boolean, // 对话框显示状态
}
const props = defineProps<PropsType>();
const emit = defineEmits(['update:visible', 'done'])

// 裁切对话框显示状态
const visible = ref(props.visible)

// 同步父子显示状态相互通信
watch(() => props.visible, v => visible.value = !!v) // 当外部修改显示状态， 内部也修改
watch(visible, v => emit('update:visible', !!v)) // 监听内部显示状态，修改则通知外部组件让外部值也修改

// 裁切引用
const cropperRef = ref();

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
}

// 缩放 正数为放大 负数为缩小
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
}

// 点击确认按钮时
const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    emit('done', blob) // 触发外部事件
    //closeModal() // 关闭裁切对话框
    visible.value = false; // 关闭对话框;
  })
}

// 点击关闭按钮时
const handleCancel = () => {
  visible.value = false;
}



</script>

<template>
  <a-modal
    class="image-cropper"
    v-model:open="visible"
    title="图片裁切"
    :footer="false"
    @cancel="handleCancel"
  >
  <!-- 裁切面板 -->
    <vue-cropper
      ref="cropperRef"
      :img="props.imageUrl"
      :autoCrop="true"
      :fixedBox="true"
      :centerBox="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />


    <div style="margin-bottom: 10px;"/>
    <!-- 操作面板 -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft" >向左旋转</a-button>
        <a-button @click="rotateRight">向右旋转</a-button>
        <a-button @click="changeScale(1)" >放大</a-button>
        <a-button @click="changeScale(-1)" >缩小</a-button>
        <a-button type="primary" @click="handleConfirm">确认</a-button>
      </a-space>
    </div>


  </a-modal>

</template>

<style scoped>

.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px !important;
}

</style>

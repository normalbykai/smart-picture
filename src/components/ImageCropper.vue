<template>
  <a-modal
    class="image-cropper"
    v-model:open="visible"
    title="编辑图片"
    :footer="false"
    @cancel="closeModal"
  >
    <vue-cropper
      ref="cropperRef"
      :img="props.imageUrl"
      :autoCrop="true"
      :fixedBox="false"
      :centerBox="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />
    <div style="margin-bottom: 16px" />
    <!-- 协同操作 -->
    <div class="image-collborate-actions" v-if="isTeamSpace">
      <a-space>
        <a-button v-if="editingUser" type="primary" ghost disabled>用户{{ editingUser?.userName }}在编辑</a-button>
        <a-button type="primary" ghost v-if="canEnterEdit" @click="enterEdit">进入编辑</a-button>
        <a-button danger ghost v-if="canExitEdit" @click="exitEdit">退出编辑</a-button>
      </a-space>
    </div>
    <div style="margin-bottom: 20px"></div>
    <!-- 图片操作 -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
        <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
        <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
        <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
        <a-button type="primary" :loading="loading" @click="handleConfirm" :disabled="!canEdit"
          >确认</a-button
        >
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onUnmounted, watchEffect, computed } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPicture } from '@/api/pictureController.ts'
import { PictureEditWebSocket } from '@/utils/pictureEditWebSocket.ts'
import {
  PICTURE_EDIT_MESSAGE_ACTION_ENUM,
  PICTURE_EDIT_MESSAGE_TYPE_ENUM,
} from '@/constants/picture.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import {SPACE_TYPE_ENUM} from "@/constants/space.ts";

interface Props {
  picture?: API.PictureVO
  imageUrl?: string | null
  onCropperSuccess?: (newPicture: API.PictureVO) => void
  spaceId?: number | undefined | string
  space?:API.SpaceVO,
  file?: File
}

const props = defineProps<Props>()

// 编辑器组件的引用
const cropperRef = ref()

// 对话框显示状态
const visible = ref(false)

/**
 * 关闭弹窗
 */
const closeModal = () => {
  visible.value = false
  // 断开ws连接
  closeWebSocket()
}

/**
 * 打开弹窗
 */
const openModal = () => {
  visible.value = true
}

/**
 * 暴露函数给父组件
 */
defineExpose({
  openModal,
})



// 确认加载状态
const loading = ref(false)

// 确认按钮回调
const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    // blob 为已剪切的文件
    let imgFileName = props?.picture?.name ?? 'image'
    imgFileName = `${imgFileName}.png`
    // 将二进制数据转换为文件
    const file = new File([blob], imgFileName, { type: blob.type })
    // 上传编辑处理后的文件
    handleRequest({ file })
  })
}

// 请求后端服务器
const handleRequest = async ({ file }: any) => {
  try {
    loading.value = true
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.picture?.spaceId

    const res = await uploadPicture(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片编辑成功')
      // 父组件成功回调执行

      console.log(res.data.data)
      props.onCropperSuccess?.(res.data.data)
      // 关闭对话框
      closeModal()
    } else {
      // console.log('图片文件失败,', error.message)
      message.error(`编辑失败，${res.data.message}`, 3)
    }
  } catch (error: any) {
    console.log('图片编辑失败,', error.message)
    message.error(error.message)
  }
  loading.value = false
}

// --------- 实时编辑 ---------

// 建立WS连接
let websocket: PictureEditWebSocket | null

// get login store
const loginUserStore = useLoginUserStore()
// current login user
const loginUser = loginUserStore.loginUser

// 当前编辑的用户
const editingUser = ref<API.UserVO>()

// 可以编辑状态
const canEdit = computed(() => {
  if (!isTeamSpace.value) {
    return true; // 可编辑
  }
  return editingUser.value?.id === loginUser.id
})

// 进入编辑状态
const canEnterEdit = computed(() => {
  return !editingUser.value
})

// 仅本人能退出
const canExitEdit = computed(() => {
  return loginUser.id === editingUser.value?.id
})

const isTeamSpace = computed(() => {
  return props?.space?.spaceType === SPACE_TYPE_ENUM.TEAM;
})

const initWebsocket = () => {
  const pictureId = props?.picture?.id as number

  // 图片id存在并且协同编辑窗口打开
  if (!pictureId || !visible.value) {
    return
  }
  // 创建WS实例
  websocket = new PictureEditWebSocket(pictureId)
  // 建立 WebSocket 连接
  websocket.connect()

  // 监听通知消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
    console.log('收到通知消息:', msg)
    message.info(msg.message)
  })

  // 监听错误消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
    console.log('收到错误消息：', msg)
    message.error(msg.message)
  })

  // 监听图片编辑动作消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    console.log('收到编辑消息:', msg)
    message.info(msg.message)
    // 收到消息要做什么,
    switch (msg.editAction) {
      case PICTURE_EDIT_MESSAGE_ACTION_ENUM.ZOOM_OUT:
        // 图片编辑放大操作
        changeScale(1)
        break
      case PICTURE_EDIT_MESSAGE_ACTION_ENUM.ZOOM_IN:
        // 图片编辑缩小操作
        changeScale(-1)
        break
      case PICTURE_EDIT_MESSAGE_ACTION_ENUM.ROTATE_RIGHT:
        // 右旋转
        rotateLeft()
        break
      case PICTURE_EDIT_MESSAGE_ACTION_ENUM.ROTATE_LEFT:
        // 左旋转
        rotateRight()
        break
    }
  })

  // 监听图片编辑退出消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    console.log('收到退出编辑消息:', msg)
    message.info(msg.message)
    // 编辑用户设置为空
    editingUser.value = undefined
  })

  // 监听图片编辑进入消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    console.log('收到图片编辑进入信息:', msg)
    message.info(msg.message)
    editingUser.value = msg.user
  })

}

// 组件销毁之后回调
onUnmounted(() => {
  // 断开ws连接
  closeWebSocket()
})

/**
 * 断开 websocket 连接
 */
const closeWebSocket = () => {
  if (websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
}

/**
 * 进入编辑
 */
const enterEdit = () => {
  if (websocket) {
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT
    })
  }
}

/**
 * 退出图片编辑
 */
const exitEdit = () => {
  if (websocket) {
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT
    })
    editingUser.value = undefined;
  }

}

// 监听协同操作
watchEffect(() => {
  if (isTeamSpace.value) {
    initWebsocket()
  }
})

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
  editAction(PICTURE_EDIT_MESSAGE_ACTION_ENUM.ROTATE_LEFT);
}

// 编辑图片操作
const editAction = (action: string) => {
  if (websocket) {
    console.log(action)
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
      editAction: action
    })
  }
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
  editAction(PICTURE_EDIT_MESSAGE_ACTION_ENUM.ROTATE_RIGHT);
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
  if (num < 0) {
    editAction(PICTURE_EDIT_MESSAGE_ACTION_ENUM.ZOOM_OUT);
  } else {
    editAction(PICTURE_EDIT_MESSAGE_ACTION_ENUM.ZOOM_IN);
  }
}
</script>

<style>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px !important;
}
</style>

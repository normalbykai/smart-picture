<script setup lang="ts">
import { ref } from 'vue'
import {message} from "ant-design-vue";
import clipboard from 'clipboard';

/**
 * 定义分享参数接口
 */
interface ShareModalProps {
  title?: string | undefined
  link?: string | undefined
}

/**
 * 初始化组件参数
 */
const props = withDefaults(defineProps<ShareModalProps>(), {
  title: () => '分享图片',
  link: () => 'https://gitee.com/normalcodes',
})

// 对话框的显示状态
const visible = ref(false)
/**
 * 关闭分享弹窗
 */
const closeModal = () => {
  visible.value = false
}
/**
 *  打开分享弹窗
 */
const openModal = () => {
  visible.value = true
}

/**
 * 弹窗中 - 确认按钮的回调
 */
const handleOk = () => {
  message.success("已复制到粘贴板中！")

  // 将路径内容复制到剪切板中
  clipboard.copy(props.link)

  // 关闭弹窗
  closeModal()
}

/**
 * 抛出打开弹窗事件
 */
defineExpose({
  openModal,
})


</script>

<template>
  <div id="share-modal">
    <a-modal v-model:open="visible" :title="props.title" :link="props.link" @cancel="closeModal" @ok="handleOk">
      <h4>复制分享链接</h4>
      <a-typography-link
        :copyable="{
          text: props.link,
          onCopy: () => {
            message.success('复制成功')
          }
        }"
        @click="() => {
          clipboard.copy(props.link);
          message.success('复制成功');
        }"
      >
        {{ props.link }}
      </a-typography-link>
      <!-- 上下间隔-->
      <div style="margin-top: 20px"></div>
      <h4>手机查看</h4>
      <a-qrcode :value="props.link"/>
    </a-modal>
  </div>
</template>

<template>
  <a-modal
    v-model:open="visible"
    title="编辑个人信息"
    :footer="null"
    @cancel="handleCancel"
    class="edit-userinfo-modal"
    width="400px"
  >
    <a-form
      :model="form"
      :rules="rules"
      ref="formRef"
      layout="vertical"
      @submit.prevent
    >
      <a-form-item label="用户名" name="userName" :rules="rules.userName">
        <a-input v-model:value="form.userName" placeholder="请输入用户名" allow-clear />
      </a-form-item>
      <a-form-item label="简介" name="userProfile">
        <a-textarea v-model:value="form.userProfile" placeholder="请输入简介" :rows="3" allow-clear />
      </a-form-item>
      <div class="edit-userinfo-modal-footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" @click="handleOk" :loading="loading" style="margin-left: 12px;">保存</a-button>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, defineEmits, defineProps } from 'vue'
import { message } from 'ant-design-vue'


const props = defineProps<{
  open: boolean
  userInfo: API.UserVO
}>()
const emit = defineEmits(['update:open', 'submit'])

const visible = ref(props.open)
watch(() => props.open, v => visible.value = v)
watch(visible, v => emit('update:open', v))

const formRef = ref()
const loading = ref(false)

const form = reactive({
  userName: props.userInfo.userName || '',
  userProfile: props.userInfo.userProfile || ''
})

const rules = {
  userName: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 2, max: 16, message: '用户名长度2-16位', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效邮箱', trigger: 'blur' }
  ]
}

const handleOk = () => {
  formRef.value.validate().then(() => {
    loading.value = true
    emit('submit', { ...form })
    loading.value = false

    // 关闭编辑用户对话框
    visible.value = false
  }).catch(() => {})
}

const handleCancel = () => {
  visible.value = false
}
</script>

<style scoped>
.edit-userinfo-modal :deep(.ant-modal-content) {
  border-radius: 14px;
}
.edit-userinfo-modal .ant-modal-header {
  border-radius: 14px 14px 0 0;
  background: linear-gradient(90deg, #f8fafc 60%, #e5e5ea 100%);
}
.edit-userinfo-modal .ant-modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #222;
}
.edit-userinfo-modal .ant-modal-body {
  padding: 24px 24px 8px 24px;
}
.edit-userinfo-modal .ant-form-item-label > label {
  font-weight: 500;
  color: #555;
}
.edit-userinfo-modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>

<script setup lang="ts">
import {computed, ref} from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {updateVo, uploadUserAvatar} from "@/api/userController.ts";
import PictureCropper from '@/components/picture/PictureCropper.vue';
import { EditOutlined } from '@ant-design/icons-vue'
import EditUserInfoModal from '@/components/user/EditUserInfoModal.vue';

const loginUserStore = useLoginUserStore() // 登录的用户信息
const uploading = ref(false) // 上传的等待状态
const previewImage = computed(() => loginUserStore.loginUser.userAvatar) // 当前显示的头像图片地址
const cropperRef = ref() // 图片裁剪引用
const selectedFile = ref<File | null>(null); // 选中图片文件
const cropperImageUrl = ref<string | null>(null); // 图片上传时临时的URL, 用于用户预览和裁切
const cropVisible = ref<boolean>(false) // 裁切对话框显示状态

// mock 字段（如无可用数据可用 '-' 占位）
const phone = (loginUserStore.loginUser as any).phone || '未绑定'
const email = (loginUserStore.loginUser as any).email || '未绑定'
const profile = computed(() => {
  return loginUserStore.loginUser.userProfile || '这个人很神秘，什么都没写...';
})

/**
 * 在头像图片上传之前的操作
 * - 打开裁切对话框
 * @param file
 */
const handleBeforeUpload = (file: File) => {
  // 1. 保存选中的文件到响应变量
  selectedFile.value = file

  // 2. 防止内存泄露, 释放已使用的图片URL
  if (cropperImageUrl.value) {
    URL.revokeObjectURL(cropperImageUrl.value) // 释放旧的URL
  }

  // 3. 为选中的图片创建一个临时的Blob URL, 用户图片预览和裁剪
  cropperImageUrl.value = URL.createObjectURL(file);

  // 4. 打开裁剪对话框
  cropVisible.value = true

  // 5. 返回false阻止自定上传, 使用裁剪后我们自己自定义上传
  return false
}

/**
 * 处理裁剪完成的图片上传操作
 * @param cropperBlob 裁剪后的图片Blob对象
 */
const handleCropDone = async (cropperBlob: Blob) => {
  // 1. 将Blob转换为File对象
  const file = new File([cropperBlob], `normal.png`  , {type: cropperBlob.type})

  try {
    // 2. 上传头像图片到服务器
    const res = await uploadUserAvatar({}, file) as { data: { code: number; data: any; message?: string } }

    // 3. 处理上传结果
    if (res.data?.code === 0  && res.data?.data){
      // 成功处理
      message.success("头像修改成功！")
      // 4. 刷新用户信息缓存
      await loginUserStore.fetchLoginUser();

    } else {
      // 显示服务端的错误信息
      message.error(res.data?.message || `头像上传失败`);
    }
  } catch(error: any) {

    // 错误处理
    console.error(`头像上传失败:${error.message}`);

    // 友好显示提示
    const errorMessage = error.message || "网络错误，请检查后重试"
    message.error(errorMessage);
  }
}


// ------------ 用户信息编辑 ---------------
// 编辑用户信息对话框显示状态
const editModalOpen = ref<boolean>(false)

// 点击编辑用户按钮的回调
const handleEditUserInfo = () => {
  editModalOpen.value = true;   // 弹出编辑用户对话框
}

// 编辑确认提交回调
const onEditUserInfoSubmit= async (form: API.UserEditRequest) => {
  // 1. 发起更新用户信息
  const res = await updateVo(form)

  // 2. 处理更新更新请求
  if (res.data?.code === 0  && res.data?.data){
    // 更新成功
    message.success("用户信息修改成功！")
    // 3.. 刷新用户信息缓存
    await loginUserStore.fetchLoginUser();

  } else {
    // 显示服务端的错误信息
    message.error(res.data?.message || `用户信息修改失败`);
  }
}
</script>

<template>
  <div class="apple-user-center">
    <a-card class="apple-user-card" bordered="false">
      <a-button
        class="edit-userinfo-btn"
        type="text"
        shape="circle"
        @click="handleEditUserInfo"
        title="修改信息"
      >
        <template #icon>
          <EditOutlined />
        </template>
      </a-button>

      <EditUserInfoModal
        :user-info="loginUserStore.loginUser"
        v-model:open="editModalOpen"
        @submit="onEditUserInfoSubmit"
      />

      <div class="apple-user-header-row">
        <a-upload
          :show-upload-list="false"
          :before-upload="handleBeforeUpload"
        >
          <PictureCropper
            ref="cropperRef"
            v-model:visible="cropVisible"
            :image-url="cropperImageUrl"
            @done="handleCropDone"
          />
          <a-avatar
            :size="104"
            :src="previewImage"
            class="apple-avatar"
            style="cursor:pointer; border: 2px solid #e5e5ea; box-shadow: 0 2px 8px #0001;"
          >
            <template #icon>
              <span style="font-size: 32px;">+</span>
            </template>
          </a-avatar>
          <div class="avatar-upload-mask">更换头像</div>
        </a-upload>
        <div class="apple-user-info-col">
          <div class="apple-username">{{ loginUserStore.loginUser.userName || '未设置昵称' }}</div>
          <div class="apple-user-meta">
            <a-tag color="blue" class="apple-role">{{ loginUserStore.loginUser.userRole || '普通用户' }}</a-tag>
            <span class="apple-user-id">ID: {{ loginUserStore.loginUser.id || '-' }}</span>
          </div>
          <div class="apple-user-profile">{{ profile }}</div>
        </div>
      </div>
    </a-card>

    <a-card class="apple-info-card" bordered="false">
      <div class="info-list">
        <div class="info-row">
          <span class="info-label">用户名</span>
          <span class="info-value">{{ loginUserStore.loginUser.userName || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">账号</span>
          <span class="info-value">{{ loginUserStore.loginUser.userAccount || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">手机号</span>
          <span class="info-value">{{ phone }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ email }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">注册时间</span>
          <span class="info-value">
            {{ loginUserStore.loginUser.createTime ? dayjs(loginUserStore.loginUser.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
          </span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.apple-user-center {
  max-width: 700px;
  margin: 48px auto 0 auto;
  padding: 0 16px;
}
.apple-user-card {
  border-radius: 20px;
  box-shadow: 0 4px 24px #0001;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #f8fafc 60%, #e5e5ea 100%);
  padding: 32px 32px 24px 32px;
  position: relative;
}
.edit-userinfo-btn {
  position: absolute;
  top: 18px;
  right: 24px;
  z-index: 10;
  background: #f4f6fa;
  border-radius: 50%;
  box-shadow: 0 2px 8px #0001;
  transition: background 0.2s, color 0.2s;
  color: #666;
  font-size: 18px;
  border: none;
}
.edit-userinfo-btn:hover {
  background: #e6f7ff;
  color: #1890ff;
}
.apple-user-header-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 32px;
}
.apple-avatar {
  transition: box-shadow 0.2s;
  background: #fff;
}
.apple-avatar:hover {
  box-shadow: 0 6px 24px #0002;
}
.avatar-upload-mask {
  font-size: 13px;
  color: #888;
  margin-top: 8px;
  cursor: pointer;
  text-align: center;
}
.apple-user-info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.apple-username {
  font-size: 26px;
  font-weight: 600;
  color: #222;
  margin-bottom: 2px;
}
.apple-user-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #888;
  font-size: 15px;
}
.apple-role {
  border-radius: 8px;
  font-size: 13px;
  padding: 0 10px;
}
.apple-user-id {
  font-size: 13px;
  color: #aaa;
}
.apple-user-profile {
  color: #666;
  font-size: 15px;
  margin-top: 2px;
  line-height: 1.6;
}
.apple-info-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px #0001;
  background: #fafbfc;
  margin-top: 0;
  padding: 0 0 8px 0;
  border: none;
}
.info-list {
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 0 12px 0;
  border-bottom: 1px solid #f0f0f5;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  color: #888;
  font-weight: 500;
  font-size: 15px;
  min-width: 90px;
}
.info-value {
  color: #222;
  font-size: 16px;
  font-weight: 500;
  text-align: right;
  flex: 1;
  margin-left: 32px;
  word-break: break-all;
}
</style>

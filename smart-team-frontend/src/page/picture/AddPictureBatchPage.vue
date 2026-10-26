<script setup lang="ts">
import {reactive, ref} from "vue";
import {uploadPictureByBatch} from "@/api/pictureController.ts";
import {message} from "ant-design-vue";

const formData = reactive<API.PictureUploadByBatchRequest>({
  number: 10,
})

const handleSubmit = async () => {
  loading.value = true
  const res = await uploadPictureByBatch(formData)
  if (res.data.code === 0 && res.data.data) {
    message.success("批量上传图片成功,数量为:" + res.data.data);
  }else {
    message.error("失败" +res.data.message)
  }

  loading.value = false;
}

const loading = ref(false)
</script>

<template>
  <div id="addPictureBatchPage">
    <h2 style="margin-bottom: 16px">批量创建图片</h2>
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="关键词" name="searchText">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" />
      </a-form-item>
      <a-form-item label="抓取数量" name="count">
        <a-input-number
          v-model:value="formData.number"
          placeholder="请输入数量"
          style="min-width: 180px"
          :min="1"
          :max="30"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="名称前缀" name="namePrefix">
        <a-input v-model:value="formData.namePrefix" placeholder="请输入名称前缀，会自动补充序号" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          执行任务
        </a-button>
      </a-form-item>
    </a-form>
  </div>

</template>

<style scoped>

#addPictureBatchPage {
  max-width: 700px;
  margin: 0 auto;
}

</style>

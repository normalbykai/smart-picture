<script setup lang="ts">

import {
  SPACE_LEVEL_ENUM,
  SPACE_LEVEL_OPTIONS,
  SPACE_TYPE_ENUM,
  SPACE_TYPE_MAP
} from "@/constants/space.ts";
import {ref, reactive, onMounted, computed} from "vue";
import {addSpace, getSpaceVoById, listSpaceLevel, updateSpace} from "@/api/spaceController.ts";
import {message} from "ant-design-vue";
import {formatSize} from "../../utils";
import {useRoute} from "vue-router";
import router from "@/router";

const formData = reactive<API.SpaceAddRequest | API.SpaceUpdateRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON,
})
const loading = ref(false)
// 当前编辑的空间 id（URL query 为字符串，统一转 number）
const spaceId = ref<number | undefined>(undefined)

const route = useRoute();

// 拿到当前页面的空间类型
const spaceType = computed(() => {
  // 空间类型
  if(route.query?.type) {
    return Number(route.query.type)
  }
  // 默认为私有空间类型
  return SPACE_TYPE_ENUM.PRIVATE;
})

const handleSubmit = async () => {
  loading.value = true
  if (oldSpace.value) {
    // 则为更新操作：显式构造 SpaceUpdateRequest，避免联合类型推断问题
    const params: API.SpaceUpdateRequest = {
      id: spaceId.value,
      spaceName: formData.spaceName,
      spaceLevel: formData.spaceLevel,
    }
    const res = await updateSpace(params)
    if (res.data.code === 0 && res.data.data) {
      message.success("更新空间成功")

    }else {
      message.error(res.data.message)
    }
  } else {
    // 则为创建操作
    const res = await addSpace({...formData, spaceType: spaceType.value});
    if (res.data.code === 0 && res.data.data) {
      message.success("创建空间成功")
      // 跳转创建的团队空间
      await router.push("/space/" + res.data.data)
    }else {
      message.error(res.data.message)
    }
  }

  loading.value = false

}

const spaceLevelList = ref<API.SpaceLevel[]>([])

// 获取空间级别
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevel()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('加载空间级别失败，' + res.data.message)
  }
}

const oldSpace = ref<API.SpaceVO>()



// 获取老数据
const getOldSpace = async () => {
  // 获取数据（query 参数是字符串，需要显式转换）
  const id = route.query?.id
  if (id) {
    const numericId = Number(id)
    const res = await getSpaceVoById({
      id: numericId,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      oldSpace.value = data
      formData.spaceName = data.spaceName
      formData.spaceLevel = data.spaceLevel
    }
    spaceId.value = numericId
  }
}

onMounted(() => {
  fetchSpaceLevelList()
  getOldSpace();
})
</script>

<template>
  <div id="add-space-page">
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <h2 style="margin-bottom: 16px">
        {{ route.query?.id ? '修改' : '创建' }}{{ SPACE_TYPE_MAP[spaceType] }}
      </h2>
      <a-form-item label="空间名称" name="spaceName">
        <a-input v-model:value="formData.spaceName" autocomplete="off" placeholder="请输入空间名称" allow-clear />
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select
          v-model:value="formData.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请输入空间级别"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          {{oldSpace ? "更新" : "提交"}}
        </a-button>
      </a-form-item>
    </a-form>


    <a-card title="空间级别介绍">
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系
        <a href="https://codefather.cn" target="_blank">常态</a>。
      </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}： 大小 {{ formatSize(spaceLevel.maxSize) }}， 数量
        {{ spaceLevel.maxCount }}
      </a-typography-paragraph>
    </a-card>

  </div>


</template>

<style scoped>

#add-space-page {
  width: 50%;
  margin: 0 auto;
}

</style>

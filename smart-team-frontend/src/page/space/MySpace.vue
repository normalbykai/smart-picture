<script setup lang="ts">
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {useRouter} from "vue-router";
import {getSpaceByPage, getSpaceVoById, getSpaceVoByPage} from "@/api/spaceController.ts";
import {message} from "ant-design-vue";
import {onMounted} from "vue";

const router = useRouter()
const loginUserStore = useLoginUserStore()
const checkUserSpace = async () => {

  // 1. 检测是否登录 未登录跳转到 登录界面
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    // 跳转到登录界面
    router.replace('/user/login')
    return;
  }

  // 2. 如果用已登录， 会获取该用户已创建的空间
  const res = await getSpaceVoByPage({
    userId: loginUser.id,
    currentNum: 1,
    pageSize: 1,
    spaceType: 0
  })

  if (res.data.code === 0) {
    if (res.data.data?.records && res.data.data?.records.length > 0) {
      // 有空间, 跳转进入第一个空间
      await router.replace(`/space/${res.data.data.records[0].id}`)
    }else {
      // 没有开通空间， 跳转创建空间页面
      router.replace(`/add_space`)
      message.warn("还未开通空间，请进行创建空间");
    }
  }else {
    message.error("加载我的空间失败, " + res.data.message)
  }
}

onMounted(()=>{
  checkUserSpace()
})


</script>

<template>
  <div id="mySpace">
    正在跳转，请稍后...
  </div>
</template>

<style scoped>

</style>

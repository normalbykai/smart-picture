<script setup lang="ts">
// 返回
import router from '@/router'

const toBack = () => {
  console.log(1)
  router.go(-1)
}

const toHome = () => {
  router.push('/')
}
</script>

<template>
  <!--  无权限页面-->
  <div id="no-auth-container">
    <div class="content">
      <h1 class="title-code">403</h1>
      <div class="desc">抱歉，您目前的权限不够，访问失败。</div>
      <div class="actives">
        <a-button type="primary" @click="toBack" style="width: 150px"> 返回 </a-button>

        <a-button type="default" @click="toHome" style="width: 150px"> 首页 </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
#no-auth-container {
  width: 600px;
  margin: 30px auto;
}

.content {
  width: 600px;
  max-width: 600px;
}

.title-code {
  color: #1677ff;
  text-align: center;
  font-size: 4rem;
}

.desc {
  font-size: 1.5rem;
  text-align: center;
  margin-bottom: 3rem;
}

.actives {
  display: flex;
  gap: 3rem;
  justify-content: center;
}

.content {
  animation: donghua 0.2s ease-in;
}

@keyframes donghua {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

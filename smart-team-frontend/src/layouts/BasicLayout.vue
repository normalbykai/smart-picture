<script setup lang="ts">
import GlobalHeader from "@/components/GlobalHeader.vue";
import GlobalSider from "@/components/GlobalSider.vue";
</script>

<template>
  <div id="BasicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader/>
      </a-layout-header>


      <a-layout>
        <GlobalSider class="sider"/>
        <a-layout-content class="content">
          <RouterView :key="$route.fullPath"/>
        </a-layout-content>
      </a-layout>



      <a-layout-footer class="footer">
        <a href="#" target="_blank"> normal版权 by 常态 </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>

#BasicLayout .sider {
  background: white;
  padding-top: 20px;
  border-right: 0.5px solid #eee;
}
#BasicLayout .header{
  background: white;
  margin-bottom: 16px;
  color: unset;
  padding-inline: 20px;
}

#BasicLayout .content{
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 28px;
  padding: 20px;
}

#BasicLayout .footer {
  background-color: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
}

#basicLayout :deep(.ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
}
</style>

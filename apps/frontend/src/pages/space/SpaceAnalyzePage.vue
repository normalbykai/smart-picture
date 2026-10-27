<script setup lang="ts">
import SpaceUsageAnalyze from '@/components/space/analyze/SpaceUsageAnalyze.vue'
import SpaceCategoryAnalyze from '@/components/space/analyze/SpaceCategoryAnalyze.vue'
import SpaceTagAnalyze from '@/components/space/analyze/SpaceTagAnalyze.vue'
import SpaceSizeAnalyze from '@/components/space/analyze/SpaceSizeAnalyze.vue'
import SpaceRankAnalyze from '@/components/space/analyze/SpaceRankAnalyze.vue'
import SpaceUserAnalyze from '@/components/space/analyze/SpaceUserAnalyze.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";

const route = useRoute()
const spaceId = computed(() => route.query.spaceId as string)

const queryPublic = computed(() => Boolean(route.query.queryPublic))

const queryAll = computed(() => Boolean(route.query.queryAll))

const userLoginStore = useLoginUserStore()

const isAdmin = computed(() => userLoginStore.loginUser.userRole === 'admin')
</script>

<template>
  <!-- 标题描述 -->
  <h2>
    空间图库分析 -
    <span v-if="queryAll">全部空间</span>
    <span v-else-if="queryPublic">公共空间</span>
    <span v-else>
      <a :href="`/space/${spaceId}`" target="_blank">id: {{ spaceId}}</a>
    </span>
  </h2>

  <div style="margin-bottom: 20px"></div>

  <!--  图表分析 -->
  <a-row :gutter="[16,16]">
    <a-col :xs="24" :md="12">
      <space-usage-analyze :space-id="spaceId" :query-public="queryPublic" :query-all="queryAll" />
    </a-col>

    <a-col :xs="24" :md="12">
      <space-category-analyze :space-id="spaceId" :query-public="queryPublic" :query-all="queryAll" />
    </a-col>

    <a-col :xs="24" :md="12">
      <space-tag-analyze :space-id="spaceId" :query-public="queryPublic" :query-all="queryAll" />
    </a-col>

    <a-col :xs="24" :md="12">
      <space-size-analyze :space-id="spaceId" :query-public="queryPublic" :query-all="queryAll" />
    </a-col>

    <a-col :xs="24" :md="12">
      <space-user-analyze :space-id="spaceId" :query-public="queryPublic" :query-all="queryAll" />
    </a-col>

    <a-col :xs="24" :md="12">
      <space-rank-analyze :space-id="spaceId" v-if="isAdmin" />
    </a-col>

  </a-row>






</template>

<style scoped>
h2{
  font-size: 1.5rem;
}
</style>

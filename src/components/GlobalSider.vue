<script setup lang="ts">
// 菜单列表
import {computed, h, onMounted, ref, watchEffect} from 'vue'
import {PictureOutlined, TeamOutlined, UserOutlined} from '@ant-design/icons-vue'
import router from "@/router";
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {SPACE_TYPE_ENUM} from "@/constants/space.ts";
import {listMyTeamSpace} from "@/api/spaceUserController.ts";
import {message} from "ant-design-vue";

// 固定菜单
const fixedMenuItems = [
  {
    key: '/',
    label: '公共图库',
    icon: () => h(PictureOutlined),
  },
  {
    key: '/my_space',
    label: '我的空间',
    icon: () => {
      return h(UserOutlined)
    }
  },
  {
    key: '/add_space?type='+SPACE_TYPE_ENUM.TEAM,
    label: '创建团队',
    icon: () => {
      return h(TeamOutlined)
    }
  }
]

// 我的团队空间列表
const teamSpaceList = ref<API.SpaceUserVO[]>([])

// 菜单项
const menuItems = computed(() => {
  // 没有团队空间， 只展示固定菜单
  if (teamSpaceList.value.length < 1) {
    return fixedMenuItems;
  }

  // 展示团队空间分组
  const teamSpaceSubMenus = teamSpaceList.value.map(spaceUser => {
    const space = spaceUser.space;
    return {
      key: '/space/' + spaceUser.spaceId,
      label: space?.spaceName,
    }
  })

  const teamSpaceMenuGroup = {
    type: "group",
    label: "我的团队",
    key: "teamSpace",
    children: teamSpaceSubMenus,
  }

  return [...fixedMenuItems, teamSpaceMenuGroup]
})

// 加载团队空间列表
const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpace();
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  } else {
    message.error('加载我的团队空间失败：' + res.data.message);
  }
}



router.afterEach((to, from) => {
  current.value = [to.path]
})

const doMenuClick = ({key}: { key: string }) => {
  // 修改不然无法携带参数
  router.replace(key)
}

// 当前选中菜单
const current = ref<string[]>([])

const loginUserStore = useLoginUserStore()

// 监控数据
watchEffect(() => {
  if (loginUserStore.loginUser.id) {
    // 加载团队空间数据
    fetchTeamSpaceList();
  }
})

</script>

<template>
  <div id="globalSider">
    <a-layout-sider class="sider" width="200" v-if="loginUserStore.loginUser.id">
      <a-menu
        mode="inline"
        v-model:selectedKeys="current"
        :items="menuItems"
        @click="doMenuClick"
      />
    </a-layout-sider>
  </div>
</template>

<style scoped>
#globalSider {
}
</style>

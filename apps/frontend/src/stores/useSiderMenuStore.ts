import { defineStore } from "pinia";
import { computed, h, ref } from "vue";
import { PictureOutlined, TeamOutlined, UserOutlined } from "@ant-design/icons-vue";
import { SPACE_TYPE_ENUM } from "@/constants/space.ts";
import { message } from "ant-design-vue";
import { listMyTeamSpace } from "@/api/spaceUserController.ts";

/**
 * 侧边栏菜单项类型声明
 */
interface MenuItem {
  key: string;
  label: string;
  icon?: () => any;
  type?: string;
  children?: MenuItem[];
  [key: string]: any;
}

/**
 * 侧边栏菜单 Pinia Store
 * 维护人：normal <normalcode@qq.com>
 * 创建时间：2025-7-1
 */
export const useSiderMenuStore = defineStore('siderMenuStore', () => {
  // 固定菜单项
  const fixedMenuItems: MenuItem[] = [
    {
      key: '/',
      label: '公共图库',
      icon: () => h(PictureOutlined),
    },
    {
      key: '/my_space',
      label: '我的空间',
      icon: () => h(UserOutlined),
    },
    {
      key: `/add_space?type=${SPACE_TYPE_ENUM.TEAM}`,
      label: '创建团队',
      icon: () => h(TeamOutlined),
    }
  ];

  // 加入团队空间列表
  const teamSpaceList = ref<API.SpaceUserVO[]>([]);
  // 加载状态
  const loading = ref(false);

  // 渲染数据菜单
  const menuItems = computed<MenuItem[]>(() => {
    if (teamSpaceList.value.length < 1) {
      return fixedMenuItems; // 只有固定菜单
    }

    // 渲染团队空间菜单到侧边栏菜单
    // 团队空间子菜单
    const teamSpaceSubMenus: MenuItem[] = teamSpaceList.value.map(spaceUser => ({
      key: `/space/${spaceUser.spaceId}`,
      label: spaceUser.space?.spaceName || '未命名空间',
    }));
    // 将团队空间进行分组
    const teamSpaceMenuGroup: MenuItem = {
      type: "group",
      label: "我的团队",
      key: "teamSpace",
      children: teamSpaceSubMenus,
    };

    // 组合侧边栏固定菜单 + 团队菜单
    return [...fixedMenuItems, teamSpaceMenuGroup];
  });

  /**
   * 获取用户加入团队空间列表
   */
  const fetchTeamSpaceList = async () => {
    loading.value = true;
    try {
      const res = await listMyTeamSpace();
      if (res.data.code === 0 && res.data.data) {
        teamSpaceList.value = res.data.data;
      } else {
        message.error('加载团队空间失败：' + res.data.message);
      }
    } catch (e) {
      message.error('网络异常，加载团队空间失败');
    } finally {
      loading.value = false;
    }
  };

  // 可扩展：重置菜单、动态添加菜单等方法

  // 暴露属性和方法
  return {
    fixedMenuItems,
    teamSpaceList,
    menuItems,
    fetchTeamSpaceList,
    loading,
  };
})

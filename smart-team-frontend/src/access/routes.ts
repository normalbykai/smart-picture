import {h} from "vue";
import {HomeOutlined} from "@ant-design/icons-vue";
import ACCESS_ENUM from "@/enums/AccessEnum.ts";

/**
 *  菜单栏全局控制
 */
export const routes = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
    meta: {
      // 无需登录就可以显示
      access: ACCESS_ENUM.NO_LOGIN
    }
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
    meta: {
      // 无需登录就可以显示
      access: ACCESS_ENUM.NO_LOGIN,
      // 影藏菜单
      hideInMenu: false,
    }
  },
  {
    key: '/add_picture',
    label: "创建图片",
    title: "创建图片",
    meta: {
      access: ACCESS_ENUM.USER
    }
  },
  {
    key: '/admin/picture_manage',
    label: '图片管理',
    title: '图片管理',
    meta: {
      // 需要管理员权限
      access: ACCESS_ENUM.ADMIN
    }
  },
  {
    key: '/admin/user',
    label: '用户管理',
    title: '用户管理',
    meta: {
      // 需要管理员权限
      access: ACCESS_ENUM.ADMIN
    }
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
    meta: {
      // 需要管理员权限
      access: ACCESS_ENUM.ADMIN
    }
  },
  {
    key: '/user/center',
    label: '个人中心',
    title: '个人中心',
    meta: {
      // 需要管理员权限
      access: ACCESS_ENUM.USER
    }
  }
  // ,
  // {
  //   key: 'normalCommunity',
  //   label: h('a', { href: '#',target: '_blank',  }, '常态社区',),
  //   title: '常态社区',
  //   meta: {
  //     access: ACCESS_ENUM.NO_LOGIN,
  //     // 禁用
  //     // disabled: false,
  //
  //   }
  // },

]

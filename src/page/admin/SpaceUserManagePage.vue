<script setup lang="ts">
import {reactive, computed, ref, onMounted, h} from "vue"
import {message} from "ant-design-vue";
import dayjs from "dayjs";
import {
  SPACE_LEVEL_MAP,
  SPACE_LEVEL_OPTIONS,
  SPACE_ROLE_MAP, SPACE_ROLE_OPTIONS,
  SPACE_TYPE_OPTIONS
} from "@/constants/space.ts";

import {
  addSpaceUser,
  deleteSpaceUser,
  editSpaceUser,
  listSpaceUser
} from "@/api/spaceUserController.ts";
// 表格列
const columns = [
  {
    title: '用户',
    dataIndex: 'userInfo',
  },
  {
    title: '角色',
    dataIndex: 'spaceRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

const props = defineProps<{
  id: string | number
}>();

// 路由参数是字符串，统一转换为数字后使用
const currentSpaceId = computed(() => Number(props.id))

const dataList = ref<API.Space[]>([])

// 搜索参数
const searchParams = reactive<API.SpaceUserQueryRequest>({
  spaceId: Number(props.id)
})

const fetchData = async () => {
  const spaceId = currentSpaceId.value

  if (!spaceId) {
    return;
  }

  const {data} = await listSpaceUser({
    spaceId
  })
  if (data.code === 0 && data.data) {
    dataList.value = data.data;
  }else {
    message.error(`获取数据失败,${data.message}`);
  }
}

onMounted(() => {
  fetchData()
})


const doDelete = async (id : any) => {
  const res = await deleteSpaceUser({id: id},{})
  if (res.data.code === 0 && res.data.data) {
    message.success("删除成功")
    // 刷新数据
    await fetchData()
  }else {
    message.error(`删除失败,${res.data.message}`);
  }
}

const doAddMember = async (page: any) => {
  console.log(page)
  const addParams: API.SpaceUserAddRequest = {
    spaceId: currentSpaceId.value,
    userId: searchParams.userId,
    spaceRole: searchParams.spaceRole,
  }

  // 添加成员
  const res = await addSpaceUser(addParams)
  if (res.data.code === 0 && res.data.data) {
    message.success("添加成功")
    // 刷新数据
    await fetchData()
  } else {
    message.error("添加失败," + res.data.message);
  }

}



const handleChange =async (spaceUserId: number,spaceRole: string) => {
  // 编辑成员角色
  const res = await editSpaceUser({
    id: spaceUserId,
    spaceRole: spaceRole
  })

  if (res.data.code === 0 && res.data.data) {
    message.success("修改成员权限成功")
  }else {
    message.error("修改失败：" + res.data.message);
  }
}

</script>

<template>
  <div id="PictureManagePage">
    <a-flex justify="space-between">
      <h2 class="title">空间管理 </h2>
    </a-flex>

    <a-form
      style="margin-bottom: 2rem"
      layout="inline"
      :model="searchParams"
      autocomplete="off"
      @finish="doAddMember">

      <a-form-item label="用户ID" name="userId">
        <a-input v-model:value="searchParams.userId" placeholder="请输入用户id" allow-clear/>
      </a-form-item>

      <a-form-item>
        <a-button html-type="submit" type="primary">添加成员</a-button>
      </a-form-item>

    </a-form>


    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userInfo'">
          <a-space>
            <a-avatar :src="record.user.userAvatar"></a-avatar>
           {{record.user.userName}}
          </a-space>
        </template>

        <template v-if="column.dataIndex === 'spaceRole'">
          <a-select
            :disabled="record.space.userId === record.userId"
            v-model:value="record.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            @change="handleChange(record.id, record.spaceRole)"
          />
        </template>

        <template v-if="column.dataIndex === 'createTime'">
          {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>

        <template v-if="column.key === 'action'">
          <a-space style="width: 120px">
            <a-popconfirm title="确定删除？" placement="left" ok-text="确认" cancel-text="取消"  @confirm="doDelete(record.id)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>

    </a-table>
  </div>
</template>

<style scoped>
h2{
  margin-bottom: 1rem;
}
</style>

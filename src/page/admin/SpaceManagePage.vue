<script setup lang="ts">
import {reactive, computed, ref, onMounted, h} from "vue"
import {deletePicture, doReviewPicture, getPictureByPage} from "@/api/pictureController.ts";
import {message} from "ant-design-vue";

import dayjs from "dayjs";
import {BarChartOutlined, LoadingOutlined} from "@ant-design/icons-vue";
import {
  getAllPictureReviewStatus,
  getPictureReviewStatusByValue, getPictureReviewStatusOptions, getPictureReviewStatusText,
  PictureReviewStatusEnum
} from "../../enums/PictureReviewEnum.ts";

// 使用 type 别名，兼容 verbatimModuleSyntax（import = 只能用于值）
type Picture = API.Picture;
import {clearCacheHome} from "@/api/huancunjiekou.ts";
import {deleteSpace, getSpaceByPage} from "@/api/spaceController.ts";
import {
  SPACE_LEVEL_MAP,
  SPACE_LEVEL_OPTIONS,
  SPACE_TYPE_MAP,
  SPACE_TYPE_OPTIONS
} from "@/constants/space.ts";
import {formatSize} from "../../utils";
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '空间名称',
    dataIndex: 'spaceName',
  },
  {
    title: '空间级别',
    dataIndex: 'spaceLevel',
  },
  {
    title: '空间类型',
    dataIndex: 'spaceType',
  },
  {
    title: '使用情况',
    dataIndex: 'spaceUseInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]


const dataList = ref<API.Space[]>([])
// 总数据
const total = ref(0)


// 搜索参数
const searchParams = reactive<API.SpaceQueryRequest>({
  currentNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'desc',
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.currentNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共${total}条`,
  }
})

const fetchData = async () => {
  const {data} = await getSpaceByPage(searchParams,{})
  if (data.code === 0 && data.data) {
    dataList.value = data.data.records as [];
    total.value = data.data?.total ?? 0;

  }else {
    message.error(`获取数据失败,${data.message}`);
  }
}

onMounted(() => {
  fetchData()
})

const doTableChange = (page: any) => {
  searchParams.currentNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}


const doDelete = async (id : any) => {
  const res = await deleteSpace({id: id},{})
  if (res.data.code === 0 && res.data.data) {
    message.success("删除成功")
    // 刷新数据
    await fetchData()
  }else {
    message.error(`删除失败,${res.data.message}`);
  }
}

const doSearch = async (page: any) => {
  // 重置到第一页
  searchParams.currentNum = 1;
  // 刷新数据
  await fetchData()
}

const resetData = async () => {
  await fetchData()
  message.success("刷新成功")
}



const handleReview = async (record: Picture, reviewStatus: number) => {
  console.log(record.reviewStatus)
  const reviewMessage = reviewStatus === PictureReviewStatusEnum.PASS ? "管理员操作通过" : "管理员操作拒绝";
  const pictureReviewParam: API.PictureReviewRequest = {
    id: record.id,
    reviewMessage: reviewMessage,
    reviewStatus: reviewStatus,
  }
  const res = await doReviewPicture(pictureReviewParam)

  if (res.data.code === 0 && res.data.data) {
    message.success("审核操作成功")
    fetchData()
  }else {
    message.error(`审核操作失败，原因：${res.data.message}`);
  }
}



</script>

<template>
  <div id="PictureManagePage">
    <a-flex justify="space-between">
      <h2 class="title">空间管理</h2>
      <a-flex>
        <a-space >
          <a-button type="primary" href="/add_space" target="_blank">+ 创建空间</a-button>
          <a-button
            type="primary"
            ghost
            :href="`/space_analyze?queryPublic=1`"
            target="_blank"
          >
            分析公共图库
          </a-button>

          <a-button
            type="primary"
            ghost
            :href="`/space_analyze?queryAll=1`"
            target="_blank"
          >
            分析全空间
          </a-button>


        </a-space>

      </a-flex>

    </a-flex>

    <a-form
      style="margin-bottom: 2rem"
      layout="inline"
      :model="searchParams"
      autocomplete="off"
      @finish="doSearch">
      <a-form-item label="空间名称" name="spaceName">
        <a-input v-model:value="searchParams.spaceName" placeholder="请输入空间名称" allow-clear/>
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select style="min-width: 200px" v-model:value="searchParams.spaceLevel" :options="SPACE_LEVEL_OPTIONS" placeholder="请输入级别" allow-clear/>
      </a-form-item>

      <a-form-item label="空间类型" name="spaceType">
        <a-select style="min-width: 200px"
                  v-model:value="searchParams.spaceType"
                  :options="SPACE_TYPE_OPTIONS"
                  placeholder="请输入类型"
                  allow-clear
        />
      </a-form-item>

      <a-form-item label="用户ID" name="userId">
        <a-input v-model:value="searchParams.userId" placeholder="请输入用户id" allow-clear/>
      </a-form-item>

      <a-form-item>
        <a-button html-type="submit" type="primary">搜索</a-button>
      </a-form-item>

    </a-form>
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      :scroll="{x: '1400'}"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image style="width: 80px" :src="record.url" alt="图片"/>
        </template>

        <template v-if="column.dataIndex === 'spaceUseInfo'">
          <div>已使用: {{formatSize(record.totalSize)}} / {{formatSize(record.maxSize)}}</div>
          <div>数量: {{record.totalCount}} / {{record.maxCount}}</div>
        </template>

        <template v-if="column.dataIndex === 'spaceLevel'">
          <a-tag>
            {{SPACE_LEVEL_MAP[record.spaceLevel]}}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'spaceType'">
          <a-tag>
            {{SPACE_TYPE_MAP[record.spaceType]}}
          </a-tag>
        </template>

        <template v-if="column.dataIndex === 'createTime'">
          {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>

        <template v-if="column.dataIndex === 'editTime'">
          {{dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>

        <template v-if="column.key === 'action'">

          <a-space style="width: 120px">
            <a-button type="link" :href="`/add_space?id=${record.id}`" target="_blank">编辑</a-button>
            <a-button
              type="link"
              :href="`/space_analyze?spaceId=${record.id}`"
              target="_blank"
            >
              分析
            </a-button>

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

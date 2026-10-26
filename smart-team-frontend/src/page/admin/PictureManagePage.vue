<script setup lang="ts">
import {reactive, computed, ref, onMounted, h} from "vue"
import {deletePicture, doReviewPicture, getPictureByPage} from "@/api/pictureController.ts";
import {message} from "ant-design-vue";

import dayjs from "dayjs";
import {LoadingOutlined} from "@ant-design/icons-vue";
import {
  getAllPictureReviewStatus,
  getPictureReviewStatusByValue, getPictureReviewStatusOptions, getPictureReviewStatusText,
  PictureReviewStatusEnum
} from "../../enums/PictureReviewEnum.ts";

// 使用 type 别名，兼容 verbatimModuleSyntax（import = 只能用于值）
type Picture = API.Picture;
import {clearCacheHome} from "@/api/huancunjiekou.ts";
import {SPACE_LEVEL_MAP} from "@/constants/space.ts";
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '图片',
    dataIndex: 'url',
    width: 100,
  },
  {
    title: '名称',
    dataIndex: 'name',
    ellipsis: true,
    width: 150
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
    width: 200
  },
  {
    title: '类型',
    dataIndex: 'category',
    width: 100,
  },
  {
    title: '标签',
    dataIndex: 'tags',
    width: 150
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
    width: 150
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 200,
  },
  {
    title: '审核信息',
    dataIndex: 'reviewInfo',
    width: 300,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
    width: 160
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
  },
]

const dataList = ref<API.Picture[]>([])
// 总数据
const total = ref(0)


// 搜索参数
const searchParams = reactive<API.PictureQueryRequest>({
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
  const {data} = await getPictureByPage(searchParams,{})
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
  const res = await deletePicture({id: id},{})
  console.log("res", res)
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


const clearHomeCache = async () => {
  const res = await clearCacheHome()
  if (res.data.code === 0 && res.data.data) {
    message.success("清除主页缓存成功");
  }else {
    message.error(res.data.message);
  }
}

</script>

<template>
  <div id="PictureManagePage">
    <a-flex justify="space-between">
      <h2 class="title">图片管理</h2>
      <a-flex>
        <a-space >
          <a-button type="link" @click="clearHomeCache">清除主页缓存</a-button>
          <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
          <a-button href="/add_picture/batch" target="_blank" >+ 批量添加图片</a-button>
        </a-space>

      </a-flex>

    </a-flex>

    <a-form
      style="margin-bottom: 2rem"
      layout="inline"
      :model="searchParams"
      autocomplete="off"
      @finish="doSearch">
      <a-form-item label="关键词" name="searchText">
        <a-input v-model:value="searchParams.searchText" placeholder="请输入关键词" allow-clear/>
      </a-form-item>

      <a-form-item label="类型" name="category">
        <a-input v-model:value="searchParams.category" placeholder="请输入类型" allow-clear/>
      </a-form-item>

      <a-form-item label="标签" name="tags">
        <a-select mode="tags" style="min-width: 200px" v-model:value="searchParams.tags" placeholder="请输入标签" allow-clear/>
      </a-form-item>

      <a-form-item label="审核状态" name="tags">
        <a-select style="min-width: 200px" v-model:value="searchParams.reviewStatus" :options="getPictureReviewStatusOptions()" placeholder="请输入审核状态" allow-clear/>
      </a-form-item>

      <a-form-item>
        <a-button html-type="submit" type="primary">搜索</a-button>
      </a-form-item>

      <a-form-item>
        <a-button  shape="circle" type="link" @click="resetData">
          <ReloadOutlined style="color: #777575"  />
        </a-button>
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

        <template v-if="column.dataIndex === 'reviewInfo'">
          <div>审核状态: {{ getPictureReviewStatusText(record.reviewStatus) }}</div>
          <div>审核id: {{ record.reviewerId ?? "待审核"}}</div>
          <div>审核信息: {{ record.reviewMessage ?? "待审核"}}</div>
        </template>

        <template v-if="column.dataIndex === 'tags'">
          <a-tag v-for="item of JSON.parse(record.tags)" :key="item.id">
            {{item}}
          </a-tag>
        </template>

        <template v-if="column.dataIndex === 'picInfo'">
          <div>格式: {{ record.picFormat}}</div>
          <div>宽度: {{ record.picWidth}}</div>
          <div>高度: {{ record.picHeight}}</div>
          <div>大小: {{ (record.picSize / 1024).toFixed(2)}}KB</div>
        </template>


        <template v-if="column.dataIndex === 'createTime'">
          {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>

        <template v-if="column.dataIndex === 'editTime'">
          {{dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>

        <template v-if="column.key === 'action'">

          <a-space style="width: 120px" wrap>
            <a-button v-if="record.reviewStatus !== PictureReviewStatusEnum.PASS" type="primary" @click="handleReview(record, PictureReviewStatusEnum.PASS)">通过</a-button>
            <a-button v-if="record.reviewStatus !== PictureReviewStatusEnum.REJECT" danger @click="handleReview(record, PictureReviewStatusEnum.REJECT)">拒绝</a-button>
            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">编辑</a-button>

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

<script setup lang="ts">
import {onMounted, reactive, ref, h, computed} from 'vue'
import { getPictureVoByPage, searchPictureByColor } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/picture/PictureList.vue'
import { getSpaceVoById } from '@/api/spaceController.ts'
import { formatSize } from '../../utils'
import PictureSearchForm from '@/components/picture/PictureSearchForm.vue'
import { AuColorPicker } from '@aurouscia/au-color-picker'
import BatchEditModal from '@/components/picture/BatchEditModal.vue'
import {
  BarChartOutlined,
  DiffOutlined,
  PlusSquareOutlined,
  TeamOutlined
} from "@ant-design/icons-vue";
import {SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM, SPACE_TYPE_MAP} from "../../constants/space.ts";

// 初始化组件参数
const props = defineProps<{
  spaceId: string | number
  id: string | number
}>()

// 当前空间信息
const space = ref<API.SpaceVO>({})

onMounted(() => {
  // 获取空间详情信息
  fetchSpaceDetail()
  // 获取当前空间所有图片数据
  fetchData()
})

// 私人图库图片数据
const dataList = ref<API.PictureVO[]>()

// 搜索参数 - 初始化
const searchParams = ref<API.PictureQueryRequest>({
  currentNum: 1, // 当前页码
  pageSize: 12, // 每页大小
  sortField: 'createTime', // 按照创建时间
  sortOrder: 'desc', // desc - 倒序 | asc - 顺序
})

// 等待状态显示
const loading = ref(true)
// 图片的总数量
const total = ref(0)

// 获取空间详情
const fetchSpaceDetail = async () => {
  const res = await getSpaceVoById({ id: props.id as number })
  if (res.data.code === 0 && res.data.data) {
    space.value = res.data.data
  } else {
    message.error('获取空间详情失败，' + res.data.message)
  }
}

/**
 * 获取空间的图片集合数据
 */
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params: API.PictureQueryRequest = {
    spaceId: props.id as number,
    ...searchParams.value,
    nullSpaceId: false,
  }

  const res = await getPictureVoByPage(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 分页参数修改回调
 * @param page 页码
 * @param size 每页大小
 */
const onPageChange = (page: number, size: number) => {
  searchParams.value.currentNum = page
  searchParams.value.pageSize = size
  // 刷新数据
  fetchData()
}

/**
 * 搜索栏提交按钮 - 回调处理
 * @param newSearchParams
 */
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  // 构建新查询条件
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    currentNum: 1,
  }
  // 刷新数据
  fetchData()
}

/**
 * 颜色选择器 - 回调
 * @param color
 */
const doColorPicker = async (color: string) => {
  loading.value = true
  // 构建请求参数
  let params: API.SearchPictureByColorRequest = {
    spaceId: space.value.id,
    mainColor: color,
  }
  // 发送参数
  const res = await searchPictureByColor(params)
  if (res.data.code === 0 && res.data.data) {
    // 获取图片数据
    dataList.value = res.data.data
  } else {
    message.error(res.data.message)
  }

  loading.value = false
}

// 批量编辑 - 组件引用
const batchEditModalRef = ref()

/**
 * 批量编辑图片
 */
const doBatchEditPicture = () => {
  if (batchEditModalRef.value) {
    batchEditModalRef.value.openModal()
  }
}

/**
 * 处理批量编辑弹窗，成功回调
 */
const handleSuccess = () => {
  // 刷新数据
  fetchData()
}


const createPermissionChecker = (permission: string) => {
  return computed(() => {
    return (space.value.permissions ?? []).includes(permission)
  })
}
/**
 * 权限控制
 */
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
// 删除按钮需要删除权限，不能复用编辑权限
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
const canUserManage = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUpload = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
// 批量编辑属于编辑操作，使用编辑权限
const canBatchEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canView = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_VIEW)

const isPrivateSpace =  computed(() => space.value.spaceType === SPACE_TYPE_ENUM.PRIVATE)

</script>

<template>
  <div class="header">
    <a-flex justify="space-between" align="middle">
      <h2 class="title">{{ space.spaceName }} {{SPACE_TYPE_MAP[space.spaceType ?? 0]}}</h2>
      <a-flex>
        <a-space :size="20">
          <a-button v-if="canUpload" type="primary" :href="`/add_picture?spaceId=${space.id}`" target="_blank"
                    :icon="h(PlusSquareOutlined)"
            >创建图片
          </a-button>
          <a-button v-if="canUserManage && !isPrivateSpace" type="primary" :href="`/spaceUserManage/${space.id}`" ghost target="_blank" :icon="h(TeamOutlined)">
            成员管理
          </a-button>
          <a-button v-if="canEdit" type="primary" ghost :icon="h(BarChartOutlined)" :href="`/space_analyze?spaceId=${props.id}`" target="_blank">空间分析</a-button>
          <a-button type="default" @click="doBatchEditPicture" :icon="h(DiffOutlined)" v-if="canBatchEdit">批量编辑图片 </a-button>
          <a-tooltip placement="bottomLeft">
            <template #title>
              <div>空间使用进度</div>
              <div>
                空间大小：{{ formatSize(space.totalSize) }} / {{ formatSize(space.maxSize) }}
              </div>
              <div>空间数量: {{ space.totalCount }}张 / {{ space.maxCount }}张</div>
            </template>
            <a-progress
              type="circle"
              :percent="(((space.totalSize ?? 0) * 100) / (space.maxSize ?? 0)).toFixed(1)"
              :size="40"
              :showInfo="true"
              :status="
                ((space.totalSize ?? 0) * 100) / (space.maxSize ?? 0) >= 100
                  ? 'exception'
                  : undefined
              "
            />
          </a-tooltip>
        </a-space>
      </a-flex>
    </a-flex>
  </div>
  <div style="height: 20px"></div>
  <!--  搜索拦-->
  <PictureSearchForm :on-search="onSearch" />

  <!-- 颜色识别 -->
  <a-flex style="margin-top: 20px">
    <a-space :size="10">
      <span>颜色识别:</span>
      <AuColorPicker :initial="'#000000'" @done="doColorPicker"></AuColorPicker>
    </a-space>
  </a-flex>

  <div style="height: 50px"></div>

  <PictureList
    :data-list="dataList ?? []"
    :do-re-load="fetchData"
    :show-operation="true"
    :canEdit="canEdit"
    :canDelete="canDelete"
  ></PictureList>
  <a-pagination
    v-if="space"
    style="text-align: right"
    v-model:current="searchParams.currentNum"
    v-model:page-size="searchParams.pageSize"
    :total="total"
    :show-total="() => `图片总数${total} / ${space.maxCount}`"
    @change="onPageChange"
  />

  <BatchEditModal
    :picture-list="dataList ?? []"
    :on-success="handleSuccess"
    :space-id="Number(props.id)"
    ref="batchEditModalRef"
  />
</template>

<style scoped>
.search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}

.tag-bar {
  margin-bottom: 16px;
}
</style>

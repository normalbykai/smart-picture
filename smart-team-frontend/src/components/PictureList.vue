<script setup lang="ts">
import { onMounted, ref, withDefaults } from 'vue'
import { useRouter } from 'vue-router'
import { deletePicture } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import ShareModal from "@/components/ShareModal.vue";

const router = useRouter()

type RenderItem = { item: API.PictureVO }

interface Props {
  dataList?: API.PictureVO[]
  doReLoad?: () => void
  showOperation?: boolean
  canEdit?: boolean
  canDelete?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  showOperation: false,
  canEdit: false,
  canDelete: false,
})

const loading = ref(false)

// 图片进行识图
const doImgSearch = (picture: API.PictureVO) => {
  // 跳转识图页面
  router.replace({
    path: '/picture_search',
    query: {
      pictureId: picture.id,
    },
  })
}

// 图片进行编辑操作
const doEdit = (picture: API.PictureVO) => {
  // 跳转到编辑页面
  router.replace({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 图片进行删除操作
const doDelete = async (picture: API.PictureVO) => {
  const res = await deletePicture({ id: picture.id })
  if (res.data.code === 0 && res.data.data) {
    message.success('删除成功')
    // 调用外部的执行成功方法 --- 外部刷新数据
    props.doReLoad?.()
  } else {
    message.error('删除失败,原因:' + res.data.message)
  }
}

// 分享弹窗引用
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()
/**
 * 分享操作
 */
const doShare = (picture: API.PictureVO) => {
  // 构建分享链接
  shareLink.value = `${window.location.protocol}://${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }

}




</script>

<template>
  <div id="pictureList">
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 2, lg: 3, xl: 4, xxl: 5 }"
      :data-source="props.dataList"
      :loading="loading"
      size="middle"
    >
      <template #renderItem="{ item }: RenderItem">
        <a-list-item style="padding: 0">
          <!--        单张图片-->
          <a-card style="width: 100%" hoverable @click="router.push(`/picture/${item.id}`)">
            <template #cover>
              <img
                v-lazy="item.thumbnailUrl ?? item.url"
                alt="item.name"
                class="img-card"
                style="height: 350px; object-fit: cover"
              />
            </template>

            <a-card-meta :title="item.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ item.category ?? '默认' }}
                  </a-tag>

                  <a-tag v-for="tag in item.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>

            <template #actions v-if="showOperation">
              <!-- 识图-->
              <a-tooltip>
                <template #title>识图</template>
                <SearchOutlined @click.stop="doImgSearch(item)" />
              </a-tooltip>
              <!-- 编辑 -->
              <a-tooltip v-if="canEdit">
                <template #title >编辑</template>
                <EditOutlined @click.stop="doEdit(item)" />
              </a-tooltip>
              <!-- 分享 -->
              <a-tooltip>
                <template #title>分享</template>
                <ShareAltOutlined @click.stop="doShare(item)" />
              </a-tooltip>
              <!-- 删除 -->
              <a-tooltip v-if="canDelete">
                <template #title >删除</template>
                <DeleteOutlined @click.stop="doDelete(item)" />
              </a-tooltip>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

    <ShareModal ref="shareModalRef" title="分享图片" :link="shareLink"></ShareModal>
  </div>
</template>

<style scoped>
.img-card {
  object-fit: cover;
  object-position: center;
  transition: transform 0.3s ease;
  /* 提高图片渲染质量 */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
  -webkit-font-smoothing: antialiased;
  backface-visibility: hidden;
  transform: translateZ(0);
  width: 100%;
}
</style>

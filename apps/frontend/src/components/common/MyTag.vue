<script setup lang="ts">

import {computed} from "vue";
import AccessEnum from "@/enums/AccessEnum.ts";

interface MyTagProps {
  value: string;
  role: string;
}
const props = defineProps<MyTagProps>()

const colorMap = {
  [AccessEnum.ADMIN] : "#f50",
  [AccessEnum.USER] : "#87d068",
}

const tagColor = computed(() => {
  let default_color = "#2db7f5";
  return colorMap[props.role] ?? default_color
})
</script>

<template>
  <a-tag :color="tagColor">
    {{value}}
  </a-tag>
</template>

<style scoped>

</style>

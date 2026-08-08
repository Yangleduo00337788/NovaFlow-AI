<template>
  <span class="form-label-with-tip">
    {{ label }}
    <FieldHelpIcon :text="tip" />
  </span>
</template>

<script setup lang="ts">
import FieldHelpIcon from '@/components/common/FieldHelpIcon.vue'

defineProps<{
  label: string
  tip: string
}>()
</script>

<style scoped>
.form-label-with-tip {
  display: inline-flex;
  align-items: center;
}
</style>

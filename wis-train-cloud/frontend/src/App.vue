<template>
  <router-view />
</template>

<script setup>
// 根组件：路由容器
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }
body {
  background: var(--bg);
  color: var(--text);
  font-family: var(--sans);
  font-size: var(--fs-body);
  letter-spacing: .02em;
}
</style>

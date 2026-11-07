<template>
    <div class="layout-container">
        <!-- 主内容区域 -->
        <main class="main-content">
            <router-view />
        </main>
    </div>
</template>

<script setup>
// 布局组件已简化，移除侧边栏
</script>

<style scoped lang="scss">
.layout-container {
    display: flex;
    height: 100vh;
    overflow: hidden;
}

.main-content {
    flex: 1;
    width: 100%;
    overflow-y: auto;
    background-color: #f0f2f5;
}
</style>

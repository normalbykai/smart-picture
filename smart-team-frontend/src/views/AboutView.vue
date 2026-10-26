<script setup lang="ts">
import { ref, onMounted } from 'vue';

const features = [
  {
    title: '简单易用',
    desc: '直观的界面设计，轻松上手图片管理'
  },
  {
    title: '安全可靠',
    desc: '严格的权限控制，确保图片安全'
  },
  {
    title: '智能分类',
    desc: '自动化的标签系统，让管理更轻松'
  },
  {
    title: '高效搜索',
    desc: '强大的搜索功能，快速定位目标图片'
  }
];

const animationReady = ref(false);

onMounted(() => {
  setTimeout(() => {
    animationReady.value = true;
  }, 100);
});
</script>

<template>
  <div class="about-container">
    <!-- 头部区域 -->
    <div class="header-section" :class="{ 'animate': animationReady }">
      <h1>关于常态云图库</h1>
      <div class="divider"></div>
      <p class="subtitle">简单而强大的图片管理平台</p>
    </div>

    <!-- 介绍区域 -->
    <div class="intro-section">
      <p>
        常态云图库致力于为用户提供专业的图片管理解决方案。
        无论是个人用户还是企业团队，都能在这里找到理想的图片管理工具。
      </p>
    </div>

    <!-- 特性区域 -->
    <div class="features-section">
      <div class="features-grid">
        <div v-for="(feature, index) in features"
             :key="index"
             class="feature-card"
             :style="{ animationDelay: `${index * 0.1}s` }">
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
        </div>
      </div>
    </div>

    <!-- 技术支持区域 -->
    <div class="tech-section">
      <h2>技术支持</h2>
      <div class="tech-list">
        <div class="tech-item">Vue 3</div>
        <div class="tech-item">TypeScript</div>
        <div class="tech-item">Ant Design Vue</div>
        <div class="tech-item">Spring Boot</div>
      </div>
    </div>

    <!-- 页脚区域 -->
    <div class="footer-section">
      <p>© 2025 常态云图库 版本 1.0.0</p>
    </div>
  </div>
</template>

<style scoped>
.about-container {
  width: 100%;
  max-width: 1700px;
  margin: 0 auto;
  padding: 40px 20px;
  color: #333;
}

.header-section {
  text-align: center;
  margin-bottom: 60px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease-out;
}

.header-section.animate {
  opacity: 1;
  transform: translateY(0);
}

h1 {
  font-size: 2.5rem;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 16px;
}

.divider {
  width: 60px;
  height: 3px;
  background: #1890ff;
  margin: 0 auto 16px;
}

.subtitle {
  font-size: 1.2rem;
  color: #666;
  margin: 0;
}

.intro-section {
  text-align: center;
  max-width: 800px;
  margin: 0 auto 60px;
  line-height: 1.8;
  color: #666;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  margin-bottom: 60px;
}

.feature-card {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  animation: fadeIn 0.5s ease forwards;
  opacity: 0;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.feature-card h3 {
  color: #1890ff;
  margin-bottom: 12px;
  font-size: 1.2rem;
}

.feature-card p {
  color: #666;
  margin: 0;
  line-height: 1.6;
}

.tech-section {
  text-align: center;
  margin-bottom: 60px;
}

.tech-section h2 {
  font-size: 1.8rem;
  color: #2c3e50;
  margin-bottom: 24px;
  font-weight: 500;
}

.tech-list {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
}

.tech-item {
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 4px;
  color: #666;
  font-size: 0.9rem;
}

.footer-section {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  margin-top: 60px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .about-container {
    padding: 20px;
  }

  h1 {
    font-size: 2rem;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .tech-list {
    flex-direction: column;
    align-items: center;
  }
}
</style>

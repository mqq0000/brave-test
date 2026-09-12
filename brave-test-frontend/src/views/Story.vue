<template>
  <div>
    <!-- 章节选择 -->
    <div v-if="!current">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px"
        title="每一次选择都会影响你的萌芽：善意带来净化，冷漠与逃避让污染渗入" />
      <el-row :gutter="16">
        <el-col v-for="c in chapters" :key="c.id" :span="8" style="margin-bottom: 16px">
          <el-card :class="{ locked: !c.unlocked }">
            <div class="ch-seq">第{{ c.seq }}章</div>
            <div class="ch-title">{{ c.title }}</div>
            <el-tag v-if="c.completed" type="success" size="small">已完成</el-tag>
            <el-tag v-else-if="c.inProgress" type="warning" size="small">进行中</el-tag>
            <el-tag v-else-if="!c.unlocked" type="info" size="small">未解锁</el-tag>
            <el-button v-if="c.unlocked" type="primary" style="width: 100%; margin-top: 12px" @click="onEnter(c)">
              {{ c.inProgress ? '继续冒险' : '开始本章' }}
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 剧情渲染 -->
    <el-card v-else>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>{{ currentTitle }}</span>
          <el-button size="small" @click="backToList">返回章节</el-button>
        </div>
      </template>
      <div class="story-text">{{ current.content }}</div>
      <el-divider />
      <div class="choices">
        <el-button v-for="opt in current.options" :key="opt.id" class="choice-btn" @click="onChoose(opt)">
          {{ opt.text }}
        </el-button>
      </div>
      <div v-if="finished" class="finished">
        <el-result icon="success" title="本章完结" sub-title="你的每一个选择，都在塑造你的萌芽。" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listChapters, enterChapter, chooseOption } from '../api'

const chapters = ref([])
const current = ref(null)
const currentTitle = ref('')
const finished = ref(false)

async function load() {
  chapters.value = await listChapters()
}

async function onEnter(c) {
  current.value = await enterChapter(c.id)
  currentTitle.value = c.title
  finished.value = false
}

async function onChoose(opt) {
  const res = await chooseOption(opt.id)
  if (res.finished) {
    finished.value = true
    ElMessage.success(res.message)
    load()
  } else {
    current.value = res
  }
}

function backToList() {
  current.value = null
  load()
}

onMounted(load)
</script>

<style scoped>
.locked { opacity: 0.55; }
.ch-seq { color: #909399; font-size: 12px; }
.ch-title { font-size: 16px; font-weight: 500; margin: 6px 0 12px; }
.story-text {
  font-size: 15px;
  line-height: 2;
  white-space: pre-wrap;
  color: #303133;
  background: #f8f9fb;
  border-radius: 8px;
  padding: 20px 24px;
}
.choices { display: flex; flex-direction: column; gap: 10px; }
.choice-btn { justify-content: flex-start; text-align: left; }
.finished { margin-top: 16px; }
</style>

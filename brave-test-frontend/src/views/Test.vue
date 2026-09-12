<template>
  <div>
    <el-alert type="info" :closable="false" style="margin-bottom: 16px"
      title="觉醒测试：了解自己的萌芽类型。答案没有对错，但回避与怨怼会让污染悄悄渗入" />

    <el-card v-if="myTitle" style="margin-bottom: 16px">
      <div class="my-title">
        <span class="badge">{{ myTitle.title }}</span>
        <span class="hint">你的最新觉醒称号</span>
      </div>
    </el-card>

    <el-card v-for="(q, qi) in questions" :key="q.id" style="margin-bottom: 16px">
      <div class="q-content">{{ qi + 1 }}. {{ q.content }}</div>
      <el-radio-group v-model="answers[q.id]" class="q-options">
        <el-radio v-for="(text, oi) in q.options" :key="oi" :value="oi" border>{{ text }}</el-radio>
      </el-radio-group>
    </el-card>

    <el-card v-if="questions.length">
      <el-button type="primary" size="large" style="width: 100%" :loading="submitting" @click="onSubmit">
        提交作答，唤醒我的萌芽
      </el-button>
    </el-card>

    <el-dialog v-model="resultVisible" title="觉醒完成" width="460px" :close-on-click-modal="false">
      <el-result :icon="icon" :title="result?.title">
        <template #sub-title>
          <div style="text-align: left; line-height: 2">
            <div>勇气 <b>{{ result?.courage }}</b> ｜ 理性 <b>{{ result?.rationality }}</b> ｜ 仁善 <b>{{ result?.kindness }}</b></div>
            <div v-if="result?.pollutionDelta > 0" style="color: #f56c6c">
              你选择了 {{ result.pollutionDelta }} 点怨怼，萌芽初始污染 +{{ result.pollutionDelta }}
            </div>
            <div v-else style="color: #67c23a">你的作答干净澄澈，萌芽未受污染</div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">带着萌芽出发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { testQuestions, submitTest, myTestResult } from '../api'

const questions = ref([])
const answers = ref({})
const submitting = ref(false)
const resultVisible = ref(false)
const result = ref(null)
const myTitle = ref(null)

const icon = ref('success')

async function load() {
  questions.value = await testQuestions()
  myTitle.value = await myTestResult()
}

async function onSubmit() {
  const payload = questions.value.map((q) => {
    const v = answers.value[q.id]
    return { questionId: q.id, optionIndex: v === undefined ? -1 : v }
  })
  if (payload.some((p) => p.optionIndex < 0)) {
    ElMessage.warning('还有题目未作答')
    return
  }
  submitting.value = true
  try {
    result.value = await submitTest(payload)
    icon.value = result.value.pollutionDelta > 0 ? 'warning' : 'success'
    resultVisible.value = true
    myTitle.value = await myTestResult()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.q-content { font-size: 15px; font-weight: 500; margin-bottom: 14px; }
.q-options { display: flex; flex-direction: column; gap: 10px; align-items: stretch; }
.q-options .el-radio { margin-right: 0; height: auto; padding: 10px 14px; white-space: normal; }
.my-title { display: flex; align-items: center; gap: 12px; }
.badge {
  background: linear-gradient(120deg, #67c23a, #378add);
  color: #fff;
  padding: 6px 18px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 500;
}
.hint { color: #909399; font-size: 12px; }
</style>

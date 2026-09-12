<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap">
        <el-radio-group v-model="sourceType" @change="load">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="1">行者善事</el-radio-button>
          <el-radio-button :value="2">冒险者委托</el-radio-button>
        </el-radio-group>
        <el-select v-model="level" placeholder="等级" clearable style="width: 120px" @change="load">
          <el-option v-for="lv in ['D', 'C', 'B', 'A', 'S']" :key="lv" :label="lv + '级'" :value="lv" />
        </el-select>
        <el-button type="primary" @click="publishVisible = true">发布委托</el-button>
        <el-button @click="applyVisible = true">向行者协会求助</el-button>
      </div>
    </el-card>

    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="任务大厅" name="hall" />
      <el-tab-pane label="我发布的" name="published" />
      <el-tab-pane label="我接取的" name="accepted" />
    </el-tabs>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="title" label="任务" min-width="180" show-overflow-tooltip />
      <el-table-column label="来源" width="110">
        <template #default="{ row }">
          <el-tag :type="row.sourceType === 1 ? 'success' : 'primary'" effect="plain">
            {{ row.sourceType === 1 ? '行者善事' : '冒险者委托' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="70">
        <template #default="{ row }"><el-tag>{{ row.taskLevel }}</el-tag></template>
      </el-table-column>
      <el-table-column label="接取者所得" width="110">
        <template #default="{ row }">{{ row.acceptorGold }} 金币</template>
      </el-table-column>
      <el-table-column label="净化值" width="80">
        <template #default="{ row }">
          <span v-if="row.purifyValue > 0" style="color: #67c23a">-{{ row.purifyValue }} 污染</span>
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <template v-if="tab === 'hall'">
            <el-button size="small" type="primary" :disabled="row.status !== 1" @click="onAccept(row)">接单</el-button>
          </template>
          <template v-else-if="tab === 'published' && row.status === 3">
            <el-button size="small" type="success" @click="onVerify(row, true)">验收通过</el-button>
            <el-button size="small" type="danger" @click="onVerify(row, false)">判失败</el-button>
          </template>
          <template v-else-if="tab === 'accepted' && row.status === 2">
            <el-button size="small" type="warning" @click="onSubmit(row)">提交成果</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发布委托 -->
    <el-dialog v-model="publishVisible" title="发布冒险者委托（预付托管，协会抽成10%）" width="480px">
      <el-form :model="publishForm" label-width="90px">
        <el-form-item label="标题"><el-input v-model="publishForm.title" /></el-form-item>
        <el-form-item label="详情"><el-input v-model="publishForm.description" type="textarea" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="publishForm.taskLevel" style="width: 100%">
            <el-option v-for="lv in ['D', 'C', 'B', 'A', 'S']" :key="lv" :value="lv"
              :label="`${lv} 级（${levelRange(lv)}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="报酬金币"><el-input-number v-model="publishForm.rewardGold" :min="80" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="doPublish">托管发布</el-button>
      </template>
    </el-dialog>

    <!-- 行者求助 -->
    <el-dialog v-model="applyVisible" title="向行者协会提出任务帮助申请" width="480px">
      <el-form label-width="60px">
        <el-form-item label="标题"><el-input v-model="applyForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="applyForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="doApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTasks, publishTask, acceptTask, submitTask, verifyTask, myPublishedTasks, myAcceptedTasks, xingzheApply } from '../api'

const tab = ref('hall')
const rows = ref([])
const loading = ref(false)
const sourceType = ref(null)
const level = ref(null)
const publishVisible = ref(false)
const applyVisible = ref(false)
const publishForm = ref({ title: '', description: '', taskLevel: 'D', rewardGold: 100 })
const applyForm = ref({ title: '', content: '' })

const LEVEL_RANGE = { D: '80~120', C: '800~1200', B: '8000~1.2万', A: '8万~12万', S: '12万以上' }
const levelRange = (lv) => LEVEL_RANGE[lv] || ''
const STATUS = ['待审核', '发布中', '进行中', '待验收', '已完成', '失败', '已驳回', '已下架']
const statusText = (s) => STATUS[s] ?? s

async function load() {
  loading.value = true
  try {
    if (tab.value === 'hall') rows.value = await listTasks({ sourceType: sourceType.value, level: level.value })
    else if (tab.value === 'published') rows.value = await myPublishedTasks()
    else rows.value = await myAcceptedTasks()
  } finally {
    loading.value = false
  }
}

async function doPublish() {
  await publishTask(publishForm.value)
  ElMessage.success('发布成功，等待冒险者协会审核')
  publishVisible.value = false
  tab.value = 'published'
  load()
}

async function doApply() {
  await xingzheApply(applyForm.value)
  ElMessage.success('申请已提交，等待行者协会审批')
  applyVisible.value = false
}

async function onAccept(row) {
  const isCross = await checkCross(row)
  await acceptTask(row.id, isCross)
  ElMessage.success('接单成功！完成后提交成果等待验收')
  load()
}

async function checkCross(row) {
  const profile = JSON.parse(localStorage.getItem('profile') || 'null')
  const order = ['D', 'C', 'B', 'A', 'S']
  const own = profile ? order.indexOf(profile.rankLevel) : 0
  const target = order.indexOf(row.taskLevel)
  if (target > own) {
    await ElMessageBox.confirm(
      `这是越级挑战（你 ${order[own]} 级 → 任务 ${row.taskLevel} 级）！成功将直升 ${row.taskLevel} 级，失败将被冒险者协会暂停开放一个月。确认接单？`,
      '越级警告', { type: 'warning', confirmButtonText: '我已知晓风险，坚持接单' })
    return true
  }
  return false
}

async function onSubmit(row) {
  await submitTask(row.id)
  ElMessage.success('已提交，等待发布者验收')
  load()
}

async function onVerify(row, success) {
  await verifyTask(row.id, success)
  ElMessage.success(success ? '验收通过，报酬与净化已发放' : '已判定失败（越级任务将封禁一个月）')
  load()
}

onMounted(load)
</script>

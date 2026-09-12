<template>
  <div>
    <el-alert :title="`当前角色：${roleName}`" type="info" :closable="false" style="margin-bottom: 16px" />

    <!-- 冒险者协会管理员 -->
    <el-card v-if="isRole('ADVENTURER_ADMIN', 'SUPER_ADMIN')" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>冒险者协会 · 待审核任务</span>
          <el-button size="small" type="success" @click="doListPurifier">上架净化剂到商城</el-button>
        </div>
      </template>
      <el-table :data="tasks" stripe size="small">
        <el-table-column prop="title" label="任务" min-width="160" />
        <el-table-column prop="taskLevel" label="等级" width="70" />
        <el-table-column prop="rewardGold" label="托管金币" width="110" />
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="onAuditTask(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="onAuditTask(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 行者协会管理员 -->
    <el-card v-if="isRole('XINGZHE_ADMIN', 'SUPER_ADMIN')" style="margin-bottom: 16px">
      <template #header>行者协会 · 待审批求助申请</template>
      <el-table :data="applies" stripe size="small">
        <el-table-column prop="title" label="申请" min-width="160" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="onAuditApply(row, true)">通过并发布</el-button>
            <el-button size="small" type="danger" @click="onAuditApply(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 知识宝库管理员 -->
    <el-card v-if="isRole('KNOWLEDGE_ADMIN', 'SUPER_ADMIN')" style="margin-bottom: 16px">
      <template #header>知识宝库 · 待筛查投稿</template>
      <el-table :data="contributions" stripe size="small">
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openPackage(row)">封装定价上架</el-button>
            <el-button size="small" type="danger" @click="onPackage(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 12px; display: flex; gap: 8px; align-items: center">
        <el-input v-model="offender" placeholder="违规冒险者用户ID" style="width: 200px" size="small" />
        <el-button size="small" type="danger" plain @click="onViolation">处置二次售卖（踢出知识宝库）</el-button>
      </div>
    </el-card>

    <!-- 总管理员 -->
    <el-card v-if="isRole('SUPER_ADMIN')">
      <template #header>总管理员 · 世界运营</template>
      <div style="display: flex; gap: 24px; flex-wrap: wrap">
        <div>
          <div class="op-title">发布善事任务</div>
          <el-input v-model="xzForm.title" placeholder="任务标题" style="margin-bottom: 8px" />
          <div style="display: flex; gap: 8px; margin-bottom: 8px">
            <el-select v-model="xzForm.level" style="width: 100px">
              <el-option v-for="lv in ['D', 'C', 'B', 'A', 'S']" :key="lv" :value="lv" :label="lv + '级'" />
            </el-select>
            <el-input-number v-model="xzForm.rewardGold" :min="80" placeholder="金币" />
            <el-input-number v-model="xzForm.purifyValue" :min="1" :max="50" placeholder="净化值" />
          </div>
          <el-button type="primary" @click="doPublishXingzhe">发布</el-button>
        </div>
        <div>
          <div class="op-title">生产净化剂（每月限100支 · 5万金币/支）</div>
          <el-input-number v-model="purifierCount" :min="1" :max="100" style="margin-bottom: 8px" />
          <el-button type="success" @click="doProduce">生产</el-button>
        </div>
        <div>
          <div class="op-title">月度结算（60%上缴 / 40%拨款）</div>
          <el-date-picker v-model="settleMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" style="margin-bottom: 8px" />
          <el-button type="warning" @click="doSettle">执行结算</el-button>
        </div>
      </div>
    </el-card>

    <!-- 封装定价对话框 -->
    <el-dialog v-model="packageVisible" title="封装为信息集并定价" width="440px">
      <el-form label-width="70px">
        <el-form-item label="摘要"><el-input v-model="packageForm.summary" type="textarea" /></el-form-item>
        <el-form-item label="定价"><el-input-number v-model="packageForm.price" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packageVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPackage">确认上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pendingTasks, auditTask, pendingApplies, auditApply,
  pendingContributions, packageContribution, reportViolation, listPurifier,
  publishXingzheTask, producePurifier, triggerSettle
} from '../api'

const role = ref(localStorage.getItem('role') || '')
const isRole = (...rs) => rs.includes(role.value)
const ROLE_NAMES = {
  SUPER_ADMIN: '总管理员', XINGZHE_ADMIN: '行者协会管理员',
  ADVENTURER_ADMIN: '冒险者协会管理员', KNOWLEDGE_ADMIN: '知识宝库管理员'
}
const roleName = ref(ROLE_NAMES[role.value] || role.value)

const tasks = ref([])
const applies = ref([])
const contributions = ref([])
const offender = ref('')
const packageVisible = ref(false)
const packaging = ref(null)
const packageForm = ref({ summary: '', price: 100 })
const xzForm = ref({ title: '', level: 'D', rewardGold: 100, purifyValue: 10 })
const purifierCount = ref(100)
const settleMonth = ref('')

async function load() {
  if (isRole('ADVENTURER_ADMIN', 'SUPER_ADMIN')) tasks.value = await pendingTasks()
  if (isRole('XINGZHE_ADMIN', 'SUPER_ADMIN')) applies.value = await pendingApplies()
  if (isRole('KNOWLEDGE_ADMIN', 'SUPER_ADMIN')) contributions.value = await pendingContributions()
}

async function onAuditTask(row, pass) {
  await auditTask(row.id, pass, pass ? '合规' : '不符合协会规范')
  ElMessage.success('已处理')
  load()
}

async function onAuditApply(row, pass) {
  await auditApply(row.id, pass, pass ? '同意帮助' : '暂不提供帮助')
  ElMessage.success(pass ? '已通过并发布为行者任务' : '已驳回')
  load()
}

function openPackage(row) {
  packaging.value = row
  packageVisible.value = true
}

async function confirmPackage() {
  await packageContribution(packaging.value.id, {
    pass: true, summary: packageForm.value.summary, price: packageForm.value.price
  })
  ElMessage.success('信息集已上架')
  packageVisible.value = false
  load()
}

async function onPackage(row, pass) {
  await packageContribution(row.id, { pass, remark: '重复或不符合规范' })
  ElMessage.success('已驳回')
  load()
}

async function onViolation() {
  await ElMessageBox.confirm('确认将其永久踢出知识宝库？此操作不可撤销。', '违规处置', { type: 'warning' })
  await reportViolation({ offenderUserId: offender.value })
  ElMessage.success('已处置')
}

async function doListPurifier() {
  await listPurifier()
  ElMessage.success('净化剂已上架商城')
}

async function doPublishXingzhe() {
  await publishXingzheTask(xzForm.value)
  ElMessage.success('善事任务已发布')
}

async function doProduce() {
  await producePurifier(purifierCount.value)
  ElMessage.success('净化剂批次已生产')
}

async function doSettle() {
  if (!settleMonth.value) return ElMessage.warning('请选择月份')
  await triggerSettle(settleMonth.value)
  ElMessage.success('结算完成')
}

onMounted(load)
</script>

<style scoped>
.op-title { font-size: 13px; color: #606266; margin-bottom: 10px; font-weight: 500; }
</style>

<template>
  <div>
    <el-alert type="success" :closable="false" style="margin-bottom: 16px"
      title="修身养成 · 自我激励系统：给自己立任务、赚修行币、发展技能树，用赚到的币兑现给自己的现实奖励。上善若水，事竟成。" />

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ============ 自我任务 ============ -->
      <el-tab-pane label="自我任务" name="tasks">
        <el-card style="margin-bottom: 16px">
          <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end">
            <div>
              <div class="f-label">任务（给自己立的约定）</div>
              <el-input v-model="taskForm.title" placeholder="如：跑步 30 分钟" style="width: 240px" />
            </div>
            <div>
              <div class="f-label">奖励修行币</div>
              <el-input-number v-model="taskForm.coinReward" :min="1" :max="100" />
            </div>
            <div>
              <div class="f-label">关联技能</div>
              <el-select v-model="taskForm.skillId" placeholder="可选" clearable style="width: 150px">
                <el-option v-for="s in allSkills" :key="s.id" :label="`${s.name} Lv.${s.level}`" :value="s.id" />
              </el-select>
            </div>
            <div>
              <div class="f-label">类型</div>
              <el-select v-model="taskForm.repeatType" style="width: 110px">
                <el-option :value="0" label="一次性" />
                <el-option :value="1" label="每日" />
              </el-select>
            </div>
            <el-button type="primary" @click="doCreateTask">立此存照</el-button>
          </div>
          <div class="tip">完成得币 + 技能经验 20 + 净化萌芽 2；自我铸币每日上限 500 币</div>
          <div class="tpl-row">
            <span class="f-label">常用好习惯，一键立任务：</span>
            <el-tag v-for="tpl in templates" :key="tpl.title" class="tpl-tag" type="success"
                    effect="plain" @click="useTemplate(tpl)">{{ tpl.title }} +{{ tpl.reward }}</el-tag>
          </div>
        </el-card>

        <el-empty v-if="!tasks.length" description="还没有给自己立任务，从一件小事开始" />
        <el-row :gutter="16">
          <el-col v-for="t in tasks" :key="t.id" :span="8" style="margin-bottom: 16px">
            <el-card :class="{ done: t.status === 2 }">
              <div class="task-title">
                <el-tag v-if="t.repeatType === 1" size="small" type="success">每日</el-tag>
                <el-tag v-if="t.repeatType === 1 && t.streakDays > 0" size="small" type="warning">
                  🔥 连续 {{ t.streakDays }} 天
                </el-tag>
                {{ t.title }}
              </div>
              <div class="task-desc">{{ t.description || '（无备注）' }}</div>
              <div class="task-meta">
                <span class="coin">+{{ t.coinReward }} 币</span>
                <span v-if="t.lastCompletedAt" style="margin-left: 8px">上次完成：{{ formatTime(t.lastCompletedAt) }}</span>
              </div>
              <div v-if="t.status === 1" style="display: flex; gap: 8px">
                <el-button type="success" size="small" @click="doComplete(t)">完成！</el-button>
                <el-button size="small" type="info" plain @click="doAbandon(t)">放弃</el-button>
              </div>
              <el-tag v-else-if="t.status === 2" type="success" size="small">已完成 ✓</el-tag>
              <el-tag v-else type="info" size="small">已放弃</el-tag>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ============ 技能树 ============ -->
      <el-tab-pane label="技能树" name="skills">
        <el-card v-if="!skills.length" style="margin-bottom: 16px">
          <el-button type="primary" @click="doInitSkills">种下我的技能树（免费初始化三系）</el-button>
          <div class="tip">初始化获得：专注（深度工作/冥想）、健体（跑步/力量训练）、心性（阅读/日记）</div>
        </el-card>
        <template v-if="skills.length">
          <el-alert type="info" :closable="false" style="margin-bottom: 16px"
            title="完成关联任务技能涨经验（每次 +20），每 100 经验升 1 级；开枝（建子技能）花费 50 币，全额入冒险者协会" />
          <div v-for="root in skillRoots" :key="root.id" class="skill-branch">
            <el-card class="root-card">
              <div class="skill-name">
                <el-icon><Share /></el-icon>
                {{ root.name }}
                <el-tag size="small" type="warning" style="margin-left: 8px">Lv.{{ root.level }}</el-tag>
              </div>
              <el-progress :percentage="skillPct(root)" :stroke-width="8" />
              <div class="skill-exp">{{ root.exp }} 经验<span v-if="root.nextLevelExp"> · 距升级还差 {{ root.nextLevelExp - root.exp }}</span></div>
              <el-button size="small" text type="primary" @click="openBranch(root)">＋ 开枝</el-button>
            </el-card>
            <div class="children">
              <el-card v-for="c in skillChildren(root.id)" :key="c.id" class="child-card">
                <div class="skill-name">
                  {{ c.name }}
                  <el-tag size="small" :type="c.level >= 3 ? 'success' : 'info'" style="margin-left: 8px">Lv.{{ c.level }}</el-tag>
                </div>
                <el-progress :percentage="skillPct(c)" :stroke-width="6" />
                <div class="skill-exp">{{ c.exp }} 经验</div>
              </el-card>
            </div>
          </div>
        </template>
      </el-tab-pane>

      <!-- ============ 自我奖励商城 ============ -->
      <el-tab-pane label="自我商城" name="shop">
        <el-card style="margin-bottom: 16px">
          <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end">
            <div>
              <div class="f-label">现实奖励</div>
              <el-input v-model="itemForm.name" placeholder="如：看一场电影" style="width: 220px" />
            </div>
            <div>
              <div class="f-label">备注</div>
              <el-input v-model="itemForm.description" placeholder="可选" style="width: 200px" />
            </div>
            <div>
              <div class="f-label">兑换价（币）</div>
              <el-input-number v-model="itemForm.cost" :min="1" :max="100000" />
            </div>
            <el-button type="primary" @click="doAddItem">上架奖励</el-button>
          </div>
          <div class="tip">兑换时花费 10% 作为会费上缴冒险者协会——修行的每一份犒赏，都与协会共担共赢</div>
        </el-card>

        <el-empty v-if="!items.length" description="给自己许几个愿望，用修行换" />
        <el-row :gutter="16">
          <el-col v-for="it in items" :key="it.id" :span="8" style="margin-bottom: 16px">
            <el-card>
              <div class="task-title">{{ it.name }}</div>
              <div class="task-desc">{{ it.description || '——' }}</div>
              <div class="task-meta"><span class="coin">{{ it.cost }} 币</span></div>
              <div v-if="it.status === 1" style="display: flex; gap: 8px">
                <el-button type="warning" size="small" @click="doRedeem(it)">兑换犒赏</el-button>
                <el-button size="small" text type="info" @click="doOffShelf(it)">下架</el-button>
              </div>
              <el-tag v-else type="info" size="small">已下架</el-tag>
            </el-card>
          </el-col>
        </el-row>

        <template v-if="redemptions.length">
          <div class="shelf-section">兑换记录</div>
          <el-table :data="redemptions" size="small">
            <el-table-column prop="itemName" label="奖励" />
            <el-table-column prop="cost" label="花费（币）" width="110" />
            <el-table-column prop="assocFee" label="协会会费" width="100" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.redeemedAt) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="branchVisible" title="开枝 · 创建子技能" width="420px">
      <el-form label-width="80px">
        <el-form-item label="父技能"><span>{{ branchParent?.name }}</span></el-form-item>
        <el-form-item label="技能名">
          <el-input v-model="branchName" placeholder="如：游泳" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="branchVisible = false">取消</el-button>
        <el-button type="primary" @click="doBranch">开枝（50 币入协会）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  selfTasks, createSelfTask, completeSelfTask, abandonSelfTask,
  selfSkills, initSelfSkills, branchSkill,
  selfShopItems, addSelfShopItem, offShelfSelfItem, redeemSelfItem, selfRedemptions,
} from '../api'

const activeTab = ref('tasks')
const tasks = ref([])
const skills = ref([])
const items = ref([])
const redemptions = ref([])
const taskForm = ref({ title: '', coinReward: 20, skillId: null, repeatType: 0 })
const itemForm = ref({ name: '', description: '', cost: 50 })
const branchVisible = ref(false)
const branchParent = ref(null)
const branchName = ref('')

const skillRoots = computed(() => skills.value.filter((s) => !s.parentId))
const allSkills = computed(() => skills.value)
const skillChildren = (id) => skills.value.filter((s) => s.parentId === id)
const skillPct = (s) =>
  s.nextLevelExp ? Math.min(100, Math.round((s.exp / s.nextLevelExp) * 100)) : 100

// 常见好习惯模板：一键立每日任务，自动关联同名/含关键词的技能
const templates = [
  { title: '晨跑 30 分钟', reward: 30, skill: '跑步' },
  { title: '力量训练 20 分钟', reward: 30, skill: '力量训练' },
  { title: '冥想 10 分钟', reward: 20, skill: '冥想' },
  { title: '阅读 30 分钟', reward: 25, skill: '阅读' },
  { title: '写日记', reward: 15, skill: '日记' },
  { title: '深度工作 2 小时', reward: 40, skill: '深度工作' },
  { title: '23 点前睡觉', reward: 20, skill: null },
  { title: '整理房间', reward: 15, skill: null },
]

async function useTemplate(tpl) {
  const skill = tpl.skill ? skills.value.find((s) => s.name === tpl.skill) : null
  await createSelfTask({ title: tpl.title, coinReward: tpl.reward, repeatType: 1, skillId: skill?.id ?? null })
  ElMessage.success(`已立任务「${tpl.title}」，明天记得来打卡`)
  loadTasks()
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

async function loadTasks() { tasks.value = await selfTasks() }
async function loadSkills() { skills.value = await selfSkills() }
async function loadShop() {
  items.value = await selfShopItems()
  redemptions.value = await selfRedemptions()
}
function onTabChange(tab) {
  if (tab === 'tasks') loadTasks()
  else if (tab === 'skills') loadSkills()
  else loadShop()
}

async function doCreateTask() {
  await createSelfTask(taskForm.value)
  ElMessage.success('任务已立下，君子一言')
  taskForm.value.title = ''
  loadTasks()
}

async function doComplete(t) {
  const res = await completeSelfTask(t.id)
  let msg = `获得 ${res.reward} 修行币`
  if (res.streakBonus > 0) msg += `（含连续打卡奖励 +${res.streakBonus}）`
  if (res.streakDays > 1) msg += `，已连续打卡 ${res.streakDays} 天`
  if (res.skillName) {
    msg += `，「${res.skillName}」+20 经验`
    if (res.levelUp) msg += '，升级！'
  }
  ElMessage.success(msg + '，萌芽净化 +2')
  loadTasks()
}

async function doAbandon(t) {
  await abandonSelfTask(t.id)
  ElMessage.info('已放弃，无妨，明日再战')
  loadTasks()
}

async function doInitSkills() {
  await initSelfSkills()
  ElMessage.success('技能树已种下，静待生长')
  loadSkills()
}

function openBranch(root) {
  branchParent.value = root
  branchName.value = ''
  branchVisible.value = true
}

async function doBranch() {
  await branchSkill({ parentId: branchParent.value.id, name: branchName.value })
  ElMessage.success('开枝成功，技能树又长了一截')
  branchVisible.value = false
  loadSkills()
}

async function doAddItem() {
  await addSelfShopItem(itemForm.value)
  ElMessage.success('奖励已上架，努力赚币去兑现它')
  itemForm.value.name = ''
  itemForm.value.description = ''
  loadShop()
}

async function doOffShelf(it) {
  await offShelfSelfItem(it.id)
  loadShop()
}

async function doRedeem(it) {
  const r = await redeemSelfItem(it.id)
  ElMessage.success(`已兑换「${r.itemName}」，花费 ${r.cost} 币（含协会会费 ${r.assocFee}）——好好享受！`)
  loadShop()
}

onMounted(() => {
  loadTasks()
  loadSkills()
})
</script>

<style scoped>
.f-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.tip { font-size: 12px; color: #909399; margin-top: 10px; }
.tpl-row { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.tpl-tag { cursor: pointer; }
.tpl-tag:hover { transform: translateY(-1px); }
.task-title { font-size: 15px; font-weight: 500; margin-bottom: 8px; }
.task-desc { color: #909399; font-size: 12px; min-height: 18px; margin-bottom: 8px; }
.task-meta { font-size: 12px; color: #909399; margin-bottom: 10px; }
.coin { color: #e6a23c; font-weight: 500; }
.done { opacity: 0.6; }
.skill-branch { display: flex; gap: 16px; margin-bottom: 16px; align-items: flex-start; }
.root-card { width: 280px; flex-shrink: 0; border-left: 3px solid #e6a23c; }
.children { display: flex; gap: 12px; flex-wrap: wrap; flex: 1; }
.child-card { width: 220px; }
.skill-name { font-size: 14px; font-weight: 500; margin-bottom: 8px; display: flex; align-items: center; }
.skill-exp { font-size: 12px; color: #909399; margin-top: 6px; }
.shelf-section { font-size: 14px; font-weight: 600; margin: 16px 0 10px; }
</style>

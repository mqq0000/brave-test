<template>
  <div v-if="profile">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>冒险者萌芽</template>
          <div class="sprout-state" :style="{ color: sproutColor }">{{ sproutText }}</div>
          <el-progress :percentage="profile.pollutionValue" :color="sproutColor" :stroke-width="14" />
          <div class="hint">污染值 {{ profile.pollutionValue }}/100 · 完成善事任务或使用净化剂可净化</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>冒险者等级</template>
          <div class="rank">{{ profile.rankLevel }} 级</div>
          <div class="hint">
            累计完成 {{ profile.completedCount }} 件任务 ·
            距下次自然升级还差 {{ 100 - (profile.completedCount % 100) }} 件
          </div>
          <div class="hint" style="margin-top: 6px">
            每完成 100 件自然升级（+10000 金币）；越级成功直升，失败封禁一个月
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>金币与状态</template>
          <div class="gold">{{ profile.goldBalance }} 金币</div>
          <div v-if="awakenTitle" class="awaken">觉醒称号：{{ awakenTitle }}</div>
          <div v-if="banned" class="ban">协会封禁至 {{ profile.bannedUntil.replace('T', ' ') }}</div>
          <div v-else class="ok">冒险者协会对你开放中</div>
          <div v-if="profile.knowledgeBanned === 1" class="ban">已被踢出知识宝库</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card>
          <template #header>金币流水（最近 50 笔）</template>
          <el-table :data="flows" stripe size="small" max-height="420">
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 19) }}</template>
            </el-table-column>
            <el-table-column prop="bizType" label="业务" width="160" />
            <el-table-column label="金额" width="140">
              <template #default="{ row }">
                <span :style="{ color: row.amount >= 0 ? '#67c23a' : '#f56c6c' }">
                  {{ row.amount >= 0 ? '+' : '' }}{{ row.amount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="余额" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>勇者榜 · 完成任务数 Top20</template>
          <el-table :data="ranks" size="small" :show-header="false">
            <el-table-column label="名次" width="60">
              <template #default="{ row }">
                <span :class="{ 'top1': row.rank === 1 }">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="冒险者" />
            <el-table-column label="等级" width="70">
              <template #default="{ row }"><el-tag size="small">{{ row.rankLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="completedCount" label="完成" width="70" />
          </el-table>
          <div v-if="!ranks.length" class="hint">暂无上榜数据，完成任务即可上榜</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyProfile, getMyFlows, getRank, myTestResult } from '../api'

const profile = ref(null)
const flows = ref([])
const ranks = ref([])
const awakenTitle = ref('')

const sproutColor = computed(() => {
  const s = profile.value?.sproutStatus
  return s === 'RED' ? '#f56c6c' : s === 'GRAY' ? '#909399' : '#67c23a'
})
const sproutText = computed(() => {
  const s = profile.value?.sproutStatus
  return s === 'RED' ? '萌芽赤化 · 愤怒在侵蚀你' : s === 'GRAY' ? '萌芽灰化 · 沮丧在蔓延' : '萌芽健康 · 勇气常在'
})
const banned = computed(() => profile.value?.bannedUntil && new Date(profile.value.bannedUntil) > new Date())

onMounted(async () => {
  profile.value = await getMyProfile()
  flows.value = await getMyFlows()
  getRank().then((r) => (ranks.value = r)).catch(() => {})
  myTestResult().then((r) => { if (r) awakenTitle.value = r.title }).catch(() => {})
})
</script>

<style scoped>
.sprout-state { font-size: 18px; font-weight: 500; margin-bottom: 12px; }
.rank { font-size: 30px; font-weight: 500; margin-bottom: 8px; }
.gold { font-size: 26px; color: #e6a23c; margin-bottom: 8px; }
.hint { color: #909399; font-size: 12px; margin-top: 8px; }
.ban { color: #f56c6c; font-size: 13px; margin-top: 4px; }
.ok { color: #67c23a; font-size: 13px; margin-top: 4px; }
.awaken { color: #7f77dd; font-size: 13px; margin-top: 4px; }
.top1 { color: #e6a23c; font-weight: 600; }
</style>

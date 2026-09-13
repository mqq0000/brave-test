<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span style="color: #909399; font-size: 13px">
          独有经验经管理员筛查封装为信息集；一经售卖，请保护版权，二次售卖将被永久踢出知识宝库
        </span>
        <el-button type="primary" @click="contributeVisible = true">贡献我的独有经验</el-button>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="信息集广场" name="market">
        <div style="display: flex; gap: 12px; margin-bottom: 16px">
          <el-input
            v-model="keyword"
            placeholder="搜索信息集标题…"
            clearable
            style="max-width: 320px"
            @keyup.enter="doSearch"
            @clear="doSearch"
          >
            <template #append>
              <el-button icon="Search" @click="doSearch" />
            </template>
          </el-input>
        </div>

        <el-empty v-if="!infoSets.length" description="没有找到相关内容" />
        <el-row :gutter="16">
          <el-col v-for="set in infoSets" :key="set.id" :span="8" style="margin-bottom: 16px">
            <el-card>
              <div class="set-title">{{ set.title }}</div>
              <div class="set-summary">{{ set.summary || '（暂无摘要）' }}</div>
              <div class="set-price">{{ set.price }} 金币</div>
              <div style="display: flex; gap: 8px">
                <el-button type="primary" size="small" @click="onBuy(set)">购买</el-button>
                <el-dropdown size="small" @command="(c) => onBorrow(set, c)">
                  <el-button size="small">借阅</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :command="1">日卡（10% · {{ day(set) }} 金币）</el-dropdown-item>
                      <el-dropdown-item :command="2">周卡（25% · {{ week(set) }} 金币）</el-dropdown-item>
                      <el-dropdown-item :command="3">月卡（50% · {{ month(set) }} 金币）</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button size="small" type="success" plain @click="onRead(set)">阅读</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane :label="`我的书架 (${shelf.purchased.length + shelf.borrowing.length})`" name="shelf">
        <template v-if="shelf.purchased.length">
          <div class="shelf-section">已购买（永久阅读权）</div>
          <el-row :gutter="16">
            <el-col v-for="row in shelf.purchased" :key="'p' + row.infoSetId" :span="8" style="margin-bottom: 16px">
              <el-card>
                <div class="set-title">{{ row.title }}</div>
                <div class="set-summary">{{ row.summary || '（暂无摘要）' }}</div>
                <div class="shelf-meta">购入价 {{ row.price }} 金币</div>
                <el-button size="small" type="success" plain @click="onRead(row)">阅读</el-button>
              </el-card>
            </el-col>
          </el-row>
        </template>

        <template v-if="shelf.borrowing.length">
          <div class="shelf-section">借阅中（到期自动失效）</div>
          <el-row :gutter="16">
            <el-col v-for="row in shelf.borrowing" :key="'b' + row.borrowId" :span="8" style="margin-bottom: 16px">
              <el-card>
                <div class="set-title">{{ row.title }}</div>
                <div class="set-summary">{{ row.summary || '（暂无摘要）' }}</div>
                <div class="shelf-meta">
                  <el-tag :type="row.remainDays <= 2 ? 'danger' : 'info'" size="small">
                    剩余 {{ row.remainDays }} 天
                  </el-tag>
                  <span style="margin-left: 8px">到期：{{ formatTime(row.expireAt) }}</span>
                </div>
                <div style="display: flex; gap: 8px">
                  <el-button size="small" type="success" plain @click="onRead(row)">阅读</el-button>
                  <el-dropdown size="small" @command="(c) => onRenew(row, c)">
                    <el-button size="small" type="warning" plain>续费</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :command="1">续日卡（{{ day(row) }} 金币）</el-dropdown-item>
                        <el-dropdown-item :command="2">续周卡（{{ week(row) }} 金币）</el-dropdown-item>
                        <el-dropdown-item :command="3">续月卡（{{ month(row) }} 金币）</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>

        <el-empty v-if="!shelf.purchased.length && !shelf.borrowing.length"
                  description="书架还是空的，去广场买一个或借一本来读吧" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="contributeVisible" title="贡献独有经验" width="520px">
      <el-form label-width="70px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="写下他人不知道的经验。不可侵犯他人隐私，重复内容将被驳回" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contributeVisible = false">取消</el-button>
        <el-button type="primary" @click="doContribute">提交（收录后净化萌芽-5）</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="readVisible" :title="reading?.title || '信息集'" width="560px">
      <div class="read-content">{{ content }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  contribute, listInfoSets, purchaseInfoSet, borrowInfoSet, readInfoSet,
  myShelf, renewBorrow,
} from '../api'

const activeTab = ref('market')
const keyword = ref('')
const infoSets = ref([])
const shelf = reactive({ purchased: [], borrowing: [] })
const contributeVisible = ref(false)
const readVisible = ref(false)
const reading = ref(null)
const content = ref('')
const form = ref({ title: '', content: '' })

const day = (s) => Math.max(1, Math.round(s.price * 0.1))
const week = (s) => Math.max(1, Math.round(s.price * 0.25))
const month = (s) => Math.max(1, Math.round(s.price * 0.5))
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

async function load() {
  infoSets.value = await listInfoSets({ page: 1, size: 20 })
}

async function doSearch() {
  infoSets.value = await listInfoSets({ page: 1, size: 20, keyword: keyword.value || undefined })
}

async function loadShelf() {
  const res = await myShelf()
  shelf.purchased = res.purchased || []
  shelf.borrowing = res.borrowing || []
}

function onTabChange(tab) {
  if (tab === 'shelf') loadShelf()
  else load()
}

async function doContribute() {
  await contribute(form.value)
  ElMessage.success('投稿成功，等待知识宝库管理员筛查')
  contributeVisible.value = false
}

async function onBuy(set) {
  await purchaseInfoSet(set.id)
  ElMessage.success('购买成功！永久阅读权已生效，请守护版权')
  if (activeTab.value === 'shelf') loadShelf()
}

async function onBorrow(set, cardType) {
  await borrowInfoSet(set.id, cardType)
  ElMessage.success('借阅成功，请在有效期内阅读')
}

async function onRenew(row, cardType) {
  await renewBorrow(row.borrowId, cardType)
  ElMessage.success('续费成功，到期时间已顺延')
  loadShelf()
}

async function onRead(set) {
  reading.value = set
  const res = await readInfoSet(set.infoSetId || set.id)
  if (res.type === 'url') {
    // MinIO 临时签名URL（有效期不超过1小时且不超过借阅剩余时间）
    window.open(res.value, '_blank')
    ElMessage.info('已通过临时链接打开（链接1小时内有效，请勿外传）')
  } else {
    content.value = res.value
    readVisible.value = true
  }
}

onMounted(load)
</script>

<style scoped>
.set-title { font-size: 15px; font-weight: 500; margin-bottom: 8px; }
.set-summary { color: #909399; font-size: 12px; min-height: 36px; margin-bottom: 8px; }
.set-price { color: #e6a23c; font-size: 14px; margin-bottom: 12px; }
.shelf-section { font-size: 14px; font-weight: 600; margin: 4px 0 12px; }
.shelf-meta { font-size: 12px; color: #909399; margin-bottom: 10px; }
.read-content { white-space: pre-wrap; line-height: 1.7; }
</style>

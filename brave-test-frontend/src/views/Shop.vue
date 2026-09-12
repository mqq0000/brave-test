<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <div style="display: flex; gap: 12px; align-items: center">
        <el-radio-group v-model="itemType" @change="load">
          <el-radio-button :value="null">全部商品</el-radio-button>
          <el-radio-button :value="1">普通商品</el-radio-button>
          <el-radio-button :value="2">净化剂</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="listVisible = true">上架我的商品</el-button>
        <el-button type="success" @click="onUsePurifier">使用净化剂（-50污染）</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="item in items" :key="item.id" :span="6" style="margin-bottom: 16px">
        <el-card>
          <div class="item-name">
            <el-tag v-if="item.itemType === 2" type="success" effect="dark" size="small">净化剂</el-tag>
            {{ item.name }}
          </div>
          <div class="item-price">{{ item.price }} 金币</div>
          <div class="item-stock">库存 {{ item.stock }}</div>
          <el-button type="primary" style="width: 100%" :disabled="item.stock <= 0" @click="onBuy(item)">
            {{ item.stock > 0 ? '购买' : '已售罄' }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 8px">
      <template #header>我的订单</template>
      <el-table :data="orders" stripe size="small">
        <el-table-column prop="itemId" label="商品ID" width="180" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.type === 2 ? '净化剂' : '普通' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">{{ row.price }} 金币</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag v-if="row.type === 2 && row.status === 1" type="warning" size="small">已购未使用</el-tag>
            <el-tag v-else-if="row.type === 2 && row.status === 3" type="success" size="small">已使用</el-tag>
            <el-tag v-else type="info" size="small">已完成</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="listVisible" title="上架商品" width="420px">
      <el-form label-width="70px">
        <el-form-item label="名称"><el-input v-model="listForm.name" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="listForm.price" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="listForm.stock" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="listVisible = false">取消</el-button>
        <el-button type="primary" @click="doList">上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listShopItems, listItem, purchaseItem, myOrders, usePurifier } from '../api'

const items = ref([])
const orders = ref([])
const itemType = ref(null)
const listVisible = ref(false)
const listForm = ref({ name: '', price: 100, stock: 1 })

async function load() {
  items.value = await listShopItems({ itemType: itemType.value })
  orders.value = await myOrders()
}

async function onBuy(item) {
  await purchaseItem(item.id)
  ElMessage.success('购买成功')
  load()
}

async function onUsePurifier() {
  await usePurifier()
  ElMessage.success('一股清泉流入心间，污染值 -50')
  load()
}

async function doList() {
  await listItem(listForm.value)
  ElMessage.success('已上架')
  listVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.item-name { font-size: 15px; font-weight: 500; margin-bottom: 8px; }
.item-price { color: #e6a23c; font-size: 14px; margin-bottom: 4px; }
.item-stock { color: #909399; font-size: 12px; margin-bottom: 12px; }
</style>

import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const logout = () => request.post('/auth/logout')

// 档案与流水
export const getMyProfile = () => request.get('/adventurer/me')
export const getMyFlows = () => request.get('/finance/flows')
export const getRank = () => request.get('/adventurer/rank')

// 任务
export const listTasks = (params) => request.get('/tasks', { params })
export const publishTask = (data) => request.post('/adventurer/tasks', data)
export const acceptTask = (id, confirmCrossLevel) =>
  request.post(`/tasks/${id}/accept`, null, { params: { confirmCrossLevel } })
export const submitTask = (id) => request.post(`/tasks/${id}/submit`)
export const verifyTask = (id, success) =>
  request.post(`/tasks/${id}/verify`, null, { params: { success } })
export const myPublishedTasks = () => request.get('/tasks/mine/published')
export const myAcceptedTasks = () => request.get('/tasks/mine/accepted')
export const xingzheApply = (data) => request.post('/xingzhe/apply', data)

// 商城
export const listShopItems = (params) => request.get('/shop/items', { params })
export const listItem = (params) => request.post('/shop/items', null, { params })
export const purchaseItem = (itemId) => request.post(`/shop/orders/${itemId}`)
export const myOrders = () => request.get('/shop/orders/mine')
export const usePurifier = () => request.post('/shop/purifier/use')

// 知识宝库
export const contribute = (params) => request.post('/knowledge/contributions', null, { params })
export const myContributions = () => request.get('/knowledge/contributions/mine')
export const listInfoSets = (params) => request.get('/knowledge/info-sets', { params })
export const purchaseInfoSet = (id) => request.post(`/knowledge/info-sets/${id}/purchase`)
export const borrowInfoSet = (id, cardType) =>
  request.post(`/knowledge/info-sets/${id}/borrow`, null, { params: { cardType } })
export const readInfoSet = (id) => request.get(`/knowledge/info-sets/${id}/content`)
export const myPurchases = () => request.get('/knowledge/purchases/mine')
export const myBorrows = () => request.get('/knowledge/borrows/mine')
export const myShelf = () => request.get('/knowledge/shelf')
export const renewBorrow = (id, cardType) =>
  request.post(`/knowledge/borrows/${id}/renew`, null, { params: { cardType } })
export const favoriteInfoSet = (id) => request.post(`/knowledge/info-sets/${id}/favorite`)
export const myFavorites = () => request.get('/knowledge/favorites')
export const myFavoriteIds = () => request.get('/knowledge/favorites/ids')
export const submitReport = (params) => request.post('/knowledge/reports', null, { params })
export const pendingReports = () => request.get('/admin/knowledge/reports/pending')
export const handleReport = (id, penalize, remark) =>
  request.post(`/admin/knowledge/reports/${id}/handle`, null, { params: { penalize, remark } })

// 剧情与觉醒测试
export const listChapters = () => request.get('/story/chapters')
export const enterChapter = (id) => request.post(`/story/chapters/${id}/enter`)
export const chooseOption = (optionId) => request.post('/story/choose', null, { params: { optionId } })
export const testQuestions = () => request.get('/test/questions')
export const submitTest = (answers) => request.post('/test/submit', answers)
export const myTestResult = () => request.get('/test/me')

// 管理后台
export const publishXingzheTask = (params) => request.post('/admin/tasks/xingzhe', null, { params })
export const producePurifier = (count) => request.post('/admin/purifier/batch', null, { params: { count } })
export const triggerSettle = (month) => request.post('/admin/settlement/monthly', null, { params: { month } })
export const pendingTasks = () => request.get('/admin/adventurer-tasks/pending')
export const auditTask = (id, pass, remark) =>
  request.post(`/admin/adventurer-tasks/${id}/audit`, null, { params: { pass, remark } })
export const pendingApplies = () => request.get('/admin/xingzhe-applies/pending')
export const auditApply = (id, pass, remark) =>
  request.post(`/admin/xingzhe-applies/${id}/audit`, null, { params: { pass, remark } })
export const listPurifier = () => request.post('/admin/shop/purifier/list')
export const pendingContributions = () => request.get('/admin/knowledge/contributions/pending')
export const packageContribution = (id, params) =>
  request.post(`/admin/knowledge/contributions/${id}/package`, null, { params })
export const reportViolation = (params) => request.post('/admin/knowledge/violations', null, { params })

// 修身养成（自我激励）
export const selfTasks = () => request.get('/self/tasks')
export const createSelfTask = (data) => request.post('/self/tasks', data)
export const completeSelfTask = (id) => request.post(`/self/tasks/${id}/complete`)
export const abandonSelfTask = (id) => request.post(`/self/tasks/${id}/abandon`)
export const selfSkills = () => request.get('/self/skills')
export const initSelfSkills = () => request.post('/self/skills/init')
export const branchSkill = (data) => request.post('/self/skills', data)
export const selfShopItems = () => request.get('/self/shop/items')
export const addSelfShopItem = (data) => request.post('/self/shop/items', data)
export const offShelfSelfItem = (id) => request.post(`/self/shop/items/${id}/offshelf`)
export const redeemSelfItem = (id) => request.post(`/self/shop/items/${id}/redeem`)
export const selfRedemptions = () => request.get('/self/redemptions')

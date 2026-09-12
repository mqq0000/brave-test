# 勇者测试 前端（brave-test-frontend）

Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios

## 页面

| 路由 | 页面 | 说明 |
|---|---|---|
| /login | 登录/觉醒 | 登录或注册（注册即成为 D 级冒险者） |
| /story | 冒险剧情 | 章节卡片（解锁链/完成状态），剧情文本渲染，选项按钮——选择实时结算污染与金币 |
| /test | 觉醒测试 | 5 道情境题作答，计算勇气/理性/仁善特质，颁发觉醒称号（炽焰觉醒者/明澈贤者/微光行者），负面选项污染萌芽 |
| /tasks | 任务大厅 | 任务筛选（行者善事/冒险者委托 × SABCD）、发布委托、向行者协会求助、接单（越级二次确认弹窗）、提交成果、发布者验收 |
| /shop | 冒险者商城 | 商品卡片、购买、上架我的商品、使用净化剂（-50 污染）、我的订单 |
| /knowledge | 知识宝库 | 信息集卡片、购买 / 借阅（日 10% · 周 25% · 月 50%）/ 阅读、贡献独有经验 |
| /profile | 我的档案 | 萌芽状态（正常/灰化/赤化 + 污染进度条）、等级与升级进度、封禁状态、金币流水 |
| /admin | 管理后台 | 按角色显示：协会审核任务 / 行者审批求助 / 知识封装定价与违规处置 / 总管理员发善事任务·产净化剂·月度结算 |

## 快速开始

```bash
npm install
npm run dev     # http://localhost:5173（已配置代理到后端 8080）
npm run build   # 产物在 dist/
```

前提：后端已在 8080 端口运行（见 ../brave-test-backend/README.md）。

默认管理员（密码均为 admin123）：superadmin / xingzhe_admin / adventurer_admin / knowledge_admin

# 勇者测试 后端（brave-test-backend）

高污染世界下的文字冒险 + 性格测试游戏后端。
> 设计文档见工作区《勇者测试-系统设计文档.md》

## 技术栈

- Java 17 / SpringBoot 3.3.x / MyBatis-Plus 3.5.7 / MySQL 8 / Redis
- JWT（jjwt 0.12）+ Redis 黑名单登出
- Knife4j 4.5（接口文档 `/doc.html`）
- MinIO 8.5（图片/信息集内容，P3 接入）
- Lombok

## 快速开始

### 方式一：Docker 一键部署（推荐）

工作区根目录的 `docker-compose.yml` 编排了全部五个服务（MySQL8 + Redis + MinIO + 后端 + Nginx 前端），首次启动自动建库建表并注入剧情/题库种子数据：

```bash
docker compose up -d --build
```

- 前端：http://localhost
- 接口文档：http://localhost/doc.html
- MinIO 控制台：http://localhost:9001

### 方式二：本地开发

1. **初始化数据库**：执行 `src/main/resources/sql/schema.sql`（自动建库 brave_test + 23 张表 + 4 个管理员账号 + 三章剧情 + 10 道觉醒测试题，管理员初始密码 `admin123`）。
   > ⚠️ **命令行导入必须指定 utf8mb4**，否则中文种子数据（剧情/题目）会乱码：
   > ```bash
   > mysql -uroot -p --default-character-set=utf8mb4 brave_test < schema.sql
   > ```
   > 验证方法：`SELECT title FROM story_chapter;` 若显示 `ç¬¬ä¸€ç«` 之类即为乱码，需删库重导。
2. **修改配置**：`application.yml` 中 MySQL/Redis 连接信息；`jwt.secret` 上线前必须更换。
3. **启动**：IDEA 打开工程，运行 `BraveTestApplication`，或：
   ```bash
   mvn spring-boot:run
   ```
4. **接口文档**：浏览器打开 http://localhost:8080/doc.html
5. **前端**：见 `../brave-test-frontend/README.md`（`npm run dev`，代理到 8080）
6. **自动化验证**（可选）：`tools/verify_e2e.py` 为后端 API 端到端冒烟脚本（38 用例），`../brave-test-frontend/tools/e2e_web.cjs` 为浏览器端到端联调脚本（12 用例，需 `npm i -D playwright-core` 与 Chrome）。

## 管理员账号

| 用户名 | 角色 |
|---|---|
| superadmin | 总管理员（发善事任务/产净化剂/月度结算） |
| xingzhe_admin | 行者协会管理员（审批任务帮助申请） |
| adventurer_admin | 冒险者协会管理员（审核发布任务） |
| knowledge_admin | 知识宝库管理员 |

## 核心业务规则（已实现）

- 任务等级 SABCD 与金币区间校验（D 80~120 / C 800~1200 / B 8k~12k / A 8w~12w / S 12w+）
- 发布任务托管全额金币，协会抽 10%，接取者得 90%（乐观扣款防超支）
- 接单：Redis 分布式锁 + 状态条件更新防超抢；赤化禁接 S 级；越级最多跨 2 级且需显式确认
- 验收成功：发 90% 报酬 + 净化萌芽 + 完成 100 件自然升级（+10000 奖励）；越级成功直升该等级（奖励只发一次）
- 验收失败：越级失败封禁协会 30 天，退还发布者 90% 托管
- 行者协会：总管理员发善事任务；冒险者申请帮助 → 管理员审批通过自动发布
- 月度结算：每月 1 日 00:30 自动执行（协会佣金 60% 上缴总管理员 → 总管理员营利 40% 拨付知识宝库），幂等可手动触发

## 里程碑进度

- [x] P1 工程骨架 / JWT 认证 / 档案 / 污染萌芽状态机
- [x] P2 任务中心（发布/审核/接单/验收/升降级/封禁）+ 金币流水 + 月度结算
- [x] P3 商城（上架/购买/卖家入账）+ 净化剂（总管理员月限生产→协会上架→购买→使用-50污染）
- [x] P4 知识宝库（投稿→管理员封装定价→购买/借阅日周月卡→70%贡献者分成→违规踢出）
- [x] P5 前端 Vue3+Element Plus（../brave-test-frontend 八页面）；MinIO 信息集内容存储+临时签名URL；Redis 完成任务数排行榜；登录页序章文案
- [x] P6 文字冒险剧情引擎（章节/节点/选项三表 + 玩家进度，选择实时结算污染/金币，章节解锁链，含序章《灰色的城》种子剧情）+ 觉醒性格测试（勇气/理性/仁善三特质计分 → 觉醒称号「炽焰觉醒者/明澈贤者/微光行者」，负面选项累计初始污染）

## 数据库

共 23 张表（17 张协会经济体系 + 6 张剧情与测试体系），全部由 `schema.sql` 一键创建，内置三章剧情与 10 道觉醒测试题种子数据。

**核心玩法闭环**：觉醒测试定位人格与初始污染 → 冒险剧情选择实时改变萌芽状态 → 三协会接单净化/赚钱/升级 → 商城与知识宝库互助成长。

## 目录结构

```
src/main/java/com/bravetest/
├── common/       统一响应 R / 业务异常 / 全局处理 / UserContext
├── config/       MyBatis-Plus / Redis / OpenAPI / WebMvc
├── security/     JwtUtil / JwtInterceptor
├── entity|mapper/ 17 张表实体与 Mapper（tools/gen_entities.py 生成）
├── dto/          请求/响应对象
├── service/      Auth / Adventurer / Finance / Task / Settlement
└── controller/   auth / adventurer / tasks / admin / shop(桩) / knowledge(桩)
```

# IDEA 导入与运行指南（勇者测试）

> 本工程已预置 `.idea/` 配置：**打开即自动挂载 Maven、UTF-8 编码、Lombok 注解处理，并自带 3 个运行配置**。按下面 3 步走完即可跑起来。

---

## 一、预置清单（已做好，无需手工配置）

| 文件 | 作用 |
|---|---|
| `.idea/misc.xml` | 打开项目自动挂载 `brave-test-backend/pom.xml` 为 Maven 模块；语言级别 JDK 17 |
| `.idea/encodings.xml` | 全工程 UTF-8（中文注释/剧情文本不乱码） |
| `.idea/compiler.xml` | Lombok 注解处理已启用（否则报 `Cannot resolve symbol log`） |
| `.idea/runConfigurations/brave_backend.xml` | 运行配置①：SpringBoot 后端（指向 docker 依赖，见下） |
| `.idea/runConfigurations/brave_frontend_dev.xml` | 运行配置②：前端 `npm run dev` |
| `.idea/runConfigurations/brave_deps_up.xml` | 运行配置③：一键拉起依赖容器（mysql/redis/minio） |

## 二、三步导入

1. **打开**：IDEA → `File → Open` → 选择本目录（含 `.idea` 的根目录）→ Trust Project。Maven 会自动开始导入（右侧 Maven 工具窗可见 `brave-test-backend`）。
2. **SDK**：`File → Project Structure → Project` → SDK 选择 **17**（本机安装于 `D:\Java\jdk-17.0.20.1`；若列表没有，点 `Add SDK → JDK` 选中该目录，命名 `17`）。
3. **Maven home 对齐**：`Settings → Build Tools → Maven` → Maven home path 选 **`D:\aruanjian\apeache\apache-maven-3.9.11-bin\apache-maven-3.9.11`**（与命令行同一个安装，命中其 `conf/settings.xml` 里的阿里云镜像，导入快且不出网问题）。User settings file 保持 default 即可（该 settings.xml 已配阿里云镜像 `mirrorOf=*`）。

## 三、启动顺序（本地 IDE 运行模式）

```
① deps: docker compose up (mysql/redis/minio)   ← 运行配置③，拉起依赖
        MySQL  localhost:13306 (root/root123)
        Redis  localhost:16379
        MinIO  localhost:9000（控制台 9001，minioadmin/minioadmin123）
② brave-backend (local)                          ← 运行配置①，8080 端口
③ brave-frontend dev                             ← 运行配置②，5173 端口（Vite 已代理 /api → 8080）
```

> 已在运行配置①中预置环境变量：`SPRING_DATASOURCE_URL`(13306)、`SPRING_DATASOURCE_PASSWORD=root123`、`SPRING_DATA_REDIS_PORT=16379`、`MINIO_SECRET_KEY=minioadmin123`。

**更省事的替代方案**：不依赖 IDE 本地跑，直接 `docker compose up -d` 一键起全套（含前端），浏览器开 `http://localhost`。

## 四、唯一必改项

- 运行配置①里的 `SPRING_DATASOURCE_PASSWORD` 当前是 docker 栈的演示密码 `root123`；如果换成自己的 MySQL，请同步修改该环境变量（IDEA 运行配置编辑框里改，或改 `brave-test-backend/src/main/resources/application.yml`）。
- `jwt.secret` 为演示占位值（带 change-me 提示），生产必须替换。

## 五、验证清单

| 检查点 | 通过标准 |
|---|---|
| Maven 导入 | External Libraries 出现 spring-boot 3.3.4 一堆 jar，无红线 |
| Lombok | 打开任意 Service，`log.` 无报错 |
| 后端启动 | 控制台出现 `Started BraveTestApplication`，访问 `http://localhost:8080/doc.html` 出 Knife4j 文档 |
| 前端启动 | `http://localhost:5173` 出登录页 |
| 登录 | `superadmin / admin123` 进入管理后台 |

## 六、常见坑

| 症状 | 原因与解法 |
|---|---|
| `Cannot resolve symbol log` | 注解处理未启用——本工程已预置 enabled，若被改动，去 `Settings → Build → Compiler → Annotation Processors` 勾选 Enable |
| MySQL `Access denied` | 密码不对——docker 栈是 `root123` 且端口 **13306**（本机 3306 被其他 MySQL 占用） |
| Redis 连接失败 | 用 **16379** 端口（compose 已映射），不是 6379 |
| 8080 端口占用 | docker 栈的 backend 也占 8080——先 `docker compose stop backend` 或改本地端口 `SERVER_PORT=18080` |
| 前端 5173 起不来 | node 版本需 ≥18；IDEA 内 Terminal 直接 `npm install && npm run dev` 也可 |
| 中文乱码 | 确认 encodings.xml 生效（File Encoding 全 UTF-8），终端加 `-Dfile.encoding=UTF-8` |

# FactBind v0.1 演示方案

**时长**：核心版 15 分钟；加问答与失败演示 30 分钟
**对应版本**：FactBind v0.1（`factbind-min` 分支）
**起点**：`main` 分支（纯原生 Spring 注解）
**日期**：2026年9月14日

> 配套文档：《FactBind v0.1 设计文档》（讲原理）、《FactBind v0.1 使用指南》（讲操作）。
> 本文只回答一件事：**站在台上这 15 分钟，手往哪放，嘴说什么。**
>
> 第五节列出的每一行输出都是 2026年9月14日在本机实测抓下来的，不是推测。

---

## 一、一句话立意

开场就讲这一句，后面所有动作都是在证明它：

> 这个项目里，同一条接口的"路径、参数名、参数位置、错误码状态码"写了不止一份：
> 后端一份、前端一份、测试一份，靠人肉对齐，改漏一处就是线上事故。
> FactBind 把它们收进**一份契约**，前后端都从这份契约**加载**——
> **不改框架、不生成代码、测试一行不用改。**

---

## 二、演示前 10 分钟的准备

### 2.1 环境检查

| 项 | 要求 | 命令 |
|---|---|---|
| JDK | 21+ | `java -version` |
| Node | 20+ | `node -v` |
| PowerShell | 系统自带的 Windows PowerShell 5.1 即可 | `powershell -Command $PSVersionTable.PSVersion` |

> 脚本对 **Windows PowerShell 5.1** 和 **PowerShell 7+** 都能跑，命令统一用 `powershell` 这个词。
> 如果你本机装了 PowerShell 7，把下面的 `powershell` 换成 `pwsh` 也可以，效果一样。
> 装了 7 但不确定的话别猜，直接用 `powershell`——它 Windows 上一定存在。

### 2.2 准备一个专用演示目录

**不要**拿你自己的工作仓库演示。开一个独立副本：

```powershell
cd <仓库根>
git worktree add ../factbind-demo main
cd ../factbind-demo
```

好处：现场随便改、随便跑、随便 `git checkout -- .` 还原，都不脏主仓库。结束后一行清掉：

```powershell
git worktree remove --force ../factbind-demo
```

### 2.3 预热依赖（关键，否则现场会卡在下载）

```powershell
cd backend
./mvnw test             # 首次会联网下载 Maven 自身 + 全部依赖，必须联网
cd ../frontend
npm ci                  # 装前端依赖
```

> **首次预热千万别加 `-o`。** `-o` 是"离线模式"，在还没下载过依赖的机器上会直接失败
> （实测报错：`Cannot access central (https://repo.maven.apache.org/maven2) in offline mode`）。
> 预热跑通一次之后，后面想快、想离线演示，再加 `-o`。

两条都成功跑过一次，现场节奏才是可预测的。

### 2.4 提前开好窗口

- **终端 A**：后端（`cd backend`）
- **终端 B**：前端（`cd frontend`）+ 后面要用的 `curl`
- **浏览器**：<http://localhost:5173>
- **编辑器**：提前把这三个文件打开，别现场找
  - `contracts/api.yaml`（跑完安装脚本后才有）
  - `backend/src/main/java/com/example/middemo/controller/UserController.java`
  - `frontend/src/api/userApi.ts`
- 确认 8080 和 5173 没被占用。后端端口被占会直接启动失败；Vite 端口被占会自己换到 5174（按提示改浏览器地址即可）

---

## 三、时间表

| 时间 | 幕 | 一句话 |
|---|---|---|
| 0:00 | 1 | 基线先跑起来 |
| 1:30 | 2 | 指出"同一件事写了两份" |
| 3:00 | 3 | **一条命令**装工具 |
| 4:30 | 4 | 装完测试照旧全绿 |
| 6:00 | 5 | 写契约 / 换注解 / 换调用点 |
| 9:00 | 6 | ★ 只改契约一处，行为跟着变 |
| 11:00 | 7 | ★ 契约被偷改，测试会红 |
| 12:30 | 8 | 写错符号，应用起不来 |
| 14:00 | 9 | 收尾：差异账 |

时间不够时，砍掉 7、8，保住 6。

---

## 四、分幕脚本

### 幕 1（0:00）基线先跑起来

```powershell
# 终端 A
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"
```

```powershell
# 终端 B
cd frontend
npm run dev
```

浏览器打开 <http://localhost:5173>，点两下：用户列表、用户详情。

**说什么**：

> 这是最标准的 Spring MVC 写法。`@GetMapping`、`@PathVariable`、`@RequestParam`，
> 路径和参数名全写在注解里。前端这边把同样的路径又写了一遍。先记住这个状态。

> PowerShell 里那个 `-D...` 参数**必须加引号**。不加会报
> `Unknown lifecycle phase ".run.profiles=h2"`——这是实测踩过的坑，不是理论。

### 幕 2（1:30）同一件事写了两份

编辑器里并排打开两个文件：

**后端 `backend/src/main/java/com/example/middemo/web/ApiPaths.java`**

```java
public static final String USERS = "/api/users";
public static final String USER_BY_ID = USERS + "/{id}";
public static final String USER_STATUS = USER_BY_ID + "/status";
```

**前端 `frontend/src/api/userApi.ts`**

```ts
return request<PageResponse<User>>(`/api/users${buildQuery({ ...params })}`)
return request<User>(`/api/users/${id}`)
return request<User>(`/api/users/${id}/status`, { method: 'PATCH', body: { status } })
```

**说什么**：

> 同一个 `/api/users/{id}`，后端写了一遍，前端又写了一遍，HTTP 方法在前端再写一遍。
> 这个项目的量级：前端 api 层有 **20 行**写着 URL 字面量，对应契约里的 20 条接口；
> 后端有 12 条路径常量。两边现在靠人肉对齐——后端把 `/users` 改成 `/members`，
> 前端不改就是 404，而且**编译不报错、测试也可能不报错**。

### 幕 3（3:00）一条命令装工具

```powershell
# 先看它要改什么，不落盘
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-factbind.ps1 -Target . -DryRun

# 真装
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-factbind.ps1 -Target .
```

**会看到**（实测输出）：

```text
FactBind 安装完成：
  - 后端实现：backend/src/main/java/com/example/middemo/factbind/ 9 个类
  - 前端实现：frontend/src/factbind/index.ts
  - 契约：contracts/api.yaml（从 factbind-min 拷入）
  - 配置：application.yml 追加 factbind.contract
  - 配置：vite.config.ts 加 fs.allow
  - 依赖：frontend 装好 yaml（契约解析用）
```

**说什么**：

> 装工具一共动了 10 个新文件、2 处配置和 1 个前端依赖，**没碰任何业务代码**。
> 现在控制器还是老写法，应用照常跑——FactBind 挂在那儿待命，你用多少它管多少。

### 幕 4（4:30）装完测试照旧全绿

```powershell
cd backend
./mvnw test
```

（已经预热过，这里加 `-o` 走离线会更快；没预热过就去掉 `-o`。）

**会看到**：

```text
Tests run: 84, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**说什么**：

> 84 个测试，数量与基线完全相同，**测试代码一行没改**。
> 这是这条路线的一个硬指标：装工具不该让任何既有测试变红，也不该逼你改测试。

### 幕 5（6:00）写契约 / 换注解 / 换调用点

**5a：现场只改一条，让大家看清"改的是什么"**

以 `User.Get` 这条为例，编辑器里演示三处：

1. 契约里已经有这一条（`contracts/api.yaml`）：

    ```yaml
    /api/users/{id}:
      get:
        operationId: User.Get
        parameters:
          - { name: id, in: path, required: true, schema: { type: integer } }
    ```

2. 控制器，改前 → 改后：

   ```java
   // 改前
   @GetMapping(ApiPaths.USER_BY_ID)
   public UserResponse getUser(@PathVariable("id") Long id) { ... }

   // 改后
   @FactBind("User.Get")
   public UserResponse getUser(@FactBindParam Long id) { ... }
   ```

3. 前端，改前 → 改后：

   ```ts
   // 改前
   return request<User>(`/api/users/${id}`)
   // 改后
   return factBindRequest<User>('User.Get', { params: { id } })
   ```

> 强调三点：`@RequestBody` / `@Valid` / `@Min` 全部原样不动；
> 参数名不再出现（用 Java 参数名）；HTTP 方法也不再出现（在契约里）。

**5b：剩下的接口一条命令到位**

```powershell
git checkout factbind-min -- `
  backend/src/main/java/com/example/middemo/controller `
  backend/src/main/java/com/example/middemo/exception `
  frontend/src/api `
  contracts/api.yaml

cd backend
./mvnw test
```

**会看到**：`Tests run: 84, Failures: 0` —— 全部接口换成符号调用之后，测试依然全绿。

重启后端、刷新页面，功能一模一样。

> 只做 5a 也可以，把 5b 当作"剩下的都是机械替换"一句话带过。

### 幕 6（9:00）★ 只改契约一处，行为跟着变

打开 `contracts/api.yaml`，把 `USER_NOT_FOUND` 的状态码改成 422，就**这一行**：

```yaml
USER_NOT_FOUND: { status: 422 }
```

重启后端（`Ctrl+C` 后重新 `./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"`），然后：

```powershell
curl -i http://localhost:8080/api/users/999999
```

**改前实测**：

```text
HTTP/1.1 404
{"code":"USER_NOT_FOUND","message":"User 999999 not found"}
```

**改后实测**：

```text
HTTP/1.1 422
{"code":"USER_NOT_FOUND","message":"User 999999 not found"}
```

**说什么**：

> 状态码这件事以前散在 11 个异常类和一个 `GlobalExceptionHandler` 里，
> 想把 404 改成 422 得翻一圈代码。现在改**契约里的一行**，
> **一个 Java 文件都没碰**，行为就变了。

> 注意：路由、参数、错误码状态码在后端是**启动期**读一次，所以后端要重启；
> 前端每次从磁盘读契约，刷新页面就生效。

### 幕 7（11:00）★ 契约被偷改，测试会红

在 `contracts/api.yaml` 里把 `/api/users/{id}` 改成 `/api/people/{id}`，然后：

```powershell
./mvnw test
```

**实测结果**：

```text
[ERROR]   UserControllerTest.getUserById:127 Status expected:<200> but was:<404>
[ERROR]   UserControllerTest.createUserReturnsCreated:154 Response header 'Location'
          expected:</api/users/21> but was:</api/people/21>
...
Tests run: 84, Failures: 6, Errors: 0, Skipped: 0
```

**说什么**：

> 测试挂了 6 个，而且挂得很具体：它拿着**自己那份**期望值去请求 `/api/users/1`，
> 而契约把路由改到了 `/api/people/1`，于是 404。
>
> 这就是"事实集中"真正买到的东西：**契约不是文档，是被执行的**。
> 如果测试也去读契约，契约一改测试跟着一起变，那就永远发现不了漂移。

演示完还原：`git checkout -- contracts/api.yaml`

### 幕 8（12:30）写错符号，应用起不来

把控制器里的 `@FactBind("User.Get")` 改成 `@FactBind("User.Gett")`，重启后端。

**实测结果**（约 6 秒后启动失败，进程自己退出，不会挂着）：

```text
WARN  ConfigServletWebServerApplicationContext : Exception encountered during context
      initialization - cancelling refresh attempt: ... BeanCreationException: Error
      creating bean with name 'requestMappingHandlerMapping' ...
      Invalid mapping on handler class [UserController]: public ... getUser(java.lang.Long)

Caused by: com.example.middemo.factbind.FactBindException:
      FactBind: no operation with symbol 'User.Gett' in the contract
```

**说什么**：

> 改错一个字母，应用**直接起不来**，而且告诉你哪个符号不存在。
> 不是等某个请求打进来才发现。契约里的拼写错误、参数名对不上、路径参数没人接，
> 全是在启动那一刻报错。

演示完还原：`git checkout -- backend`

### 幕 9（14:00）收尾：差异账

```powershell
git diff --shortstat main factbind-min
```

```text
 35 files changed, 910 insertions(+), 193 deletions(-)
```

**说什么**：

> 从基线到 v0.1 一共 35 个文件：607 行是工具实现、145 行是契约、
> 148 行是调用点改造，还有 193 行是**删掉的**——删掉的是散在各处的状态码。
>
> 测试文件：**0 改动**。
>
> 这就是 v0.1 的全部。没有代码生成，没有自造框架，
> Spring 还是那个 Spring，Vite 还是那个 Vite。

---

## 五、实测记录（可直接对照，均为 2026-09-14 本机结果）

| 环节 | 操作 | 实测结果 |
|---|---|---|
| 启动 | `./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"` | `FactBind loaded 20 operation(s) from URL [file:../contracts/api.yaml]`；`Started MidDemoApplication in 2.965 seconds` |
| 读列表 | `curl "http://localhost:8080/api/users?size=1"` | 200，返回一条用户 JSON |
| 资源不存在 | `curl -i http://localhost:8080/api/users/999999` | 404 + `{"code":"USER_NOT_FOUND","message":"User 999999 not found"}` |
| 方法不支持 | `curl -i -X PATCH http://localhost:8080/api/users` | 405 + `{"code":"METHOD_NOT_ALLOWED","message":"HTTP method PATCH is not supported by this endpoint"}` |
| 契约改状态码 | `USER_NOT_FOUND` 的 404 改成 422 后重启 | `HTTP/1.1 422`，body 不变，**零 Java 改动** |
| 契约改路径 | `/api/users/{id}` 改成 `/api/people/{id}` 后跑测试 | `Tests run: 84, Failures: 6`，含 `Status expected:<200> but was:<404>` |
| 符号写错 | `@FactBind("User.Get")` 改成 `User.Gett` 后启动 | 约 6 秒后启动失败：`FactBind: no operation with symbol 'User.Gett' in the contract` |
| 走完演示流程 | 安装脚本 + 切换调用点后跑测试 | `Tests run: 84, Failures: 0`，BUILD SUCCESS |
| 前端读契约 | 把契约里 `size` 的 default 从 20 改成 50，请求 dev server | dev server 返回 `"default":50`，刷新页面即生效，**不必重启 Vite** |
| 生产构建 | `npm run build` | 契约被打进产物，因此改契约后**生产包要重新构建**（dev 模式才免重启） |

---

## 六、兜底预案

| 如果现场…… | 那就…… |
|---|---|
| 后端起不来，提示端口占用 | `netstat -ano \| findstr :8080` 找到 PID 结束掉，或临时换端口 |
| `./mvnw` 卡在下载 | 说明预热没做；改用本机 Maven：`mvn test`（**不要加 `-o`**，冷机器离线必失败） |
| 前端端口被占 | Vite 会自动换到 5174，按终端提示改浏览器地址 |
| 现场把代码改坏了 | `git checkout -- .` 一键还原（演示目录是独立 worktree，随便还原） |
| 时间不够 | 砍掉幕 7、8，保住幕 6（全场最有说服力的一幕） |
| 现场没有网络 | 只有在**提前预热过**的前提下才行：Maven 全程加 `-o`，前端用 `npm ci --offline` |

---

## 七、可能会被问到

**Q：这和 OpenAPI / Swagger 有什么区别？**
OpenAPI 通常是"先写注解 → 生成文档"，契约是产物。这里是反过来：契约是**输入**，
路由和参数从它来，而且是一份**被执行的**事实——写错就启动失败。

**Q：这和代码生成（按契约生成 Controller / 类型）有什么区别？**
代码生成会多出一层产物：契约改了要重新生成、生成物要不要提交、生成物和手写代码谁说了算。
这里是**加载式**：契约是一份运行时读进来的数据，没有任何生成物。

**Q：有性能影响吗？**
路由和参数只在**启动期**解析一次，请求路径上没有额外开销。

**Q：前端还是得写 `'User.Get'` 这个字符串，不也是硬编码？**
是，而且是刻意的：符号是前后端唯一需要共同记住的稳定标识。
它比 URL 稳定得多——改路径、改参数位置、改 HTTP 方法都不会让它变。

**Q：契约会不会让前后端耦合更紧？**
契约里只放"双方都必须知道的传输事实"。DTO 结构、校验规则、业务规则都不在里面，
所以改业务逻辑不会牵动契约。

**Q：测试为什么一行没改还能全绿？**
因为改造只换了"路由和参数从哪来"，业务行为完全没动。
测试是黑盒的（发请求、看响应），所以感受不到这个变化——这恰好是它该有的样子。

**Q：真实项目里契约放哪？**
这个 Demo 放在仓库根的 `contracts/`，前后端各读一份。
多仓场景需要单独放一处（独立契约仓库或包），这是 v0.1 没解决的问题。

---

## 八、演示里不要做的事

- **不要现场 `npm install` / 首次 `./mvnw`**——现场下载依赖是事故高发区，提前预热。
- **不要讲 Maven 生态、包管理那些枝节**——听众关心的是接口事实集中，不是依赖怎么装。
- **不要打开 `factbind-d-deleted` 等废弃分支**——那是探索路径，只会把听众绕晕。
- **不要说"消灭硬编码"**——准确的定位是"消灭同一事实的多处独立声明"。
- **不要承诺 v0.1 没有的东西**：发布成包、启动期对账报告、契约版本号、请求体结构进契约，都还没做。

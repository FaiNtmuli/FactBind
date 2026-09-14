# FactBind v0.1 使用指南

**适用范围**：本仓库的中型 Java 前后端 Demo（Spring Boot + React/Vite）
**起点**：`main` 分支那份"纯原生注解"的基线代码
**日期**：2026年9月14日

> 本文档讲**怎么用**。原理和设计见同目录《FactBind v0.1 设计文档》。

---

## 0. 你要做的事情有多少

把基线改造成 FactBind v0.1，一共三类事：

| 类别 | 谁来做 | 量 |
|---|---|---|
| 装工具（拷 10 个文件 + 改 3 处配置） | **脚本** | 一条命令 |
| 写契约 | 人 | 20 条 operation，145 行 JSON |
| 调用点换写法（4 个控制器 + 4 个前端 api 文件） | 人 | 约 21 个文件 |

工具本身不碰你的业务代码。装完之后应用照常能跑，`@GetMapping` 继续工作——FactBind 只是挂在那儿待命，你用多少它管多少。

---

## 1. 装

在仓库根目录：

```powershell
# 先看它要改什么（不会落盘）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-factbind.ps1 -Target . -DryRun

# 真装
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-factbind.ps1 -Target .
```

它会做四件事：

> 命令里的 `powershell` 就是 Windows 自带的 Windows PowerShell 5.1，不需要额外安装。
> 本机如果装了 PowerShell 7，把 `powershell` 换成 `pwsh` 也可以（脚本对两者都测过）。
> `-ExecutionPolicy Bypass` 是为了避免系统执行策略禁止脚本运行，加着没有副作用。

1. 拷入后端实现：`backend/src/main/java/com/example/middemo/factbind/`（9 个类）
2. 拷入前端实现：`frontend/src/factbind/index.ts`
3. 契约文件不存在时，拷入一份 `contracts/api.json` 模板
4. 改三处配置：`application.yml` / `vite.config.ts` / `tsconfig.app.json`（已经有就跳过）

你已有的文件不会被覆盖（契约除外——契约只在不存在时拷入；实现类重复安装时会覆盖为脚本来源的版本并在输出里说明）。

> 这个脚本是本仓库的演示用安装器，不是 FactBind 的产品形态。
> 产品化之后，第 1、2 步变成"装一个包"，第 3 步变成一个初始化命令，都不该由使用者手动拷文件。

---

## 2. 跑起来

后端（用 H2 内存库，免装 PostgreSQL）：

```powershell
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"
```

> PowerShell 里 `-D` 参数**必须加引号**，否则报 `Unknown lifecycle phase ".run.profiles=h2"`（实测过）。
> 用 Git Bash / WSL / macOS 时加不加都行。

前端：

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 <http://localhost:5173>（Vite 把 `/api` 代理到 `localhost:8080`）。

**怎么确认 FactBind 生效了**：后端启动日志里应该有一行

```text
FactBind loaded 20 operation(s) from URL [file:../contracts/api.json]
```

没有这一行说明契约没被读到，检查 `application.yml` 里的 `factbind.contract`。

这一步装完就能跑，因为控制器还是老注解。接下来才是演示的重点。

---

## 3. 三段"人的活"

### 3.1 写契约

在 `contracts/api.json` 里，每一条接口写成一段：

```json
"/api/users/{id}": {
  "get": {
    "operationId": "User.Get",
    "parameters": [
      { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
    ]
  }
}
```

错误码 → 状态码写在 `x-factbind-errors`：

```json
"x-factbind-errors": {
  "USER_NOT_FOUND": { "status": 404 }
}
```

### 3.2 控制器换注解

改前：

```java
@GetMapping(ApiPaths.USER_BY_ID)
public UserResponse getUser(@PathVariable("id") Long id) {
    return userService.getUser(id);
}
```

改后：

```java
@FactBind("User.Get")
public UserResponse getUser(@FactBindParam Long id) {
    return userService.getUser(id);
}
```

要点：

- `@FactBind("符号")` 顶替掉 `@GetMapping` / `@PostMapping` / ... 全部五个注解，路径和方法都从契约来
- `@FactBindParam` 顶替 `@PathVariable` / `@RequestParam`
- **不写名字**，参数名用 Java 参数名（Spring Boot 的 parent POM 默认开启 `-parameters`）；想显式写也行：`@FactBindParam("id")`
- `@RequestBody`、`@Valid`、`@Min` / `@Max` **保持原样不动**
- 只有一处需要绕一下：`POST` 返回 `Location` 头时不再拼字符串，而是问契约

  ```java
  // 改前
  .created(URI.create(ApiPaths.withId(ApiPaths.USER_BY_ID, created.id())))
  // 改后
  .created(URI.create(contract.path("User.Get", Map.of("id", created.id()))))
  ```

  这需要控制器多注入一个 `ContractRegistry contract;`

如果错误码的状态码也要收进契约，异常类的改法是**删**：`BusinessException` 不再持有 `HttpStatus`，
`GlobalExceptionHandler` 统一从契约查。11 个异常类每个少一行。

### 3.3 前端换符号调用

改前：

```ts
export function getUser(id: number): Promise<User> {
  return request<User>(`/api/users/${id}`)
}
```

改后：

```ts
import { request as factBindRequest } from '../factbind'

export function getUser(id: number): Promise<User> {
  return factBindRequest<User>('User.Get', { params: { id } })
}
```

调用方只给"符号 + 参数值"，`in: path` 还是 `in: query`、HTTP 方法是什么，全部由契约决定。
查询参数也不用再手工 `buildQuery`。

---

## 4. 契约写法速查

| 你要的 | 契约里怎么写 |
|---|---|
| path 参数 | `{ "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }` |
| 可选查询参数 | `{ "name": "keyword", "in": "query", "required": false, "schema": { "type": "string" } }` |
| 带默认值的查询参数 | 同上，`schema` 里加 `"default": 20` |
| 没有参数的接口 | 不写 `parameters` |
| 请求体 | **不用写**，仍由 `@RequestBody` + DTO 负责 |
| 新增一个错误码 | 在 `x-factbind-errors` 里加 `"CODE": { "status": 4xx }` |

---

## 5. 报错对照表

用错了会看到这些消息（后缀统一是 `FactBind` 或 `FactBind contract error`）：

| 消息 | 意思 | 怎么修 |
|---|---|---|
| `no operation with symbol 'X' in the contract` | 控制器或前端用了一个契约里没有的符号 | 检查拼写，或在契约里补上这条 operation |
| `controller declares parameter 'x' but operation 'X' does not declare it` | 控制器声明了契约里没有的参数 | 在契约的 `parameters` 里补上，或删掉这个参数；没写 `@FactBindParam` 名字时也可能是编译没开 `-parameters` |
| `operation 'X' declares path parameter 'x' but no controller parameter consumes it` | 契约声明了 path 参数，控制器没人接 | 给方法加一个 `@FactBindParam` 参数 |
| `operation "X" requires parameter "x"` | 调用时必填参数没给 | 前端传这个参数，或把它标成 `required: false` 并给默认值 |
| `parameter 'x' of 'X' uses unsupported location 'y'` | `in` 只能写 `path` 或 `query` | 改契约 |
| `error code 'X' is not declared in 'x-factbind-errors'` | 代码抛了一个契约没声明的错误码 | 在 `x-factbind-errors` 里补上 |
| `missing or invalid 'paths' object` / `has no operationId` / `duplicate operationId` | 契约本身写坏了 | 按消息修 JSON |

以上全部是**启动期**失败，应用不会半死不活地跑起来。

---

## 6. 自查清单

装完、改完之后，按这个顺序验：

```powershell
# 后端测试（应全绿，数量与基线相同）
cd backend; ./mvnw test

# 前端测试与构建
cd frontend; npm test; npm run build
```

再手工打三个请求确认行为没变：

```powershell
# 正常读取
curl "http://localhost:8080/api/users?size=1"
# 资源不存在 → 404 + USER_NOT_FOUND
curl -i "http://localhost:8080/api/users/999999"
# 方法不被支持 → 405 + METHOD_NOT_ALLOWED
curl -i -X PATCH "http://localhost:8080/api/users"
```

---

## 7. 常见问题

**改一条接口的默认值要改几个地方？**
只改契约一处。比如把 `size` 的 `default` 从 20 改成 50——后端重启生效，前端**不用改代码**（它读的是同一份契约）。

**改了契约的 path，测试会怎样？**
测试会红。因为测试用的是自己那份期望值（`ApiPaths` 常量），它构造的 URL 和契约里的新 path 对不上，请求就 404 了。
这是故意的——如果测试也去读契约，契约被改时测试会跟着一起变，等于没测。

**契约文件能换位置吗？**
能，但要同时改两处：后端 `application.yml` 的 `factbind.contract`，前端 `frontend/src/factbind/index.ts` 顶部那行相对 import。

**前后端会不会因为共享契约而耦合过紧？**
契约里放的是双方都必须知道的传输事实。业务规则、校验规则、DTO 结构都不在里面，所以"改业务"不会牵动契约。

**新增一个接口要动几个地方？**
契约加一条 operation；控制器加一个方法（`@FactBind`）；前端 api 层加一个函数；需要新错误码时在 `x-factbind-errors` 加一行。

---

## 8. 分支说明

| 分支 | 用途 |
|---|---|
| `main` | 纯原生注解的基线，作为对照 |
| `factbind-min` | FactBind v0.1 本体（最小可用） |
| `native-baseline`（tag） | 更早的原始基线，保留备查 |

其余的实验分支已统一加 `-deleted` 后缀，表示不再演进：
`factbind-d-deleted`（产品化/独立包路线，已验证可行）、`factbind-v1-deleted`（生成常量类路线，已否定）、
`archive/factbind-d-stage3-deleted`、`wip/startup-conformance-deleted`。

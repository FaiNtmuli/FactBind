# FactBind v0.1 设计文档

**版本**：v0.1（最小可用）
**成形分支**：`factbind-min`
**对照基线**：`main`
**日期**：2026年9月14日

> 本文档记录 v0.1 的**实际形态**（已实现、已实测），不是设想稿。
> 方向取舍的讨论过程见同目录《FactBind 设计讨论工作记录》。
> 在 `main`（基线）上阅读本文时请注意：v0.1 的代码在 `factbind-min` 分支上，基线本身不含 FactBind。

---

## 一、v0.1 要解决的问题

工作记录里把问题界定得很清楚：接口事实不可能消失，危险的是**同一份事实在多个位置各写一遍，然后各自漂移**。

基线（`main`）里的实际情况：

| 边界事实 | 基线写在哪 |
|---|---|
| 路由（method + path） | 4 个控制器的 `@GetMapping(ApiPaths.USERS)` 等 |
| 参数名与参数位置 | `@PathVariable("id")`、`@RequestParam("size")` |
| 参数默认值 | `@RequestParam(defaultValue = "20")` |
| 前端请求 URL | `frontend/src/api/*.ts` 里的模板字符串，例如 `` `/api/users/${id}` `` |
| 错误码 → HTTP 状态码 | 11 个异常类 + `GlobalExceptionHandler` 里写死 |

v0.1 的目标不是覆盖所有接口能力，而是证明一件事：

> **一份契约能同时当后端路由的元数据源和前端的请求构造源，而 Spring 和 Vite 仍然按原生方式运行。**

---

## 二、总体形态

```text
                contracts/api.json          ← 唯一的边界事实
                        │
        ┌───────────────┴───────────────┐
        │                               │
   后端（启动期加载）              前端（构建期打包）
   ContractRegistry                src/factbind/index.ts
        │                               │
   FactBindHandlerMapping            request('User.Get', {...})
   FactBindParamArgumentResolver          │
        │                               │
   Spring MVC 原生分发/绑定/序列化    fetch（经 Vite 代理到后端）
        │                               │
   控制器方法（业务代码）            页面 / 组件（业务代码）
```

两侧都是**加载式**：没有代码生成，没有编译期插件，没有中间 DSL。契约是一份运行时读进来的数据。

---

## 三、边界事实的边界划在哪

收进契约的（HTTP 传输层事实，前后端都要对齐的东西）：

- HTTP 方法、路径模板
- 参数名、参数位置（`path` / `query`）、是否必填、默认值
- 错误码 → HTTP 状态码

**不**收进契约的（留在代码里）：

- 请求体 / 响应体的字段结构 —— 仍由 DTO 记录类型，Jackson 负责序列化
- 字段级校验规则（`@NotBlank`、`@Min`、`@Max`）—— 仍在 DTO 和控制器参数上，由 Jakarta Validation 执行
- 业务规则、事务边界、Repository 查询

判据一句话：

> **只搬"HTTP 上看得见、且双方必须对齐"的事实。**
> 某个规则如果只对一方有意义（比如"单价必须 ≥ 0.01"是业务规则），它就不属于接口边界。

v0.1 的请求体因此仍然写作 `@RequestBody`，契约里完全不提 DTO 结构；也正因为如此，
"新增一个字段"这类改动在 v0.1 里不产生任何契约改动。

---

## 四、文件清单

新增的实现（共 10 个文件、607 行）：

| 文件 | 行 | 作用 |
|---|---:|---|
| `backend/src/main/java/com/example/middemo/factbind/ContractRegistry.java` | 148 | 启动时把契约 JSON 读成内存表：`符号 → 方法/路径/参数`，以及 `错误码 → 状态码` |
| `.../factbind/FactBind.java` | 19 | 方法注解，写一个稳定符号，**替代** `@GetMapping("/users/{id}")` |
| `.../factbind/FactBindParam.java` | 19 | 参数注解，**替代** `@PathVariable("id")` / `@RequestParam("size")` |
| `.../factbind/FactBindHandlerMapping.java` | 68 | 建路由表时把 `@FactBind` 翻成 Spring 的 URL 条目；顺带校验参数是否对得上 |
| `.../factbind/FactBindParamArgumentResolver.java` | 94 | 请求期按契约声明的 `in` 去 path / query 取值，并交给 Spring 做类型转换 |
| `.../factbind/FactBindWebConfig.java` | 49 | 用 Spring 官方扩展点把上面两个挂进 MVC，并声明注册表 Bean |
| `.../factbind/ResolvedOperation.java` | 24 | 契约里"一条接口"的内存形态 |
| `.../factbind/ResolvedParameter.java` | 9 | 契约里"一个参数"的内存形态 |
| `.../factbind/FactBindException.java` | 42 | 所有失败的统一出口，一律 fail fast |
| `frontend/src/factbind/index.ts` | 135 | 前端半边：读同一份契约，按符号拼 URL 并发请求 |

另加一份契约 `contracts/api.json`（145 行，20 条 operation + 20 条错误码），以及 `scripts/install-factbind.ps1`（演示用安装脚本，不属于 FactBind 本体）。

---

## 五、后端：四次介入

v0.1 的后端没有"自己实现一个 Web 框架"。它只在 Spring 原有的四个位置各插了一手。

### 5.1 启动期 —— `ContractRegistry`

- **输入**：`factbind.contract` 配置指向的资源（默认 `classpath:contracts/api.json`），本项目里是 `file:../contracts/api.json`
- **隐式输入**：Spring 的 `ObjectMapper`、`ResourceLoader`
- **输出**：构建好的内存表，并打一行启动日志
- **隐式输出**：契约读不到、`paths` 缺失、某条 operation 没有 `operationId`、`operationId` 重复、错误码缺 `status` —— 全部在这里抛异常，**应用直接起不来**

这就是"契约不是文档，是被执行的东西"的第一层含义：写错了启动就失败，而不是等到某个请求打进来才发现。

### 5.2 建路由表期 —— `FactBindHandlerMapping`

Spring 建路由表的动作本来就是"扫控制器方法 → 读注解 → 构造 `RequestMappingInfo`"。

- **输入**：控制器方法上的 `@FactBind("User.Get")`
- **隐式输入**：`ContractRegistry` 里的那条 operation
- **输出**：一个 `RequestMappingInfo`，内容是契约里的 path + method
- **隐式输出**：把 `path`、`query` 参数写全之后，Spring 的**分发、内容协商、序列化、CORS、拦截器全部照旧**

顺带做一件 Spring 自己不做的事：校验"控制器声明的参数"和"契约声明的参数"是否一致——
契约声明了 path 参数 `id` 而没有任何控制器参数消费它，或者控制器声明了契约里没有的参数名，都在**启动期**报错。
这是唯一能在这个时间点发现参数名写错的地方。

解析只发生一次，请求路径上没有额外开销。

### 5.3 请求期 —— `FactBindParamArgumentResolver`

和 Spring 自带的 `PathVariableMethodArgumentResolver` 做同一件事，区别只有一处：**参数在 HTTP 上的名字和位置不再来自注解，而来自契约**。

- **输入**：`@FactBindParam` 标注的控制器参数
- **隐式输入**：契约里该参数的 `in`（`path` / `query`）、`required`、`schema.default`
- **输出**：转换好的参数值
- **隐式输出**：类型转换仍走 Spring 的 `WebDataBinder`，所以枚举、数字的转换规则与原生注解**完全一致**；必填缺失抛的是 Spring 自己的 `MissingServletRequestParameterException`，因此错误响应格式和基线一模一样

### 5.4 错误期 —— `GlobalExceptionHandler` + `x-factbind-errors`

- **输入**：任意异常携带的错误码（例如 `USER_NOT_FOUND`）
- **隐式输入**：契约的 `x-factbind-errors` 表
- **输出**：`ResponseEntity`，状态码从契约查出来
- **隐式输出**：异常类里**不再出现任何状态码字面量**。把 `409` 改成 `422` 只需要改契约一处，不必碰 11 个异常类

契约里没有声明的错误码会立刻抛异常，而不是猜一个状态码返回。

---

## 六、前端：一个文件

`frontend/src/factbind/index.ts` 做三件事：

1. **建表**（模块加载时）：遍历契约的 `paths`，得到 `符号 → 方法/路径/参数` 的表
2. **拼路径**：`path(symbol, params)` 把 `{id}` 这类模板参数填实
3. **发请求**：`request(symbol, { params, body })` 把参数按契约声明的 `in` 分到 path 或 query，套上默认值，然后交给 `frontend/src/api/http.ts` 里原有的 `request` 发出去

调用方只写符号和参数值：

```ts
request('User.Get', { params: { id } })
```

契约里的 `in` 是 `path` 还是 `query`、HTTP 方法是什么，调用方一律不关心。

---

## 七、契约格式

沿用 OpenAPI 3.0 的形状，只用到其中很小一部分：

```json
{
  "openapi": "3.0.3",
  "info": { "title": "MidDemo API", "version": "1.0.0" },

  "x-factbind-errors": {
    "USER_NOT_FOUND": { "status": 404 },
    "DUPLICATE_EMAIL": { "status": 409 }
  },

  "paths": {
    "/api/users/{id}": {
      "get": {
        "operationId": "User.Get",
        "parameters": [
          { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
        ]
      }
    }
  }
}
```

命名规则：

- `operationId` 是**稳定符号**，命名建议 `资源.动作`；它是前后端唯一需要共同记住的词
- `paths` 的键是路径模板，`{id}` 形式
- `parameters[].in` 目前只支持 `path` 和 `query`
- `schema.default` 是参数默认值；不写 `default` 且 `required: false`，缺席时值为 `null`

用 OpenAPI 的形状（而不是自创格式）的目的：编辑器、校验器、未来的文档生成工具都能直接吃这份文件。

---

## 八、配置项（三处）

| 文件 | 改动 | 为什么 |
|---|---|---|
| `backend/src/main/resources/application.yml` | `factbind.contract: file:../contracts/api.json` | 告诉后端契约在哪；默认值是 `classpath:contracts/api.json` |
| `frontend/vite.config.ts` | `server.fs.allow: ['..']` | 契约放在 `frontend/` 外面，Vite 默认只允许读项目目录内的文件 |
| `frontend/tsconfig.app.json` | `"resolveJsonModule": true` | 允许前端 `import` 一份 JSON |

契约文件本身放在**仓库根的 `contracts/`**，前后端各读同一份。

---

## 九、与基线的差异账

`git diff main factbind-min` 共 35 个文件、**+910 / −193**，可分四类：

| 类别 | 内容 | 文件数 | 增 / 删 |
|---|---|---:|---|
| 包内内容（工具实现） | 后端 9 类 472 行 + 前端 `index.ts` 135 行 | 10 | +607 / 0 |
| 契约 | `contracts/api.json` | 1 | +145 / 0 |
| 调用点修改与 import | 4 个控制器、13 个异常类、4 个前端 api 文件 | 21 | +148 / −193 |
| 配置项修改 | `application.yml`、`vite.config.ts`、`tsconfig.app.json` | 3 | +10 / 0 |

两点说明：

1. 第二类里那 193 行删除，大头是 13 个异常类里被删掉的 `HttpStatus`。它不只是"换个注解"，而是**删掉代码里那份重复的事实**。
2. 测试**一行没改**。后端 84 个测试、前端 17 个测试在两条线上数量与内容完全相同。

---

## 十、v0.1 明确不做的事

| 不做 | 原因 |
|---|---|
| 代码生成（生成常量类 / Controller / 类型） | 与"加载式"相冲突；契约改动不应触发一次构建产物变更 |
| 独立发布成包（starter / npm 包） | 已在 `factbind-d` 上验证过可行，但 v0.1 的定位是"最小可用"，不是分发形态 |
| 启动期对账报告、契约版本号、破坏性变更检测 | 功能尚未稳定，先不加 |
| 请求体 / 响应体结构、校验规则进契约 | 属于业务规则，进契约会让接口负责的边界变得不清（见第三节） |
| 安全、限流、弃用等轴 | 空着的轴不预填，等真的需要再加 |
| 为 FactBind 自己新增测试 | v0.1 的验收标准是"基线测试原样全绿"，不是"给工具写测试" |

---

## 十一、已知瑕疵与遗留

1. **`backend/src/main/java/com/example/middemo/web/ApiPaths.java` 成了只被测试使用的类。**
   控制器改用 `@FactBind` 后不再 import 它，但 5 个测试文件仍在用它构造请求路径。
   这本身是**好事**（测试保留了独立的路径期望值，契约里的路径被偷改时测试会红），
   瑕疵在于它的位置：住在生产源码目录里却只有测试用，更合适的位置是 `backend/src/test/java/com/example/middemo/support/`。
   尚未改动，因为它会牵动测试文件的 import。

2. **契约的位置写在两个地方。** 后端读 `application.yml` 的 `factbind.contract`，
   前端读 `index.ts` 里写死的相对 import（`../../../contracts/api.json`）。
   换契约文件位置时要同时改这两处。

3. **`main` 的 `README.md` 写的是"本项目不包含任何 FactBind 代码"**，
   这句话在 `factbind-min` 上已经过期。

4. **参数位置只支持 `path` 和 `query`。** 遇到别的位置会启动期报错，而不是静默忽略。

# FactBind 完整行动指南 v0.1

> **Facts live once. Bind everywhere.**

## 一、项目最终要解决什么

FactBind 不应该被定义成“另一个 OpenAPI 工具”，也不应该被定义成“无代码生成版 OpenAPI Client”。

它试图解决一个更基础的问题：

> **一个软件边界事实，本来只应该存在一次，却经常被重复硬编码在多个应用、多个语言、多个框架和多个文件中。**

例如一个普通 HTTP 接口的 URL、HTTP Method、参数位置、字段结构等，可能同时出现在后端路由、前端请求、类型定义和接口文档中。原始设计文档已经准确指出，这些重复产生的主要就是“一致性成本”和“沟通成本”。

FactBind 的目标不是消灭这些事实，而是：

> **把可变化的边界事实集中到 Contract 中，让应用代码尽可能只保留稳定身份，再以最符合原语言和框架习惯的方式把事实绑定回来。**

可以概括成：

```text
Changing Facts
     ↓
  Contract

Stable Identity
     ↓
   Symbol

Contract + Symbol
     ↓
   Binding
     ↓
Existing Application
```

最简单的 HTTP 例子：

```text
Stable Symbol:
User.Get

Contract:
GET /api/users/{id}

Application:
我要执行 User.Get
```

以后：

```text
/api/users/{id}
        ↓
/api/v2/members/{id}
```

只属于 Contract 的变化。

---

# 二、FactBind 的八条核心原则

这些原则应该写进仓库的 `PRINCIPLES.md`，以后所有功能、PR、Adapter 都用它们判断。

## 1. Single Source of Truth

**边界事实只定义一次。**

例如：

```text
method
path
参数位置
transport field name
request schema
response schema
content-type
security requirement
```

原则上都属于 Contract。

应用代码不应该为了“再次描述接口”而重复它们。

---

## 2. Stable Symbol

应用代码必须表达：

> “我要做什么？”

所以不能把所有东西都抽掉。

最终应该保留一个稳定身份，例如：

```text
User.Get
User.Create
Order.Submit
Payment.Refund
```

这个 Symbol 承载业务身份。

而：

```text
GET
/api/users/{id}
```

只是这个身份当前的一种网络实现。

因此：

> **String as identity, not string as semantics.**

字符串负责“它是谁”。

Contract 负责“它怎么工作”。

---

## 3. Hardcode Equivalence

Contract Binding 后的行为，应当和程序员亲手把同样的协议事实硬编码进去保持等价。

例如：

```ts
fetch("/users/123");
```

和：

```ts
fetch(
  factbind.request("User.Get", { id: "123" })
);
```

最终发出的 HTTP 请求应当一致。

FactBind 不是一种新网络协议。

它只是把：

```text
原本散落在源码里的事实
```

移到：

```text
Contract
```

然后绑定回来。

---

## 4. Consumption Transparency

普通开发者不应该被迫理解：

```text
runtime interpretation
compile-time binding
source generation
reflection
embedding
```

这些都是实现技术。

不同语言可以选择不同的最佳实现。

例如：

```text
JavaScript
→ runtime / build-time 都可以

Java
→ compile-time adapter 可以更自然

C#
→ compiler integration 可以更自然

Go
→ 必要时可以使用生成缓存
```

FactBind 对用户提供的是：

```text
Contract → usable behavior
```

至于中间怎么完成，属于 Adapter 内部。

---

## 5. Zero Framework Ownership

FactBind 不应该试图成为新的：

```text
Express
FastAPI
Spring
Axios
fetch
React Query
Kafka
```

正确方向是：

```text
Contract
↓
产生现有生态本来就认识的东西
↓
Existing Framework
```

例如浏览器 `fetch()` 本来接受 `Request`。

那么可以：

```ts
fetch(
  factbind.request("User.Get", { id })
);
```

而不是非要强迫用户：

```ts
factbind.superHttpFramework.call(...)
```

FactBind 应该进入现有世界，而不是把开发者拖进自己的世界。

---

## 6. Minimize Hardcode Surface

Hardcode 不只是字符串。

例如：

```js
app.get("/users/:id", handler);
```

其中：

```text
get
```

本身就在表达 HTTP Method。

而：

```text
"/users/:id"
```

表达 Path。

下面这些也都是 Hardcode Surface：

```js
req.query.page

req.headers["x-tenant-id"]

JSON.stringify(body)

new FormData()

response.json()
```

所以 FactBind 要研究的不是：

> “怎么把 URL 字符串拿出去？”

而是：

> **应用代码到底有哪些位置正在重复表达边界事实？**

---

## 7. Minimize Habit Distance

消灭 Hardcode 不能以破坏开发体验为代价。

例如：

```js
app.get("/users/:id", handler);
```

非常自然。

为了消灭所有 Hardcode 写成：

```js
app[operation.method](operation.path, handler);
```

虽然理论上更纯，却可能更难读。

因此：

> **统一的是含义，不一定是语法。**

JavaScript 应该像 JavaScript。

Python 应该像 Python。

Spring 应该尽量像 Spring。

不同 Adapter 可以拥有不同代码形式，只要底层 Stable Symbol 和 Contract Semantics 一致。

---

## 8. Change-Weighted Trade-off

最终目标不是：

```text
Hardcode Surface = 0
```

而应该综合考虑：

```text
Hardcode Surface ↓
Habit Distance ↓
Change Cost ↓
```

一个 Hardcode 是否值得消灭，可以粗略理解为：

```text
价值
≈
重复次数
×
变化频率
×
变化出错成本
```

因此：

```text
URL / Path
```

通常优先级很高。

而：

```text
GET → POST
```

虽然理论上可以自动处理，但实际变化频率往往较低，不值得为了极低概率变化永久牺牲代码可读性。

FactBind 应追求：

> **以尽可能小的开发习惯改变，消除尽可能多、且最值得消除的边界硬编码。**

---

# 三、产品定位

FactBind 应该定位为：

> **A contract binding ecosystem for keeping changing boundary facts out of application code.**

不要定位为：

```text
❌ Better OpenAPI
❌ OpenAPI replacement
❌ Another API framework
❌ Another code generator
❌ Cross-language tRPC
❌ Swagger killer
```

更准确的是：

```text
OpenAPI / Contract
        ↓
      FactBind
        ↓
Existing Applications
```

OpenAPI 已经非常成熟地解决：

> API 如何被描述。

FactBind解决：

> 描述完成以后，怎样以最少 Hardcode 和最小 Habit Distance 进入应用程序。

截至目前，OpenAPI 最新正式规范已经有 3.2.0，因此第一阶段完全没必要重新发明 API 描述标准。

---

# 四、第一阶段不要发明自己的 Contract 格式

第一版建议直接：

```text
OpenAPI YAML
```

作为主要输入。

例如：

```yaml
openapi: 3.1.0

info:
  title: Demo API
  version: 1.0.0

paths:
  /api/users/{id}:
    get:
      operationId: User.Get

      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

      responses:
        "200":
          description: User
```

FactBind 自己规定：

> **FactBind-managed operations 必须有稳定 `operationId`。**

虽然 OpenAPI 本身没有要求所有 Operation 都必须存在 `operationId`，但对于 FactBind：

```text
operationId
=
Stable Symbol
```

所以它应该是必要条件。

第一阶段推荐 Symbol 命名：

```text
Domain.Verb
```

例如：

```text
User.Get
User.List
User.Create

Order.Get
Order.Submit
Order.Cancel
```

不要一开始设计复杂 namespace 规范。

---

# 五、内部一定要有自己的 Normalized Contract Model

虽然外面使用 OpenAPI，但核心层不要让所有 Adapter 直接操作 OpenAPI 原始 JSON。

应该：

```text
OpenAPI
   ↓
OpenAPI Loader
   ↓
Normalized Contract Model
   ↓
Binding Adapters
```

例如内部：

```ts
interface Operation {
  symbol: string;

  transport: {
    kind: "http";
    method: string;
    path: string;
  };

  parameters: Parameter[];

  request?: Schema;

  responses: ResponseDefinition[];
}
```

原因很重要。

以后你可能支持：

```text
OpenAPI
TypeSpec
FactBind Simple YAML
Events Contract
AI Tool Contract
```

如果 Adapter 全部直接理解 OpenAPI：

FactBind 永远被 OpenAPI 数据结构绑死。

正确结构是：

```text
OpenAPI ─────┐
             │
TypeSpec ────┼──→ Normalized Contract
             │
Other ───────┘
                     ↓
                   Binding
```

---

# 六、第一个 MVP 到底做什么

第一版一定克制。

## FactBind v0.1 只支持

```text
语言：
TypeScript / JavaScript

Contract：
OpenAPI YAML / JSON

Protocol：
HTTP/JSON

客户端：
fetch

服务端：
Express

Operation：
必须有 operationId

参数：
path
query
header
JSON body

Response：
JSON

Validation：
最基本输入/输出验证即可
```

## 第一版坚决不做

```text
❌ Python
❌ Go
❌ Java
❌ C#
❌ Kafka
❌ AI Tools
❌ Registry
❌ Web UI
❌ 自创 Contract DSL
❌ OAuth 完整系统
❌ Mock 平台
❌ API Gateway
❌ Documentation UI
❌ Workflow
❌ Streaming
❌ GraphQL
❌ gRPC
```

这些不是“不重要”。

而是：

> **在你证明最核心 Binding 模型有价值之前，它们全部属于干扰。**

---

# 七、MVP 最重要的三个 API

第一阶段甚至只需要把三件事设计漂亮。

## 1. Resolve

最底层：

```ts
const op = contract.operation("User.Get");
```

得到：

```ts
{
  symbol: "User.Get",
  method: "GET",
  path: "/api/users/{id}",
  ...
}
```

这是整个系统的地基。

---

## 2. Native Binding

浏览器：

```ts
fetch(
  factbind.request("User.Get", {
    id: "123"
  })
);
```

这里：

```text
fetch
```

仍然是浏览器原生 fetch。

FactBind 只负责产生：

```text
Request
```

这是最符合 Zero Framework Ownership 的形态。

---

## 3. Full Binding

为了展示理论极限，可以同时提供：

```ts
const user =
  await factbind.call("User.Get", {
    id: "123"
  });
```

`call()` 本质只是：

```text
request()
+
fetch()
+
response parsing
```

属于便利 API。

核心不应该依赖它。

服务端初期可以：

```ts
factbind.bind(
  app,
  "User.Get",
  handler
);
```

以后再研究有没有更低 Habit Distance 的 Express 适配。

---

# 八、第一版最重要的 Demo

不要做 Todo List 大而全 Demo。

只做一个极其简单的：

```text
User.Get
```

传统版本：

### Backend

```js
app.get("/api/users/:id", handler);
```

### Frontend

```js
fetch(`/api/users/${id}`);
```

FactBind：

### Frontend

```js
fetch(
  contract.request("User.Get", { id })
);
```

### Backend

```js
contract.bind(
  app,
  "User.Get",
  handler
);
```

然后现场修改 Contract：

```text
/api/users/{id}
        ↓
/api/v2/members/{id}
```

重新启动。

结果：

```text
Frontend      0 行修改
Backend       0 行修改
Business      0 行修改
Contract      1 处修改
```

这个 Demo 是整个项目第一阶段的“核武器”。

README 第一屏就展示它。

---

# 九、必须同时准备一个传统版本作为 Baseline

仓库：

```text
examples/

  manual/
      frontend/
      backend/

  factbind/
      frontend/
      backend/

  openapi-runtime/
      frontend/
      backend/
```

这样可以真正比较：

```text
纯人工
vs
当前 OpenAPI runtime 最轻方案
vs
FactBind
```

不能只自己说 FactBind 简单。

要让开发者直接看到 Diff。

---

# 十、建立自己的评价指标

这是 FactBind 最有机会形成理论贡献的地方。

建议开始记录四个指标。

## Hardcode Surface Count

统计应用源码中还有多少位置在表达协议事实。

例如：

```text
Manual:
17

FactBind:
3
```

---

## Protocol Change Diff

进行：

```text
/api/users/{id}
↓
/api/v2/members/{id}
```

后应用代码需要改多少行。

理想：

```text
Manual:
Frontend + Backend + Docs

FactBind:
Contract only
```

---

## Habit Distance

这是难以完全量化的指标，可以用几个代理指标：

```text
需要学习的新 API 数量
需要替换的原框架 API 数量
迁移修改行数
是否仍使用原生对象
```

例如：

```text
fetch(contract.request(...))
```

Habit Distance 很低。

---

## Generated Surface

记录：

```text
新增生成文件数
生成源码行数
必须维护的中间 artifact 数量
```

FactBind 应尽量接近：

```text
0 persistent generated API surface
```

但不要把“绝对不生成任何东西”当宗教。

---

# 十一、测试体系

第一版至少建立五类测试。

## 1. Resolution Test

```text
User.Get
↓
GET /users/{id}
```

必须正确。

---

## 2. Request Equivalence Test

比较：

```ts
fetch("/users/123")
```

和：

```ts
factbind.request(
  "User.Get",
  { id: "123" }
)
```

生成：

```text
method
URL
headers
body
```

必须一致。

这就是 Hardcode Equivalence 的最直接自动化证明。

---

## 3. Change Isolation Test

修改：

```text
/users/{id}
↓
/members/{id}
```

确保：

```text
application source
```

无需修改。

---

## 4. Symbol Error Test

```ts
"Uesr.Get"
```

必须产生极好理解的报错：

```text
Unknown operation "Uesr.Get".

Did you mean:
  User.Get
  User.List
```

错误体验非常重要。

因为 Stable Symbol 最大的天然攻击点就是：

> “字符串拼错怎么办？”

FactBind 必须把这个问题做得特别漂亮。

---

## 5. Adapter Conformance Test

所有未来 Adapter 都必须通过同一组测试：

```text
同一个 Contract
同一个 Symbol
同一个输入

↓

应该得到语义等价行为
```

这会成为未来多语言生态的基础。

---

# 十二、类型安全怎么处理

第一版不要把全部时间耗在类型系统。

v0.1：

```ts
factbind.call(
  "User.Get",
  { id: "123" }
)
```

先能正确运行。

v0.2/v0.3 再解决：

输入：

```ts
factbind.call("User.Get", {
  id: 123
});
```

如果 Contract 说：

```text
id:string
```

IDE 应该提前报错。

而：

```ts
factbind.call("Uesr.Get", ...)
```

也应该在编辑阶段提示。

长期目标：

```text
Stable Symbol
仍然是真正的 canonical identity

但：

IDE
Compiler
Language Server
```

帮助用户正确使用它。

不要因为追求 IDE 补全，又重新把：

```text
User.Get
```

生成成唯一一级对象：

```ts
client.users.get()
```

否则会慢慢回到老路。

---

# 十三、版本设计

Contract 不应该：

```text
永远自动加载 latest
```

必须支持固定版本。

例如：

```text
user-api@1.4.2
```

或者内部拥有：

```text
version
digest
```

应用应该能够明确：

> 我绑定的是哪一份 Contract。

否则远程 Contract 被修改后：

```text
程序代码没变
程序行为突然改变
```

就破坏 Hardcode Equivalence。

核心原则：

> **固定 Contract → 固定行为。**

---

# 十四、FactBind 的包结构

第一阶段建议 Monorepo。

Monorepo 就是：

> 一个 GitHub 仓库里面放多个相互关联的包。

例如：

```text
factbind/
│
├── packages/
│   ├── core/
│   ├── openapi/
│   ├── fetch/
│   └── express/
│
├── examples/
│   ├── manual/
│   └── basic-http/
│
├── docs/
│   ├── PRINCIPLES.md
│   ├── ARCHITECTURE.md
│   └── WHY_FACTBIND.md
│
├── benchmarks/
│   └── hardcode-surface/
│
├── README.md
├── CONTRIBUTING.md
└── LICENSE
```

发布包：

```text
@factbind/core
@factbind/openapi
@factbind/fetch
@factbind/express
```

---

# 十五、第一阶段尽量复用别人的底层能力

不要自己写：

```text
YAML parser
OpenAPI parser
$ref resolver
JSON Schema validator
HTTP client
Express router
```

FactBind 的价值不在这些东西。

应该复用成熟库。

你真正需要自己负责的是：

```text
Stable Symbol model
Normalized Contract model
Binding abstraction
Adapter model
Error model
Hardcode/Habit principles
Developer experience
```

OpenAPI 本身已经有成熟规范和生态，没有必要重新实现一遍。当前 TypeSpec 甚至已经能够输出 OpenAPI 3.0、3.1、3.2，因此以后如果用户嫌 YAML 难写，也可以支持 TypeSpec 作为 Contract Authoring Frontend，而 FactBind 继续消费标准 OpenAPI。

---

# 十六、现实开发路线

不要再按照最初设计文档里的“3–5 人日做 MVP”要求自己。原文的工作量估计更接近已经熟悉 TS、HTTP、OpenAPI 的开发者。

对学习型个人开发者，应该按“完成一个阶段，再进入下一阶段”推进，而不是按天数追赶。

## Phase 0：真正理解传统代码

目标：

你能够独立写：

```text
Express 后端
+
fetch 前端
```

不用 FactBind。

完成：

```text
GET /users/{id}
GET /users?page=1
POST /users
```

你必须真正知道：

```text
path
query
header
body
response
status code
```

分别是什么。

这是绝对不能跳过的一关。

---

## Phase 1：最小 Contract Resolver

完成：

```ts
contract.operation("User.Get")
```

能从 OpenAPI 找到：

```text
method
path
parameters
response
```

此时先别发 HTTP。

只解决：

> Symbol → Operation Facts。

---

## Phase 2：Fetch Adapter

完成：

```ts
contract.request(
  "User.Get",
  { id }
)
```

真正返回标准 `Request`。

然后：

```ts
fetch(request)
```

能够工作。

这一阶段第一次证明：

> Contract 可以替代前端协议硬编码。

---

## Phase 3：Express Adapter

让：

```text
User.Get
```

绑定到原来的 Express handler。

先做到可用。

不要立即追求最完美语法。

---

## Phase 4：Hardcode Equivalence Test

建立自动测试证明：

```text
manual implementation
≈
FactBind implementation
```

这是理论第一次真正落地。

---

## Phase 5：第一个公开版本

发布：

```text
v0.1.0
```

只宣布：

> OpenAPI → fetch + Express binding。

不要宣传：

> 下一代软件架构革命。

先让代码说话。

---

# 十七、README 第一屏应该怎么写

不要先写：

```text
FactBind is an invariant symbolic contract binding...
```

没人会继续看。

第一屏：

```text
# FactBind

Facts live once. Bind everywhere.
```

然后直接：

```ts
// Before
fetch(`/api/users/${id}`);
```

```ts
// After
fetch(
  api.request("User.Get", { id })
);
```

后端：

```ts
// Before
app.get("/api/users/:id", handler);
```

FactBind：

```ts
api.bind(app, "User.Get", handler);
```

然后：

```text
Change:

/api/users/{id}
        ↓
/api/v2/members/{id}

With FactBind:

Contract: 1 change
Frontend: 0 changes
Backend: 0 changes
```

30 秒内让开发者明白。

---

# 十八、第一篇技术文章应该写什么

标题不要：

> Introducing a revolutionary API paradigm

而应该非常朴素：

> **Why is the same API path hardcoded three times?**

中文：

> **为什么一个 API 地址，要在项目里写三遍？**

文章结构：

```text
传统代码
↓
重复在哪里
↓
为什么 OpenAPI codegen 仍然存在中间 representation
↓
runtime tools 已经做到了哪些
↓
还缺什么
↓
Stable Symbol
↓
Binding
↓
FactBind
```

这种叙事比声称“别人十几年都错了”更容易获得技术社区尊重。

---

# 十九、最重要的竞品 Benchmark

长期至少持续比较：

```text
Manual fetch + Express
OpenAPI Generator
Orval
Kiota
openapi-client-axios
openapi-backend
FactBind
```

但不要比较：

> 谁功能最多。

而比较：

```text
同一个 API 修改

需要：
修改多少源码？
生成多少东西？
执行多少步骤？
理解多少新概念？
原框架改变多少？
```

尤其应该设计几组真实 Change Cases：

```text
Case A:
path 修改

Case B:
query 参数名称修改

Case C:
query → header

Case D:
新增 optional response field

Case E:
security requirement 修改
```

不要把：

```text
GET → POST
```

作为唯一主打案例。

---

# 二十、什么时候才能开始 Python

不要：

```text
TS 做了一半
↓
觉得不够酷
↓
开 Python
↓
开 Go
↓
全部半成品
```

建议设置 Gate。

只有当：

```text
TS Core 稳定
Fetch Adapter 可用
Express Adapter 可用
至少一个完整 Demo
README 完整
测试体系稳定
```

并且最好已经有：

```text
至少几个你之外的真实用户
```

再进入 Python。

因为：

> 一个别人真的愿意使用的 TS 库，比三个没人敢用的语言 SDK 有价值得多。

---

# 二十一、第二阶段最值得做什么

推荐顺序：

```text
1. TypeScript 类型体验
2. Better error messages
3. Validation
4. Authentication Provider
5. Contract Diff
6. Python
7. IDE support
```

而不是：

```text
Java
Go
Kafka
WebSocket
AI Agent
Registry
```

一起开。

---

# 二十二、Authentication 的正确边界

Contract：

```text
User.Get
需要 BearerAuth
```

这是 Fact。

但：

```text
当前 Token 是多少？
```

不是 Contract Fact。

因此可以：

```ts
factbind.provide(
  "BearerAuth",
  () => currentToken
);
```

形成原则：

> **Facts in Contract. Values from Environment.**

例如：

```text
Contract：
需要 BearerAuth

Environment：
Bearer token = xxx
```

FactBind 负责把两者绑定。

但：

```text
这个用户有没有删除其他用户的权限？
```

是业务逻辑。

FactBind 不管。

---

# 二十三、坚决不要进入这些职责

FactBind Core 不应该负责：

```text
真正的登录系统
用户权限决策
数据库访问
业务 workflow
Mock 策略
测试调度
文档 UI
Dependency Injection
缓存策略
业务重试策略
```

它们都可以消费 Contract。

但不能进入 Binding Core。

否则项目最终会长成另一个巨大 API Framework。

---

# 二十四、理论文档的组织

建议将理论暂称：

> **Invariant Contract Binding（ICB）**

但不要急着把它包装成某种“新学术理论”。

仓库里：

```text
docs/principles/
```

分别写：

```text
01-single-source.md
02-stable-symbol.md
03-hardcode-equivalence.md
04-consumption-transparency.md
05-zero-framework-ownership.md
06-hardcode-surface.md
07-habit-distance.md
08-change-weight.md
```

每篇都包含：

```text
问题
原则
简单例子
反例
设计后果
```

这样未来贡献者可以理解：

> 为什么某个看似方便的新功能会被拒绝。

---

# 二十五、建立 ADR

ADR = Architecture Decision Record。

白话：

> 每次做一个重要架构选择，就写一页解释“为什么”。

例如：

```text
ADR-001
OpenAPI as initial contract format

ADR-002
operationId is mandatory

ADR-003
No framework ownership

ADR-004
Request generation before client replacement

ADR-005
No custom DSL in v1
```

半年以后你自己都会忘记为什么当时这么决定。

ADR 会非常有价值。

---

# 二十六、品牌体系

生态：

> **FactBind**

Slogan：

> **Facts live once. Bind everywhere.**

理论：

> Invariant Contract Binding

初期包：

```text
FactBind Core
FactBind OpenAPI
FactBind Fetch
FactBind Express
```

以后：

```text
FactBind Events
FactBind Tools
FactBind Telemetry
FactBind Hub
FactBind Lens
FactBind Diff
```

但后面这些名字现在只作为 roadmap，不要提前创建十几个空仓库。

正式发布前还需要单独核验 `FactBind` 在 GitHub、npm、PyPI、Maven、域名和商标上的可用性。

---

# 二十七、如果 HTTP 成功，第二个真正值得探索的领域

不是更多 HTTP 功能。

而是：

> **Events / Messaging**

例如传统：

```js
producer.send(
  "order-created-v2",
  event
);
```

消费者：

```js
subscribe(
  "order-created-v2",
  handler
);
```

FactBind：

```text
Order.Created
↓
Contract
↓
Kafka topic:
order-created-v2
```

应用：

```js
emit("Order.Created", event)
```

和：

```js
on("Order.Created", handler)
```

如果这个实验也成功，就第一次证明：

> FactBind 不是 OpenAPI wrapper。

而是一种可迁移的 Binding Model。

---

# 二十八、第三个长期方向：Capability Binding

例如 AI Tool：

```text
CRM.Customer.Get
Mail.Send
Calendar.Create
Order.Search
```

AI、插件、Workflow 不需要知道底层：

```text
HTTP
Python function
第三方 SDK
数据库
```

它只知道：

```text
Stable Capability Symbol
```

然后：

```text
Capability
↓
Contract
↓
Binding
↓
Implementation
```

如果 HTTP、Events、AI Tools 都能用同一套核心原则解释，FactBind 才真正有资格从“一个 API 工具”升级成“Binding Ecosystem”。

---

# 二十九、最危险的十个陷阱

第一，**自己发明 Contract DSL 太早。**

第二，**一开始就做五种语言。**

第三，**为了理论纯洁把 Habit Distance 搞得很高。**

第四，**把 No Codegen 当宗教。**

第五，**为了支持所有 OpenAPI 功能把核心做成巨型框架。**

第六，**Stable Symbol 没有稳定规则，用户随便改 operationId。**

第七，**字符串体验很差，却没有补全和友好报错。**

第八，**只做概念文章，不做真实 Baseline Demo。**

第九，**把其他项目没有采用你的形式解释成“他们都错了”。**

第十，**还没有一个真实用户，就开始设计 FactBind 2.0 世界观。**

---

# 三十、最重要的项目纪律

每加一个新功能，都必须回答四个问题：

```text
1. 它消除了什么 Hardcode Surface？

2. 它增加了多少 Habit Distance？

3. 这个 Hardcode 的 Change Weight 高吗？

4. 这件事情真的应该由 Binding Layer 负责吗？
```

答不清楚：

> 暂时不做。

这四问可以直接加入 PR Template。

---

# 三十一、第一个阶段的“毕业标准”

不要用：

```text
GitHub Stars
```

判断第一阶段是否成功。

v0.1 真正毕业需要满足：

```text
✓ 传统 User.Get Demo 能运行

✓ OpenAPI Contract 能运行

✓ Stable Symbol 能解析

✓ fetch Request Binding 能运行

✓ Express Binding 能运行

✓ URL 改变以后应用代码 0 修改

✓ Hardcode Equivalence 有自动测试

✓ 错误 Symbol 有友好错误

✓ README 能在 60 秒内让人理解

✓ 新用户能按照 Quick Start 自己跑通
```

完成这十条，才开始认真推广。

---

# 三十二、发布之后观察什么

真正有价值的反馈不是：

> “Cool project!”

而是：

```text
“能不能支持 Fastify？”

“为什么不能直接接我的现有 OpenAPI？”

“这里类型提示不够好。”

“我们的参数是 header，这里怎么处理？”

“我们用了这个以后删掉了 14 个 API wrapper。”

“能不能给 Python 做一个？”
```

这些说明用户真的开始把 FactBind 放进项目。

尤其如果有人主动问：

> “Python 什么时候支持？”

比你自己提前写 Python Adapter 有价值得多。

---

# 三十三、开源传播路线

发布顺序建议：

```text
GitHub
↓
npm
↓
一篇完整设计文章
↓
Reddit / Hacker News / Dev.to / 掘金等
↓
收反馈
↓
修 DX
↓
再扩能力
```

不要第一次发布就同时建：

```text
Discord
论坛
官网
YouTube
几十篇博客
```

GitHub Issues 和 Discussions 初期完全够用。

---

# 三十四、许可证

个人基础设施开源项目第一阶段推荐：

```text
MIT
```

或者：

```text
Apache-2.0
```

原设计中也已经考虑这两种。

如果以后企业采用较多，可以重新评估治理和 Contributor License 等问题。

---

# 三十五、你个人应该同步补哪些知识

不要先读完十本书再写代码。

按照项目需要学习。

顺序：

```text
JavaScript / TypeScript 基础

↓

HTTP 基础
GET / POST
path / query / header / body
status code

↓

Express
fetch

↓

OpenAPI

↓

JSON Schema

↓

npm package / monorepo

↓

Testing

↓

TypeScript 类型系统

↓

Compiler / codegen / reflection
```

最后几个现在完全不用急。

每遇到一个问题再补对应知识。

---

# 三十六、最现实的学习开发节奏

第一阶段只要求：

> **先成为能够看懂传统前后端代码的人。**

然后：

> 能够解释传统代码为什么出现重复。

然后：

> 能够手动写出最小 Binding。

然后：

> 才讨论漂亮抽象。

不要反过来：

```text
先设计宏大理论
↓
两个月以后
还不能独立写一个 GET 接口
```

FactBind 最好的学习价值之一就是：

> 让理论强迫你补齐底层知识。

---

# 三十七、未来真正可能形成的生态

如果长期验证成功：

```text
                     FactBind
                        │
                  Contract Core
                        │
                  Stable Symbols
                        │
                    Binding
                        │
       ┌────────────────┼────────────────┐
       ↓                ↓                ↓
     HTTP             Events        Capabilities
       │                │                │
       ↓                ↓                ↓
  Fetch/Spring     Kafka/NATS       AI Tools
  Express/...      RabbitMQ/...     Plugins/...
```

旁边：

```text
Contract → Diff
Contract → Docs
Contract → Test
Contract → Mock
Contract → Registry
Contract → IDE
```

但：

> **这些都是未来可能性，不是当前 backlog。**

---

# 三十八、FactBind 的最终 North Star

North Star 就是：

> 项目永远不应该忘记的最高目标。

FactBind 的 North Star 可以定义成：

> **Move changing boundary facts out of application code, preserve stable identities, and bind facts back with the smallest practical hardcode surface and habit distance.**

中文：

> **把易变化的边界事实从应用代码中抽离，保留稳定身份，并以尽可能小的硬编码表面和开发习惯距离将这些事实绑定回程序。**

而：

> **Facts live once. Bind everywhere.**

负责让别人记住它。

---

# 三十九、你现在立刻应该做的五件事

从今天开始不要继续扩理论。

第一件：

创建：

```text
factbind-lab
```

私人或公开仓库均可。

第二件：

自己从零写：

```text
Express backend
+
HTML/React frontend
+
fetch
```

先真正理解传统 `User.Get`。

第三件：

写：

```text
openapi.yaml
```

把同一个 User API 表达出来。

第四件：

只实现：

```ts
operation("User.Get")
```

能够返回：

```text
GET
/api/users/{id}
```

第五件：

实现：

```ts
request("User.Get", { id })
```

让：

```ts
fetch(request)
```

真正请求成功。

**到这里以前，不开 Python，不做 Registry，不做 Events，不写自己的 DSL。**

---

# 四十、第一座真正的里程碑

第一座里程碑不是：

> “FactBind 发布 v1.0。”

而是你能打开两个终端：

```text
Frontend running
Backend running
```

浏览器里：

```text
User.Get works.
```

然后打开：

```yaml
openapi.yaml
```

把：

```text
/api/users/{id}
```

改成：

```text
/api/v2/members/{id}
```

重新运行。

前端和后端应用代码：

```text
一行没改。
```

页面仍然正常显示用户。

然后你能够准确解释：

> 为什么这不是魔法。

> 为什么这是 Hardcode Equivalence。

> 哪些 Hardcode 被拿走了。

> 哪些业务代码仍然必须存在。

> 为什么 Stable Symbol 没变。

> 为什么开发者不需要关心底层是 runtime 还是 compile-time。

> 为什么它没有替代 Express 和 fetch。

**这个时刻，FactBind 才真正从“一个很有想象力的思想”，第一次变成了一个软件项目。**
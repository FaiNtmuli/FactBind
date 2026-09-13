# FactBind 设计讨论工作记录

**日期：2026年9月10日**

## 一、FactBind 核心问题重新界定

本次讨论首先进一步明确了 FactBind 真正需要解决的问题。

接口中的 `method`、`path`、参数位置、字段名称等事实不可能真正消失。无论写在 Spring Annotation、YAML Contract 还是其他描述文件中，本质上都属于显式定义。

因此，FactBind 要解决的并不是“消灭硬编码”，而是：

> **避免同一接口事实被前端、后端、SDK、测试、文档等不同位置独立重复声明。**

例如传统项目中可能同时存在：

```text
后端：
@GetMapping("/users/{id}")

前端：
fetch(`/users/${id}`)

测试：
GET /users/123

文档：
GET /users/{id}
```

真正危险的是这些事实独立维护后产生漂移，而不是 `/users/{id}` 本身被显式写出来。

因此，本次讨论形成了一条较为明确的原则：

> **Transport Fact 可以显式定义，但原则上只能存在一个权威来源，其他使用方应从该来源获得。**

FactBind 的长期目标因此进一步明确为：

```text
FactBind Contract
      ↓
统一保存接口事实
      ↓
不同框架和客户端消费
```

---

## 二、习惯距离与 Single Source of Truth 的权衡

在后端接入方式上，重点讨论了两个目标之间的矛盾：

1. 尽量减少开发者学习新写法的成本；
2. 尽量让 Contract 真正成为接口事实源。

传统 Spring Boot：

```java
@GetMapping("/users/{id}")
public User getUser(
    @PathVariable Long id
) {
    ...
}
```

这种写法的优势是习惯距离极低，但接口事实仍然保存在 Spring 代码中。

如果额外增加：

```java
@Fact("User.Get")
@GetMapping("/users/{id}")
```

虽然可以增加 Stable Symbol（稳定业务标识），但整体会形成一个尴尬的中间状态。

### 方案比较

| 方案 | 开发习惯距离 | Contract 控制力 | 重复程度 | 当前判断 |
|---|---:|---:|---:|---|
| 完全沿用 Spring Annotation | 最小 | 低 | 低 | 可兼容，但不是最终方向 |
| `@Fact + Spring Annotation` | 较小 | 中 | 较高 | 不推荐作为核心形态 |
| Contract 完全描述接口 | 中 | 高 | 最低 | 更符合 FactBind 初衷 |
| 自创全新 Controller DSL | 大 | 高 | 低 | 不推荐 |

本次讨论中逐渐形成的判断是：

> **不应为了最小习惯距离而长期保留两套接口描述。**

如果 FactBind 只是给 Spring Controller 再增加一个 Stable Symbol，而 transport facts 仍然全部保留在 Spring Annotation 中，那么项目价值会被明显削弱。

---

## 三、代码生成方案讨论

为了让更多接口事实转移到 Contract，一度讨论过 Contract-first 的代码生成方案。

基本思路为：

```text
FactBind Contract
       ↓
生成 Spring Interface / Controller Metadata
       ↓
开发者实现 Java Interface
```

例如 Contract 保存：

```yaml
User.Get:
  method: GET
  path: /users/{id}
```

然后生成带 Spring Annotation 的 Java Interface。

该方案能够让 Contract 成为接口事实源，同时保留较为熟悉的 Java 开发形式。

但最终明确不采用代码生成作为 FactBind 的主要路线。

### 主要问题

| 问题 | 影响 |
|---|---|
| Generated Source | 增加构建与 IDE 复杂度 |
| 调试体验 | 用户代码与生成代码之间需要跳转 |
| Contract 更新 | 生成文件频繁变化 |
| 版本管理 | 需要决定生成文件是否进入仓库 |
| 多框架扩展 | 需要维护多套 generator |
| 项目定位 | 容易演变为另一套 OpenAPI Generator |

因此形成明确约束：

> **FactBind 核心实现不依赖源码生成。**

---

## 四、硬编码等价原则

在继续讨论 Java 后端实现时，本次形成了一个更重要的原则：

> **FactBind 替换的是硬编码事实的来源，而不是框架本身已经成熟的行为。**

例如传统 Spring：

```java
@GetMapping("/users/{id}")
public User getUser(
    @PathVariable("id") Long id
)
```

FactBind 希望改为：

```java
@FactHandler("User.Get")
public User getUser(Long id)
```

接口事实转移到 Contract：

```yaml
User.Get:
  method: GET
  path: /users/{id}

  input:
    id:
      in: path
```

但替换之后，Spring 原有的行为应尽可能保持一致。

### 等价范围

| 行为 | 目标 |
|---|---|
| Route Matching | 保持 Spring 原有行为 |
| Path Variable 解析 | 保持 |
| Query Parameter 解析 | 保持 |
| 类型转换 | 保持 |
| JSON Body 解析 | 保持 |
| Validation | 保持 |
| Response 处理 | 保持 |
| Exception Handling | 保持 |
| Interceptor | 保持 |
| Spring Security | 保持 |
| AOP | 保持 |

因此 FactBind 不应该重新实现一套：

```text
Router
Parameter Parser
JSON Binder
Validator
Dispatcher
Response Serializer
```

否则虽然最终接口可能“能跑”，但行为已经不再与原来的 Spring 写法等价。

本次由此形成了一个较重要的设计原则：

> **Replace metadata, not behavior.**

即：

> **只替换接口元数据的来源，不替换宿主框架原有行为。**

---

## 五、Java/Spring 中间层设计方向

基于硬编码等价原则，本次进一步收敛了 Java Adapter 的设计方向。

FactBind 在 Java 中不应该成为位于 HTTP 和 Spring 之间的新 Web Runtime，而更适合作为：

> **Contract 到 Spring 原生接口元数据之间的转换层。**

整体关系为：

```text
FactBind Contract
       ↓
FactBind Spring Adapter
       ↓
Spring 原生 Metadata
       ↓
Spring MVC
       ↓
业务 Handler
```

其中：

```text
GET
/users/{id}
```

等 Route 信息转换为 Spring 原生 Mapping 信息。

参数：

```text
id → path
keyword → query
request → body
```

则转换为 Spring 原本能够识别的参数绑定语义。

之后的参数获取、类型转换、JSON 解析、Validation 和 Handler Invocation 继续由 Spring 完成。

### Java Adapter 自己负责的内容

| FactBind 负责 | Spring 继续负责 |
|---|---|
| Contract 读取 | HTTP Request 生命周期 |
| Stable Symbol | Route Matching |
| Symbol 与 Handler 对应 | Path / Query 实际取值 |
| Contract → Spring Mapping | 类型转换 |
| Contract → Spring 参数语义 | Jackson |
| Contract 一致性检查 | Validation |
| 启动阶段错误检测 | Exception Handling |
|  | Security / AOP |

因此 Java Adapter 的工程目标可以概括为：

> **尽可能正常调用 Spring 原有代码，只解决如何把 Contract 中的正确参数信息交给 Spring。**

本次对这一部分尚未进一步锁定具体实现方式，后续重点需要验证 Spring 原生参数解析机制接受外部元数据的最小接入方式。

---

## 六、FactBind 项目命名讨论

讨论过是否将 FactBind 改名为类似 `SpringBind`，以降低 Spring Boot 开发者的理解门槛。

最终不建议修改核心项目名称。

### 名称比较

| 维度 | FactBind | SpringBind |
|---|---:|---:|
| Spring 用户直观性 | 6 | 9 |
| 跨框架能力 | 10 | 3 |
| 与项目核心抽象匹配 | 9 | 5 |
| 长期扩展空间 | 9 | 4 |
| 语义独立性 | 8 | 4 |

`SpringBind` 的主要问题在于，它会让项目天然被理解为 Spring 专用组件。

而当前设计中的核心关系实际上是：

```text
Stable Fact
   ↓
Transport Binding
   ↓
Framework Handler
```

Spring 只是其中一个 Framework Adapter。

因此推荐保持：

```text
项目：
FactBind

Java / Spring Adapter：
factbind-spring

Spring Boot Starter：
factbind-spring-boot-starter
```

产品表达上可以使用：

```text
FactBind for Spring Boot
```

从而兼顾项目抽象性和 Spring 用户的识别成本。

---

## 七、MVP 后端技术栈选型评分

讨论了 FactBind 第一阶段应优先实现哪些语言和框架。

评分时重点考虑：

1. 实现复杂度；
2. 后端生态规模；
3. 与常见 Web 前端技术栈的组合情况；
4. FactBind 在该框架中的实际痛点强度；
5. 是否适合验证 FactBind 的跨框架抽象。

### 评分

10 分表示更有优势。

| 技术栈 | 实现容易度 | 生态覆盖 | 前端搭配广度 | FB 痛点强度 | 适合验证核心设计 | 综合判断 |
|---|---:|---:|---:|---:|---:|---:|
| **Python + FastAPI** | 9.0 | 8.0 | 8.5 | 6.5 | 9.5 | **高** |
| **Java + Spring Boot** | 4.5 | 8.5 | 9.0 | 10.0 | 9.0 | **高** |
| C# + ASP.NET Core | 8.0 | 9.0 | 8.5 | 8.0 | 8.0 | 较高 |
| TypeScript + Node.js | 9.0 | 10.0 | 10.0 | 4.5 | 6.0 | 中 |
| Go Web Framework | 8.0 | 6.0 | 8.0 | 7.0 | 7.0 | 中 |

这里形成的主要判断并不是单纯选择“综合分最高”的框架。

FastAPI 的优势主要在于：

```text
实现简单
反射自然
参数类型信息丰富
适合快速验证 Contract → Handler 的基本机制
```

Spring Boot 的优势则主要在于：

```text
实际企业项目占比高
接口 Annotation 较重
前后端跨语言重复更加明显
FactBind 的价值更容易体现
```

因此两者分别代表两种不同价值：

| 框架 | 更适合验证 |
|---|---|
| FastAPI | 技术机制是否成立 |
| Spring Boot | 产品价值是否成立 |

---

## 八、MVP 第一阶段范围

本次讨论总体上不建议一开始覆盖过多语言。

FactBind Core 尚未稳定前，如果同时适配多个框架，很容易把大量时间消耗在 Framework Adapter 的边缘问题上。

第一阶段应重点验证：

```text
Contract
   ↓
Stable Symbol
   ↓
Route
   ↓
Parameter Binding
   ↓
普通业务 Handler
```

基础支持范围可以控制在：

```text
GET
POST

Path Parameter
Query Parameter
JSON Body

简单 Response
基础 Schema
Stable Symbol
```

暂不优先覆盖：

```text
Multipart
File Upload
Streaming
WebSocket
复杂 Authentication
复杂泛型
多 Content-Type
高度框架特定功能
```

MVP 的核心目标不是接口覆盖率，而是证明：

> **同一份 Contract 是否能够成为接口事实源，同时让不同后端框架继续以接近原生的方式运行。**

---

## 九、当前阶段形成的设计结论

截至本次讨论，可以将 FactBind 当前方向概括如下。

| 问题 | 当前结论 |
|---|---|
| 是否消灭硬编码 | 否，重点是消灭独立重复声明 |
| 接口事实主要放在哪里 | FactBind Contract |
| 是否保留 Spring Transport Annotation | 核心接口事实尽量不保留 |
| 是否采用 `@Fact + @GetMapping` 中间态 | 不推荐 |
| 是否生成 Java Controller / Interface | 不采用 |
| Java 是否自己实现 Web Runtime | 不采用 |
| Java Adapter 核心目标 | Contract → Spring 原生 Metadata |
| 框架已有能力是否重新实现 | 原则上不实现 |
| 设计约束 | 硬编码等价 |
| 核心原则 | Replace metadata, not behavior |
| Java 业务方法连接方式 | Stable Symbol 与 Handler 建立明确绑定 |
| 项目名称 | FactBind |
| Spring 模块名称 | `factbind-spring` |
| 第一阶段重点 | 验证 Contract 驱动接口事实能否保持框架原生行为 |

当前最值得继续验证的问题已经比较集中：

> **在 Java/Spring 中，如何以最小改造把 FactBind Contract 中的接口参数和路由事实交给 Spring 原有代码，使 Spring 像读取普通 Annotation 一样继续完成后续处理。**

如果这一点能够成立，FactBind 在 Java 侧就不需要构建复杂的新中间层，而可以保持为一个非常薄的 Contract Adapter。
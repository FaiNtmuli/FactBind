# MidDemo Management System

一个采用 **React + TypeScript + Vite** 前端与 **Java 21 + Spring Boot + Spring MVC + Spring
Data JPA + PostgreSQL** 后端的现代前后端分离订单管理系统。

它实现了一个接近中型项目结构的 Demo：

```text
React Page  ->  Frontend API  ->  HTTP  ->  Spring Controller
                                              |
                                           Service
                                              |
                                          Repository
                                              |
                                       Entity  ->  PostgreSQL
```

> **本项目是 Native Spring Baseline，不包含任何 FactBind 代码。**
> 
> 它使用最普通、最标准的 Spring MVC 注解（`@GetMapping`、`@PathVariable`、`@RequestParam`、
> `@RequestBody`、`@Valid`）实现全部接口，用来作为未来 FactBind 的对照基线。

---

## 1. 技术栈

| 层    | 技术                                                                                            |
| ---- | --------------------------------------------------------------------------------------------- |
| 前端   | React 19、TypeScript 5.9、Vite 7、React Router 7、原生 Fetch API、普通 CSS                             |
| 前端测试 | Vitest 3、React Testing Library、jsdom                                                          |
| 后端   | Java 21、Spring Boot 3.5、Spring MVC、Spring Data JPA、Hibernate、Jakarta Validation、Jackson、Maven |
| 后端测试 | JUnit 5、MockMvc、AssertJ、Mockito、H2（测试数据库）                                                     |
| 数据库  | PostgreSQL（默认），H2（本地免安装 profile 与自动化测试）                                                       |

明确**没有**使用：Redux、任何 UI 组件库、Spring Security/JWT、Redis、消息队列、微服务、
容器编排、缓存框架、Mapper 框架、通用 BaseController/BaseService、DDD/CQRS 等重型抽象。

---

## 2. 目录结构

```text
.
├── README.md
├── docker-compose.yml            # 可选：一键启动 PostgreSQL
│
├── backend/                      # Spring Boot 后端
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd           # Maven Wrapper（无需本机安装 Maven）
│   └── src/
│       ├── main/java/com/example/middemo/
│       │   ├── MidDemoApplication.java
│       │   ├── controller/       # UserController / ProductController / OrderController / DashboardController
│       │   ├── service/          # 业务规则、事务边界、Entity <-> DTO 转换
│       │   ├── repository/       # Spring Data JPA Repository + Specification 过滤条件
│       │   ├── entity/           # User / Product / Order / OrderItem + 枚举
│       │   ├── dto/              # user / product / order / dashboard / common(PageResponse)
│       │   ├── exception/        # BusinessException 体系 + GlobalExceptionHandler
│       │   └── config/           # JPA Auditing、CORS、演示数据初始化
│       ├── main/resources/
│       │   ├── application.yml       # 默认 profile：PostgreSQL
│       │   ├── application-h2.yml    # 免安装数据库 profile
│       │   └── application-test.yml  # 自动化测试 profile
│       └── test/java/com/example/middemo/
│           ├── controller/       # @WebMvcTest：参数绑定、校验、HTTP 状态码
│           ├── service/          # @SpringBootTest + H2：真实业务规则
│           └── api/              # @SpringBootTest + MockMvc：端到端 HTTP 流程
│
└── frontend/                     # React + TypeScript 前端
    ├── package.json
    ├── vite.config.ts            # 开发代理 /api -> localhost:8080，Vitest 配置
    └── src/
        ├── api/                  # http.ts + userApi / productApi / orderApi / dashboardApi
        ├── types/                # 与后端 DTO 对应的 TypeScript 类型
        ├── components/           # Layout / Pagination / Loading / ErrorMessage / StatusBadge / ConfirmDialog
        ├── pages/                # dashboard、users、products、orders 共 10 个页面
        ├── router/               # React Router 路由表
        ├── utils/                # 日期与金额格式化
        └── test/                 # 测试 setup 与 fetch mock 工具
```

---

## 3. 环境要求

* JDK 21（后端编译与运行）
* Node.js 20+ 与 npm（前端）
* PostgreSQL 14+（可选，见下一节）

不需要本机安装 Maven，仓库已包含 Maven Wrapper。

---

## 4. 数据库配置

### 方案 A：使用 PostgreSQL（默认，推荐）

1. 创建数据库与账号：

```sql
CREATE USER middemo WITH PASSWORD 'middemo';
CREATE DATABASE middemo OWNER middemo;
```

2. 或者直接用 Docker：

```bash
docker compose up -d
```

默认连接信息在 `backend/src/main/resources/application.yml`：

```text
url      jdbc:postgresql://localhost:5432/middemo
username middemo
password middemo
```

可以用环境变量覆盖，不需要改动代码：

```text
MIDDEMO_DB_URL / MIDDEMO_DB_USERNAME / MIDDEMO_DB_PASSWORD / MIDDEMO_PORT
```

表结构由 `spring.jpa.hibernate.ddl-auto=update` 自动创建。

### 方案 B：完全不安装数据库（H2 profile）

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2     # Windows: mvnw.cmd ...
```

> **Windows / PowerShell 注意**：`-D` 参数必须加引号，否则 Maven 会把它拆开并报
> `Unknown lifecycle phase ".run.profiles=h2"`：
>
> ```powershell
> ./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"
> ```

数据保存在 `backend/data/`，第一次启动会自动建表并写入演示数据。
这个 profile 只是为了让新环境“零配置也能跑起来”，长期基线仍然以 PostgreSQL 为准。

### 演示数据

默认 profile 下，如果三张表都是空的，应用启动时会写入一份**确定性**演示数据：

```text
20 个 User（每 5 个有 1 个 DISABLED）
25 个 Product（每 6 个有 1 个 OFF_SALE）
40 个 Order（固定随机种子 20240101）
```

写入逻辑在 `config/DemoDataSeeder.java`，可以通过 `middemo.seed.enabled=false` 关闭。

---

## 5. 启动后端

```bash
cd backend

# 使用默认 profile（PostgreSQL）
./mvnw spring-boot:run

# 或者使用免安装数据库
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2

# 或者先打包再运行
./mvnw -DskipTests package
java -jar target/middemo-backend-0.0.1-SNAPSHOT.jar
```

> **Windows / PowerShell 注意**：上面这些 `-D` 参数都要加引号，例如
> `./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"`。
> 不加引号时 PowerShell 会把参数拆开，Maven 报 `Unknown lifecycle phase ".run.profiles=h2"`（实测过）。

后端监听 `http://localhost:8080`。启动后可以直接验证：

```bash
curl http://localhost:8080/api/dashboard/summary
curl "http://localhost:8080/api/users?keyword=alice&status=ACTIVE&page=0&size=10"
```

---

## 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 <http://localhost:5173>。Vite 开发服务器会把 `/api` 代理到 `http://localhost:8080`，
因此开发时不需要处理跨域。

```bash
npm run build     # 类型检查 + 生产构建，输出到 frontend/dist
npm run preview   # 预览构建产物
npm run lint      # ESLint
```

如果想直接访问远端后端，可以设置环境变量：

```text
VITE_API_BASE_URL=http://localhost:8080
```

---

## 7. 运行测试

后端：

```bash
cd backend
./mvnw test
```

测试使用 H2 内存数据库（`application-test.yml`），不会影响开发库。
覆盖内容：

* Controller 层：`@WebMvcTest` 验证 Path / Query / Body 绑定、枚举转换、参数缺失、
  非法类型、Validation、业务异常、HTTP 状态码与响应体（User 15、Product 12、
  Order 12、Dashboard 4 个用例）。
* Service 层：`@SpringBootTest` + H2 验证真实业务规则（订单金额计算、扣库存、
  事务回滚、库存不足、DISABLED 用户、OFF_SALE 商品、非法状态跳转、删除保护等）。
* API 层：`@SpringBootTest` + `MockMvc` 走完整 HTTP 链路（创建订单 → 查询订单 →
  修改状态 → 库存不足冲突）。

前端：

```bash
cd frontend
npm test
```

覆盖：HTTP 客户端错误转换、Dashboard 汇总渲染、列表 Loading/Error/空状态、
搜索参数拼装、表单客户端校验、后端字段错误展示、创建成功后跳转。

---

## 8. 主要页面

| 页面             | 路由                                   | 功能                    |
| -------------- | ------------------------------------ | --------------------- |
| Dashboard      | `/`                                  | 汇总统计 + 最近订单           |
| User List      | `/users`                             | 搜索、状态筛选、分页、启停、删除      |
| User Detail    | `/users/:id`                         | 详情、启用/禁用、删除           |
| User Form      | `/users/new`、`/users/:id/edit`       | 创建/编辑，前后端双重校验         |
| Product List   | `/products`                          | 搜索、上下架、删除             |
| Product Detail | `/products/:id`                      | 详情、修改库存、上下架           |
| Product Form   | `/products/new`、`/products/:id/edit` | 创建/编辑                 |
| Order List     | `/orders`                            | 按用户/状态筛选、分页           |
| Order Detail   | `/orders/:id`                        | 订单明细、状态流转、`notify` 参数 |
| Create Order   | `/orders/new`                        | 选择用户与商品、动态添加明细        |

---

## 9. 主要 API

共 20 个接口，全部以 `/api` 开头。

| 模块        | 方法     | 路径                                     | 说明                                  |
| --------- | ------ | -------------------------------------- | ----------------------------------- |
| User      | GET    | `/api/users`                           | 列表：`keyword`、`status`、`page`、`size` |
| User      | GET    | `/api/users/{id}`                      | 详情                                  |
| User      | POST   | `/api/users`                           | 创建，201 + `Location`                 |
| User      | PUT    | `/api/users/{id}`                      | 更新                                  |
| User      | PATCH  | `/api/users/{id}/status`               | 修改状态                                |
| User      | DELETE | `/api/users/{id}`                      | 删除，204                              |
| Product   | GET    | `/api/products`                        | 列表：`keyword`、`status`、`page`、`size` |
| Product   | GET    | `/api/products/{id}`                   | 详情                                  |
| Product   | POST   | `/api/products`                        | 创建                                  |
| Product   | PUT    | `/api/products/{id}`                   | 更新                                  |
| Product   | PATCH  | `/api/products/{id}/stock`             | 修改库存                                |
| Product   | PATCH  | `/api/products/{id}/status`            | 上下架                                 |
| Product   | DELETE | `/api/products/{id}`                   | 删除                                  |
| Order     | GET    | `/api/orders`                          | 列表：`userId`、`status`、`page`、`size`  |
| Order     | GET    | `/api/orders/{id}`                     | 详情（含用户信息与明细）                        |
| Order     | POST   | `/api/orders`                          | 创建订单（服务端计算金额、扣库存）                   |
| Order     | PATCH  | `/api/orders/{id}/status?notify=true`  | Path + Query + Body                 |
| Order     | DELETE | `/api/orders/{id}`                     | 删除                                  |
| Dashboard | GET    | `/api/dashboard/summary`               | 汇总统计                                |
| Dashboard | GET    | `/api/dashboard/recent-orders?limit=5` | 最近订单                                |

### HTTP Binding 覆盖（FactBind 对照用）

| 场景                        | 例子                                                        |
| ------------------------- | --------------------------------------------------------- |
| 纯 Path                    | `GET /api/users/{id}`                                     |
| 多 Query + 可选参数 + 枚举 + 分页  | `GET /api/users?keyword=tom&status=ACTIVE&page=0&size=20` |
| Body                      | `POST /api/users`                                         |
| Path + Body               | `PUT /api/users/{id}`                                     |
| Path + Query + Body + 默认值 | `PATCH /api/orders/{id}/status?notify=true`               |
| 嵌套 DTO + 列表 Body          | `POST /api/orders`                                        |
| 空响应体                      | `DELETE /api/users/{id}` → 204                            |
| 状态码                       | 200 / 201 / 204 / 400 / 404 / 409 / 500                   |

### 请求示例：创建订单

```http
POST /api/orders
Content-Type: application/json
```

```json
{
  "userId": 1,
  "remark": "Please deliver soon",
  "items": [
    { "productId": 10, "quantity": 2 },
    { "productId": 20, "quantity": 1 }
  ]
}
```

客户端**不能**指定 `totalAmount`、`unitPrice`、`subtotal`，它们全部由后端计算。
创建订单与扣减库存在同一个事务中，任何一项失败都会整体回滚。

### 请求示例：修改订单状态

```http
PATCH /api/orders/5/status?notify=true
Content-Type: application/json
```

```json
{ "status": "PAID" }
```

允许的状态流转：

```text
CREATED -> PAID
CREATED -> CANCELLED
PAID    -> COMPLETED
```

其他跳转（例如 `CANCELLED -> PAID`）返回 `409 INVALID_ORDER_STATUS`。
`notify=true` 目前只在 Service 中写一条 `notification requested` 日志。

---

## 10. 错误格式与 HTTP 状态码

所有失败响应使用同一结构：

```json
{ "code": "USER_NOT_FOUND", "message": "User 100 not found" }
```

字段校验失败时额外带上 `fields`：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fields": { "email": "must be a well-formed email address" }
}
```

已实现的错误码：`VALIDATION_ERROR`、`INVALID_PARAMETER`、`MISSING_PARAMETER`、
`MALFORMED_REQUEST_BODY`、`USER_NOT_FOUND`、`PRODUCT_NOT_FOUND`、`ORDER_NOT_FOUND`、
`DUPLICATE_EMAIL`、`DUPLICATE_SKU`、`USER_NOT_ACTIVE`、`PRODUCT_NOT_ON_SALE`、
`INSUFFICIENT_STOCK`、`INVALID_ORDER_STATUS`、`USER_HAS_ORDERS`、`PRODUCT_IN_USE`、
`DATA_INTEGRITY_VIOLATION`、`RESOURCE_NOT_FOUND`、`INTERNAL_ERROR`。

状态码约定：

```text
200 查询/更新成功        201 创建成功（带 Location）
204 删除成功（无响应体）  400 参数或请求体不合法
404 数据不存在           409 业务冲突
500 未预期的服务器异常
```

---

## 11. 已知限制（刻意保留的简单之处）

* 取消订单不会把库存加回去；删除订单同样不会回滚库存。
* `notify=true` 不发送真实通知，只写日志。
* 没有登录、权限、限流、缓存、消息队列、容器编排与生产监控。
* 前端 TypeScript 类型与后端 DTO 是两份手写定义（这是普通项目的现实，本项目刻意不提前解决）。
* 数据库表结构由 Hibernate 自动维护，没有引入 Flyway/Liquibase 之类的迁移工具。

这些都属于“写起来像中型项目、运行起来仍然像一个可控 Demo”的取舍。

---

## 12. 与 FactBind 的关系

本仓库是 **Native Spring Baseline**：

```text
                同一个 Service / Repository / Entity / DTO
                              |
              +---------------+---------------+
              |                               |
        Native Spring                    (future)
        Controller                       FactBind
              |                          Handler
              +---------------+---------------+
                              |
                       相同业务结果
```

后续引入 FactBind 时，应当复用同一套 Service、Repository、Entity、DTO、数据库与业务逻辑，
只替换 Controller / API Binding 这一层，从而比较两种绑定方式是否满足硬编码等价。

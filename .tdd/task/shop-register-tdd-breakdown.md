---
title: "`TDD 任务拆解：店铺（用户）用户名密码注册`"
description: "基于 shop-service /shop/register 的 TDD 四层拆解：功能点 → 功能上下文 → 任务项 → 测试思路"
author: "TDD 教练"
created: "2026-10-13"
updated: "2026-10-13"
version: "1.0.0"
status: "draft"
tags: ["tdd", "task-breakdown", "register", "shop", "spring-cloud"]
category: "spec"
audience: ["backend-team", "qa"]
---

# TDD 任务拆解：店铺（用户）用户名密码注册

> 本文按 TDD 工作流把「用户使用用户名密码注册」拆解为**可直接指导写测试**的任务项。
> 拆解链路严格为：需求 → 功能点 → 功能上下文 → 任务项 → 测试思路。
> 执行原则：先让任务项对应的测试变红（Fail），再写最小实现使其变绿（Pass），最后重构。

## 目录

- [1. 需求概述](#1-需求概述)
- [2. 现有架构分析](#2-现有架构分析)
- [3. 功能点总览](#3-功能点总览)
- [4. 功能点拆解](#4-功能点拆解)
- [5. 架构与重构思考](#5-架构与重构思考)
- [附录 A：测试基础设施任务项](#附录-a测试基础设施任务项)
- [附录 B：任务项到测试类映射](#附录-b任务项到测试类映射)
- [附录 C：现状差异与红灯清单](#附录-c现状差异与红灯清单)

---

## 1. 需求概述

在 B2C 电商系统的商家侧，用户使用「店铺名 + 管理员账号 + 管理员密码」提交注册：

1. 注册成功后店铺数据落库，返回成功响应；
2. 注册成功后系统自动为该店铺创建管理员员工账号（跨服务）；
3. 注册成功后系统自动下发一条欢迎站内信；
4. 店铺名重复、入参不合法、下游异常时，用户能得到明确一致的反馈。

### 1.1 需求澄清结论（交互式问答记录）

| 问题 | 结论 |
| --- | --- |
| 注册主体是哪一个 | 店铺/商家注册，复用 shop-service 的 `POST /shop/register` |
| View 层如何界定 | 仓库内无前端工程，以 **HTTP 接口契约 + 网关路由** 作为 View 层 |
| 异步链路覆盖到什么程度 | **完整覆盖**（Guava EventBus + Feign），含失败场景与补偿 |
| 测试技术栈 | JUnit 5 + Mockito + MyBatis/SQLite 内存库 + MockMvc，允许补充测试依赖 |

### 1.2 范围边界

**本次范围内**：shop-service 注册主流程、入参校验、店铺名唯一性、注册衍生事件（员工账号、欢迎消息）、
employee-service 被调用侧契约、网关路由可达性、注册链路失败的可观测性。

**本次范围外**：C 端会员注册、前端页面、登录与鉴权、Nacos 真实集群部署、真实支付与订单链路。

---

## 2. 现有架构分析

### 2.1 服务与职责清单

| 模块 | 类型 | 关键职责（与注册相关） |
| --- | --- | --- |
| `gateway` | 接入层 | Spring Cloud Gateway，路由 `/shop/**` → `lb://shop-service` |
| `shop` | 业务服务 | 店铺注册/登录，持有 `shop.db`，注册后发布 `ShopRegisterEvent` |
| `employee` | 业务服务 | 员工账号保存/登录，持有 `employee.db` |
| `common` | 公共模块 | `BaseResponseVO`、`ClientInfo`、`ClientInfoUtil` |
| `product` / `order` | 业务服务 | 与本次注册需求无直接关系 |

### 2.2 单个服务内的分层（MVC 视角）

| 分层 | 注册链路中的组件 |
| --- | --- |
| 接入层（View 的前置） | `LogginFilter`（`GlobalFilter`，仅打日志）、`application.yml` 路由 |
| View 层（HTTP 契约） | `ShopRegisterRequestVO` + Bean Validation、`BaseResponseVO` 响应结构 |
| Controller 层 | `ShopController#register` |
| Service 层 | `ShopService` / `ShopServiceImpl#register` |
| Model/DAO 层 | `ShopPO`、`ShopMapper` + `ShopMapper.xml`、表 `tb_shop`（SQLite） |
| 事件/集成层 | `EventBus`（`AsyncEventBus`）、`ShopRegisterEvent`、两个 `@Subscribe` 处理器 |
| 远程调用层 | `EmployeeFeignClient` → employee-service `/employee/save` |
| 横切层 | `MethodArgNotValidExceptionAspect`、`GlobalExceptionHandler`（employee 侧） |

### 2.3 注册链路的组件协作

``mermaid
flowchart TD
    A["POST /shop/register"] --> B["gateway: LogginFilter + 路由 /shop/**"]
    B --> C["shop: ShopController.register"]
    C --> D["ShopServiceImpl.register"]
    D --> E["ShopMapper.exist 校验店铺名"]
    E --> F["ShopMapper.registerShop 写入 tb_shop"]
    D --> G["po.id 返回给 Controller"]
    G --> H["eventBus.post(ShopRegisterEvent)"]
    H --> I["InitEmployeeEventHandle"]
    H --> J["InitShopMessageEventHandle"]
    I --> K["EmployeeFeignClient.save"]
    K --> L["employee: /employee/save 写入 tb_employee"]
    J --> M["MessageServiceImpl 写入 tb_messages"]
    C --> N["BaseResponseVO 返回 status=200"]
``

``mermaid
sequenceDiagram
    participant U as 用户
    participant G as gateway
    participant C as ShopController
    participant S as ShopServiceImpl
    participant DB as tb_shop
    participant EB as AsyncEventBus
    participant H1 as InitEmployeeEventHandle
    participant H2 as InitShopMessageEventHandle
    participant EM as employee-service
    U->>G: POST /shop/register
    G->>C: 转发请求
    C->>S: register(vo)
    S->>DB: exist(shopName)
    S->>DB: registerShop(po) 回填 id
    S-->>C: shopId
    C->>EB: post(ShopRegisterEvent)
    Note over C,EB: 事件在 Controller 层发布，异步执行
    par 异步分支 1
        EB->>H1: ShopRegisterEvent
        H1->>EM: POST /employee/save
        EM-->>H1: BaseResponseVO
    and 异步分支 2
        EB->>H2: ShopRegisterEvent
        H2->>H2: 写欢迎站内信
    end
    C-->>U: BaseResponseVO status=200 data=null
``

### 2.4 影响拆解的关键现状事实

| 编号 | 现状 | 对测试拆解的影响 |
| --- | --- | --- |
| P1 | 密码明文存储（shop 与 employee 两处） | 需一条「明文透传」断言锁住当前语义，作为加密改造的锚点 |
| P2 | 业务错误用 hutool `Assert` 抛 `IllegalArgumentException` | 异常类型本身就是可断言的行为契约 |
| P3 | 所有响应 HTTP 状态码恒为 200，业务码在 body | 断言必须查 body 的 `status`，不能查 HTTP 4xx |
| P4 | `register` 返回空 `BaseResponseVO`，不携带 shopId | 是需求增强点，需红灯用例驱动 |
| P5 | 无 `@Transactional`，无 `shop_name` 唯一索引 | 并发重复注册是必须显式覆盖的失败边界 |
| P6 | 事件在 Controller 层发布，Service 不感知 | 「先落库后发布」与「失败不发布」需在 Controller 层断言 |
| P7 | `AsyncEventBus` 线程池异步，订阅者异常只 `log.error` | 失败场景断言需要同步事件替身与异常处理器 |
| P8 | 跨服务调用是同步 Feign，无重试无补偿 | 补偿与幂等只能先写成红灯用例 |
| P9 | 项目无任何测试代码与测试依赖 | 必须先完成任务拆解附录 A 的测试基座 |

---

## 3. 功能点总览

| 功能点 | 用户可感知行为 | 主要功能上下文 | 优先级 |
| --- | --- | --- | --- |
| F1 注册成功主流程 | 提交合法信息即注册成功并落库 | View / Controller / Service / Model | P0 |
| F2 非法入参被拒绝 | 缺字段、超长、格式非法时收到明确提示 | View / Controller 异常转换 / Service | P0 |
| F3 店铺名重复被拒绝 | 用已存在的店铺名注册时被拒绝 | Service / Model / 数据层约束 | P0 |
| F4 自动创建管理员员工账号 | 注册后可用管理员账号登录员工端 | Service 事件契约 / 订阅者 / Feign / employee 服务 | P0 |
| F5 自动下发欢迎站内信 | 注册后在站内信中看到欢迎消息 | 订阅者 / Service / Model | P1 |
| F6 链路失败可观测与一致性 | 下游故障时不出现半成品数据且可追踪 | 事务边界 / 异步异常 / 补偿 | P1 |
| F7 网关统一入口可达 | 通过网关地址即可完成注册 | 网关路由 / 网关过滤器 | P1 |

---

## 4. 功能点拆解

### 功能点 1：用户提交合法的店铺信息即可完成注册

**用户可感知行为**：`POST /shop/register` 携带合法的店铺名、管理员账号与密码，返回业务码 200。

#### 功能上下文 1.1：View 层 - HTTP 接口契约

- **任务项 1.1.1**：`POST /shop/register` 接受 `application/json` 请求体
  - *测试思路*：MockMvc 发送合法 JSON，断言响应 HTTP 200；边界：不带 `Content-Type` 或用表单编码发送时不得返回业务成功码
- **任务项 1.1.2**：请求字段名与 VO 属性严格对应（`shopName`/`adminAccount`/`adminPassword`）
  - *测试思路*：断言反序列化后三个属性等于请求值；边界：下发 `shop_name` 下划线格式时必须映射不上并落到校验失败
- **任务项 1.1.3**：成功响应结构为 `BaseResponseVO`（`status`/`message`/`data`）
  - *测试思路*：断言 body 含三字段且 `status=200`；边界：断言 HTTP 4xx/5xx 的写法必须失败（业务码走 body）
- **任务项 1.1.4**：成功响应回传新建店铺 ID ⚠️ 现状差异 P4
  - *测试思路*：断言 `data == shopId`；边界：当前 `new BaseResponseVO<>()` 的 `data` 为 null，用例先红，驱动补出参

#### 功能上下文 1.2：Controller 层 - 注册请求编排

- **任务项 1.2.1**：仅 `POST /shop/register` 命中注册端点
  - *测试思路*：断言 POST 返回 200；边界：`GET /shop/register` 必须 405，`POST /shop/registe` 必须 404
- **任务项 1.2.2**：将请求 VO 原样传给 `shopService.register`
  - *测试思路*：`@MockBean ShopService` 后 `verify(shopService).register(same(vo))`；边界：参数被重新构造导致 `same` 失败即缺陷
- **任务项 1.2.3**：Service 返回 id 时回填成功响应
  - *测试思路*：mock service 返回 100，断言 body `status=200`；边界：service 抛异常时不得返回 200
- **任务项 1.2.4**：Service 抛 `IllegalArgumentException` 时转为业务码 400
  - *测试思路*：断言 `status=400` 且 `message` 等于异常消息；边界：异常消息被包装或吞掉必须失败
- **任务项 1.2.5**：注册成功后发布 `ShopRegisterEvent` 且四个字段完整
  - *测试思路*：`ArgumentCaptor` 捕获 `eventBus.post` 参数，断言 `shopId/shopName/adminAccount/adminPwd`；
    边界：`adminPwd` 为 null 或与请求不一致必须失败
- **任务项 1.2.6**：落库失败时不发布事件
  - *测试思路*：mock service 抛异常，`verify(eventBus, never()).post(any())`；边界：任何一次事件早于落库成功都视为断链

#### 功能上下文 1.3：Service 层 - 注册业务编排

- **任务项 1.3.1**：执行唯一性校验后才落库
  - *测试思路*：mock `exist` 返回 0 → 断言调用 `registerShop`；边界：`exist` 返回 1 时 `verify(shopMapper, never()).registerShop(any())`
- **任务项 1.3.2**：VO 到 PO 的属性拷贝不丢字段
  - *测试思路*：断言 PO 的 `shopName/adminAccount/adminPassword` 与 VO 一致；边界：改名导致 `BeanUtils` 漏拷必须被断言捕获
- **任务项 1.3.3**：写入默认状态 `status = 1`
  - *测试思路*：捕获传入 mapper 的 PO，断言 `status == 1`；边界：null、0（禁用）都必须失败
- **任务项 1.3.4**：落库影响行数必须为 1，否则抛业务异常
  - *测试思路*：mock 返回 0 → 断言抛 `IllegalArgumentException("保存到数据库异常")`；边界：返回 1 必须正常返回
- **任务项 1.3.5**：返回数据库回填的自增主键
  - *测试思路*：mock mapper 回填 id=100 → 断言返回 100；边界：`id` 为 null 时必须视为缺陷（红灯）
- **任务项 1.3.6**：Service 层独立兜底非空断言（防绕过 Controller）
  - *测试思路*：直接调用 service 传 null/空串 → 断言抛异常；边界：`"  "` 空白串必须被 `notBlank` 语义拒绝
- **任务项 1.3.7**：密码以请求值原样写入（当前明文语义，锚点 P1）
  - *测试思路*：断言 `PO.adminPassword == 请求明文`；边界：引入加密后该断言必须改写为 `BCrypt.matches(明文, 密文)`，用红绿切换锁定改造点

#### 功能上下文 1.4：Model/DAO 层 - 店铺持久化

- **任务项 1.4.1**：`INSERT` 覆盖 `shop_name/admin_account/admin_password/logo_url/status` 五列
  - *测试思路*：内存 SQLite 执行 mapper 后 `SELECT` 断言五列值；边界：`logo_url` 允许为 NULL
- **任务项 1.4.2**：自增主键回填到 `ShopPO.id`
  - *测试思路*：插入后断言 `po.getId()` 非空且等于 `SELECT last_insert_rowid()`；边界：`useGeneratedKeys` 配置错误必须失败
- **任务项 1.4.3**：`shop_name/admin_account/admin_password` 的 NOT NULL 约束生效
  - *测试思路*：直插 NULL → 断言 `SQLException`；边界：允许写入 NULL 说明约束缺失
- **任务项 1.4.4**：`created_at/updated_at` 由默认值兜底
  - *测试思路*：不传时间字段插入后断言两列非空；边界：显式传 null 时仍应由默认值补齐
- **任务项 1.4.5**：`idx_shop_admin_account` 索引存在（登录链路回归保护）
  - *测试思路*：查 `sqlite_master` 断言索引存在；边界：索引被删除时该保护用例如期失败

---

### 功能点 2：注册入参不合法时请求被拒绝并返回明确提示

**用户可感知行为**：字段缺失、空白、超长或格式非法时，收到业务码 400 与可读的错误信息，且不产生任何数据。

#### 功能上下文 2.1：View 层 - 请求 VO 校验约束

- **任务项 2.1.1**：`shopName` 非空且拒绝纯空白
  - *测试思路*：传 null、`""`、`"   "` 三种输入，断言均返回 400 且提示「店铺名不能为空」；边界：`"   "` 必须失败（`@NotNull` 会放行，`@NotBlank` 才拒绝）
- **任务项 2.1.2**：`adminAccount` 非空且拒绝纯空白
  - *测试思路*：同上断言「账户不能为空」；边界：只有空格时不得进入 Service 层
- **任务项 2.1.3**：`adminPassword` 非空且拒绝纯空白
  - *测试思路*：断言提示「账户密码」；边界：null 与空白串都必须被拒
- **任务项 2.1.4**：`shopName` 长度上界为 6
  - *测试思路*：长度 6 必须通过、7 必须 400 且提示「店铺名过长」；边界：正好等于上界是包含关系，5 与 6 通过
- **任务项 2.1.5**：`adminAccount` 需要长度与格式约束 ⚠️ 现状缺失（现状仅 `@NotBlank`）
  - *测试思路*：断言长度 >100（超列宽）或含非法字符时返回 400；边界：用例先红，驱动补约束
- **任务项 2.1.6**：`adminPassword` 需要最小长度约束 ⚠️ 现状缺失
  - *测试思路*：断言长度 <6 时返回 400；边界：长度 6 必须通过，5 必须拒绝

#### 功能上下文 2.2：Controller 层 - 校验异常统一转换

- **任务项 2.2.1**：`MethodArgumentNotValidException` 转为业务码 400
  - *测试思路*：断言 `status=400`；边界：不得返回 500，也不得抛出到容器产生 HTML 错误页
- **任务项 2.2.2**：多字段同时非法时聚合全部错误信息
  - *测试思路*：三个字段一起置空，断言 message 含三个提示（当前以逗号连接）；边界：只出现第一条提示即视为聚合不完整
- **任务项 2.2.3**：`IllegalArgumentException` 转为业务码 400 且保留原消息
  - *测试思路*：mock service 抛「店铺名已存在」，断言 `status=400` 且 message 完全一致；边界：消息被改写为「内部错误」必须失败
- **任务项 2.2.4**：校验失败时不得调用 Service、不得写库、不得发事件
  - *测试思路*：`verify(shopService, never()).register(any())` 且 `verify(eventBus, never()).post(any())`；边界：任何一次穿透都说明校验时机过晚
- **任务项 2.2.5**：错误列表为空时返回 500「内部错误」兜底
  - *测试思路*：构造无 `getAllErrors` 内容的异常，断言 `status=500`；边界：不得因为空列表抛出 NPE

#### 功能上下文 2.3：Service 层 - 领域兜底校验

- **任务项 2.3.1**：直接调用 Service 传空白字段仍被拒绝
  - *测试思路*：`register(vo)` 传空白 `shopName` → 断言抛异常且未调用 mapper；边界：Controller 校验被绕过时 Service 必须独立守住
- **任务项 2.3.2**：null 与空白串的拒绝语义一致
  - *测试思路*：参数化 null 与 `"  "` 两组输入，断言都触发异常；边界：仅 null 被拒说明用了 `notNull` 而非 `notBlank`

---

### 功能点 3：使用已存在的店铺名注册时被拒绝

**用户可感知行为**：店铺名重复时收到「店铺名已存在」，且不产生第二条店铺记录。

#### 功能上下文 3.1：Service 层 - 唯一性业务规则

- **任务项 3.1.1**：`exist` 返回 0 时允许继续注册
  - *测试思路*：mock `exist=0` → 断言流程走到 `registerShop`；边界：`exist=1` 必须提前中断
- **任务项 3.1.2**：`exist` 返回 ≥1 时抛「店铺名已存在」且不落库
  - *测试思路*：断言异常消息等于「店铺名已存在」并 `verify(never()).registerShop`；边界：消息变更或先落库即为缺陷
- **任务项 3.1.3**：唯一性判定维度是 `shopName`，不是 `adminAccount`
  - *测试思路*：账号相同、店铺名不同 → 断言允许注册（当前语义）；边界：若产品要求账号全局唯一，此用例当前为红灯，需显式决策

#### 功能上下文 3.2：Model/DAO 层 - 重复判定查询

- **任务项 3.2.1**：`exist` 计数查询语义正确
  - *测试思路*：空表返回 0、存在一条同名返回 1；边界：存在不同名记录时必须返回 0
- **任务项 3.2.2**：大小写与首尾空格的比较语义明确
  - *测试思路*：`ShopA` 与 `shopa` 在默认 BINARY 排序下断言为「不同」；边界：该用例把当前敏感语义固化为契约，若改为忽略大小写需同步改断言

#### 功能上下文 3.3：数据层约束 - 并发与唯一索引

- **任务项 3.3.1**：`tb_shop.shop_name` 缺少唯一索引（现状暴露）
  - *测试思路*：查 `sqlite_master` 断言不存在唯一索引 → 用例通过即证明约束缺口真实存在
- **任务项 3.3.2**：并发下两个同名请求会双双成功 ⚠️ 红灯（先写失败用例）
  - *测试思路*：两个线程/两次连续插入同名，断言最终只有一条记录；边界：当前会写入两条，用例必然红，驱动加唯一索引
- **任务项 3.3.3**：唯一索引生效后，重复插入必须转为业务码 400
  - *测试思路*：捕获数据库约束异常并断言 `status=400` 且消息可读；边界：不得把原始 SQL 异常直接返回给用户

---

### 功能点 4：注册成功后自动创建管理员员工账号（跨服务）

**用户可感知行为**：注册完成后，管理员账号可立即在员工端登录（employee 服务中已存在该账号）。

#### 功能上下文 4.1：Service/事件契约层 - 注册事件定义与发布

- **任务项 4.1.1**：`ShopRegisterEvent` 携带 `shopId/shopName/adminAccount/adminPwd`
  - *测试思路*：断言事件四个字段可读写且与请求一致；边界：字段缺失或命名漂移（`adminPassword` 改名）必须失败
- **任务项 4.1.2**：注册事件在店铺落库成功之后发布
  - *测试思路*：mock mapper 返回 0 → 断言 `eventBus.post` 未被调用；边界：先发事件后落库的写法必须被判失败
- **任务项 4.1.3**：事件发布方当前位置为 Controller 层（现状契约，锚点 P6）
  - *测试思路*：断言 `ShopController` 持有并调用 `EventBus`；边界：一旦事件下沉到 Service，该用例需同步修改，用红绿切换暴露重构影响面

#### 功能上下文 4.2：事件订阅者 - `InitEmployeeEventHandle`

- **任务项 4.2.1**：订阅者随 Spring 容器启动注册到 EventBus
  - *测试思路*：断言 `afterPropertiesSet` 调用了 `eventBus.register(this)`；边界：未注册时事件静默丢失必须被断言捕获
- **任务项 4.2.2**：事件到 `AddEmployeeRequestVO` 的字段映射正确
  - *测试思路*：断言 `shopId=事件.shopId`、`username=adminAccount`、`password=adminPwd`；边界：`username` 与 `password` 映射互换必须失败
- **任务项 4.2.3**：映射后调用 `employeeFeignClient.save`
  - *测试思路*：mock Feign 客户端，`verify(save(captor.capture()))` 且参数字段一致；边界：不得跳过调用直接返回
- **任务项 4.2.4**：响应 `status != 200` 时记录错误且不抛异常
  - *测试思路*：mock 返回 `status=500` → 断言方法正常返回并输出 error 日志；边界：抛出异常或在异步线程静默丢弃都必须被断言识别
- **任务项 4.2.5**：响应 `status = 200` 时记录新建员工 ID
  - *测试思路*：mock 返回 `status=200,data=7` → 断言读取到 7；边界：`data` 为 null 时不得抛 NPE

#### 功能上下文 4.3：远程调用层 - Feign 契约

- **任务项 4.3.1**：Feign 客户端声明 `name = employee-service`、`path = /employee`
  - *测试思路*：基于注解元数据断言服务名与路径；边界：服务名或路径漂移必须失败（否则运行时 404）
- **任务项 4.3.2**：`POST /employee/save` 请求体字段为 `shopId/username/password`
  - *测试思路*：MockWebServer/WireMock 断言请求体 JSON 键名；边界：多出或缺失字段必须失败
- **任务项 4.3.3**：响应可反序列化为 `BaseResponseVO<Integer>`
  - *测试思路*：桩返回 `{"status":200,"data":7}`，断言 `resp.getData() == 7`；边界：`status` 缺失时不得当作成功

#### 功能上下文 4.4：employee 服务侧 - 员工账号写入

- **任务项 4.4.1**：`/employee/save` 接收请求并返回新建 ID
  - *测试思路*：MockMvc 断言 `status=200` 且 `data` 为整数；边界：请求缺 `shopId` 时必须 400
- **任务项 4.4.2**：`addEmployee` 对 `username/shopId/password` 做非空断言
  - *测试思路*：分别置空三个字段，断言抛异常且未落库；边界：空白串必须被拒
- **任务项 4.4.3**：写入默认 `status=1` 与默认头像
  - *测试思路*：捕获 PO 断言 `status==1` 且 `avatarUrl` 非空；边界：请求显式传入头像时不覆盖为默认值
- **任务项 4.4.4**：落库影响行数必须为 1
  - *测试思路*：mock 返回 0 → 断言抛「保存员工信息失败」；边界：返回 1 正常返回
- **任务项 4.4.5**：重复调用同一 `shopId + username` 的幂等行为 ⚠️ 红灯（现状会插入第二条）
  - *测试思路**：连续调用两次，断言最终只有一条记录或第二次返回既有 ID；边界：当前必红，驱动加唯一约束与幂等处理

#### 功能上下文 4.5：employee Model/DAO 层

- **任务项 4.5.1**：`INSERT` 写入 `shop_id/username/password/avatar_url`
  - *测试思路*：内存库插入后 `SELECT` 断言四列；边界：`status` 依赖表默认值 1
- **任务项 4.5.2**：主键回填到 `EmployeePO.id`
  - *测试思路*：断言 `po.getId()` 非空；边界：回填失败将导致 Feign 响应 `data` 为 null
- **任务项 4.5.3**：`findByUsername` 的 `shopId` 为可空条件
  - *测试思路*：传 null 断言忽略该条件、传具体值断言同时过滤；边界：`shopId` 为空时不得报错
- **任务项 4.5.4**：`tb_employee` 缺 `(shop_id, username)` 唯一索引（现状暴露）
  - *测试思路*：查 `sqlite_master` 断言唯一索引不存在；边界：证明 4.4.5 红灯的根因在数据层

---

### 功能点 5：注册成功后自动下发欢迎站内信

**用户可感知行为**：注册完成后在站内信中看到一条欢迎消息。

#### 功能上下文 5.1：事件订阅者 - `InitShopMessageEventHandle`

- **任务项 5.1.1**：订阅者注册到 EventBus
  - *测试思路*：断言 `afterPropertiesSet` 调用 `register(this)`；边界：未注册时欢迎消息静默缺失
- **任务项 5.1.2**：固定标题与内容并指定消息类型
  - *测试思路*：捕获入参断言 `shopId` 来自事件、标题为「欢迎注册本系统」；边界：标题文案变更必须让用例失败以触发评审
- **任务项 5.1.3**：欢迎消息失败不影响员工账号创建
  - *测试思路*：让 `messageService` 抛异常，断言员工分支仍完成；边界：两个订阅者之间的异常不得互相传染

#### 功能上下文 5.2：Service 层 - `MessageServiceImpl`

- **任务项 5.2.1**：对 `shopId/title/content` 做非空断言
  - *测试思路*：分别置空断言抛异常且不落库；边界：`shopId` 为 null 必须拒绝
- **任务项 5.2.2**：写入影响行数必须为 1
  - *测试思路*：mock 返回 0 → 断言抛「保存欢迎消息异常」；边界：返回 1 正常返回

#### 功能上下文 5.3：Model/DAO 层 - 站内信持久化

- **任务项 5.3.1**：`INSERT` 写入 `shop_id/title/content/msg_type/is_read`
  - *测试思路*：插入后 `SELECT` 断言 `msg_type=1`、`is_read=0`；边界：`is_read` 默认 1 会导致消息被标为已读
- **任务项 5.3.2**：`shop_id` 上的查询索引存在
  - *测试思路*：查 `sqlite_master` 断言 `idx_messages_shop_id` 存在；边界：索引缺失时该回归用例失败

---

### 功能点 6：注册链路失败时可观测且不产生半成品数据

**用户可感知行为**：下游故障时用户不会看到「注册成功但账号不可用」这类不一致结果，运维可定位问题。

#### 功能上下文 6.1：事务边界

- **任务项 6.1.1**：当前注册流程无事务，店铺落库后事件失败不回滚（现状语义锚点）
  - *测试思路*：断言落库后订阅者异常时店铺记录仍存在；边界：该语义被固化为「已知不一致」，一旦引入事务需改写用例
- **任务项 6.1.2**：唯一性校验与落库之间的竞态窗口
  - *测试思路*：在 `exist` 与 `registerShop` 之间插入并发同名写入，断言只成功一次；边界：当前会双成功（红灯），驱动唯一索引 + 事务
- **任务项 6.1.3**：本地事务内不得执行远程调用
  - *测试思路*：断言 `register` 执行期间未发生 Feign 调用；边界：把 Feign 塞进事务会延长持锁时间，需被断言拦截

#### 功能上下文 6.2：异步事件异常处理

- **任务项 6.2.1**：`AsyncEventBus` 订阅者异常不静默丢失
  - *测试思路*：注入自定义 `SubscriberExceptionHandler`，让订阅者抛异常，断言异常处理器被调用；边界：无处理器时 Guava 只打印日志，视为不可观测
- **任务项 6.2.2**：异步分支异常不影响主线程响应
  - *测试思路*：订阅者抛异常后断言注册接口仍返回 200；边界：主流程被异步异常拖垮必须失败
- **任务项 6.2.3**：同步测试替身与生产异步行为一致可测
  - *测试思路*：测试 Profile 使用同步 `EventBus`，断言断言点不依赖线程休眠；边界：用 `Thread.sleep` 的用例视为不稳定用例，必须重构

#### 功能上下文 6.3：补偿与重试

- **任务项 6.3.1**：Feign 失败时存在可补偿记录 ⚠️ 红灯（现状仅 `log.error`）
  - *测试思路*：mock Feign 抛异常，断言落一条待补偿任务或事件表记录；边界：当前断言必红，驱动引入本地消息表/Outbox
- **任务项 6.3.2**：补偿重试具备幂等性
  - *测试思路*：同一事件重试两次，断言员工账号只创建一条；边界：重复插入即失败，与 4.4.5 联动
- **任务项 6.3.3**：重试次数与最终失败告警
  - *测试思路*：连续失败 N 次后断言进入死信/告警状态；边界：无限重试不退避必须失败

#### 功能上下文 6.4：响应语义与日志

- **任务项 6.4.1**：失败原因只通过 `status + message` 表达，HTTP 恒 200
  - *测试思路*：对所有失败分支断言 HTTP 200 且 `status != 200`；边界：出现 HTTP 4xx/5xx 说明契约被破坏
- **任务项 6.4.2**：关键链路日志包含 `shopId`
  - *测试思路*：用 Logback `ListAppender` 捕获日志，断言事件发布与订阅日志含 shopId；边界：日志缺少定位标识即不可排查

---

### 功能点 7：注册请求可通过网关统一入口访问

**用户可感知行为**：通过 `http://gateway:8080/shop/register` 能完成注册。

#### 功能上下文 7.1：网关路由层

- **任务项 7.1.1**：`/shop/**` 路由到 `lb://shop-service`
  - *测试思路*：加载 `RouteLocator`，断言 `/shop/register` 命中的 route id 与目标 URI；边界：目标写成固定 IP 或错误服务名必须失败
- **任务项 7.1.2**：`/shop/register` 不被其他路由或降级规则抢占
  - *测试思路*：断言该路径只匹配一条路由且顺序稳定；边界：新增路由导致覆盖必须被断言捕获

#### 功能上下文 7.2：网关过滤器层

- **任务项 7.2.1**：`LogginFilter` 记录路径后放行请求
  - *测试思路*：断言过滤器返回 `chain.filter` 的结果且未修改请求体；边界：过滤器改写请求体导致注册参数丢失必须失败
- **任务项 7.2.2**：注册接口在鉴权白名单内 ⚠️ 红灯（当前无鉴权）
  - *测试思路*：未携带 Token 调用 `/shop/register`，断言未被拦截；边界：一旦引入鉴权，白名单遗漏该路径必须让用例变红

---

## 5. 架构与重构思考

### 5.1 组件间关系分析

``mermaid
flowchart LR
    subgraph GW["网关层"]
        R["RouteLocator /shop/**"]
        F["LogginFilter"]
    end
    subgraph SHOP["shop-service"]
        CTRL["ShopController"]
        SVC["ShopServiceImpl"]
        MAP["ShopMapper"]
        EV["AsyncEventBus"]
        H1["InitEmployeeEventHandle"]
        H2["InitShopMessageEventHandle"]
    end
    subgraph EMP["employee-service"]
        ECTRL["EmployeeController"]
        ESVC["EmployeeServiceImpl"]
        EMAP["EmployeeMapper"]
    end
    DB1[("shop.db")]
    DB2[("employee.db")]
    GW --> CTRL
    CTRL --> SVC
    CTRL --> EV
    SVC --> MAP --> DB1
    EV --> H1
    EV --> H2
    H2 --> DB1
    H1 -.->|Feign| ECTRL
    ECTRL --> ESVC --> EMAP --> DB2
``

关键关系与风险：

1. **Controller 与 EventBus 直接耦合**：Controller 同时承担「HTTP 适配」和「领域事件发布」两种职责，
   Service 并未真正表达「注册完成」这一领域事实。
2. **shop 与 employee 通过同步 Feign 连接，但调用发生在异步事件线程内**：调用链跨越进程与线程，
   失败不可回传、不可重试，属于典型的分布式一致性缺口。
3. **两个服务各持一个 SQLite 库**：不存在跨库事务，注册与员工创建无法原子提交。
4. **测试友好性不足**：`AsyncEventBus` 全局单例、`EmployeeFeignClient` 为接口桩可替身，但事件发布点
   位于 Controller 层，导致「领域行为」难以在 Service 层被单测覆盖。

### 5.2 架构对上下文划分的影响

- **为什么分 View 与 Controller**：View 层承载对外契约（字段名、校验注解、响应结构），是跨端复用的稳定接口；
  Controller 只做适配与编排。二者测试方式不同（MockMvc 契约测试 vs Mockito 编排测试），因此必须分开。
- **为什么 Service 层要独立成上下文**：唯一性校验、默认值、落库行数断言属于领域不变量，
  与 HTTP 无关；只有把它单独作为上下文，才能在无容器环境下用 Mockito 快速验证。
- **为什么 Model/DAO 单独成上下文**：SQL 语义（列默认值、NOT NULL、唯一索引、主键回填）无法被 Service 层单测发现，
  必须用内存 SQLite 做真实 SQL 集成测试，这也是 F3 唯一索引红灯唯一可暴露的位置。
- **为什么把异步事件从 Controller 上下文中拆出来**：事件订阅者是独立的可测试单元，且它的失败模式（静默丢弃、
  异常传染）与主流程完全不同，必须独立断言。
- **为什么把 Feign 与 employee 服务分开**：Feign 是「契约」上下文（只验证请求/响应形状），
  employee 服务是「提供方」上下文（验证落库与幂等），两者的破坏原因与修复方都不同。
- **为什么单列网关上下文**：路由与过滤器属于接入层配置，回归成本极低但破坏力极大（整条链路 404），
  值得用一组廉价断言长期守护。

### 5.3 耦合点与坏味道（含重构方向）

| 编号 | 坏味道 | 证据 | 影响 | 重构方向 |
| --- | --- | --- | --- | --- |
| S1 | 事件发布位置错误 | `ShopController` 注入 `EventBus` 并 `post` | 领域行为泄漏到适配层，Service 薄 | 事件下沉到 `ShopService.register` 末尾 |
| S2 | 明文密码 | `admin_password`/`password` 明文入库 | 严重安全风险，注册即泄露 | 引入 `PasswordEncoder`（BCrypt），注册与登录同步改造 |
| S3 | 业务异常类型单一 | hutool `Assert` 抛 `IllegalArgumentException` | 无法按错误码区分分支 | 定义 `BizException(code, message)` 与错误码枚举 |
| S4 | 校验规则分散 | VO 注解 + Service `notBlank` 重复 | 规则漂移，改一处漏一处 | 规则集中在 VO/Validator，Service 只守领域不变量 |
| S5 | 无唯一约束 | `tb_shop` 仅普通索引 | 并发重复注册 | 加 `UNIQUE(shop_name)`、`UNIQUE(shop_id, username)`，捕获约束异常转业务码 |
| S6 | 无事务边界 | `register` 无 `@Transactional` | 半成品数据 | 本地写库加事务；跨服务用本地消息表/Outbox 保证最终一致 |
| S7 | 异步事件不可靠 | Guava `AsyncEventBus` + 订阅者仅 `log.error` | 事件丢失无感知、无补偿 | 换 `ApplicationEventPublisher` 或 MQ，加本地消息表与重试 |
| S8 | 契约对象重复定义 | `AddEmployeeRequestVO` 在 shop 与 employee 各一份 | 字段漂移引发运行时错误 | 契约收敛到 `common`（或独立 API 模块） |
| S9 | 魔法数字状态码 | `BaseResponseVO` 直接用 200/400/500 | 语义不可读、易错 | 提取 `ResponseCode` 枚举 |
| S10 | 响应与 HTTP 语义脱节 | 恒返回 HTTP 200 | 网关/客户端无法按状态做通用处理 | 业务码与 HTTP 状态双轨并明确边界 |
| S11 | 无测试基座 | 无测试依赖、无测试代码 | 无法 TDD | 先完成附录 A |
| S12 | 手动注册订阅者 | `InitializingBean.afterPropertiesSet` | 排序与生命周期不可控 | 交给 Spring 事件机制或显式初始化器 |

### 5.4 测试通过后的重构路线（每一步都保持绿灯）

1. **补测试基座**（附录 A）→ 现有行为先被「特性测试」锁定，形成安全网。
2. **S3/S9：错误处理归一**——提取 `BizException` 与错误码枚举，异常转换 Advice 同步调整，断言从字符串匹配升级为错误码匹配。
3. **S6：加事务 + S5：加唯一索引**——并发用例（3.3.2、6.1.2）由红转绿。
4. **S1：事件下沉到 Service**——同步修改 4.1.3 与 1.2.5/1.2.6 的断言位置，验证编排职责归位。
5. **S7/S6：引入本地消息表 + 重试幂等**——补偿用例（6.3.1、6.3.2、4.4.5）转绿。
6. **S2：密码加密**——改写 1.3.7 断言为 BCrypt 校验，注册与登录链路同步。
7. **S8：契约收敛**——把重复 VO 迁到 `common`，用契约测试防漂移。
8. **S12/S4：事件注册与校验规则收敛**——清理 `InitializingBean` 手动注册，统一校验入口。

> 重构原则：**每次只改一个坏味道，测试全绿再进入下一步**；红灯用例是重构的驱动力，不是障碍。

---

## 附录 A：测试基础设施任务项

> 这是在写任何业务测试之前必须完成的前置工作（对应 S11/P9）。项目当前无测试依赖、无测试代码。

### 上下文 A.1：构建层

- **任务项 A.1.1**：根 `pom.xml` 引入 `spring-boot-starter-test`（`test` scope）
  - *测试思路*：执行 `mvn -q test` 能发现并运行测试类；边界：依赖缺失时构建必须报错而非静默跳过
- **任务项 A.1.2**：shop/employee 模块补齐 JUnit 5 与 Mockito 相关依赖
  - *测试思路*：`mvn -pl shop test` 输出测试统计；边界：0 个测试被执行时视为失败（需开启 failIfNoTests）
- **任务项 A.1.3**：统一测试 Profile，屏蔽 Nacos 与真实远程调用
  - *测试思路*：启动 Spring 上下文不尝试连接 `47.100.22.158:8848`；边界：测试依赖外部网络即为失败

### 上下文 A.2：测试基座

- **任务项 A.2.1**：内存 SQLite 数据源（`jdbc:sqlite::memory:` 或临时文件）
  - *测试思路*：Mapper 测试可执行真实 SQL；边界：测试不得写坏仓库中的 `shop.db` / `employee.db`
- **任务项 A.2.2**：可重复执行的建表脚本（`schema.sql`）
  - *测试思路*：连续执行两次不报错（`CREATE TABLE IF NOT EXISTS`）；边界：第二次执行失败说明脚本不可重入
- **任务项 A.2.3**：Mapper 切片测试基类（`@MybatisTest` 风格 + 自动建表）
  - *测试思路*：新建 Mapper 测试无需重复配置；边界：基类不足以隔离用例时立即重构
- **任务项 A.2.4**：Controller 契约测试基类（`@WebMvcTest` + `@MockBean`）
  - *测试思路*：只加载 Web 层，启动时间可控；边界：加载了整个 Spring 上下文说明切片边界错误
- **任务项 A.2.5**：同步 `EventBus` 测试替身
  - *测试思路*：事件发布后立即同步执行订阅者，断言无需 `Thread.sleep`；边界：任何依赖等待的用例视为不稳定
- **任务项 A.2.6**：Feign 契约测试桩（MockWebServer/WireMock）
  - *测试思路*：断言请求路径、方法、请求体 JSON 与响应反序列化；边界：桩未启动时用例必须失败而不是跳过

### 上下文 A.3：数据隔离

- **任务项 A.3.1**：每个用例独立数据集，`@BeforeEach` 清理相关表
  - *测试思路*：用例执行顺序打乱后仍全部通过；边界：单独运行通过与全量运行失败说明存在数据污染
- **任务项 A.3.2**：固定测试数据工厂（店铺、员工、消息）
  - *测试思路*：构造器提供合法基线数据；边界：每个用例重复手写数据视为可维护性缺陷

---

## 附录 B：任务项到测试类映射

| 功能上下文 | 建议测试类 | 测试形态 | 核心断言 |
| --- | --- | --- | --- |
| 1.1、2.1、2.2 | `ShopRegisterContractTest` | MockMvc 切片 | 字段名、400 提示、响应结构 |
| 1.2 | `ShopControllerTest` | Mockito 单测 | service 调用、事件发布、异常转换 |
| 1.3、2.3、3.1 | `ShopServiceImplTest` | Mockito 单测 | 唯一性、默认值、行数、返回值 |
| 1.4、3.2、3.3 | `ShopMapperTest` | MyBatis + 内存 SQLite | 列默认值、主键回填、约束与索引 |
| 4.1、4.2、6.2 | `ShopRegisterEventTest` | 同步 EventBus 替身 | 事件字段、订阅者行为、异常隔离 |
| 4.3 | `EmployeeFeignContractTest` | WireMock/MockWebServer | 路径、请求体、响应反序列化 |
| 4.4、4.5 | `EmployeeSaveTest` | MockMvc + Mapper 集成 | 非空断言、默认值、幂等 |
| 5.1、5.2、5.3 | `ShopWelcomeMessageTest` | Mockito + Mapper 集成 | 文案、`is_read=0`、索引 |
| 6.1、6.3、6.4 | `ShopRegisterConsistencyTest` | 集成测试 | 竞态、补偿记录、日志含 shopId |
| 7.1、7.2 | `GatewayRouteTest` | 配置断言 | 路由目标、过滤器放行 |

推荐执行顺序（红-绿-重构节奏）：

1. 附录 A 基座 → 2. F1（打通成功路径）→ 3. F2（校验）→ 4. F3（唯一性）→ 5. F4（跨服务）
   → 6. F5（站内信）→ 7. F7（网关回归）→ 8. F6（一致性，成本最高，最后做）。

---

## 附录 C：现状差异与红灯清单

| 编号 | 对应任务项 | 期望行为 | 当前实现 | 处置 |
| --- | --- | --- | --- | --- |
| G1 | 1.1.4 | 成功响应返回 shopId | `data` 为 null | 先写红灯用例，再补出参 |
| G2 | 2.1.5、2.1.6 | 账号/密码有长度与格式约束 | 仅 `@NotBlank` | 补注解并加边界用例 |
| G3 | 3.3.1、3.3.2 | `shop_name` 唯一 | 仅普通索引，并发双写成功 | 加唯一索引，约束异常转 400 |
| G4 | 4.4.5、4.5.4 | 员工账号幂等 | 无 `(shop_id, username)` 唯一约束 | 加唯一约束 + 幂等返回 |
| G5 | 6.3.1 | 下游失败可补偿 | 仅 `log.error` | 引入本地消息表 + 重试 |
| G6 | 7.2.2 | 注册在鉴权白名单 | 当前无鉴权逻辑 | 引入鉴权时同步补白名单用例 |
| G7 | 1.3.7 | 密码加密存储 | 明文 | 作为独立重构步骤，红绿切换断言 |

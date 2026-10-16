---
title: "登录功能 TDD 实施记录"
description: "按 login-tdd-breakdown.md 实施的结果：测试基座、19 个红灯转绿、生产代码变更、真实库迁移与后续建议"
author: "TDD 教练"
created: "2026-10-16"
updated: "2026-10-16"
version: "1.0.0"
status: "published"
tags: ["tdd", "implementation", "login", "authentication", "status"]
category: "spec"
audience: ["backend-team", "qa"]
related:
  - "./login-tdd-breakdown.md"
  - "./shop-register-tdd-breakdown.md"
  - "./implementation-status.md"
---

# 登录功能 TDD 实施记录

> 记录按 [登录 TDD 任务拆解](./login-tdd-breakdown.md) 实施的实际结果。
> 运行环境：JDK 17.0.19，Maven 3.9.12，本地仓库 `D:\AI_Coding\b2c\.m2repo`（离线）。
> 最终结果：`mvn -B -o test` → **BUILD SUCCESS，MVN_EXIT=0，212 个测试全绿**。

## 1. 阶段状态

| 阶段 | 内容 | 状态 | 测试增量 |
| --- | --- | --- | --- |
| A | 测试基座（`tb_login_logs` 建表、DAO 基座解析 `LoginLogMapper`、logback-test、数据工厂） | 完成 | - |
| F1 | 登录成功主流程（View / Controller / Service / Model / 事件契约） | 完成 | +49 |
| F2 | 凭证错误与防枚举 | 完成 | +18 |
| F3 | 禁用账号 | 完成 | +8 |
| F4 | 审计日志、`ClientInfoUtil`、employee 事件总线装配 | 完成 | +33 |
| F5 | 登录状态更新（含 `COALESCE` 修复） | 完成 | +16 |
| F6 | 多租户与账号唯一性（唯一索引、`shopId` 必填） | 完成 | +15 |
| F7 | 网关路由与过滤器 | 完成 | +4 |
| - | 迁移与一致性核对 | 完成 | - |

最终测试规模（对比注册侧基线 69 个）：

| 模块 | 测试数 | 其中本次新增 | 结果 |
| --- | --- | --- | --- |
| gateway | 7 | 4 | 全绿 |
| common | 6 | 6 | 全绿 |
| shop | 130 | 73 | 全绿 |
| employee | 69 | 60 | 全绿 |
| 合计 | **212** | **143** | `BUILD SUCCESS` |

本次新增测试类：

| 模块 | 测试类 | 覆盖功能上下文 |
| --- | --- | --- |
| common | `ClientInfoUtilTest` | 4.3 |
| shop | `ShopLoginContractTest`、`ShopLoginServiceImplTest` | 1.1、1.2、2.1、3.3；1.3、2.2、3.1 |
| shop | `ShopLoginMapperTest`、`LoginLogMapperTest` | 1.4、2.3、3.2、5.2、6.1、6.3；4.2 |
| shop | `ShopAuditLogEventHandleTest`、`ShopUserStatusEventHandleTest` | 4.1；5.1 |
| shop | `ShopLoginEventTest`、`ShopRegisterAccountUniquenessTest` | 1.5；6.1 |
| employee | `EmployeeLoginContractTest`、`EmployeeLoginServiceImplTest` | 1.1、1.2、2.1、3.3、6.2；1.3、2.2、3.1、6.2 |
| employee | `EmployeeLoginMapperTest`、`EmployeeLoginLogMapperTest` | 1.4、3.2、5.2、6.2；4.2 |
| employee | `EmployeeAuditLogEventHandleTest`、`EmployeeUserStatusEventHandleTest` | 4.1；5.1 |
| employee | `EmployeeLoginEventTest`、`EmployeeEventBusConfigTest` | 1.5；4.1.8 |
| gateway | `GatewayFilterTest`（+ `GatewayRouteConfigTest` 增补） | 7.2；7.1.3 |

## 2. 红灯 → 绿灯对照

实施严格遵循「先看到红灯，再写最小实现」。共观察并转绿 **19 个红灯**：

| 任务项 | 红灯用例 | 观察到的失败 | 修复 |
| --- | --- | --- | --- |
| 4.3.5 | `ClientInfoUtilTest.missingUserAgentIsUnknown` | 设备串为 `Unknown/Unknown` | `resolveDevice` 双未知回落「未知」 |
| 2.2.6 | `ShopLoginServiceImplTest.toStringDoesNotExposePassword` | `@Data` 的 toString 含明文口令 | 口令字段 `@ToString.Exclude` |
| 1.2.9 | `ShopLoginContractTest.unexpectedExceptionBecomesServerError` | 异常穿透为 `NestedServletException` | Advice 增加 `RuntimeException` 兜底 → 500 |
| L6 | `ShopLoginContractTest.overlongAccountRejected` | 101 字符账号返回 200 | 登录 VO 补 `@Length(max=100)` |
| 4.1.6 | `ShopAuditLogEventHandleTest.nullClientInfoDoesNotThrow` | `clientInfo` 为 null 时 NPE | 订阅者空值跳过审计 |
| 5.2.5 | `ShopLoginMapperTest.updateLoginStatusHandlesNullLoginCount` | `NULL + 1 = NULL` | `COALESCE(login_count, 0) + 1` |
| 6.3.1 | `ShopLoginMapperTest.adminAccountUniqueIndexExists` | 唯一索引不存在 | 建表脚本补 `uk_shop_admin_account` |
| 6.1.2 | `ShopLoginMapperTest.duplicateAdminAccountRejected` | 同账号可写入两条 | 同上（数据层兜底） |
| 6.1.3 | `ShopRegisterAccountUniquenessTest.registerRejectsDuplicateAdminAccount` | 注册不校验账号唯一 | `register` 增 `existByAccount` 校验 |
| 6.1.4 | `ShopRegisterAccountUniquenessTest.accountUniqueViolationTranslatedToBadRequest` | `admin_account` 冲突被误报为「店铺名已存在」 | 按约束名分别翻译为业务码 400 |
| 2.2.6 | `EmployeeLoginServiceImplTest.toStringDoesNotExposePassword` | 同 shop 侧 | 口令字段 `@ToString.Exclude` |
| 5.2.5 | `EmployeeLoginMapperTest.updateHandlesNullLoginCount` | 同 shop 侧 | `COALESCE` |
| 4.1.6 | `EmployeeAuditLogEventHandleTest.nullClientInfoDoesNotThrow` | 同 shop 侧 | 订阅者空值跳过审计 |
| 4.1.8 | `EmployeeEventBusConfigTest.eventBusIsWiredWithExceptionHandler` | `eventBus()` 无参、无异常处理器 | 新增 `EventBusExceptionHandler` 并注入 |
| 6.2.1 | `EmployeeLoginServiceImplTest.missingShopIdRejected` | `shopId` 为空时跨店检索 | `login` 首校验 `shopId` 非空 |
| 6.2.1 | `EmployeeLoginContractTest.missingShopIdRejected` | 缺 `shopId` 仍返回 200 | VO 补 `@NotNull` |
| L6 | `EmployeeLoginContractTest.overlongUsernameRejected` | 51 字符账号返回 200 | VO 补 `@Length(max=50)` |
| 1.2.8 | `EmployeeLoginContractTest.validationFailureBecomesBiz400` | 无校验异常处理器 | Advice 补 `MethodArgumentNotValidException` |
| 1.2.9 | `EmployeeLoginContractTest.unexpectedExceptionBecomesServerError` | 异常穿透 | Advice 补 `RuntimeException` 兜底 → 500 |

## 3. 生产代码变更清单

| 文件 | 变更 |
| --- | --- |
| [ClientInfoUtil.java](common/src/main/java/com/yjx/b2c/common/util/ClientInfoUtil.java) | `resolveDevice` 双未知回落「未知」，新增 `isUnknown` |
| [ShopLoginRequestVO.java](shop/src/main/java/com/yjx/b2c/shop/web/request/ShopLoginRequestVO.java) | 账号/密码补 `@Length(max=100)`；口令 `@ToString.Exclude` |
| [MethodArgNotValidExceptionAspect.java](shop/src/main/java/com/yjx/b2c/shop/web/aspect/MethodArgNotValidExceptionAspect.java) | 新增 `RuntimeException` 兜底 → 500；补 `@Slf4j` |
| [ShopServiceImpl.java](shop/src/main/java/com/yjx/b2c/shop/service/impl/ShopServiceImpl.java) | `register` 增账号唯一校验；唯一约束冲突按列名分别翻译 |
| [ShopMapper.java](shop/src/main/java/com/yjx/b2c/shop/dao/mapper/ShopMapper.java) / [ShopMapper.xml](shop/src/main/resources/mapper/ShopMapper.xml) | 新增 `existByAccount`；`updateLoginStatus` 改 `COALESCE` |
| [ShopAuditLogEventHandle.java](shop/src/main/java/com/yjx/b2c/shop/service/event/handler/ShopAuditLogEventHandle.java) | `clientInfo` 为空时记录告警并跳过审计 |
| [shop.sql](shop.sql) / [shop-schema.sql](shop/src/test/resources/schema/shop-schema.sql) | 新增 `uk_shop_admin_account` |
| [LoginRequestVO.java](employee/src/main/java/com/yjx/b2c/employee/web/request/LoginRequestVO.java) | `username` `@NotBlank`+`@Length(max=50)`；`password` `@NotBlank`+`@ToString.Exclude`；`shopId` `@NotNull` |
| [GlobalExceptionHandler.java](employee/src/main/java/com/yjx/b2c/employee/web/aspect/GlobalExceptionHandler.java) | 新增校验异常 → 400、`RuntimeException` → 500 兜底 |
| [EmployeeServiceImpl.java](employee/src/main/java/com/yjx/b2c/employee/service/impl/EmployeeServiceImpl.java) | `login` 首校验 `shopId` 非空 |
| [EventBusExceptionHandler.java](employee/src/main/java/com/yjx/b2c/employee/service/event/EventBusExceptionHandler.java) | 新增：与 shop 侧对齐的订阅者异常出口 |
| [EmployeeApplication.java](employee/src/main/java/com/yjx/b2c/employee/EmployeeApplication.java) | `eventBus` 注入 `SubscriberExceptionHandler` |
| [EmployeeAuditLogEventHandle.java](employee/src/main/java/com/yjx/b2c/employee/service/event/handler/EmployeeAuditLogEventHandle.java) | `clientInfo` 为空时跳过审计 |
| [EmployeeMapper.xml](employee/src/main/resources/mapper/EmployeeMapper.xml) | `updateLoginStatus` 改 `COALESCE` |

测试基座变更：`common/pom.xml` 引入 `spring-boot-starter-test`；两个测试 schema 补 `tb_login_logs`；
两个 DAO 基类解析 `LoginLogMapper.xml` 并清理新表；shop/employee 新增 `logback-test.xml`（根级别 WARN，抑制 MyBatis DEBUG 噪音）；
shop/employee 各新增 `LoginFixtures` 数据工厂。

## 4. 真实库迁移

实施前后用只读核对工具 [CheckLoginSchema.java](.tdd/tools/CheckLoginSchema.java) 核对真实库，结论与处置：

| 库 | 核对结果 | 处置 |
| --- | --- | --- |
| `shop.db` | 2 行店铺，账号无重复，`login_count` 无 NULL；缺 `uk_shop_admin_account` | 用 [MigrateLoginIndexes.java](.tdd/tools/MigrateLoginIndexes.java) 补建，已确认就绪 |
| `employee.db` | 4 行员工，`(shop_id, username)` 无重复，`uk_employee_shop_username` 已存在 | 无需迁移 |
| 两库 | `tb_login_logs` 均已存在（shop 2 行 / employee 3 行历史数据） | 保留，未清理 |

迁移工具沿用 `MigrateShopIndex` 的风格：**先查重，有重复则拒绝创建并提示人工清理**，因此可安全重复执行。

## 5. 与拆解文档的偏差与补充决策

1. **1.3.6（非 BCrypt 口令）未新增兼容分支**：实测 hutool `BCrypt.checkpw` 对非法盐返回 `false` 而非抛异常，
   用例直接通过。该用例保留为「不泄露加密内部错误」的回归保护。
2. **1.4.7 断言语义调整**：新增唯一索引后 SQLite 选择 `uk_shop_admin_account`（`USING COVERING INDEX`），
   断言改为「命中账号索引且非全表扫描」，而非写死旧索引名。
3. **1.4.6 / 5.2.6 用反射断言代替不可读字段**：`ShopPO` 未映射 `lastLoginTime`/`loginCount`，
   改用反射断言字段不存在，一旦补字段该用例即失败，保留「模型变更需人工确认」的信号。
4. **1.2.9 在 employee 侧同步补齐**：拆解文档标注「仅 shop」，但按坏味道 L5（两侧装配不对称）
   一并补齐通用兜底，避免员工侧异常穿透。
5. **4.1.6 采用「跳过审计」语义**（文档允许「跳过或写未知」）：缺少客户端信息时不写入误导性的审计记录，
   只记录告警日志。
6. **6.2.1 采用双重校验**：VO 层 `@NotNull` 拦截请求，Service 层 `Assert` 兜底防绕过。
7. **L6 登录口令只加长度上限、未加下限**：避免拒绝历史短口令；下限规则留给注册/改密链路。
8. **异步断言不使用 sleep**：4.1.7/4.1.8 用 Guava `SubscriberExceptionHandler` + `CountDownLatch` 验证，
   测试无时序抖动。
9. **未做 L4（登录错误统一 `BizException`）**：登录失败仍依赖 `IllegalArgumentException`，测试已锁定文案与防枚举语义，
   错误码归一作为后续独立重构。
10. **未做 L12（登录事件接 Outbox）**：登录事件仍是进程内 `AsyncEventBus`。

## 6. 已知问题与后续建议

| 优先级 | 问题 | 建议 |
| --- | --- | --- |
| 高 | 禁用账号提示仍可区分（`3.3.1` 决策点），账号存在性可被枚举 | 产品决策后统一失败文案，同步改写 `2.2.3`/`3.3.1` |
| 高 | `4.3.7`：审计 IP 仍直接信任客户端 `X-Forwarded-For`，可伪造 | 网关统一覆写该头，服务端只信任网关注入值 |
| 高 | 登录失败不写审计（`4.2.6` 现状锚点），撞库行为不可观测 | 引入失败审计（`status=0`）与失败次数限制 |
| 中 | L4：登录错误未统一为 `BizException`，与注册侧不一致 | 登录链路统一错误码枚举 |
| 中 | L12：登录事件为进程内异步事件，重启丢失、无补偿 | 复用注册侧 Outbox |
| 中 | L11/附录 D：仍无 Token/会话，登录只返回身份 ID | 独立需求：Token 签发 + 网关鉴权 + 白名单 |
| 低 | `ShopPO` 缺 `lastLoginTime`/`loginCount`，店铺侧登录状态只能原生 SQL 回读 | 两侧模型对齐或抽公共审计字段 |
| 低 | 真实库 `tb_login_logs` 有历史数据，未做保留策略 | 明确审计日志留存与归档 |

## 7. 复现命令

```powershell
$env:JAVA_HOME = 'D:\Program Files\Java\jdk17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_ARGS = '-Dmaven.repo.local=D:\AI_Coding\b2c\.m2repo'
cd D:\AI_Coding\b2c

# 全量测试（最终确认：212 个测试全绿，退出码 0）
mvn -B -o test

# 只跑登录相关
mvn -B -o test -pl shop    -Dtest='ShopLogin*Test,ShopAudit*Test,ShopUserStatus*Test,LoginLogMapperTest,ShopRegisterAccountUniquenessTest'
mvn -B -o test -pl employee -Dtest='EmployeeLogin*Test,EmployeeAudit*Test,EmployeeUserStatus*Test,EmployeeLoginLogMapperTest,EmployeeEventBusConfigTest'

# 真实库核对与迁移（只读核对；迁移先查重）
$jar = '.m2repo\org\xerial\sqlite-jdbc\3.42.0.0\sqlite-jdbc-3.42.0.0.jar'
java -cp $jar .tdd\tools\CheckLoginSchema.java jdbc:sqlite:D:/AI_Coding/b2c/shop.db jdbc:sqlite:D:/AI_Coding/b2c/employee.db
java -cp $jar .tdd\tools\MigrateLoginIndexes.java jdbc:sqlite:D:/AI_Coding/b2c/shop.db
```

## 8. 环境说明

本次会话最初 **所有模块的 `target` 目录写入被拒绝**（`UnauthorizedAccessException`），
导致 Maven 无法写 surefire 报告与测试资源。用 `diagnose-windows-sandbox-acl` 技能核对后的结论是
**`NOT_THIS_CLASS`**：`D:\AI_Coding\b2c` 的 Windows 文件权限本身完整（`WRITE_DAC`/`WRITE_OWNER` 均可用，
未发现第三方应用包遗留项），因此**未做任何权限修改、无回滚需要**。该诊断报告保留在
`.tdd/notes/acl-recovery/`。随后会话文件策略放宽为完全权限，`target` 写入恢复正常，构建与测试得以执行。

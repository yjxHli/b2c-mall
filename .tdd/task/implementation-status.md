---
title: "`注册功能 TDD 实施记录`"
description: "按 .tdd/task 下拆解文档与落地手册实施注册功能的执行记录：阶段状态、测试统计、偏差与后续建议"
author: "TDD 教练"
created: "2026-10-15"
updated: "2026-10-15"
version: "1.0.0"
status: "published"
tags: ["tdd", "implementation", "register", "status"]
category: "spec"
audience: ["backend-team", "qa"]
related:
  - "./shop-register-tdd-breakdown.md"
  - "./shop-register-tdd-implementation-guide.md"
---

# 注册功能 TDD 实施记录

> 记录按 [TDD 任务拆解](./shop-register-tdd-breakdown.md) 与 [落地手册](./shop-register-tdd-implementation-guide.md)
> 实施的实际结果。运行环境：JDK 17，Maven 3.9.12，全量 `mvn -B test` 通过。

## 1. 阶段状态

| 阶段 | 内容 | 状态 | 测试增量 |
| --- | --- | --- | --- |
| 0 | JDK 17 + `spring-boot-starter-test` | 完成 | - |
| 1 | 测试基座（SQLite schema + 手工 MyBatis 基类） | 完成 | 1 冒烟 |
| 2 | F1 注册主流程、响应回传 shopId | 完成 | 19 |
| 3 | F2 入参校验（含账号/密码长度约束） | 完成 | +8 |
| 4 | F3 店铺名唯一性（唯一索引 + 冲突翻译） | 完成 | +3 |
| 5 | F4 员工账号（跨服务契约 + 幂等） | 完成 | +12 |
| 6 | F5 欢迎站内信 | 完成 | +7 |
| 7 | F7 网关路由回归 | 完成 | +3 |
| 8 | F6 事件可观测 + Outbox 补偿重试 | 完成 | +13 |
| 9.1 | 错误码归一（`BizException`） | 完成 | +1 |
| 9.2 | 密码 BCrypt 加密 + 历史数据迁移 | 完成 | 改写锚点用例 |
| 9.3 | 事件发布下沉到 Service | 完成 | 断言位置迁移 |
| 9.4 | `AddEmployeeRequestVO` 契约收敛到 `common` | 完成 | - |

最终测试规模：

| 模块 | 测试数 | 结果 |
| --- | --- | --- |
| shop | 57 | 全绿 |
| employee | 9 | 全绿 |
| gateway | 3 | 全绿 |
| 合计 | 69 | `BUILD SUCCESS` |

## 2. 与手册的偏差与补充决策

1. **测试不启动 Spring 容器**（按手册既定策略）：Controller 用 `standaloneSetup`，
   Service/订阅者用纯 Mockito，DAO 用手工 `SqlSessionFactory`＋文件型 SQLite（`target/test-db`）。
2. **构建必须能写模块 `target` 目录**：本次会话在受限沙箱下无法写入，改用工作区内的
   `.m2repo` 作为本地仓库并放行构建；`.m2repo/` 已加入 `.gitignore`，可随时删除后改用默认仓库。
3. **新增迁移工具**（放在 `.tdd/tools/`，不属于业务代码）：
   - `MigrateShopIndex.java` / `MigrateEmployeeIndex.java`：给真实库补唯一索引（先查重）。
   - `RunSqlFile.java`：对已有 SQLite 库执行幂等 DDL（用于补建 Outbox 表）。
   - `HashPlainPasswords.java`：把历史明文密码原地升级为 BCrypt（密码不变）。
   - `CheckPasswords.java`：复核是否还存在明文密码。
4. **唯一索引已在真实库落地**：`uk_shop_shop_name`、`uk_employee_shop_username` 均已创建；
   `tb_register_event_outbox` 已建表。
5. **历史密码已迁移**：`tb_shop` 2 行、`tb_employee` 4 行，复核后明文 0 行。
6. **误建草稿文件**：`shop/src/test/.../web/ShopController.java`（只有注解与方法、无类声明，
   会导致测试编译失败）已移至 `.tdd/notes/stray-ShopController.java.draft`，未删除。
7. **未做 git 提交**（按要求）。

## 3. 已知问题与后续建议

| 优先级 | 问题 | 建议 |
| --- | --- | --- |
| 高 | Outbox 的 `payload` 里含明文密码（重试需要） | 改为加密负载或一次性凭据；至少限制该表访问权限 |
| 高 | 登录链路缺测试，且 `ShopServiceImpl.login` 仍用 `IllegalArgumentException` | 补登录契约/服务测试，并把登录类业务错误统一为 `BizException` |
| 中 | Guava `AsyncEventBus` 仍是进程内事件，重启会丢未入队的通知 | 评估替换为 MQ 或 `ApplicationEventPublisher` + 本地消息表 |
| 中 | `InitEmployeeEventHandle` 与 `InitShopMessageEventHandle` 用 `InitializingBean` 手动注册 | 改为 Spring 事件机制或显式初始化器 |
| 中 | 唯一性判断大小写敏感（SQLite BINARY） | 与产品确认语义，必要时按统一大小写/去空格后唯一 |
| 低 | `BaseResponseVO` 使用魔法数字 200/400/500 | 提取响应码枚举 |
| 低 | 部分历史 `[红灯]` 标记已从测试名移除，用例名与任务项编号仍需人工对齐 | 定期对照拆解文档更新状态 |

## 4. 复现命令

``powershell
$env:JAVA_HOME = 'D:\Program Files\Java\jdk17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd D:\AI_Coding\b2c

# 全量测试（使用工作区本地仓库；删除 .m2repo 后可用默认仓库）
$env:MAVEN_ARGS = '-Dmaven.repo.local=D:\AI_Coding\b2c\.m2repo'
mvn -B test

# 只跑注册相关
mvn -B test -pl shop -Dtest='Shop*Test'
``

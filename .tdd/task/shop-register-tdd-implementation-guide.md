---
title: "`注册功能 TDD 落地手册：分步操作步骤与代码`"
description: "配套 TDD 任务拆解文档的可执行手册：9 个阶段、每步的红灯测试代码、运行命令与绿灯实现代码"
author: "TDD 教练"
created: "2026-10-13"
updated: "2026-10-13"
version: "1.0.0"
status: "draft"
tags: ["tdd", "step-by-step", "register", "junit5", "mockito", "sqlite"]
category: "guide"
audience: ["backend-team", "qa"]
related:
  - "./shop-register-tdd-breakdown.md"
---

# 注册功能 TDD 落地手册：分步操作步骤与代码

> 本文是 [TDD 任务拆解：店铺（用户）用户名密码注册](./shop-register-tdd-breakdown.md) 的**执行手册**。
> 任务项编号（如 1.2.5）与拆解文档中的编号一一对应，可直接回溯需求。
> 每一步都遵循同一节奏：**写红灯测试 → 运行并观察失败原因 → 写最小实现 → 回归全绿 → 提交**。

## 目录

- [0. 本机环境核对结果](#0-本机环境核对结果)
- [使用说明](#使用说明)
- [阶段 0：前置检查与依赖补齐](#阶段-0前置检查与依赖补齐)
- [阶段 1：测试基座](#阶段-1测试基座)
- [阶段 2：F1 注册成功主流程](#阶段-2f1-注册成功主流程)
- [阶段 3：F2 入参校验](#阶段-3f2-入参校验)
- [阶段 4：F3 店铺名唯一性](#阶段-4f3-店铺名唯一性)
- [阶段 5：F4 管理员员工账号](#阶段-5f4-管理员员工账号)
- [阶段 6：F5 欢迎站内信](#阶段-6f5-欢迎站内信)
- [阶段 7：F7 网关路由](#阶段-7f7-网关路由)
- [阶段 8：F6 一致性、补偿与可观测](#阶段-8f6-一致性补偿与可观测)
- [阶段 9：重构（保持全绿）](#阶段-9重构保持全绿)
- [附录 A：命令速查](#附录-a命令速查)
- [附录 B：常见坑与排查](#附录-b常见坑与排查)
- [附录 C：阶段验收清单与提交信息](#附录-c阶段验收清单与提交信息)

---

## 0. 本机环境核对结果

写文档前我在当前机器上做了静态核对，结论如下（决定了后面代码的写法）：

| 项 | 核对结果 | 对方案的影响 |
| --- | --- | --- |
| Maven | 3.9.12，配置在 `D:\Maven\apache-maven-3.9.12` | 命令可直接使用 |
| JDK | 默认 `JAVA_HOME` 指向 jdk21，另有 `D:\Program Files\Java\jdk17` | **测试必须切到 JDK 17**，见阶段 0.1 与附录 B-4 |
| 本地仓库 | `~/.m2/repository` 里**没有** Spring Boot 2.7.18、`sqlite-jdbc` 等项目依赖 | 首次跑测试需能访问 Maven 中央仓库 |
| 中央仓库连通性 | 当前 shell 访问 `repo.maven.apache.org` 连接被关闭 | 本文命令**未在本机实际执行验证**，代码按项目现有 API 静态编写 |
| 测试代码/依赖 | 全部模块均无测试目录、无测试依赖 | 必须从阶段 0 开始 |
| 运行期外部依赖 | `application.yml` 指向 Nacos、Redis 与真实 `shop.db`/`employee.db` | **禁止用 `@SpringBootTest` 跑单测**，见下方测试策略 |

### 测试策略（贯穿全文的三个决定）

1. **不启动 Spring 容器**。Controller 用 `MockMvcBuilders.standaloneSetup`，Service/订阅者用纯 Mockito，
   DAO 用手工构建的 MyBatis `SqlSessionFactory`。这样可以完全绕开 Nacos、Redis 与真实数据库文件。
2. **只新增一个测试依赖**：`spring-boot-starter-test`。MyBatis、SQLite JDBC、Jackson、Guava、hutool
   已经在现有依赖里，不需要额外引入。
3. **DAO 测试用文件型 SQLite**（`target/test-db/*.db`），不用 `:memory:`。原因见附录 B-1。

---

## 使用说明

- **运行目录**：除特别说明外，命令都在项目根目录 `D:\AI_Coding\b2c` 执行。
- **文件路径**：新增测试文件一律放在对应模块的 `src/test/java` 与 `src/test/resources` 下。
- **命名约定**：测试类 `XxxTest`（单元/契约测试）、`XxxDaoTest`（SQL 集成测试）；
  测试方法用**英文**，中文说明放 `@DisplayName`（避免编码问题，见附录 B-10）。
- **每个步骤的三段式**：`红灯：测试代码` → `运行命令与预期输出` → `绿灯：实现代码`。
- **TDD 纪律**：没有先看到红灯，就不要写实现；红灯失败原因必须与预期一致，否则先修测试。

### 阶段总览

| 阶段 | 对应功能点 | 新增测试类（约） | 产出 |
| --- | --- | --- | --- |
| 0 | 前置 | 0 | 测试依赖、JDK 17 运行方式 |
| 1 | 附录 A | 1 | 测试基座（schema + DAO 基类） |
| 2 | F1 | 3 | 成功主流程可测 |
| 3 | F2 | 2 | 校验约束与错误响应 |
| 4 | F3 | 1 | 店铺名唯一性（含唯一索引） |
| 5 | F4 | 4 | 跨服务员工账号（含幂等） |
| 6 | F5 | 3 | 欢迎站内信 |
| 7 | F7 | 1 | 网关路由回归 |
| 8 | F6 | 3 | 事件可观测 + Outbox 补偿 |
| 9 | 重构 | 改写既有 | 错误码、BCrypt、事件下沉 |

---

## 阶段 0：前置检查与依赖补齐

### 0.1 切换 JDK 并验证工具链

```powershell
# 每个新终端都要执行一次（Spring Boot 2.7 自带的 Mockito 4.5.1 在 JDK 21 下会与 ByteBuddy 冲突）
$env:JAVA_HOME = 'D:\Program Files\Java\jdk17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

mvn -v
java -version
```

预期：`Java version: 17.x`。如果坚持用 JDK 21，请按附录 B-4 显式升级 mockito 与 byte-buddy。

### 0.2 补齐测试依赖

**操作 1**：在根 `pom.xml` 的 `dependencyManagement` 节点中加入版本管理（`spring-boot.version` 已存在）。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>${spring-boot.version}</version>
    <scope>test</scope>
</dependency>
```

**操作 2**：在 `shop/pom.xml`、`employee/pom.xml`、`gateway/pom.xml` 的 `dependencies` 节点末尾各加一段。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

`spring-boot-starter-test` 会带来 JUnit 5、Mockito、AssertJ、Hamcrest、spring-test 与 JsonPath，
本手册用到的全部测试 API 都在其中。

### 0.3 验证依赖可解析

```powershell
mvn -pl shop -am test-compile
```

预期：`BUILD SUCCESS`。若报找不到 `spring-boot-starter-test:2.7.18`，说明本机离线且无缓存，
需要先在能联网的环境执行 `mvn -pl shop -am test-compile` 预热本地仓库。

---

## 阶段 1：测试基座

对应拆解文档附录 A。目标：能连上一个干净的 SQLite 测试库并执行真实 SQL。

### 1.1 新建测试库 schema

**新增** `shop/src/test/resources/schema/shop-schema.sql`：

```sql
CREATE TABLE IF NOT EXISTS tb_shop
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_name       VARCHAR(100) NOT NULL,
    admin_account   VARCHAR(100) NOT NULL,
    admin_password  VARCHAR(100) NOT NULL,
    logo_url        VARCHAR(500),
    status          TINYINT  DEFAULT 1,
    last_login_time DATETIME,
    login_count     INTEGER  DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_messages
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id    INTEGER,
    sender_id  INTEGER,
    title      VARCHAR(200),
    content    TEXT,
    msg_type   TINYINT  DEFAULT 1,
    is_read    BOOLEAN  DEFAULT 0,
    read_time  DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shop_admin_account ON tb_shop (admin_account);
CREATE INDEX IF NOT EXISTS idx_messages_shop_id ON tb_messages (shop_id);
-- 唯一索引在阶段 4 才会加上，这里刻意保持与现状一致
```

**新增** `employee/src/test/resources/schema/employee-schema.sql`：

```sql
CREATE TABLE IF NOT EXISTS tb_employee
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id         INTEGER      NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(500) DEFAULT '/avatars/default.png',
    last_login_time DATETIME,
    login_count     INTEGER      DEFAULT 0,
    status          TINYINT      DEFAULT 1,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shop_id ON tb_employee (shop_id);
CREATE INDEX IF NOT EXISTS idx_employee_username ON tb_employee (username);
-- 唯一索引在阶段 5 才会加上
```

### 1.2 DAO 测试基类（手工构建 MyBatis，不启动 Spring）

**新增** `shop/src/test/java/com/yjx/b2c/shop/support/ShopDaoTestSupport.java`：

```java
package com.yjx.b2c.shop.support;

import com.yjx.b2c.shop.dao.mapper.MessageMapper;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DAO 层测试基座：每个测试类独享一个 SQLite 文件库，隔离真实 shop.db。
 */
public abstract class ShopDaoTestSupport {

    private static SqlSessionFactory sqlSessionFactory;

    protected SqlSession session;
    protected ShopMapper shopMapper;
    protected MessageMapper messageMapper;

    @BeforeAll
    static void createDatabase() throws Exception {
        Path dbFile = Path.of("target", "test-db", "shop-test.db");
        Files.createDirectories(dbFile.getParent());
        Files.deleteIfExists(dbFile);

        DataSource dataSource = new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite:" + dbFile, null);
        Configuration configuration = new Configuration(
                new Environment("test", new JdbcTransactionFactory(), dataSource));
        parseMapper(configuration, "mapper/ShopMapper.xml");
        parseMapper(configuration, "mapper/MessageMapper.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/shop-schema.sql"));
        }
    }

    private static void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @BeforeEach
    void openSession() throws Exception {
        session = sqlSessionFactory.openSession(true);
        shopMapper = session.getMapper(ShopMapper.class);
        messageMapper = session.getMapper(MessageMapper.class);
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("DELETE FROM tb_shop");
            statement.executeUpdate("DELETE FROM tb_messages");
        }
    }

    @AfterEach
    void closeSession() {
        if (session != null) {
            session.close();
        }
    }
}
```

要点说明：

- `parseMapper` 直接解析 `resources/mapper/*.xml`，因为项目里的 XML 不在接口同包路径下，
  不能用 `configuration.addMapper()` 自动发现。
- `openSession(true)` 表示自动提交，避免每个用例手工 commit。
- 不能用 `@MybatisTest`？可以，但要额外引 `mybatis-spring-boot-starter-test` 且必须
  `@AutoConfigureTestDatabase(replace = NONE)`，还要处理 Nacos 干扰。本手册选择零容器方案。

**新增** `employee/src/test/java/com/yjx/b2c/employee/support/EmployeeDaoTestSupport.java`（结构同上）：

```java
package com.yjx.b2c.employee.support;

import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
// ... 与 ShopDaoTestSupport 完全相同的 import 与逻辑，差异只有三处：

public abstract class EmployeeDaoTestSupport {

    private static SqlSessionFactory sqlSessionFactory;

    protected SqlSession session;
    protected EmployeeMapper employeeMapper;

    @BeforeAll
    static void createDatabase() throws Exception {
        Path dbFile = Path.of("target", "test-db", "employee-test.db");
        Files.createDirectories(dbFile.getParent());
        Files.deleteIfExists(dbFile);

        DataSource dataSource = new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite:" + dbFile, null);
        Configuration configuration = new Configuration(
                new Environment("test", new JdbcTransactionFactory(), dataSource));
        // 差异 1：换成 employee 的 mapper
        try (InputStream in = Resources.getResourceAsStream("mapper/EmployeeMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/EmployeeMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        // 差异 2：换成 employee 的 schema
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/employee-schema.sql"));
        }
    }

    @BeforeEach
    void openSession() throws Exception {
        session = sqlSessionFactory.openSession(true);
        employeeMapper = session.getMapper(EmployeeMapper.class);
        try (Statement statement = session.getConnection().createStatement()) {
            // 差异 3：清空员工表
            statement.executeUpdate("DELETE FROM tb_employee");
        }
    }

    @AfterEach
    void closeSession() {
        if (session != null) {
            session.close();
        }
    }
}
```

### 1.3 红灯：冒烟测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/dao/mapper/ShopMapperSmokeTest.java`：

```java
package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShopMapperSmokeTest extends ShopDaoTestSupport {

    @Test
    @DisplayName("测试基座可用：能连上 SQLite 测试库并执行计数查询")
    void mapperIsUsable() {
        assertThat(shopMapper.exist("不存在")).isZero();
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=ShopMapperSmokeTest
```

预期输出：`Tests run: 1, Failures: 0, Errors: 0`。
若报 `no sqlite in java.library.path` 或找不到 driver，检查 `sqlite-jdbc` 是否已下载（阶段 0.3）。
⚠️ 注意：DAO 测试必须把工作目录设为模块目录，surefire 默认就是模块目录，用 IDE 跑时请确认
工作目录为 `shop`，否则 `target/test-db` 会建到别处。

### 1.4 阶段验收

- [ ] `mvn -pl shop -am test` 能跑起来且冒烟测试通过
- [ ] `shop.db` / `employee.db` 的修改时间没有变化（测试没有污染真实库）

---

## 阶段 2：F1 注册成功主流程

对应任务项 1.1.1 - 1.4.5。

### 2.1 红灯：Service 层业务编排测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/service/impl/ShopServiceImplTest.java`：

```java
package com.yjx.b2c.shop.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private ShopServiceImpl shopService;

    private static ShopRegisterRequestVO request(String shopName, String account, String password) {
        ShopRegisterRequestVO vo = new ShopRegisterRequestVO();
        vo.setShopName(shopName);
        vo.setAdminAccount(account);
        vo.setAdminPassword(password);
        return vo;
    }

    @Test
    @DisplayName("1.3.1/1.3.2/1.3.3/1.3.5 店铺名不存在时注册成功并返回自增主键")
    void registerSuccess() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenAnswer(invocation -> {
            ShopPO po = invocation.getArgument(0);
            po.setId(100);
            return 1;
        });

        Integer id = shopService.register(request("测试店铺", "admin", "123456"));

        assertThat(id).isEqualTo(100);
        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopMapper).registerShop(captor.capture());
        ShopPO saved = captor.getValue();
        assertThat(saved.getShopName()).isEqualTo("测试店铺");
        assertThat(saved.getAdminAccount()).isEqualTo("admin");
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("1.3.7 当前明文语义：密码以请求值原样写入（加密改造的锚点）")
    void registerKeepsPlainPassword() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenReturn(1);

        shopService.register(request("测试店铺", "admin", "123456"));

        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopMapper).registerShop(captor.capture());
        assertThat(captor.getValue().getAdminPassword()).isEqualTo("123456");
    }

    @Test
    @DisplayName("1.3.4 落库影响行数不为 1 时抛出业务异常")
    void registerFailsWhenRowCountIsZero() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenReturn(0);

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("保存到数据库异常");
    }

    @Test
    @DisplayName("1.3.6 绕过 Controller 直接调用 Service，空白密码仍被拒绝")
    void registerRejectsBlankPassword() {
        // 注意：唯一性查询在校验之前执行，这是当前实现的顺序（见本阶段观察结论）
        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "   ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(shopMapper, never()).registerShop(any(ShopPO.class));
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=ShopServiceImplTest
```

观察结论（务必先跑一次再往下看）：

- `registerSuccess`、`registerKeepsPlainPassword`、`registerFailsWhenRowCountIsZero` 应当**一次就绿**，
  因为它们锁定的是已完成的行为（特性测试）。
- 若 `registerRejectsBlankPassword` 报 `verify(shopMapper).registerShop` 被调用，说明空白校验位置不对。
- **新发现的重构点**：`ShopServiceImpl.register` 先执行 `exist(shopName)` 再做 `notBlank` 校验，
  空白店铺名会白跑一次数据库查询。用测试锁住后，在阶段 9 把校验提到查询之前。

### 2.2 红灯：Controller 契约测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/web/ShopControllerTest.java`：

```java
package com.yjx.b2c.shop.web;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import com.yjx.b2c.shop.web.aspect.MethodArgNotValidExceptionAspect;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShopControllerTest {

    private static final String VALID_BODY =
            "{\"shopName\":\"测试店铺\",\"adminAccount\":\"admin\",\"adminPassword\":\"123456\"}";

    private ShopService shopService;
    private EventBus eventBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        eventBus = mock(EventBus.class);

        ShopController controller = new ShopController();
        ReflectionTestUtils.setField(controller, "shopService", shopService);
        ReflectionTestUtils.setField(controller, "eventBus", eventBus);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MethodArgNotValidExceptionAspect())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("1.1.1/1.2.1 POST /shop/register 接受 JSON 并返回业务码 200")
    void registerReturnsOk() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("1.2.1 非 POST 方法访问注册路径返回 405")
    void getRegisterIsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/shop/register")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("1.1.1 未声明 Content-Type 时不得返回业务成功码")
    void missingContentTypeIsRejected() throws Exception {
        mockMvc.perform(post("/shop/register").content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("1.2.2 请求字段完整映射到 VO 并传递给 Service")
    void requestIsDeserializedIntoVo() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<ShopRegisterRequestVO> captor = ArgumentCaptor.forClass(ShopRegisterRequestVO.class);
        verify(shopService).register(captor.capture());
        ShopRegisterRequestVO vo = captor.getValue();
        assertThat(vo.getShopName()).isEqualTo("测试店铺");
        assertThat(vo.getAdminAccount()).isEqualTo("admin");
        assertThat(vo.getAdminPassword()).isEqualTo("123456");
    }

    @Test
    @DisplayName("1.2.5 注册成功后发布 ShopRegisterEvent 且字段完整")
    void registerPublishesEvent() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus).post(captor.capture());
        ShopRegisterEvent event = (ShopRegisterEvent) captor.getValue();
        assertThat(event.getShopId()).isEqualTo(100);
        assertThat(event.getShopName()).isEqualTo("测试店铺");
        assertThat(event.getAdminAccount()).isEqualTo("admin");
        assertThat(event.getAdminPwd()).isEqualTo("123456");
    }

    @Test
    @DisplayName("1.2.4 Service 抛业务异常时响应业务码 400 且保留原始消息")
    void serviceFailureBecomesBadRequest() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class)))
                .thenThrow(new IllegalArgumentException("店铺名已存在"));

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("店铺名已存在"));
    }

    @Test
    @DisplayName("1.2.6 落库失败时不发布任何事件")
    void noEventWhenRegisterFails() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class)))
                .thenThrow(new IllegalArgumentException("保存到数据库异常"));

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(400));

        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("1.1.4 [红灯] 成功响应应回传新建店铺 ID")
    void registerReturnsShopId() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.data").value(100));
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=ShopControllerTest
```

预期：**7 绿 1 红**。红灯是 `registerReturnsShopId`，失败信息类似
`No value at JSON path "$.data"`，因为当前实现返回的是 `new BaseResponseVO<>()`。

### 2.3 绿灯：让响应回传 shopId

**修改** `shop/src/main/java/com/yjx/b2c/shop/web/ShopController.java`：

```java
    @PostMapping("/register")
    public BaseResponseVO<Integer> register(@RequestBody @Validated ShopRegisterRequestVO req) {
        Integer id = shopService.register(req);

        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(id);
        event.setShopName(req.getShopName());
        event.setAdminAccount(req.getAdminAccount());
        event.setAdminPwd(req.getAdminPassword());
        eventBus.post(event);
        return new BaseResponseVO<>(id);   // 原来是 new BaseResponseVO<>()，data 为 null
    }
```

回归：

```powershell
mvn -pl shop -am test -Dtest=ShopControllerTest
```

预期：8 个用例全绿。注意这是**对外契约变更**（响应体 dari `data:null` 变为 `data:店铺ID`），
需要同步通知调用方；如果调用方依赖 `data` 为空来判成功，请改用 `status` 判断。

### 2.4 红灯：DAO 层持久化测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/dao/mapper/ShopMapperTest.java`：

```java
package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopMapperTest extends ShopDaoTestSupport {

    private ShopPO po(String shopName, String account) {
        ShopPO po = new ShopPO();
        po.setShopName(shopName);
        po.setAdminAccount(account);
        po.setAdminPassword("123456");
        po.setStatus(1);
        return po;
    }

    @Test
    @DisplayName("1.4.1/1.4.2 插入 tb_shop 并回填自增主键")
    void insertAndBackfillId() {
        ShopPO po = po("测试店铺", "admin");

        assertThat(shopMapper.registerShop(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull().isPositive();

        ShopPO saved = shopMapper.findByAccount("admin");
        assertThat(saved.getShopName()).isEqualTo("测试店铺");
        assertThat(saved.getAdminPassword()).isEqualTo("123456");
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("1.4.4 created_at / updated_at 由数据库默认值兜底")
    void timestampsHaveDefaults() throws Exception {
        shopMapper.registerShop(po("测试店铺", "admin"));

        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT created_at, updated_at FROM tb_shop WHERE shop_name = '测试店铺'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("created_at")).isNotBlank();
            assertThat(rs.getString("updated_at")).isNotBlank();
        }
    }

    @Test
    @DisplayName("1.4.3 shop_name 为 NULL 时插入必须失败")
    void shopNameIsNotNull() {
        ShopPO po = po("测试店铺", "admin");
        po.setShopName(null);

        assertThatThrownBy(() -> shopMapper.registerShop(po)).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("1.4.5 管理员账号索引存在（登录链路回归保护）")
    void adminAccountIndexExists() throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'index' "
                             + "AND name = 'idx_shop_admin_account'")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    @DisplayName("3.2.1 exist 查询语义正确")
    void existCountsExactly() {
        assertThat(shopMapper.exist("测试店铺")).isZero();
        shopMapper.registerShop(po("测试店铺", "admin"));
        assertThat(shopMapper.exist("测试店铺")).isEqualTo(1);
        assertThat(shopMapper.exist("别的店铺")).isZero();
    }

    @Test
    @DisplayName("3.2.2 大小写敏感语义固化：ShopA 与 shopa 视为不同店铺")
    void shopNameComparisonIsCaseSensitive() {
        shopMapper.registerShop(po("ShopA", "admin1"));

        assertThat(shopMapper.exist("shopa")).isZero();
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=ShopMapperTest
```

预期：`1.4.3` 首次可能因异常类型不符而报错——先看实际异常类型，再把 `Exception.class`
收窄为真实类型（通常是 `org.apache.ibatis.exceptions.PersistenceException` 包装 `SQLiteException`）。
这正是 TDD 里「先观察，再断言」的正常动作。

### 2.5 阶段验收

- [ ] `mvn -pl shop -am test` 全绿
- [ ] 已确认 `data` 回传店铺 ID 的契约变更对外可见

建议提交：`test(shop): 补注册主流程契约/服务/DAO 测试并回传 shopId`

---

## 阶段 3：F2 入参校验

对应任务项 2.1.1 - 2.3.2。

### 3.1 红灯：校验契约测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/web/ShopRegisterValidationTest.java`：

```java
package com.yjx.b2c.shop.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.web.aspect.MethodArgNotValidExceptionAspect;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShopRegisterValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ShopService shopService;
    private EventBus eventBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        eventBus = mock(EventBus.class);

        ShopController controller = new ShopController();
        ReflectionTestUtils.setField(controller, "shopService", shopService);
        ReflectionTestUtils.setField(controller, "eventBus", eventBus);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MethodArgNotValidExceptionAspect())
                .setValidator(validator)
                .build();
    }

    private String body(String shopName, String account, String password) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("shopName", shopName);
        map.put("adminAccount", account);
        map.put("adminPassword", password);
        return objectMapper.writeValueAsString(map);   // null 也会被写出
    }

    private void expectRejected(String json, String expectedMessagePart) throws Exception {
        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString(expectedMessagePart)));
    }

    @Test
    @DisplayName("2.1.1 店铺名为 null / 空串 / 纯空白都返回 400")
    void blankShopNameRejected() throws Exception {
        for (String bad : new String[]{null, "", "   "}) {
            expectRejected(body(bad, "admin", "123456"), "店铺名不能为空");
        }
    }

    @Test
    @DisplayName("2.1.2 管理员账号为空返回 400")
    void blankAccountRejected() throws Exception {
        expectRejected(body("测试店铺", "  ", "123456"), "账户不能为空");
    }

    @Test
    @DisplayName("2.1.3 管理员密码为空返回 400")
    void blankPasswordRejected() throws Exception {
        expectRejected(body("测试店铺", "admin", ""), "账户密码");
    }

    @Test
    @DisplayName("2.1.4 店铺名长度上界为 6：6 通过、7 拒绝")
    void shopNameLengthBoundary() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("一二三四五六", "admin", "123456")))
                .andExpect(jsonPath("$.status").value(200));

        expectRejected(body("一二三四五六七", "admin", "123456"), "店铺名过长");
    }

    @Test
    @DisplayName("2.1.5 [红灯] 管理员账号超长应被拒绝")
    void accountTooLongRejected() throws Exception {
        expectRejected(body("测试店铺", "a".repeat(101), "123456"), "账户");
    }

    @Test
    @DisplayName("2.1.6 [红灯] 密码少于 6 位应被拒绝")
    void passwordTooShortRejected() throws Exception {
        expectRejected(body("测试店铺", "admin", "12345"), "密码");
    }

    @Test
    @DisplayName("2.2.2 多个字段同时非法时聚合全部提示")
    void multipleErrorsAggregated() throws Exception {
        expectRejected(body("", "", ""), "店铺名不能为空");
        expectRejected(body("", "", ""), "账户不能为空");
    }

    @Test
    @DisplayName("2.2.4 校验失败时不得调用 Service、不得发事件")
    void noServiceCallWhenInvalid() throws Exception {
        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "admin", "123456")))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(shopService);
        verify(eventBus, org.mockito.Mockito.never()).post(any());
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=ShopRegisterValidationTest
```

预期：6 绿 2 红。红灯为 `accountTooLongRejected` 与 `passwordTooShortRejected`，
因为当前 VO 只有 `@NotBlank`。这就是拆解文档里的 G2。

### 3.2 绿灯：补齐 VO 约束

**修改** `shop/src/main/java/com/yjx/b2c/shop/web/request/ShopRegisterRequestVO.java`：

```java
@Data
public class ShopRegisterRequestVO {
    @NotBlank(message = "店铺名不能为空")
    @Length(max = 6, message = "店铺名过长")
    private String shopName;

    @NotBlank(message = "账户不能为空")
    @Length(max = 100, message = "账户过长")
    private String adminAccount;

    @NotBlank(message = "账户密码")
    @Length(min = 6, max = 100, message = "密码长度必须在 6 到 100 位之间")
    private String adminPassword;
}
```

回归：

```powershell
mvn -pl shop -am test -Dtest=ShopRegisterValidationTest
```

预期：8 绿。同时确认阶段 2 的 `ShopControllerTest` 仍然全绿（`123456` 正好 6 位，不受影响）。

### 3.3 绿灯：Service 兜底断言（对应 2.3.1 / 2.3.2）

当前实现已满足，只需保留阶段 2.1 中的 `registerRejectsBlankPassword`。
在阶段 9 重构时，把 `notBlank` 断言提到 `exist` 查询之前。

### 3.4 阶段验收

- [ ] 两个红灯用例转绿，其余用例无回归
- [ ] 校验失败时 Service 与 EventBus 零交互（可用 Mockito 的 `verifyNoInteractions` 证明）

建议提交：`test(shop): 锁住注册入参校验边界并补齐长度约束`

---

## 阶段 4：F3 店铺名唯一性

对应任务项 3.1.1 - 3.3.3。

### 4.1 绿灯：Service 层唯一性（现有行为）

先用测试锁住已有行为，**追加**到 `ShopServiceImplTest`：

```java
    @Test
    @DisplayName("3.1.2 店铺名已存在时抛出业务异常且不落库")
    void registerRejectsDuplicatedShopName() {
        when(shopMapper.exist("测试店铺")).thenReturn(1);

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("店铺名已存在");
        verify(shopMapper, never()).registerShop(any(ShopPO.class));
    }
```

预期：一次就绿。它证明「应用层查重」存在，但不能防止并发。

### 4.2 红灯：并发下的重复注册（DAO 层等价覆盖）

**追加**到 `ShopMapperTest`：

```java
    @Test
    @DisplayName("3.3.2 [红灯] 同名店铺只能写入一条")
    void duplicateShopNameRejectedByDatabase() {
        shopMapper.registerShop(po("测试店铺", "admin1"));

        assertThatThrownBy(() -> shopMapper.registerShop(po("测试店铺", "admin2")))
                .hasMessageContaining("UNIQUE constraint failed");
    }
```

运行：

```powershell
mvn -pl shop -am test -Dtest=ShopMapperTest
```

预期：**红灯**——两次插入都成功，第二次没有任何异常。
说明应用层的 `exist` 查询在并发下形同虚设（两个请求可能同时查到 0）。

关于「真并发」用例：SQLite 文件库在双写时会抛 `SQLITE_BUSY`，写出来的并发测试往往不稳定。
**建议做法**：用本用例（DAO 级重复插入）等价覆盖并发风险，把正确性交给数据库唯一约束；
如果将来换 MySQL，再启用 `ExecutorService` 双线程用例（见附录 B-9）。

### 4.3 绿灯：加唯一索引

**修改** `shop.sql`（生产脚本）与 `shop/src/test/resources/schema/shop-schema.sql`（测试脚本），
在两处都补上：

```sql
-- 店铺名唯一：防并发重复注册
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_shop_name ON tb_shop (shop_name);
```

**对已存在的 `shop.db` 执行迁移**（先在 SQLite 客户端或 IDE 数据库工具里查重：

`SELECT shop_name, COUNT(*) FROM tb_shop GROUP BY shop_name HAVING COUNT(*) > 1;`，
有重复数据必须先人工合并/清理，否则建索引会失败）：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_shop_name ON tb_shop (shop_name);
```

回归：

```powershell
mvn -pl shop -am test -Dtest=ShopMapperTest
```

预期：转绿。第一次跑请留意实际异常类型：MyBatis-Spring 通常包装成
`org.springframework.dao.DataAccessException`（常见为 `UncategorizedSQLException`），
消息里包含 `UNIQUE constraint failed: tb_shop.shop_name`。因此断言用**消息**匹配，不要断言具体子类。

### 4.4 红灯到绿灯：把约束异常翻译成业务错误

**追加**到 `ShopServiceImplTest`：

```java
    @Test
    @DisplayName("3.3.3 数据库唯一约束冲突转成业务错误「店铺名已存在」")
    void duplicateKeyTranslatedToBusinessError() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException(
                        "UNIQUE constraint failed: tb_shop.shop_name"));

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("店铺名已存在");
    }
```

先运行看红灯，再**修改** `ShopServiceImpl.register`：

```java
        int row;
        try {
            row = shopMapper.registerShop(shopPO);
        } catch (org.springframework.dao.DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new IllegalArgumentException("店铺名已存在");
            }
            throw e;
        }
        Assert.equals(row, 1, "保存到数据库异常");
```

回归：

```powershell
mvn -pl shop -am test
```

### 4.5 阶段验收

- [ ] 同名插入被数据库拒绝，冲突被翻译为 400「店铺名已存在」
- [ ] `shop.db` 已建唯一索引（记录执行时间与执行人）

建议提交：`fix(shop): 店铺名唯一索引并将约束冲突翻译为业务错误`

---

## 阶段 5：F4 管理员员工账号

对应任务项 4.1.1 - 4.5.4。

### 5.1 红灯：订阅者行为测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/service/event/handler/InitEmployeeEventHandleTest.java`：

```java
package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.feign.EmployeeFeignClient;
import com.yjx.b2c.shop.feign.request.AddEmployeeRequestVO;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitEmployeeEventHandleTest {

    private EventBus eventBus;
    private EmployeeFeignClient employeeFeignClient;
    private InitEmployeeEventHandle handle;

    @BeforeEach
    void setUp() throws Exception {
        eventBus = new EventBus();   // 同步事件总线，避免 Thread.sleep（拆解文档 A.2.5）
        employeeFeignClient = mock(EmployeeFeignClient.class);

        handle = new InitEmployeeEventHandle();
        ReflectionTestUtils.setField(handle, "eventBus", eventBus);
        ReflectionTestUtils.setField(handle, "employeeFeignClient", employeeFeignClient);
        handle.afterPropertiesSet();   // 相当于容器启动时的注册动作
    }

    private ShopRegisterEvent event() {
        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(88);
        event.setShopName("测试店铺");
        event.setAdminAccount("admin");
        event.setAdminPwd("123456");
        return event;
    }

    @Test
    @DisplayName("4.2.1/4.2.2/4.2.3 收到注册事件后按契约调用 employee 服务")
    void callsEmployeeService() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class))).thenReturn(new BaseResponseVO<>(7));

        eventBus.post(event());

        ArgumentCaptor<AddEmployeeRequestVO> captor = ArgumentCaptor.forClass(AddEmployeeRequestVO.class);
        verify(employeeFeignClient).save(captor.capture());
        AddEmployeeRequestVO req = captor.getValue();
        assertThat(req.getShopId()).isEqualTo(88);
        assertThat(req.getUsername()).isEqualTo("admin");
        assertThat(req.getPassword()).isEqualTo("123456");
    }

    @Test
    @DisplayName("4.2.4 employee 返回非 200 时不抛异常（不影响主流程）")
    void nonSuccessResponseDoesNotThrow() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class)))
                .thenReturn(new BaseResponseVO<>(500, "保存用户失败"));

        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=InitEmployeeEventHandleTest
```

预期：两个用例一次就绿（现有实现已满足 4.2.1 - 4.2.4）。这是**特性测试**，
它的价值在于把「事件注册 + 字段映射」固化成回归网。

### 5.2 红灯：Feign 契约形状测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/feign/EmployeeFeignContractTest.java`：

```java
package com.yjx.b2c.shop.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.feign.request.AddEmployeeRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeFeignContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("4.3.1 Feign 客户端指向 employee-service 的 /employee")
    void feignClientAnnotation() {
        FeignClient annotation = EmployeeFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("employee-service");
        assertThat(annotation.path()).isEqualTo("/employee");
    }

    @Test
    @DisplayName("4.3.2 save 方法声明为 POST /save")
    void saveMethodMapping() throws Exception {
        PostMapping mapping = EmployeeFeignClient.class
                .getMethod("save", AddEmployeeRequestVO.class)
                .getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/save");
    }

    @Test
    @DisplayName("4.3.2 请求体 JSON 恰好是 shopId/username/password 三个键")
    void requestBodyShape() throws Exception {
        AddEmployeeRequestVO req = new AddEmployeeRequestVO();
        req.setShopId(88);
        req.setUsername("admin");
        req.setPassword("123456");

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(req));

        assertThat(node.size()).isEqualTo(3);
        assertThat(node.get("shopId").asInt()).isEqualTo(88);
        assertThat(node.get("username").asText()).isEqualTo("admin");
        assertThat(node.get("password").asText()).isEqualTo("123456");
    }

    @Test
    @DisplayName("4.3.3 响应可反序列化为 BaseResponseVO<Integer>")
    void responseShape() throws Exception {
        BaseResponseVO<Integer> resp = objectMapper.readValue(
                "{\"status\":200,\"message\":null,\"data\":7}",
                objectMapper.getTypeFactory().constructType(
                        new com.fasterxml.jackson.core.type.TypeReference<BaseResponseVO<Integer>>() {
                        }.getType()));

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(resp.getData()).isEqualTo(7);
    }
}
```

说明：这里用 Jackson 直接验证**线格式**，不引入 WireMock/MockWebServer。
如果团队后续需要真实 HTTP 级别的契约验证，再补 WireMock（可选，非本手册必需）。
`import java.lang.reflect.Type;` 若未使用可删除。

运行：

```powershell
mvn -pl shop -am test -Dtest=EmployeeFeignContractTest
```

预期：4 绿。若 `requestBodyShape` 红灯，说明有人给 VO 加了多余字段，需要评估是否影响 employee 侧。

### 5.3 红灯：employee 侧保存接口

**新增** `employee/src/test/java/com/yjx/b2c/employee/web/EmployeeControllerTest.java`：

```java
package com.yjx.b2c.employee.web;

import com.yjx.b2c.employee.service.EmployeeService;
import com.yjx.b2c.employee.web.aspect.GlobalExceptionHandler;
import com.yjx.b2c.employee.web.request.AddEmployeeRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeControllerTest {

    private static final String BODY =
            "{\"shopId\":88,\"username\":\"admin\",\"password\":\"123456\"}";

    private EmployeeService employeeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);

        EmployeeController controller = new EmployeeController();
        ReflectionTestUtils.setField(controller, "employeeService", employeeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("4.4.1 /employee/save 返回新建员工 ID")
    void saveReturnsId() throws Exception {
        when(employeeService.addEmployee(any(AddEmployeeRequestVO.class))).thenReturn(7);

        mockMvc.perform(post("/employee/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(7));

        ArgumentCaptor<AddEmployeeRequestVO> captor = ArgumentCaptor.forClass(AddEmployeeRequestVO.class);
        verify(employeeService).addEmployee(captor.capture());
        assertThat(captor.getValue().getShopId()).isEqualTo(88);
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("4.4.1 缺少 shopId 时 Service 抛异常，接口返回 400")
    void missingShopIdBecomesBadRequest() throws Exception {
        when(employeeService.addEmployee(any(AddEmployeeRequestVO.class)))
                .thenThrow(new IllegalArgumentException("保存员工信息失败"));

        mockMvc.perform(post("/employee/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
    }
}
```

**新增** `employee/src/test/java/com/yjx/b2c/employee/service/impl/EmployeeServiceImplTest.java`：

```java
package com.yjx.b2c.employee.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.web.request.AddEmployeeRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private static AddEmployeeRequestVO request(Integer shopId, String username, String password) {
        AddEmployeeRequestVO vo = new AddEmployeeRequestVO();
        vo.setShopId(shopId);
        vo.setUsername(username);
        vo.setPassword(password);
        return vo;
    }

    @Test
    @DisplayName("4.4.3/4.4.4 保存员工时写入默认状态与默认头像")
    void addEmployeeUsesDefaults() {
        when(employeeMapper.findByUsername("admin", 88)).thenReturn(null);
        when(employeeMapper.save(any(EmployeePO.class))).thenAnswer(invocation -> {
            EmployeePO po = invocation.getArgument(0);
            po.setId(7);
            return 1;
        });

        Integer id = employeeService.addEmployee(request(88, "admin", "123456"));

        assertThat(id).isEqualTo(7);
        ArgumentCaptor<EmployeePO> captor = ArgumentCaptor.forClass(EmployeePO.class);
        verify(employeeMapper).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getAvatarUrl()).isNotBlank();
    }

    @Test
    @DisplayName("4.4.2 缺少 shopId 时拒绝且不落库")
    void addEmployeeRejectsMissingShopId() {
        assertThatThrownBy(() -> employeeService.addEmployee(request(null, "admin", "123456")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(employeeMapper, never()).save(any(EmployeePO.class));
    }
}
```

运行：

```powershell
mvn -pl employee -am test
```

预期：`addEmployeeUsesDefaults` 首次**红灯**——现有 `addEmployee` 不查重，所以
`when(employeeMapper.findByUsername(...)).thenReturn(null)` 会成为「未使用的桩」而报
`UnnecessaryStubbingException`。这正好是下一步 5.4 要加幂等查重的驱动力。

### 5.4 绿灯：员工账号幂等

**先加红灯 DAO 用例**，追加到 employee 的 Mapper 测试（新建
`employee/src/test/java/com/yjx/b2c/employee/dao/mapper/EmployeeMapperTest.java`，
继承 `EmployeeDaoTestSupport`）：

```java
    @Test
    @DisplayName("4.5.1/4.5.2 保存员工并回填主键")
    void saveAndBackfillId() {
        EmployeePO po = new EmployeePO();
        po.setShopId(88);
        po.setUsername("admin");
        po.setPassword("123456");
        po.setAvatarUrl("/avatars/default.png");
        po.setStatus(1);

        assertThat(employeeMapper.save(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull().isPositive();

        EmployeePO saved = employeeMapper.findByUsername("admin", 88);
        assertThat(saved.getShopId()).isEqualTo(88);
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("4.5.3 findByUsername 在 shopId 为 null 时忽略该条件")
    void findByUsernameWithoutShopId() {
        // 先插入两条不同店铺的同名账号
        // ...
        assertThat(employeeMapper.findByUsername("admin", null)).isNotNull();
    }

    @Test
    @DisplayName("4.4.5 [红灯] 同一店铺同一账号只能存在一条")
    void duplicateEmployeeRejected() {
        EmployeePO first = employee(88, "admin");
        employeeMapper.save(first);

        assertThatThrownBy(() -> employeeMapper.save(employee(88, "admin")))
                .hasMessageContaining("UNIQUE constraint failed");
    }
```

**绿灯 1**：在 `employee.sql` 与 `employee/src/test/resources/schema/employee-schema.sql` 补：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_shop_username ON tb_employee (shop_id, username);
```

**绿灯 2**：给 `EmployeeServiceImpl.addEmployee` 加幂等（对应 4.4.5）：

```java
    @Override
    public Integer addEmployee(AddEmployeeRequestVO vo) {
        Assert.notBlank(vo.getUsername());
        Assert.notNull(vo.getShopId());
        Assert.notBlank(vo.getPassword());

        // 幂等：事件重放/补偿重试时返回既有账号，避免重复创建
        EmployeePO existing = employeeMapper.findByUsername(vo.getUsername(), vo.getShopId());
        if (existing != null) {
            return existing.getId();
        }
        // ... 其余逻辑不变
    }
```

**追加幂等单测**到 `EmployeeServiceImplTest`：

```java
    @Test
    @DisplayName("4.4.5 重复创建返回既有员工 ID，不新增记录")
    void addEmployeeIsIdempotent() {
        EmployeePO existing = new EmployeePO();
        existing.setId(7);
        when(employeeMapper.findByUsername("admin", 88)).thenReturn(existing);

        assertThat(employeeService.addEmployee(request(88, "admin", "123456"))).isEqualTo(7);
        verify(employeeMapper, never()).save(any(EmployeePO.class));
    }
```

回归：

```powershell
mvn -pl employee -am test
mvn -pl shop -am test
```

### 5.5 阶段验收

- [ ] 注册事件能驱动 employee 建号，字段映射有断言保护
- [ ] 同名员工重复创建返回既有 ID，数据库有唯一索引兜底
- [ ] `employee.db` 已建唯一索引

建议提交：`feat(employee): 员工账号幂等 + 唯一索引，补跨服务契约测试`

---

## 阶段 6：F5 欢迎站内信

对应任务项 5.1.1 - 5.3.2。

### 6.1 红灯：订阅者与 Service 测试

**新增** `shop/src/test/java/com/yjx/b2c/shop/service/event/handler/InitShopMessageEventHandleTest.java`：

```java
package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.service.MessageService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InitShopMessageEventHandleTest {

    private EventBus eventBus;
    private MessageService messageService;
    private InitShopMessageEventHandle handle;

    @BeforeEach
    void setUp() throws Exception {
        eventBus = new EventBus();
        messageService = mock(MessageService.class);

        handle = new InitShopMessageEventHandle();
        ReflectionTestUtils.setField(handle, "eventBus", eventBus);
        ReflectionTestUtils.setField(handle, "messageService", messageService);
        handle.afterPropertiesSet();
    }

    private ShopRegisterEvent event() {
        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(88);
        event.setShopName("测试店铺");
        return event;
    }

    @Test
    @DisplayName("5.1.1/5.1.2 收到事件后写入欢迎站内信")
    void savesWelcomeMessage() {
        eventBus.post(event());

        verify(messageService).saveWeclomeMessage(88, "欢迎注册本系统", "系统规范，系统规约");
    }

    @Test
    @DisplayName("5.1.3 站内信写入失败不应影响注册主流程")
    void messageFailureIsIsolated() {
        doThrow(new IllegalStateException("db down"))
                .when(messageService).saveWeclomeMessage(88, "欢迎注册本系统", "系统规范，系统规约");

        // Guava EventBus 会为每个订阅者单独捕获异常，因此不会向调用方抛出
        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();
    }
}
```

**新增** `shop/src/test/java/com/yjx/b2c/shop/service/impl/MessageServiceImplTest.java`：

```java
package com.yjx.b2c.shop.service.impl;

import com.yjx.b2c.shop.dao.mapper.MessageMapper;
import com.yjx.b2c.shop.dao.po.MessagePO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageMapper messageMapper;
    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    @DisplayName("5.2.2 写入欢迎消息时 msgType=1、isRead=0")
    void savesWelcomeMessageWithDefaults() {
        when(messageMapper.saveMessage(any(MessagePO.class))).thenReturn(1);

        messageService.saveWeclomeMessage(88, "欢迎注册本系统", "系统规范，系统规约");

        ArgumentCaptor<MessagePO> captor = ArgumentCaptor.forClass(MessagePO.class);
        verify(messageMapper).saveMessage(captor.capture());
        assertThat(captor.getValue().getMsgType()).isEqualTo(1);
        assertThat(captor.getValue().getIsRead()).isZero();
    }

    @Test
    @DisplayName("5.2.1 店铺 ID 为空时拒绝写入")
    void rejectsMissingShopId() {
        assertThatThrownBy(() -> messageService.saveWeclomeMessage(null, "标题", "内容"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("5.2.2 影响行数不为 1 时抛出业务异常")
    void failsWhenRowCountIsZero() {
        when(messageMapper.saveMessage(any(MessagePO.class))).thenReturn(0);

        assertThatThrownBy(() -> messageService.saveWeclomeMessage(88, "标题", "内容"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("保存欢迎消息异常");
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest='*Message*'
```

预期：一次全绿（现有实现已满足）；这是特性测试，用来防止后续重构改坏消息文案与默认值。

### 6.2 追加 DAO 用例

**追加**到 `ShopMapperTest`（或新建 `MessageMapperTest` 继承同一个基类）：

```java
    @Test
    @DisplayName("5.3.1 站内信落库列与默认值正确")
    void insertMessageColumns() throws Exception {
        MessagePO po = new MessagePO();
        po.setShopId(88);
        po.setTitle("欢迎注册本系统");
        po.setContent("系统规范，系统规约");
        po.setMsgType(1);
        po.setIsRead(0);

        assertThat(messageMapper.saveMessage(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull();

        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT shop_id, msg_type, is_read FROM tb_messages")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("shop_id")).isEqualTo(88);
            assertThat(rs.getInt("msg_type")).isEqualTo(1);
            assertThat(rs.getInt("is_read")).isZero();
        }
    }

    @Test
    @DisplayName("5.3.2 shop_id 索引存在")
    void shopIdIndexExists() throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'idx_messages_shop_id'")) {
            assertThat(rs.next()).isTrue();
        }
    }
```

### 6.3 阶段验收

- [ ] 欢迎消息的文案、`msg_type`、`is_read` 都被断言锁定
- [ ] 站内信失败被隔离，不影响员工账号创建

建议提交：`test(shop): 锁住注册欢迎站内信链路`

---

## 阶段 7：F7 网关路由

对应任务项 7.1.1 - 7.2.2。

### 7.1 红灯：路由配置回归测试

**新增** `gateway/src/test/java/com/yjx/b2c/gateway/GatewayRouteConfigTest.java`：

```java
package com.yjx.b2c.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteConfigTest {

    private static final int MAX_ROUTES = 20;

    private PropertySource<?> applicationConfig() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        return sources.get(0);
    }

    /** 找到声明了该 Path 断言的路由目标地址 */
    private Optional<String> targetOf(PropertySource<?> source, String path) {
        for (int i = 0; i < MAX_ROUTES; i++) {
            Object predicate = source.getProperty("spring.cloud.gateway.routes[" + i + "].predicates[0]");
            if (predicate == null) {
                return Optional.empty();
            }
            if (("Path=" + path).equals(String.valueOf(predicate))) {
                return Optional.of(String.valueOf(
                        source.getProperty("spring.cloud.gateway.routes[" + i + "].uri")));
            }
        }
        return Optional.empty();
    }

    @Test
    @DisplayName("7.1.1 /shop/** 路由到 lb://shop-service")
    void shopRouteTargetsShopService() throws Exception {
        assertThat(targetOf(applicationConfig(), "/shop/**")).contains("lb://shop-service");
    }

    @Test
    @DisplayName("7.1.2 /employee/** 路由到 lb://employee-service")
    void employeeRouteTargetsEmployeeService() throws Exception {
        assertThat(targetOf(applicationConfig(), "/employee/**")).contains("lb://employee-service");
    }

    @Test
    @DisplayName("7.1.2 所有路由都必须走服务发现，禁止写死 IP")
    void allRoutesUseServiceDiscovery() throws Exception {
        PropertySource<?> source = applicationConfig();
        for (int i = 0; i < MAX_ROUTES; i++) {
            Object uri = source.getProperty("spring.cloud.gateway.routes[" + i + "].uri");
            if (uri == null) {
                break;
            }
            assertThat(String.valueOf(uri)).startsWith("lb://");
        }
    }
}
```

运行：

```powershell
mvn -pl gateway test
```

预期：3 绿。若有人把 `lb://` 改成固定 IP，第三个用例会立刻变红。

### 7.2 结论：鉴权白名单（任务项 7.2.2）

当前 `LogginFilter` 只打印日志、不做拦截，因此 7.2.2 暂时**无法写出有意义的红灯**。
处理方式：

- 现在：在测试里留一条 `@Disabled("引入鉴权后启用")` 的占位用例，写明未来断言点；
- 一旦网关加入鉴权：把 `/shop/register`、`/shop/login` 加进白名单，并启用该用例。

### 7.3 阶段验收

- [ ] 注册路径的路由目标被测试锁住
- [ ] 鉴权白名单占位用例已登记在册

建议提交：`test(gateway): 增加路由配置回归测试`

---

## 阶段 8：F6 一致性、补偿与可观测

对应任务项 6.1.1 - 6.4.2。这一步会**新增表与类**，属于功能增强，请单独分支评审。
拆解文档把它排在最后，因为它的改动面最大。

### 8.1 红灯：异步订阅者异常必须可观测

**新增** `shop/src/test/java/com/yjx/b2c/shop/service/event/AsyncEventBusObservabilityTest.java`：

```java
package com.yjx.b2c.shop.service.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncEventBusObservabilityTest {

    @Test
    @DisplayName("6.2.1 订阅者异常被 SubscriberExceptionHandler 捕获，而不是静默丢失")
    void subscriberExceptionIsReported() {
        List<Throwable> captured = new CopyOnWriteArrayList<>();
        EventBus eventBus = new AsyncEventBus(Runnable::run,
                (exception, context) -> captured.add(exception));

        eventBus.register(new Object() {
            @Subscribe
            public void on(ShopRegisterEvent event) {
                throw new IllegalStateException("employee service down");
            }
        });

        eventBus.post(new ShopRegisterEvent());

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isInstanceOf(IllegalStateException.class);
    }
}
```

运行：

```powershell
mvn -pl shop -am test -Dtest=AsyncEventBusObservabilityTest
```

预期：绿。它证明「只要注入处理器，异常就能被接住」——接下来把这个行为落到生产代码里。

### 8.2 绿灯：注入 SubscriberExceptionHandler

**新增** `shop/src/main/java/com/yjx/b2c/shop/service/event/EventBusExceptionHandler.java`：

```java
package com.yjx.b2c.shop.service.event;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订阅者异常统一出口：先落日志，后续在这里接告警与补偿入队。
 */
@Component
@Slf4j
public class EventBusExceptionHandler implements SubscriberExceptionHandler {

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        log.error("事件订阅者执行失败, event={}, subscriber={}",
                context.getEvent(), context.getSubscriberMethod(), exception);
    }
}
```

**修改** `shop/src/main/java/com/yjx/b2c/shop/ShopApplication.java` 的 EventBus Bean：

```java
    @Bean
    public EventBus eventBus(SubscriberExceptionHandler subscriberExceptionHandler) {
        return new AsyncEventBus(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                subscriberExceptionHandler);
    }
```

**新增** 日志断言测试（用 Logback 的 `ListAppender`）：

```java
    @Test
    @DisplayName("6.4.2 订阅者异常会写出包含事件信息的 ERROR 日志")
    void handlerWritesErrorLog() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(EventBusExceptionHandler.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        EventBusExceptionHandler handler = new EventBusExceptionHandler();
        EventBus eventBus = new AsyncEventBus(Runnable::run, handler);
        eventBus.register(new Object() {
            @Subscribe
            public void on(ShopRegisterEvent event) {
                throw new IllegalStateException("boom");
            }
        });
        eventBus.post(new ShopRegisterEvent());
        logger.detachAppender(appender);

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list.get(0).getFormattedMessage()).contains("事件订阅者执行失败");
    }
```

### 8.3 红灯：Feign 失败必须留下补偿记录

**新增表**（`shop.sql` 与测试 schema 同步）：

```sql
CREATE TABLE IF NOT EXISTS tb_register_event_outbox
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type    VARCHAR(64) NOT NULL,          -- 例如 EMPLOYEE_INIT
    aggregate_id  INTEGER     NOT NULL,          -- shopId
    payload       TEXT        NOT NULL,          -- JSON
    status        TINYINT     DEFAULT 0,         -- 0-待处理 1-成功 2-待重试 3-已放弃
    retry_count   INTEGER     DEFAULT 0,
    next_retry_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
    last_error    VARCHAR(500),
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_retry
    ON tb_register_event_outbox (status, next_retry_at);
```

**红灯测试**（追加到 `InitEmployeeEventHandleTest`，需先注入 OutboxService）：

```java
    @Test
    @DisplayName("6.3.1 [红灯] employee 调用异常时留下待补偿记录")
    void feignFailureRecordsOutbox() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class)))
                .thenThrow(new RuntimeException("connect timed out"));

        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();

        ArgumentCaptor<OutboxPO> captor = ArgumentCaptor.forClass(OutboxPO.class);
        verify(outboxService).markRetry(captor.capture(), org.mockito.ArgumentMatchers.contains("timed out"));
        assertThat(captor.getValue().getEventType()).isEqualTo("EMPLOYEE_INIT");
    }
```

### 8.4 绿灯：Outbox 落地

**新增** `shop/src/main/java/com/yjx/b2c/shop/dao/po/OutboxPO.java`（字段与表一一对应），
以及 Mapper 接口与 XML：

```java
@Mapper
public interface OutboxMapper {

    int insert(OutboxPO po);

    List<OutboxPO> findRetryable(@Param("maxRetry") int maxRetry, @Param("limit") int limit);

    int markSuccess(@Param("id") Integer id);

    int markRetry(@Param("id") Integer id, @Param("lastError") String lastError);
}
```

```xml
<mapper namespace="com.yjx.b2c.shop.dao.mapper.OutboxMapper">
    <insert id="insert" parameterType="com.yjx.b2c.shop.dao.po.OutboxPO"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO tb_register_event_outbox (event_type, aggregate_id, payload, status, next_retry_at)
        VALUES (#{eventType}, #{aggregateId}, #{payload}, 0,
                datetime('now', 'localtime', '+' || #{delaySeconds} || ' seconds'))
    </insert>

    <select id="findRetryable" resultType="com.yjx.b2c.shop.dao.po.OutboxPO">
        SELECT id, event_type AS eventType, aggregate_id AS aggregateId, payload,
               status, retry_count AS retryCount, next_retry_at AS nextRetryAt, last_error AS lastError
        FROM tb_register_event_outbox
        WHERE status IN (0, 2)
          AND retry_count &lt; #{maxRetry}
          AND next_retry_at &lt;= datetime('now', 'localtime')
        ORDER BY id
        LIMIT #{limit}
    </select>

    <update id="markSuccess">
        UPDATE tb_register_event_outbox
        SET status = 1, updated_at = datetime('now', 'localtime')
        WHERE id = #{id}
    </update>

    <update id="markRetry">
        UPDATE tb_register_event_outbox
        SET status = 2,
            retry_count = retry_count + 1,
            next_retry_at = datetime('now', 'localtime', '+' || (retry_count * 10) || ' seconds'),
            last_error = #{lastError},
            updated_at = datetime('now', 'localtime')
        WHERE id = #{id}
    </update>
</mapper>
```

**改造订阅者**（`InitEmployeeEventHandle`）为「先入队、再调用、按结果更新状态」：

```java
    @Subscribe
    public void handler(ShopRegisterEvent event) {
        AddEmployeeRequestVO req = new AddEmployeeRequestVO();
        req.setShopId(event.getShopId());
        req.setUsername(event.getAdminAccount());
        req.setPassword(event.getAdminPwd());

        OutboxPO outbox = outboxService.enqueue("EMPLOYEE_INIT", event.getShopId(), req);
        try {
            BaseResponseVO<Integer> resp = employeeFeignClient.save(req);
            if (resp != null && Objects.equals(resp.getStatus(), 200)) {
                outboxService.markSuccess(outbox.getId());
            } else {
                outboxService.markRetry(outbox, "employee 返回非 200: "
                        + (resp == null ? "null" : resp.getStatus()));
            }
        } catch (Exception e) {
            outboxService.markRetry(outbox, e.getMessage());
        }
    }
```

**新增重试任务** `shop/src/main/java/com/yjx/b2c/shop/service/RegisterEventRelayJob.java`：

```java
@Component
@Slf4j
public class RegisterEventRelayJob {

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 50;

    private final OutboxMapper outboxMapper;
    private final EmployeeFeignClient employeeFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterEventRelayJob(OutboxMapper outboxMapper, EmployeeFeignClient employeeFeignClient) {
        this.outboxMapper = outboxMapper;
        this.employeeFeignClient = employeeFeignClient;
    }

    @Scheduled(fixedDelay = 30_000)
    public void relay() {
        for (OutboxPO po : outboxMapper.findRetryable(MAX_RETRY, BATCH_SIZE)) {
            try {
                AddEmployeeRequestVO req = objectMapper.readValue(po.getPayload(), AddEmployeeRequestVO.class);
                BaseResponseVO<Integer> resp = employeeFeignClient.save(req);
                if (resp != null && Objects.equals(resp.getStatus(), 200)) {
                    outboxMapper.markSuccess(po.getId());
                } else {
                    outboxMapper.markRetry(po.getId(), "retry failed");
                }
            } catch (Exception e) {
                log.warn("补偿重试失败, outboxId={}", po.getId(), e);
                outboxMapper.markRetry(po.getId(), e.getMessage());
            }
        }
    }
}
```

并在 `ShopApplication` 上开启 `@EnableScheduling`。

**幂等重试用例**（对应 6.3.2）：让重试任务对同一 payload 执行两次，
断言 employee 侧最终只有一条记录——它依赖阶段 5.4 的员工幂等实现，两条链路在此汇合。

### 8.5 阶段验收

- [ ] 订阅者异常有 ERROR 日志且带事件信息
- [ ] Feign 失败/非 200 都会留下状态为 2 的待重试记录
- [ ] 重试任务对同一事件重复执行不会产生重复员工
- [ ] 超过重试上限的记录状态可被人工识别（3-已放弃，需要补一个标记逻辑）

建议提交：`feat(shop): 注册事件 Outbox 与补偿重试，订阅者异常统一处理`

---

## 阶段 9：重构（保持全绿）

重构的前提：阶段 2-8 的测试**全部绿**。每次只做一个重构，做完跑全量测试。

### 9.1 错误码归一（S3/S9）

**新增** `common/src/main/java/com/yjx/b2c/common/exception/BizException.java`：

```java
package com.yjx.b2c.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }
}
```

**改 Advice**：`MethodArgNotValidExceptionAspect` 与 employee 的 `GlobalExceptionHandler` 增加

```java
    @ExceptionHandler(BizException.class)
    public BaseResponseVO<?> handleBizException(BizException ex) {
        return new BaseResponseVO<>(ex.getCode(), ex.getMessage());
    }
```

**改测试**：把 `assertThatThrownBy(...).isInstanceOf(IllegalArgumentException.class)`
改为断言 `BizException` 与错误码。顺序建议：先改测试（红灯）→ 再改实现（绿灯）。

### 9.2 密码加密（S2，对应任务项 1.3.7 锚点）

hutool 已包含 BCrypt，**无需新增依赖**：

```java
// 注册：ShopServiceImpl.register
shopPO.setAdminPassword(cn.hutool.crypto.digest.BCrypt.hashpw(
        vo.getAdminPassword(), cn.hutool.crypto.digest.BCrypt.gensalt()));

// 登录：ShopServiceImpl.login
Assert.isTrue(cn.hutool.crypto.digest.BCrypt.checkpw(vo.getAdminPassword(), shop.getAdminPassword()),
        "账号或密码错误");
```

同步改写断言（这就是「锚点测试」的价值：它把改造点固定在一个用例里）：

```java
    @Test
    @DisplayName("1.3.7 密码以 BCrypt 哈希存储，且可以通过明文校验")
    void registerHashesPassword() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenReturn(1);

        shopService.register(request("测试店铺", "admin", "123456"));

        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopMapper).registerShop(captor.capture());
        String stored = captor.getValue().getAdminPassword();
        assertThat(stored).isNotEqualTo("123456");
        assertThat(cn.hutool.crypto.digest.BCrypt.checkpw("123456", stored)).isTrue();
    }
```

employee 侧同理（`EmployeeServiceImpl.addEmployee` 与 `login`）。
**数据迁移**：已存在的明文记录必须重置密码或标记为待激活，否则登录会全部失败；
迁移前先备份 `shop.db` / `employee.db`。
若 hutool 版本没有 `checkpw`，改用 `org.springframework.security:spring-security-crypto:5.7.11` 的
`BCryptPasswordEncoder`（`matches` / `encode`）。

### 9.3 事件下沉到 Service（S1）

1. 修改 `ShopServiceImpl`：注入 `EventBus`，在 `register` 成功返回前发布事件；
2. 修改 `ShopController`：删除 `EventBus` 字段与 `post` 调用；
3. 移动断言：`ShopControllerTest` 里的 `verify(eventBus).post(...)` 与
   `verify(eventBus, never()).post(...)` 迁到 `ShopServiceImplTest`；
4. 顺带把 2.1 发现的问题修掉：先做 `notBlank` 校验，再做唯一性查询。

这一步是「测试位置随职责移动」的典型例子——测试没变少，只是换了宿主。

### 9.4 契约收敛（S8）

把 `AddEmployeeRequestVO` 收敛到 `common`（如 `com.yjx.b2c.common.contract.AddEmployeeRequestVO`），
删除 shop 与 employee 各一份的副本，统一 import。契约形状测试（5.2）保持不变，
它会在字段漂移时立刻报错。

---

## 附录 A：命令速查

```powershell
# 0. 环境（每个新终端）
$env:JAVA_HOME = 'D:\Program Files\Java\jdk17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn -v

# 1. 依赖与编译
mvn -pl shop -am test-compile

# 2. 全量测试
mvn -pl shop -am test
mvn -pl employee -am test
mvn -pl gateway test
mvn test                                  # 根目录跑所有模块

# 3. 单个测试类 / 单个方法
mvn -pl shop -am test -Dtest=ShopServiceImplTest
mvn -pl shop -am test -Dtest=ShopControllerTest#registerReturnsOk
mvn -pl shop -am test -Dtest='Shop*Test'
mvn -pl shop -am test -Dtest=ShopMapperTest -Dsurefire.failIfNoSpecifiedTests=false

# 4. 只看失败
mvn -pl shop -am test | Select-String -Pattern 'FAIL|ERROR|Tests run'
```

---

## 附录 B：常见坑与排查

| 编号 | 现象 | 原因 | 处理 |
| --- | --- | --- | --- |
| B-1 | DAO 测试表不见/数据丢失 | `:memory:` 在连接池下每个连接是独立库 | 用文件库 `target/test-db/*.db`，或共享缓存内存库并限制池大小为 1 |
| B-2 | 测试卡住或连不上 Nacos | 主配置被加载，触发注册与配置中心 | 本手册用零容器测试；若必须用容器，加测试专用 `application.yml` 并关闭 nacos 的 discovery/config |
| B-3 | 测试写坏了真实 `shop.db` | 主配置里的 `jdbc:sqlite:D:/...` 被继承 | 断言测试数据源指向 `target/`；DAO 基类显式指定测试路径 |
| B-4 | Mockito 报 ByteBuddy 不支持当前 JDK | JDK 21 + Mockito 4.5.1 | 切 JDK 17（阶段 0.1），或升级 mockito 与 byte-buddy |
| B-5 | 事件测试偶发失败 | `AsyncEventBus` 真异步 | 用 `new EventBus()` 或 `new AsyncEventBus(Runnable::run, handler)` |
| B-6 | `UnnecessaryStubbingException` | Mockito 严格模式下的多余桩 | 删掉本用例用不到的 `when(...)`；不要用 `lenient()` 掩盖设计问题 |
| B-7 | 断言 hutool `Assert` 的默认消息失败 | 无消息参数时错误文案由 hutool 决定 | 只断言异常类型；需要文案时在源码里显式传消息 |
| B-8 | 自增主键为 null | 未走 `RETURN_GENERATED_KEYS` | 确认 XML 的 `useGeneratedKeys`/`keyProperty`；sqlite-jdbc 3.42 支持 |
| B-9 | 并发测试报 `SQLITE_BUSY` | SQLite 文件写锁 | 并发正确性交给唯一索引；真并发用例留到 MySQL/Testcontainers 环境 |
| B-10 | 测试方法名乱码 | 中文标识符 + 平台编码 | 方法名用英文，中文放 `@DisplayName` |
| B-11 | 建唯一索引失败 | 历史数据已有重复 | 先按附录 A 的查询语句清重，再建索引 |
| B-12 | `@Validated` 校验不生效 | 上下文缺少 Validator | standalone MockMvc 显式 `setValidator(new LocalValidatorFactoryBean())` |
| B-13 | 找不到 `spring-boot-starter-test:2.7.18` | 本地仓库无缓存且离线 | 在联网环境预热本地仓库，或配置私服镜像 |

---

## 附录 C：阶段验收清单与提交信息

| 阶段 | 完成定义（DoD） | 建议提交信息 |
| --- | --- | --- |
| 0 | `mvn -pl shop -am test-compile` 成功，JDK 17 生效 | `chore(build): 引入 spring-boot-starter-test 测试依赖` |
| 1 | 冒烟测试通过，真实 DB 未被污染 | `test(build): 搭建 SQLite 测试基座` |
| 2 | 三个测试类全绿，`data` 返回 shopId | `test(shop): 注册主流程测试并回传 shopId` |
| 3 | 校验用例全绿，超长/过短约束补齐 | `test(shop): 注册入参校验边界与约束补齐` |
| 4 | 唯一索引生效，冲突翻译为业务错误 | `fix(shop): 店铺名唯一索引与冲突翻译` |
| 5 | 员工账号链路 + 幂等全绿 | `feat(employee): 员工账号幂等与唯一索引` |
| 6 | 欢迎站内信链路全绿 | `test(shop): 欢迎站内信链路测试` |
| 7 | 网关路由回归全绿 | `test(gateway): 路由配置回归测试` |
| 8 | Outbox 与重试用例全绿，异常有日志 | `feat(shop): 注册事件 Outbox 与补偿重试` |
| 9 | 全量测试绿的前提下完成 4 项重构 | 每项一个提交，单独可回滚 |

**每阶段固定收尾动作**：

1. `mvn test` 全量回归；
2. 确认没有测试指向真实 `*.db`；
3. 更新拆解文档里对应用例的状态（红 → 绿）；
4. 提交，提交信息说明「对应任务项编号」。

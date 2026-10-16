package com.yjx.b2c.shop.support;

import com.yjx.b2c.shop.dao.mapper.LoginLogMapper;
import com.yjx.b2c.shop.dao.mapper.MessageMapper;
import com.yjx.b2c.shop.dao.mapper.OutboxMapper;
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
 * @author yjx
 * @date 2026/10/15
 * @description
 */
public abstract class ShopDaoTestSupport {

    private static SqlSessionFactory sqlSessionFactory;

    protected SqlSession session;
    protected ShopMapper shopMapper;
    protected MessageMapper messageMapper;
    protected OutboxMapper outboxMapper;
    protected LoginLogMapper loginLogMapper;

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
        parseMapper(configuration, "mapper/OutboxMapper.xml");
        parseMapper(configuration, "mapper/LoginLogMapper.xml");
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
        outboxMapper = session.getMapper(OutboxMapper.class);
        loginLogMapper = session.getMapper(LoginLogMapper.class);
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("DELETE FROM tb_shop");
            statement.executeUpdate("DELETE FROM tb_messages");
            statement.executeUpdate("DELETE FROM tb_register_event_outbox");
            statement.executeUpdate("DELETE FROM tb_login_logs");
        }
    }

    @AfterEach
    void closeSession() {
        if (session != null) {
            session.close();
        }
    }
}
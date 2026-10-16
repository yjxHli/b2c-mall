import cn.hutool.crypto.digest.BCrypt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 把历史明文密码原地升级为 BCrypt 哈希（密码本身不变），
 * 使开启加密存储后老账号仍能登录。
 */
public class HashPlainPasswords {

    public static void main(String[] args) throws Exception {
        migrate("jdbc:sqlite:D:/AI_Coding/b2c/shop.db", "tb_shop", "admin_password");
        migrate("jdbc:sqlite:D:/AI_Coding/b2c/employee.db", "tb_employee", "password");
    }

    private static void migrate(String url, String table, String column) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, " + column + " FROM " + table)) {
            while (rs.next()) {
                String value = rs.getString(2);
                if (value != null && !value.startsWith("$2")) {
                    rows.add(new Object[]{rs.getInt(1), value});
                }
            }
        }
        if (rows.isEmpty()) {
            System.out.println(table + ": 无可迁移的明文密码");
            return;
        }
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE " + table + " SET " + column + " = ? WHERE id = ?")) {
            for (Object[] row : rows) {
                ps.setString(1, BCrypt.hashpw((String) row[1], BCrypt.gensalt()));
                ps.setInt(2, (Integer) row[0]);
                ps.addBatch();
            }
            System.out.println(table + ": 已加密迁移 " + ps.executeBatch().length + " 行");
        }
    }
}

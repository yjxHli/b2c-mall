import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** 只读核对：登录链路相关的表结构、索引与数据一致性（不改动数据库）。 */
public class CheckLoginSchema {

    public static void main(String[] args) throws Exception {
        for (String url : args) {
            System.out.println("=== " + url + " ===");
            try (Connection connection = DriverManager.getConnection(url);
                 Statement statement = connection.createStatement()) {

                System.out.println("tables: " + query(statement,
                        "SELECT group_concat(name) FROM sqlite_master WHERE type='table'"));

                boolean hasShop = tableExists(statement, "tb_shop");
                boolean hasEmployee = tableExists(statement, "tb_employee");
                boolean hasLogs = tableExists(statement, "tb_login_logs");

                if (hasShop) {
                    System.out.println("tb_shop rows        : " + query(statement, "SELECT count(1) FROM tb_shop"));
                    System.out.println("tb_shop 重复账号    : " + query(statement,
                            "SELECT group_concat(admin_account) FROM (SELECT admin_account FROM tb_shop "
                                    + "GROUP BY admin_account HAVING count(1) > 1)"));
                    System.out.println("tb_shop NULL 计数   : " + query(statement,
                            "SELECT count(1) FROM tb_shop WHERE login_count IS NULL"));
                    System.out.println("tb_shop indexes     : " + query(statement,
                            "SELECT group_concat(name) FROM sqlite_master WHERE type='index' AND tbl_name='tb_shop' "
                                    + "AND name NOT LIKE 'sqlite_%'"));
                }
                if (hasEmployee) {
                    System.out.println("tb_employee rows    : " + query(statement, "SELECT count(1) FROM tb_employee"));
                    System.out.println("tb_employee 重复账号: " + query(statement,
                            "SELECT group_concat(k) FROM (SELECT shop_id || ':' || username AS k FROM tb_employee "
                                    + "GROUP BY shop_id, username HAVING count(1) > 1)"));
                    System.out.println("tb_employee NULL计数: " + query(statement,
                            "SELECT count(1) FROM tb_employee WHERE login_count IS NULL"));
                    System.out.println("tb_employee indexes : " + query(statement,
                            "SELECT group_concat(name) FROM sqlite_master WHERE type='index' AND tbl_name='tb_employee' "
                                    + "AND name NOT LIKE 'sqlite_%'"));
                }
                System.out.println("tb_login_logs 存在  : " + hasLogs
                        + (hasLogs ? " rows=" + query(statement, "SELECT count(1) FROM tb_login_logs") : ""));
            }
        }
    }

    private static boolean tableExists(Statement statement, String name) throws Exception {
        try (ResultSet rs = statement.executeQuery(
                "SELECT count(1) FROM sqlite_master WHERE type='table' AND name='" + name + "'")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static String query(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? String.valueOf(rs.getString(1)) : "<none>";
        }
    }
}

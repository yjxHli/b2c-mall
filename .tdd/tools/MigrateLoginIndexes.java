import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 为已存在的 shop.db 补建管理员账号唯一索引（先检查重复数据）。
 * 与 MigrateShopIndex 同风格：有重复就拒绝创建并提示人工清理。
 */
public class MigrateLoginIndexes {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "jdbc:sqlite:D:/AI_Coding/b2c/shop.db";
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            try (ResultSet rs = statement.executeQuery(
                    "SELECT admin_account, COUNT(*) AS c FROM tb_shop GROUP BY admin_account HAVING COUNT(*) > 1")) {
                boolean duplicated = false;
                while (rs.next()) {
                    duplicated = true;
                    System.out.println("DUPLICATE: " + rs.getString("admin_account") + " x" + rs.getInt("c"));
                }
                if (duplicated) {
                    System.out.println("存在重复管理员账号，需先人工清理，未创建唯一索引");
                    return;
                }
            }

            statement.executeUpdate(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_admin_account ON tb_shop (admin_account)");
            System.out.println("OK: uk_shop_admin_account 已就绪");

            // 登录计数历史脏数据兜底：把 NULL 归零，避免累计次数永远为空
            int fixed = statement.executeUpdate("UPDATE tb_shop SET login_count = 0 WHERE login_count IS NULL");
            System.out.println("OK: login_count NULL 行已归零 " + fixed + " 行");
        }
    }
}

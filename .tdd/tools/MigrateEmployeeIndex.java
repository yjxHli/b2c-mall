import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** 为已存在的 employee.db 补建 (shop_id, username) 唯一索引（先检查重复数据）。 */
public class MigrateEmployeeIndex {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "jdbc:sqlite:D:/AI_Coding/b2c/employee.db";
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            try (ResultSet rs = statement.executeQuery(
                    "SELECT shop_id, username, COUNT(*) AS c FROM tb_employee "
                            + "GROUP BY shop_id, username HAVING COUNT(*) > 1")) {
                boolean duplicated = false;
                while (rs.next()) {
                    duplicated = true;
                    System.out.println("DUPLICATE: shopId=" + rs.getInt("shop_id")
                            + " username=" + rs.getString("username") + " x" + rs.getInt("c"));
                }
                if (duplicated) {
                    System.out.println("存在重复员工账号，需先人工清理，未创建唯一索引");
                    return;
                }
            }

            statement.executeUpdate(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_shop_username ON tb_employee (shop_id, username)");
            System.out.println("OK: uk_employee_shop_username 已就绪");
        }
    }
}

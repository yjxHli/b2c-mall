import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** 为已存在的 shop.db 补建店铺名唯一索引（先检查重复数据）。 */
public class MigrateShopIndex {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "jdbc:sqlite:D:/AI_Coding/b2c/shop.db";
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            try (ResultSet rs = statement.executeQuery(
                    "SELECT shop_name, COUNT(*) AS c FROM tb_shop GROUP BY shop_name HAVING COUNT(*) > 1")) {
                boolean duplicated = false;
                while (rs.next()) {
                    duplicated = true;
                    System.out.println("DUPLICATE: " + rs.getString("shop_name") + " x" + rs.getInt("c"));
                }
                if (duplicated) {
                    System.out.println("存在重复店铺名，需先人工清理，未创建唯一索引");
                    return;
                }
            }

            statement.executeUpdate(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_shop_name ON tb_shop (shop_name)");
            System.out.println("OK: uk_shop_shop_name 已就绪");
        }
    }
}

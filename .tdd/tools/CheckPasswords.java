import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** 检查真实库中密码是否已是 BCrypt 形态（$2 开头）。 */
public class CheckPasswords {

    public static void main(String[] args) throws Exception {
        check("jdbc:sqlite:D:/AI_Coding/b2c/shop.db",
                "SELECT COUNT(*) FROM tb_shop",
                "SELECT COUNT(*) FROM tb_shop WHERE admin_password NOT LIKE '$2%'",
                "tb_shop.admin_password");
        check("jdbc:sqlite:D:/AI_Coding/b2c/employee.db",
                "SELECT COUNT(*) FROM tb_employee",
                "SELECT COUNT(*) FROM tb_employee WHERE password NOT LIKE '$2%'",
                "tb_employee.password");
    }

    private static void check(String url, String totalSql, String plainSql, String label) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(totalSql)) {
                rs.next();
                System.out.println(label + " 总行数=" + rs.getInt(1));
            }
            try (ResultSet rs = statement.executeQuery(plainSql)) {
                rs.next();
                System.out.println(label + " 非BCrypt(明文)=" + rs.getInt(1));
            }
        }
    }
}

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 对一个已存在的 SQLite 库执行 DDL 脚本（脚本内的语句应幂等）。 */
public class RunSqlFile {

    public static void main(String[] args) throws Exception {
        String url = args[0];
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readString(Path.of(args[1])).split("\\R")) {
            if (line.trim().startsWith("--")) {
                continue;
            }
            sql.append(line).append('\n');
        }
        int applied = 0;
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            for (String stmt : sql.toString().split(";")) {
                if (stmt.isBlank()) {
                    continue;
                }
                statement.execute(stmt);
                applied++;
            }
        }
        System.out.println("OK: applied " + applied + " statements from " + args[1] + " -> " + url);
    }
}

package javaSQLite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class App {
    private static Connection connect(String url) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void main(String[] args) {
        String url = args[0];
        write(url);
        read(url);
    }

    public static void write(String url) {
        Connection conn = null;
        try {
            conn = connect(url);
            /**
             * pragma locking_mode=EXCLUSIVE;
             * pragma mmap_size = 30000000000;
             * pragma temp_store = memory;
             **/
            String sql = """
					 	pragma journal_mode = WAL;
						pragma synchronous = normal;
					""";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);

            String insertSQL = "INSERT INTO stuff (leftside, rightside) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(insertSQL);
            for (int i = 0; i < 100000; i++) {
                ps.setString(1, "left thing " + i);
                ps.setInt(2, i);
                ps.execute();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public static void read(String url) {
        Connection conn = null;
        try {
            conn = connect(url);
            Statement stmt = conn.createStatement();
            String sql = "SELECT leftside, rightside FROM stuff";
            ResultSet rs = stmt.executeQuery(sql);
            int count = 0;
            while (rs.next()) {
                count++;
                // System.out.println( rs.getString("leftside") + "\t" + rs.getInt("rightside")
                // );
            }
            System.out.println(count);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}

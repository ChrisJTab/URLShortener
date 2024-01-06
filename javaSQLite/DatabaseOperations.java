package javaSQLite;

import java.sql.*;

public class DatabaseOperations {

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
        
        // Example: Insert a new record
        insertRecord(url, "SC123456789", "http://example.com");

        // Example: Read a record by key
        String keyToRead = "SC123456789";
        String value = readRecord(url, keyToRead);
        if (value != null) {
            System.out.println("Value for key " + keyToRead + ": " + value);
        } else {
            System.out.println("Record not found for key " + keyToRead);
        }
    }

    public static void insertRecord(String url, String key, String value) {
        Connection conn = null;
        try {
            conn = connect(url);
            String insertSQL = "INSERT INTO your_table_name (key_column, value_column) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(insertSQL);
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
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

    public static String readRecord(String url, String key) {
        Connection conn = null;
        try {
            conn = connect(url);
            String selectSQL = "SELECT value_column FROM your_table_name WHERE key_column = ?";
            PreparedStatement ps = conn.prepareStatement(selectSQL);
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("value_column");
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
        return null; // Return null if the record is not found
    }
}

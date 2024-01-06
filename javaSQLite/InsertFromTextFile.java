package javaSQLite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertFromTextFile {

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
        insertFromTextFile(url, "../database.txt");
    }

    public static void insertFromTextFile(String url, String filePath) {
        Connection conn = null;
        BufferedReader reader = null;
        try {
            conn = connect(url);
            conn.setAutoCommit(false); // Start a transaction

            String insertSQL = "INSERT INTO stuff (short, long) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(insertSQL);

            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    ps.setString(1, parts[0]);
                    ps.setString(2, parts[1]);
                    ps.addBatch(); // Add the prepared statement to the batch
                }
            }

            // Execute the batch insert
            ps.executeBatch();

            // Commit the transaction
            conn.commit();
        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback(); // Rollback the transaction in case of an exception
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (SQLException | IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}

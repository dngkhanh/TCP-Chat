package dack.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dngnguyen
 */
public class MyConnect {
    public Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String URL = "jdbc:mysql://localhost:3306/Chat?allowPublicKeyRetrieval=true&useSSL=false";
            Connection conn = DriverManager.getConnection(URL, "nigga", "nigga666");
            if (conn == null) {
                throw new Exception("Khong the tao ket noi");
            }
            return conn;
        } catch (ClassNotFoundException e) {
            System.err.println("Loi: Driver MySQL khong tim thay - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Loi ket noi DB: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getGroups() {
        List<String> groups = new ArrayList<>();
        Connection conn = getConnection();
        if (conn == null) return groups;

        try (Connection connection = conn;
             Statement stmt = connection.createStatement()) {

            String sql = "SELECT group_name FROM chat_groups ORDER BY group_name";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                groups.add(rs.getString("group_name"));
            }

        } catch (Exception e) {
            System.err.println("Loi query groups: " + e.getMessage());
            e.printStackTrace();
        }

        return groups;
    }

    /**
     * Lấy lịch sử chat của một nhóm, giới hạn số lượng tin nhắn gần nhất.
     * Trả về danh sách theo thứ tự thời gian tăng dần (cũ → mới).
     */
    public List<ChatMessage> getHistory(String groupName, int limit) {
        List<ChatMessage> history = new ArrayList<>();
        Connection conn = getConnection();
        if (conn == null) return history;

        // Dùng subquery để lấy N tin nhắn mới nhất rồi sắp xếp lại cũ → mới
        String sql = "SELECT sender, message, created_at FROM ("
                   + "  SELECT sender, message, created_at FROM chat_history"
                   + "  WHERE group_name = ?"
                   + "  ORDER BY created_at DESC LIMIT ?"
                   + ") AS sub ORDER BY created_at ASC";

        try (Connection connection = conn;
             PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setString(1, groupName);
            pst.setInt(2, limit);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                history.add(new ChatMessage(
                        rs.getString("sender"),
                        rs.getString("message"),
                        rs.getString("created_at")
                ));
            }

        } catch (Exception e) {
            System.err.println("Loi query history: " + e.getMessage());
            e.printStackTrace();
        }

        return history;
    }

    /**
     * Data class đơn giản chứa một tin nhắn lịch sử.
     */
    public static class ChatMessage {
        public final String sender;
        public final String message;
        public final String createdAt;

        public ChatMessage(String sender, String message, String createdAt) {
            this.sender = sender;
            this.message = message;
            this.createdAt = createdAt;
        }
    }

    public boolean validateGroup(String groupName) {
        Connection conn = getConnection();
        if (conn == null) return false;

        try (Connection connection = conn;
             PreparedStatement pst = connection.prepareStatement("SELECT COUNT(*) as cnt FROM chat_groups WHERE group_name = ?")) {

            pst.setString(1, groupName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("cnt") > 0;
            }

        } catch (Exception e) {
            System.err.println("Loi validate group: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}

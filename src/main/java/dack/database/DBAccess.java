package dack.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dngnguyen
 */
public class DBAccess {

    public int update(String sql, Object... params) {

        Connection con = new MyConnect().getConnection();
        if (con == null) {
            System.err.println("Khong the ket noi database");
            return -1;
        }

        try (
                Connection connection = con;
                PreparedStatement pst = connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < params.length; i++) {
                pst.setObject(i + 1, params[i]);
            }

            return pst.executeUpdate();

        } catch (Exception e) {
            System.err.println("Loi thuc thi query: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    // Thực thi câu query SELECT và trả về danh sách các row dưới dạng Map.
    // Mỗi Map là một row: key = tên cột, value = giá trị.
    public List<Map<String, Object>> query(String sql, Object... params) {

        List<Map<String, Object>> results = new ArrayList<>();

        Connection con = new MyConnect().getConnection();
        if (con == null) {
            System.err.println("Khong the ket noi database");
            return results;
        }

        try (
                Connection connection = con;
                PreparedStatement pst = connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < params.length; i++) {
                pst.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = pst.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }

        } catch (Exception e) {
            System.err.println("Loi thuc thi query: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}

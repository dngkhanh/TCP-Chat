package dack.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
}

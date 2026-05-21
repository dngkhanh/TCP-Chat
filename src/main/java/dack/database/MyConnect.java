/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dack.database;

import java.sql.Connection;
import java.sql.DriverManager;

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
}

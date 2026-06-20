/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dack.client;

/**
 *
 * @author Asus
 */
public class SecurityUtil {
    // Số đường ray (Depth/Key) dùng để mã hóa. 
    // Tất cả các client phải dùng chung số này để hiểu được nhau.
    private static final int DEPTH = 3;

    // Hàm Mã hóa
    public static String encrypt(String text) {
        if (text == null || text.isEmpty()) return text;
        int r = DEPTH, c = text.length();
        char[][] mat = new char[r][c];
        
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                mat[i][j] = '\n';
            }
        }
        
        boolean dirDown = false;
        int row = 0, col = 0;
        
        for (int i = 0; i < c; i++) {
            if (row == 0 || row == r - 1) dirDown = !dirDown;
            mat[row][col++] = text.charAt(i);
            if (dirDown) row++;
            else row--;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (mat[i][j] != '\n') result.append(mat[i][j]);
            }
        }
        return result.toString();
    }

    // Hàm Giải mã
    public static String decrypt(String cipher) {
        if (cipher == null || cipher.isEmpty()) return cipher;
        int r = DEPTH, c = cipher.length();
        char[][] mat = new char[r][c];
        
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                mat[i][j] = '\n';
            }
        }
        
        boolean dirDown = false;
        int row = 0, col = 0;
        
        for (int i = 0; i < c; i++) {
            if (row == 0) dirDown = true;
            if (row == r - 1) dirDown = false;
            mat[row][col++] = '*';
            if (dirDown) row++;
            else row--;
        }
        
        int index = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (mat[i][j] == '*' && index < cipher.length()) {
                    mat[i][j] = cipher.charAt(index++);
                }
            }
        }
        
        StringBuilder result = new StringBuilder();
        dirDown = false;
        row = 0; col = 0;
        
        for (int i = 0; i < c; i++) {
            if (row == 0) dirDown = true;
            if (row == r - 1) dirDown = false;
            if (mat[row][col] != '*') result.append(mat[row][col++]);
            if (dirDown) row++;
            else row--;
        }
        return result.toString();
    }
}

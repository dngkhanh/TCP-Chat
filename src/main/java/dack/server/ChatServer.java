/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package dack.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author dngnguyen
 */
public class ChatServer {

    public static void main(String[] args) {

        int port = 8888;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server dang chay tai port " + port);

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("Client moi: " + socket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }

        } catch (Exception e) {
            System.err.println("Loi server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}


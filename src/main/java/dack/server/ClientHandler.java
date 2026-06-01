package dack.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import dack.database.DBAccess;

/**
 *
 * @author dngnguyen
 */
public class ClientHandler extends Thread{
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<ClientHandler>> groups
            = new ConcurrentHashMap<>();

    private static final DBAccess db = new DBAccess();

    private Socket socket;

    private BufferedReader reader;
    private BufferedWriter writer;

    private String hoTen = "Unknown";
    private String maSV;
    private String maNhom;

    public ClientHandler(Socket socket) {
        this.socket = socket;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8")
            );

            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8")
            );

        } catch (Exception e) {
            System.err.println("Loi tao streams trong ClientHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {

            if (reader == null || writer == null) {
                System.err.println("Reader hoac writer la null");
                closeEverything();
                return;
            }

            String firstMessage = reader.readLine();

            if (firstMessage == null) {
                closeEverything();
                return;
            }

            // Xử lý GET_GROUPS request từ client
            if (firstMessage.equals("GET_GROUPS")) {
                handleGetGroups();
                firstMessage = reader.readLine();

                if (firstMessage == null) {
                    closeEverything();
                    return;
                }
            }

            String[] loginParts = firstMessage.split("\\|");

            if (loginParts.length < 4 || !loginParts[0].equals("LOGIN")) {
                System.err.println("Login message khong hop le");
                closeEverything();
                return;
            }

            maSV = loginParts[1].trim();
            hoTen = loginParts[2].trim();
            maNhom = loginParts[3].trim();

            if (maSV.isEmpty() || hoTen.isEmpty() || maNhom.isEmpty()) {
                System.err.println("MSSV, ho ten hoac nhom khong hop le");
                closeEverything();
                return;
            }

            // Validate group từ database
            dack.database.MyConnect connect = new dack.database.MyConnect();
            if (!connect.validateGroup(maNhom)) {
                System.err.println("Group '" + maNhom + "' khong ton tai trong database");
                sendMessage("LOGIN_FAILED|Group khong hop le");
                closeEverything();
                return;
            }

            groups.putIfAbsent(maNhom, new CopyOnWriteArrayList<>());
            groups.get(maNhom).add(this);

            sendMessage("LOGIN_SUCCESS|" + maNhom);

            broadcast(
                    "SYSTEM|" + hoTen + " da tham gia nhom",
                    this
            );

            String sql = "INSERT INTO chat_history(sender, message, group_name, created_at) VALUES(?, ?, ?, NOW())";

            String message;

            while ((message = reader.readLine()) != null) {

                if (message == null || message.trim().isEmpty()) continue;

                String[] parts = message.split("\\|", 2);

                if (parts.length >= 2 && parts[0].equals("CHAT")) {

                    String content = parts[1];

                    String time = LocalDateTime.now().toString();

                    String formatted =
                            "BROADCAST|" + hoTen + "|" + content + "|" + time;

                    broadcast(formatted, this);

                    int result = db.update(sql, hoTen, content, maNhom);
                    if (result < 0) {
                        System.err.println("Loi luu tin nhan vao database");
                    }
                }
            }

        } catch (Exception e) {

            System.out.println(hoTen + " da ngat ket noi: " + e.getMessage());

        } finally {
            closeEverything();
        }
    }

    private void broadcast(String message, ClientHandler sender) {

        if (maNhom == null) return;

        CopyOnWriteArrayList<ClientHandler> clients = groups.get(maNhom);

        if (clients != null) {

            for (ClientHandler client : clients) {

                if (client != null && client != sender) {
                    try {
                        client.sendMessage(message);
                    } catch (Exception e) {
                        System.err.println("Loi gui tin nhan: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void sendMessage(String message) {

        try {

            if (writer == null) {
                throw new Exception("Writer la null");
            }

            writer.write(message);
            writer.newLine();
            writer.flush();

        } catch (java.io.IOException e) {
            System.err.println(hoTen + " - Loi IO: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Loi gui tin nhan cho " + hoTen + ": " + e.getMessage());
        }
    }

    private void handleGetGroups() {
        try {
            dack.database.MyConnect connect = new dack.database.MyConnect();
            java.util.List<String> groupList = connect.getGroups();

            if (groupList.isEmpty()) {
                sendMessage("GROUPS|");
                return;
            }

            String groupsStr = String.join(",", groupList);
            sendMessage("GROUPS|" + groupsStr);

        } catch (Exception e) {
            System.err.println("Loi lay danh sach groups: " + e.getMessage());
            try {
                sendMessage("GROUPS|");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void closeEverything() {

        try {

            if (maNhom != null && groups.containsKey(maNhom)) {

                groups.get(maNhom).remove(this);

                if (hoTen != null && !hoTen.isEmpty()) {
                    broadcast(
                            "SYSTEM|" + hoTen + " da roi nhom",
                            this
                    );
                }
            }

            if (reader != null) {
                reader.close();
            }

            if (writer != null) {
                writer.close();
            }

            if (socket != null) {
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

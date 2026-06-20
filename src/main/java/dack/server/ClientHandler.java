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
    
    private long lastMessageTime = 0;      // Lưu thời điểm gửi tin nhắn cuối cùng
    private int spamWarningCount = 0;      // Đếm số lần bị cảnh báo
    private static final long SPAM_THRESHOLD = 500; // Giới hạn tốc độ: 500 mili-giây

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

//            String firstMessage = reader.readLine();
//
//            if (firstMessage == null) {
//                closeEverything();
//                return;
//            }
//
//            // Xử lý GET_GROUPS request từ client
//            if (firstMessage.equals("GET_GROUPS")) {
//                handleGetGroups();
//                firstMessage = reader.readLine();
//
//                if (firstMessage == null) {
//                    closeEverything();
//                    return;
//                }
//            }
//
//            String[] loginParts = firstMessage.split("\\|");
//
//            if (loginParts.length < 4 || !loginParts[0].equals("LOGIN")) {
//                System.err.println("Login message khong hop le");
//                closeEverything();
//                return;
//            }
            String currentMessage = reader.readLine();

            if (currentMessage == null) {
                closeEverything();
                return;
            }

            // Vòng lặp chờ các lệnh khởi tạo (GET_GROUPS, CREATE_GROUP) trước khi LOGIN
            while (currentMessage != null) {
                if (currentMessage.equals("GET_GROUPS")) {
                    handleGetGroups();
                } 
                else if (currentMessage.startsWith("CREATE_GROUP|")) {
                    String[] parts = currentMessage.split("\\|");
                    if (parts.length < 2) continue;
                    
                    String newGroup = parts[1].trim();
                    dack.database.MyConnect connect = new dack.database.MyConnect();
                    
                    if (connect.validateGroup(newGroup)) {
                        sendMessage("CREATE_GROUP_FAILED|Ma nhom nay da ton tai!");
                    } else if (connect.createGroup(newGroup)) {
                        sendMessage("CREATE_GROUP_SUCCESS|" + newGroup);
                    } else {
                        sendMessage("CREATE_GROUP_FAILED|Loi he thong, khong the tao nhom.");
                    }
                } 
                else if (currentMessage.startsWith("LOGIN|")) {
                    break; // Thoát vòng lặp để tiến hành xử lý đăng nhập
                }
                currentMessage = reader.readLine();
            }

            if (currentMessage == null) {
                closeEverything();
                return;
            }

            // Bắt đầu xử lý đăng nhập bằng chuỗi LOGIN bắt được
            String[] loginParts = currentMessage.split("\\|");

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
            CopyOnWriteArrayList<ClientHandler> currentClients = groups.get(maNhom);
            
            // KIỂM TRA TRÙNG LẶP TRONG PHẠM VI NHÓM
//            String myIP = this.socket.getInetAddress().getHostAddress(); Kiem tra dia chi IP
            boolean isDuplicate = false;

            for (ClientHandler client : currentClients) {
                if (client.maSV.equals(this.maSV)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            // Kiem tra dia chi IP
//            for (ClientHandler client : currentClients) {
//                // Kiểm tra: Nếu trong CÙNG NHÓM NÀY, đã có MSSV này HOẶC thiết bị máy tính (IP) này đang online
//                if (client.maSV.equals(this.maSV) || 
//                    client.socket.getInetAddress().getHostAddress().equals(myIP)) {
//                    isDuplicate = true;
//                    break;
//                }
//            }
            
            if (isDuplicate) {
                sendMessage("LOGIN_FAILED|Tai khoan (MSSV: " + this.maSV + ") dang hoat dong trong nhom nay!");
                closeEverything();
                return;
            }

            // Nếu không trùng, thêm vào nhóm
            currentClients.add(this);

            sendMessage("LOGIN_SUCCESS|" + maNhom);

            // Gửi lịch sử chat cho client vừa join
            sendHistory(maNhom);

            broadcast(
                    "SYSTEM|" + hoTen + " da tham gia nhom",
                    this
            );
            
            // Báo cáo danh sách cho tất cả thành viên nhóm chat
            broadcastOnlineList();

            String sql = "INSERT INTO chat_history(sender, message, group_name, created_at) VALUES(?, ?, ?, NOW())";

            String message;

            while ((message = reader.readLine()) != null) {

                if (message == null || message.trim().isEmpty()) continue;

                String[] parts = message.split("\\|", 2);

                if (parts.length >= 2 && parts[0].equals("CHAT")) {
                    
                    long currentTime = System.currentTimeMillis();
                    
                    // Nếu không phải tin nhắn đầu tiên, tiến hành kiểm tra tốc độ
                    if (lastMessageTime != 0) {
                        long interval = currentTime - lastMessageTime;
                        
                        if (interval < SPAM_THRESHOLD) { // Nếu gửi nhanh hơn 500ms
                            spamWarningCount++;
                            lastMessageTime = currentTime; // Vẫn cập nhật thời gian để chặn
                            
                            if (spamWarningCount >= 3) {
                                // Kick user nếu vi phạm 3 lần
                                System.err.println("KICK " + hoTen + " (" + maSV + ") do hanh vi spam.");
                                sendMessage("SYSTEM|Ban da bi KICK khoi Server do spam lien tuc!");
                                broadcast("SYSTEM|" + hoTen + " da bi he thong KICK do hanh vi spam!", this);
                                closeEverything();
                                return; // Kết thúc hoàn toàn luồng của Client này
                            } else {
                                // Gửi cảnh báo riêng cho người vi phạm (không broadcast)
                                sendMessage("SYSTEM|Canh bao spam (" + spamWarningCount + "/3): Ban chat qua nhanh, vui long cham lai!");
                                continue; // Bỏ qua phần lưu DB và Broadcast tin nhắn này
                            }
                        } else if (interval > 2000) {
                            // Nếu chat ngoan ngoãn cách nhau hơn 2 giây, Server sẽ tha thứ (reset bộ đếm)
                            spamWarningCount = 0;
                        }
                    }
                    
                    lastMessageTime = currentTime;

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

    private void sendHistory(String groupName) {
        try {
            dack.database.MyConnect connect = new dack.database.MyConnect();
            java.util.List<dack.database.MyConnect.ChatMessage> history =
                    connect.getHistory(groupName, 50);

            for (dack.database.MyConnect.ChatMessage msg : history) {
                sendMessage("HISTORY|" + msg.sender + "|" + msg.message + "|" + msg.createdAt);
            }

            // Báo hiệu client đã nhận xong history
            sendMessage("HISTORY_END");

        } catch (Exception e) {
            System.err.println("Loi gui history: " + e.getMessage());
            // Vẫn gửi HISTORY_END để client không bị treo chờ
            try {
                sendMessage("HISTORY_END");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
    
    private void broadcastOnlineList() {
        if (maNhom == null) return;
        CopyOnWriteArrayList<ClientHandler> clients = groups.get(maNhom);
        if (clients == null) return;

        StringBuilder sb = new StringBuilder("ONLINE_LIST|");
        for (int i = 0; i < clients.size(); i++) {
            sb.append(clients.get(i).hoTen).append(" - ").append(clients.get(i).maSV);
            if (i < clients.size() - 1) sb.append(",");
        }
        
        String listMsg = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(listMsg);
        }
    }

    private void closeEverything() {

        try {

            if (maNhom != null && groups.containsKey(maNhom)) {

                groups.get(maNhom).remove(this);
                    
                if (maNhom != null && groups.containsKey(maNhom)) {
                    groups.get(maNhom).remove(this);

                    if (hoTen != null && !hoTen.isEmpty()) {
                        broadcast("SYSTEM|" + hoTen + " da roi nhom", this);
                        broadcastOnlineList(); // <-- THÊM DÒNG NÀY ĐỂ CẬP NHẬT UI KHI CÓ NGƯỜI THOÁT
                    }
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

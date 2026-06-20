package dack.client;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 *
 * @author dngnguyen
 */
public class ChatClientGUI extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;

    private BufferedWriter writer;
    private BufferedReader reader;
    private Socket socket;

    private String hoTen;

    private javax.swing.DefaultListModel<String> onlineListModel;

    public ChatClientGUI() {

        loginAndConnect();
    }

    private void loginAndConnect() {

        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("8888");
        JTextField nameField = new JTextField();
        JTextField mssvField = new JTextField();

        Object[] fields = {
                "IP Server:", ipField,
                "Port:", portField,
                "Ho ten:", nameField,
                "MSSV:", mssvField
        };

        while (true) {

            int option = JOptionPane.showConfirmDialog(
                    null,
                    fields,
                    "Dang nhap chat",
                    JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) {
                System.exit(0);
            }

            try {

                String ip = ipField.getText().trim();
                if (ip.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "IP không được để trống!");
                    continue;
                }

                int port;
                try {
                    port = Integer.parseInt(portField.getText().trim());
                    if (port < 1024 || port > 65535) {
                        JOptionPane.showMessageDialog(null, "Port phải từ 1024 đến 65535");
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Port phải là số nguyên");
                    continue;
                }

                hoTen = nameField.getText().trim();
                if (hoTen.isEmpty() || hoTen.length() < 2 || hoTen.length() > 100) {
                    JOptionPane.showMessageDialog(null, "Họ tên phải từ 2 đến 100 ký tự");
                    continue;
                }

                String maSV = mssvField.getText().trim();
                if (!maSV.matches("^[a-zA-Z0-9_]+$") || maSV.length() != 10) {
                    JOptionPane.showMessageDialog(null,
                            "MSSV phải là chuỗi không dấu, không khoảng trắng, bắt buộc phải 10 ký tự");
                    continue;
                }

                this.socket = new Socket(ip, port);
                this.socket.setSoTimeout(180000);

                try {
                    writer = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                    reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                } catch (Exception e) {
                    if (socket != null)
                        socket.close();
                    throw e;
                }

                // Fetch groups từ server
                // List<String> groupList = fetchGroupsFromServer();
                //
                // if (groupList.isEmpty()) {
                // JOptionPane.showMessageDialog(null, "Khong the lay danh sach nhom tu
                // server");
                // if (this.socket != null) this.socket.close();
                // this.reader = null;
                // this.writer = null;
                // continue;
                // }
                //
                // // Hiển thị dialog chọn group
                // JComboBox<String> groupCombo = new JComboBox<>(groupList.toArray(new
                // String[0]));
                //
                // Object[] chooseGroupFields = {
                // "Chon nhom:", groupCombo
                // };
                //
                // int groupOption = JOptionPane.showConfirmDialog(
                // null,
                // chooseGroupFields,
                // "Chon nhom",
                // JOptionPane.OK_CANCEL_OPTION
                // );
                //
                // if (groupOption != JOptionPane.OK_OPTION) {
                // if (this.socket != null) this.socket.close();
                // this.reader = null;
                // this.writer = null;
                // continue;
                // }
                //
                // String maNhom = (String) groupCombo.getSelectedItem();
                //
                // writer.write("LOGIN|" + maSV + "|" + hoTen + "|" + maNhom);
                // writer.newLine();
                // writer.flush();
                //
                // String response = reader.readLine();
                //
                // if (response == null || response.isEmpty()) {
                // JOptionPane.showMessageDialog(null, "Server khong phan hoi");
                // if (this.socket != null) this.socket.close();
                // this.reader = null;
                // this.writer = null;
                // continue;
                // }
                //
                // String[] parts = response.split("\\|");
                //
                // if (parts.length < 2 || !parts[0].equals("LOGIN_SUCCESS")) {
                // JOptionPane.showMessageDialog(null, "Dang nhap that bai: " + (parts.length >
                // 1 ? parts[1] : ""));
                // if (this.socket != null) this.socket.close();
                // this.reader = null;
                // this.writer = null;
                // continue;
                // }
                // Fetch groups từ server
                List<String> groupList = fetchGroupsFromServer();

                // Thêm tùy chọn Tạo nhóm mới vào đầu danh sách
                groupList.add(0, "[+] Tao nhom moi...");

                // Hiển thị dialog chọn group
                JComboBox<String> groupCombo = new JComboBox<>(groupList.toArray(new String[0]));

                Object[] chooseGroupFields = {
                        "Chon nhom:", groupCombo
                };

                int groupOption = JOptionPane.showConfirmDialog(
                        null,
                        chooseGroupFields,
                        "Chon nhom hoac tao moi",
                        JOptionPane.OK_CANCEL_OPTION);

                if (groupOption != JOptionPane.OK_OPTION) {
                    if (this.socket != null)
                        this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                String maNhom = (String) groupCombo.getSelectedItem();

                // Xử lý logic nếu người dùng muốn Tạo nhóm mới
                if ("[+] Tao nhom moi...".equals(maNhom)) {
                    String newGroupCode = JOptionPane.showInputDialog(null, "Nhap ma nhom moi (VD: PTIT_04):");

                    if (newGroupCode == null || newGroupCode.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Ma nhom khong duoc de trong!");
                        if (this.socket != null)
                            this.socket.close();
                        continue;
                    }

                    newGroupCode = newGroupCode.trim().toUpperCase().replace(" ", "_"); // Format chuẩn

                    // Gửi lệnh tạo nhóm lên server
                    writer.write("CREATE_GROUP|" + newGroupCode);
                    writer.newLine();
                    writer.flush();

                    String createResponse = reader.readLine();
                    if (createResponse != null && createResponse.startsWith("CREATE_GROUP_SUCCESS")) {
                        JOptionPane.showMessageDialog(null, "Tao nhom thanh cong! Dang vao phong...");
                        maNhom = newGroupCode; // Gán lại mã nhóm vừa tạo để tiến hành LOGIN
                    } else {
                        String error = createResponse != null ? createResponse.split("\\|")[1] : "Khong xac dinh";
                        JOptionPane.showMessageDialog(null, "Tao nhom that bai: " + error);
                        if (this.socket != null)
                            this.socket.close();
                        continue;
                    }
                }

                // Gửi lệnh LOGIN với mã nhóm (nhóm có sẵn hoặc nhóm vừa tạo thành công)
                writer.write("LOGIN|" + maSV + "|" + hoTen + "|" + maNhom);
                writer.newLine();
                writer.flush();

                String response = reader.readLine();

                if (response == null || response.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Server khong phan hoi");
                    if (this.socket != null)
                        this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                String[] parts = response.split("\\|");

                if (parts.length < 2 || !parts[0].equals("LOGIN_SUCCESS")) {
                    JOptionPane.showMessageDialog(null, "Dang nhap that bai: " + (parts.length > 1 ? parts[1] : ""));
                    if (this.socket != null)
                        this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                initGUI(maNhom);

                IncomingReader incomingReader = new IncomingReader(reader, chatArea, onlineListModel);

                incomingReader.start();

                break;

            } catch (java.net.ConnectException e) {
                JOptionPane.showMessageDialog(null,
                        "❌ LỖI KẾT NỐI\n\n" +
                                "Không thể kết nối đến server.\n\n" +
                                "Vui lòng kiểm tra:\n" +
                                "  • IP address có đúng không?\n" +
                                "  • Port 8888 có đúng không?\n" +
                                "  • Server có đang chạy không?\n" +
                                "  • Firewall có chặn port không?");
                System.err.println("[ConnectException] " + e.getMessage());
                try {
                    if (this.socket != null)
                        this.socket.close();
                    this.reader = null;
                    this.writer = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(null,
                        "❌ LỖI MẠNG\n\n" +
                                "Mất kết nối hoặc lỗi I/O.\n" +
                                "Kiểm tra lại kết nối mạng.");
                System.err.println("[IOException] " + e.getMessage());
                try {
                    if (this.socket != null)
                        this.socket.close();
                    this.reader = null;
                    this.writer = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "❌ LỖI: " + e.getMessage());
                System.err.println("[Exception] " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                try {
                    if (this.socket != null)
                        this.socket.close();
                    this.reader = null;
                    this.writer = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initGUI(String maNhom) {

        setTitle("TCP Chat - " + maNhom + " - " + hoTen);

        setSize(800, 600);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        setLocationRelativeTo(null);

        // chatArea = new JTextPane();
        // chatArea.setContentType("text/html");
        // chatArea.setEditable(false);
        // chatArea.setText("<html><body style='font-family: Arial; font-size:
        // 12px;'></body></html>");
        chatArea = new JTextPane();
        chatArea.setContentType("text/html");
        chatArea.setEditable(false);

        // Light theme
        chatArea.setBackground(new java.awt.Color(255, 255, 255));
        chatArea.setForeground(new java.awt.Color(0, 0, 0));
        chatArea.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));

        // Khởi tạo HTML với light theme
        chatArea.setText("<html><body style='font-family: Arial, sans-serif; " +
                "font-size: 13px; background-color: #ffffff; color: #333333; padding: 10px;'>" +
                "</body></html>");

        JScrollPane scrollPane = new JScrollPane(chatArea);

        add(scrollPane, BorderLayout.CENTER);

        // THÊM CỘT BÊN PHẢI HIỂN THỊ DANH SÁCH ONLINE
        // onlineListModel = new javax.swing.DefaultListModel<>();
        // javax.swing.JList<String> onlineList = new
        // javax.swing.JList<>(onlineListModel);
        // JScrollPane rightScroll = new JScrollPane(onlineList);
        // rightScroll.setPreferredSize(new java.awt.Dimension(220, 0));
        // rightScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Online ("
        // + maNhom + ")"));
        onlineListModel = new javax.swing.DefaultListModel<>();
        javax.swing.JList<String> onlineList = new javax.swing.JList<>(onlineListModel);

        // Dark theme cho online list
        // Light theme cho online list
        onlineList.setBackground(new java.awt.Color(255, 255, 255));
        onlineList.setForeground(new java.awt.Color(33, 150, 243));
        onlineList.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        onlineList.setSelectionBackground(new java.awt.Color(33, 150, 243));
        onlineList.setSelectionForeground(new java.awt.Color(255, 255, 255));

        JScrollPane rightScroll = new JScrollPane(onlineList);
        rightScroll.setPreferredSize(new java.awt.Dimension(220, 0));
        rightScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("👥 Online (" + maNhom + ")"));
        rightScroll.getViewport().setBackground(new java.awt.Color(255, 255, 255));
        add(rightScroll, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        messageField = new JTextField();

        // JButton sendButton = new JButton("Gui");

        // bottomPanel.add(messageField, BorderLayout.CENTER);
        // bottomPanel.add(sendButton, BorderLayout.EAST);
        JButton sendButton = new JButton("Gửi");
        sendButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        sendButton.setBackground(new java.awt.Color(33, 150, 243));
        sendButton.setForeground(java.awt.Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(true);
        sendButton.setOpaque(true);
        sendButton.setPreferredSize(new java.awt.Dimension(80, 35));

        JButton emojiBtn = new JButton("😀");
        emojiBtn.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
        emojiBtn.setPreferredSize(new java.awt.Dimension(45, 35));
        emojiBtn.setFocusPainted(false);
        emojiBtn.setBorderPainted(false);
        emojiBtn.setContentAreaFilled(false);
        emojiBtn.setOpaque(false);

        // Xử lý emoji button
        emojiBtn.addActionListener(e -> {
            String[] emojis = { "😀", "😂", "❤️", "👍", "🎉", "😍", "🔥", "✨" };
            JComboBox<String> emojiCombo = new JComboBox<>(emojis);
            int result = JOptionPane.showConfirmDialog(null, emojiCombo,
                    "Chọn emoji", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String emoji = (String) emojiCombo.getSelectedItem();
                messageField.setText(messageField.getText() + emoji);
                messageField.requestFocus();
            }
        });

        // Message field styling
        messageField.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        messageField.setBackground(new java.awt.Color(245, 245, 245));
        messageField.setForeground(new java.awt.Color(0, 0, 0));
        messageField.setCaretColor(new java.awt.Color(33, 150, 243));

        // Bottom panel
        // Bottom panel
        bottomPanel.setBackground(new java.awt.Color(240, 240, 240));
        bottomPanel.add(messageField, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 5));
        buttonPanel.setBackground(new java.awt.Color(240, 240, 240));
        buttonPanel.add(emojiBtn);
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        setVisible(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    if (writer != null)
                        writer.close();
                    if (reader != null)
                        reader.close();
                    if (socket != null)
                        socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private List<String> fetchGroupsFromServer() {
        List<String> groups = new ArrayList<>();

        try {
            writer.write("GET_GROUPS");
            writer.newLine();
            writer.flush();

            String response = reader.readLine();

            if (response == null || response.isEmpty()) {
                return groups;
            }

            String[] parts = response.split("\\|");

            if (parts.length < 2 || !parts[0].equals("GROUPS")) {
                return groups;
            }

            String groupsStr = parts[1];
            if (!groupsStr.isEmpty()) {
                String[] groupArray = groupsStr.split(",");
                for (String group : groupArray) {
                    groups.add(group.trim());
                }
            }

        } catch (Exception e) {
            System.err.println("Loi lay danh sach nhom: " + e.getMessage());
            e.printStackTrace();
        }

        return groups;
    }

    private void appendHtmlMessage(String htmlContent) {
        try {
            HTMLDocument doc = (HTMLDocument) chatArea.getDocument();
            HTMLEditorKit kit = (HTMLEditorKit) chatArea.getEditorKit();
            kit.insertHTML(doc, doc.getLength(), htmlContent, 0, 0, null);
            chatArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {

        try {

            String msg = messageField.getText().trim();

            if (!msg.isEmpty()) {
                if (writer == null) {
                    JOptionPane.showMessageDialog(null, "Mat ket noi toi server");
                    return;
                }

                // 1. GỌI HÀM MÃ HÓA TIN NHẮN
                String encryptedMsg = SecurityUtil.encrypt(msg);

                // 2. GỬI BẢN MÃ LÊN SERVER
                writer.write("CHAT|" + encryptedMsg);
                writer.newLine();
                writer.flush();

                // 3. HIỂN THỊ TIN NHẮN GỐC
                String currentTime = java.time.LocalDateTime.now().toString();
                // String htmlMsg = "<table width='100%' cellpadding='0' cellspacing='0'
                // style='margin-bottom:5px;'>"
                // + "<tr>"
                // + "<td>Ban: " + escapeHtml(msg) + "</td>"
                // + "<td align='right' nowrap='nowrap' style='font-size:10px;
                // padding-left:8px;'>[" + currentTime
                // + "]</td>"
                // + "</tr>"
                // + "</table>";
                String htmlMsg = "<div style='background-color: #e8f5e9; color: #2e7d32; " +
                        "border-left: 4px solid #4CAF50; padding: 8px; margin: 5px 0; " +
                        "border-radius: 4px;'>" +
                        "<b style='color: #1b5e20;'>👤 Bạn</b><br/>" +
                        "<span style='color: #333333;'>" + escapeHtml(msg) + "</span>" +
                        "<br/><span style='font-size: 11px; color: #999999;'>[" + currentTime + "]</span>" +
                        "</div>";
                appendHtmlMessage(htmlMsg);

                messageField.setText("");
            }

        } catch (java.io.IOException e) {
            String htmlMsg = "<div style='background-color: #ffebee; color: #c62828; " +
                    "border-left: 4px solid #ff0000; padding: 10px; margin: 5px 0; " +
                    "border-radius: 4px;'>" +
                    "<b>❌ [LỖI MẠNG]</b> Mất kết nối với server. Kiểm tra lại...</div>";
            appendHtmlMessage(htmlMsg);
            messageField.setEditable(false);
            System.err.println("[SendMessage IOException] " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Loi: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}

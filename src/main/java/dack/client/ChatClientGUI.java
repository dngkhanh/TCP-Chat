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
public class ChatClientGUI extends JFrame{
    private JTextPane chatArea;
    private JTextField messageField;

    private BufferedWriter writer;
    private BufferedReader reader;
    private Socket socket;

    private String hoTen;

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
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (option != JOptionPane.OK_OPTION) {
                System.exit(0);
            }

            try {

                String ip = ipField.getText().trim();
                if (ip.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Vui long nhap IP server");
                    continue;
                }

                int port;
                try {
                    port = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Port phai la so");
                    continue;
                }

                hoTen = nameField.getText().trim();
                String maSV = mssvField.getText().trim();

                if (hoTen.isEmpty() || maSV.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Vui long nhap day du thong tin");
                    continue;
                }

                this.socket = new Socket(ip, port);
                this.socket.setSoTimeout(180000);

                try {
                    writer = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                    );

                    reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8")
                    );
                } catch (Exception e) {
                    if (socket != null) socket.close();
                    throw e;
                }

                // Fetch groups từ server
                List<String> groupList = fetchGroupsFromServer();

                if (groupList.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Khong the lay danh sach nhom tu server");
                    if (this.socket != null) this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                // Hiển thị dialog chọn group
                JComboBox<String> groupCombo = new JComboBox<>(groupList.toArray(new String[0]));

                Object[] chooseGroupFields = {
                        "Chon nhom:", groupCombo
                };

                int groupOption = JOptionPane.showConfirmDialog(
                        null,
                        chooseGroupFields,
                        "Chon nhom",
                        JOptionPane.OK_CANCEL_OPTION
                );

                if (groupOption != JOptionPane.OK_OPTION) {
                    if (this.socket != null) this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                String maNhom = (String) groupCombo.getSelectedItem();

                writer.write("LOGIN|" + maSV + "|" + hoTen + "|" + maNhom);
                writer.newLine();
                writer.flush();

                String response = reader.readLine();

                if (response == null || response.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Server khong phan hoi");
                    if (this.socket != null) this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                String[] parts = response.split("\\|");

                if (parts.length < 2 || !parts[0].equals("LOGIN_SUCCESS")) {
                    JOptionPane.showMessageDialog(null, "Dang nhap that bai: " + (parts.length > 1 ? parts[1] : ""));
                    if (this.socket != null) this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                initGUI(maNhom);

                IncomingReader incomingReader =
                        new IncomingReader(reader, chatArea);

                incomingReader.start();

                break;

            } catch (java.net.ConnectException e) {

                JOptionPane.showMessageDialog(
                        null,
                        "Khong the ket noi toi server: " + e.getMessage()
                );
                try {
                    if (this.socket != null) this.socket.close();
                    this.reader = null;
                    this.writer = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        null,
                        "Loi: " + e.getMessage()
                );
                try {
                    if (this.socket != null) this.socket.close();
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

        setSize(600, 400);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        setLocationRelativeTo(null);

        chatArea = new JTextPane();
        chatArea.setContentType("text/html");
        chatArea.setEditable(false);
        chatArea.setText("<html><body style='font-family: Arial; font-size: 12px;'></body></html>");

        JScrollPane scrollPane = new JScrollPane(chatArea);

        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        messageField = new JTextField();

        JButton sendButton = new JButton("Gui");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        setVisible(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    if (writer != null) writer.close();
                    if (reader != null) reader.close();
                    if (socket != null) socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
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

                writer.write("CHAT|" + msg);
                writer.newLine();
                writer.flush();

                String currentTime = java.time.LocalDateTime.now().toString();
                String htmlMsg = "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:5px;'>"
                    + "<tr>"
                    + "<td>Ban: " + escapeHtml(msg) + "</td>"
                    + "<td align='right' nowrap='nowrap' style='font-size:10px; padding-left:8px;'>[" + currentTime + "]</td>"
                    + "</tr>"
                    + "</table>";
                appendHtmlMessage(htmlMsg);

                messageField.setText("");
            }

        } catch (java.io.IOException e) {
            String htmlMsg = "<div style='color: #cc0000; margin-bottom:5px;'><b>[ERROR]</b> Mat ket noi toi server</div>";
            appendHtmlMessage(htmlMsg);
            messageField.setEditable(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Loi: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}

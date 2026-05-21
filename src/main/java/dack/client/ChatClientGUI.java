package dack.client;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * @author dngnguyen
 */
public class ChatClientGUI extends JFrame{
    private JTextArea chatArea;
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

                writer.write("LOGIN|" + maSV + "|" + hoTen);
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
                    JOptionPane.showMessageDialog(null, "Dang nhap that bai");
                    if (this.socket != null) this.socket.close();
                    this.reader = null;
                    this.writer = null;
                    continue;
                }

                String maNhom = parts[1];

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

        chatArea = new JTextArea();
        chatArea.setEditable(false);

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

                String message_text = "Ban: " + msg;
                int fixedPosition = 120;
                int paddingNeeded = Math.max(1, fixedPosition - message_text.length());
                String paddingStr = " ".repeat(paddingNeeded);
                String currentTime = java.time.LocalDateTime.now().toString();
                String formatted = message_text + paddingStr + "[" + currentTime + "]\n";
                chatArea.append(formatted);

                messageField.setText("");
            }

        } catch (java.io.IOException e) {
            chatArea.append("[ERROR] Mat ket noi toi server\n");
            messageField.setEditable(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Loi: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}

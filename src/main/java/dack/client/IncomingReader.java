package dack.client;

import java.io.BufferedReader;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 *
 * @author dngnguyen
 */
public class IncomingReader extends Thread{
    private BufferedReader reader;
    private JTextPane chatArea;

    public IncomingReader(BufferedReader reader, JTextPane chatArea) {
        this.reader = reader;
        this.chatArea = chatArea;
        setDaemon(true);
    }

    @Override
    public void run() {

        try {

            String message;

            while ((message = reader.readLine()) != null) {

                String[] parts = message.split("\\|", 4);

                if (parts.length < 2) continue;

                String action = parts[0];

                if (action.equals("BROADCAST") && parts.length >= 4) {

                    String sender = parts[1];
                    String content = parts[2];
                    String time = parts[3];

                    SwingUtilities.invokeLater(() -> {
                        String htmlMsg = buildChatRow(sender, content, time, false);
                        appendHtmlMessage(htmlMsg);
                    });

                } else if (action.equals("HISTORY") && parts.length >= 4) {

                    String sender = parts[1];
                    String content = parts[2];
                    String time = parts[3];

                    SwingUtilities.invokeLater(() -> {
                        // Tin lịch sử hiển thị mờ hơn để phân biệt với tin mới
                        String htmlMsg = buildChatRow(sender, content, time, true);
                        appendHtmlMessage(htmlMsg);
                    });

                } else if (action.equals("HISTORY_END")) {

                    SwingUtilities.invokeLater(() -> {
                        String separator = "<div style='border-top: 1px dashed #aaaaaa; "
                                + "color: #888888; font-size: 10px; text-align: center; "
                                + "margin: 6px 0; padding: 2px;'>"
                                + "--- Tin nhắn mới ---</div>";
                        appendHtmlMessage(separator);
                    });

                } else if (action.equals("SYSTEM") && parts.length >= 2) {

                    String content = parts[1];

                    SwingUtilities.invokeLater(() -> {
                        String htmlMsg = "<div style='color: #0066cc; margin-bottom:5px;'><b>[SYSTEM]</b> "
                                + escapeHtml(content) + "</div>";
                        appendHtmlMessage(htmlMsg);
                    });
                }
            }

        } catch (Exception e) {

            SwingUtilities.invokeLater(() -> {
                appendHtmlMessage("<div style='color: #cc0000; margin-bottom:5px;'><b>[ERROR]</b> Mat ket noi toi server</div>");
            });
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    /**
     * Escape các ký tự HTML đặc biệt để tránh vỡ layout khi nội dung
     * chứa <, >, &, ", '
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * Tạo HTML row cho một tin nhắn.
     * @param isHistory true = tin lịch sử (màu mờ), false = tin mới (màu bình thường)
     */
    private String buildChatRow(String sender, String content, String time, boolean isHistory) {
        String color = isHistory ? "color: #888888;" : "";
        return "<table width='100%' cellpadding='0' cellspacing='0' "
                + "style='margin-bottom:5px; " + color + "'>"
                + "<tr>"
                + "<td>" + escapeHtml(sender) + ": " + escapeHtml(content) + "</td>"
                + "<td align='right' nowrap='nowrap' style='font-size:10px; padding-left:8px;'>"
                + "[" + escapeHtml(time) + "]</td>"
                + "</tr>"
                + "</table>";
    }
}

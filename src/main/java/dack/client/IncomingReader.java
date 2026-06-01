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
                        String htmlMsg = "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:5px;'>"
                            + "<tr>"
                            + "<td>" + sender + ": " + content + "</td>"
                            + "<td align='right' nowrap='nowrap'>[" + time + "]</td>"
                            + "</tr>"
                            + "</table>";
                        appendHtmlMessage(htmlMsg);
                    });

                } else if (action.equals("SYSTEM") && parts.length >= 2) {

                    String content = parts[1];

                    SwingUtilities.invokeLater(() -> {
                        String htmlMsg = "<div style='color: #0066cc; margin-bottom:5px;'><b>[SYSTEM]</b> " + content + "</div>";
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
}

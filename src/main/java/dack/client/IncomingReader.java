/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dack.client;

import java.io.BufferedReader;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author dngnguyen
 */
public class IncomingReader extends Thread{
    private BufferedReader reader;
    private JTextArea chatArea;

    public IncomingReader(BufferedReader reader, JTextArea chatArea) {
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
                        String message_text = sender + ": " + content;
                        int fixedPosition = 120;
                        int paddingNeeded = Math.max(1, fixedPosition - message_text.length());
                        String paddingStr = " ".repeat(paddingNeeded);
                        String formatted = message_text + paddingStr + "[" + time + "]\n";
                        chatArea.append(formatted);
                    });

                } else if (action.equals("SYSTEM") && parts.length >= 2) {

                    String content = parts[1];

                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(
                                "[SYSTEM] " + content + "\n"
                        );
                    });
                }
            }

        } catch (Exception e) {

            SwingUtilities.invokeLater(() -> {
                chatArea.append(
                        "\nMat ket noi toi server.\n"
                );
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
}

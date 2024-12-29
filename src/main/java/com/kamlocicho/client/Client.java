package com.kamlocicho.client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    private final JTextArea messageArea;
    private final JTextField inputField;

    public Client() {
        JFrame frame = new JFrame("Chat Client");
        messageArea = new JTextArea(20, 50);
        messageArea.setEditable(false);
        inputField = new JTextField(40);
        JButton sendButton = new JButton("Send");

        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.add(inputField);
        panel.add(sendButton);

        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // todo: Close the TCP connection when closing GUI
    }

    public void connect() {
        try {
            client = new Socket("127.0.0.1", 15001);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            Thread t = new Thread(new InputHandler());
            t.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                messageArea.append(inMessage + "\n");
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        if ("/quit".equals(message)) {
            shutdown();
        } else {
            out.println(message);
            inputField.setText("");
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (in != null) {
                in.close();
            }
            out.close();
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException ignored) {}
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                while (!done) {
                    String message = in.readLine();
                    if (message == null) {
                        shutdown();
                    } else {
                        messageArea.append(message + "\n");
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connect();
    }
}
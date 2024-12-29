package com.kamlocicho.server;

import com.kamlocicho.common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private final DatabaseService databaseService = new DatabaseService();

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        System.out.println("Server is running.");
        try {
            server = new ServerSocket(15001);
            while (!done) {
                pool = Executors.newCachedThreadPool();
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
                loadOldMessages(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }

    private void loadOldMessages(ConnectionHandler ch) {
        Message[] messages = databaseService.getRecentMessages(5);
        Arrays.stream(messages)
                .filter(Objects::nonNull)
                .forEach(message -> {
                    broadcast(message, ch.getToken());
                });
    }

    public void broadcast(Message message) {
        String formattedMessage = String.format("%s - %s: %s", message.timestamp(), message.username(), message.message());
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(formattedMessage);
            }
        }
    }

    public void broadcast(Message message, String token) {
        String formattedMessage = String.format("%s - %s: %s", message.timestamp(), message.username(), message.message());
        connections.stream()
                .filter(con -> con.getToken().equals(token))
                .forEach(con -> con.sendMessage(formattedMessage));
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
            databaseService.close();
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable {
        private final Socket client;
        private final String token;
        private BufferedReader in;
        private PrintWriter out;

        public ConnectionHandler(Socket client) {
            this.client = client;
            this.token = UUID.randomUUID().toString();
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                String nickname = in.readLine();
                String joinedMessage = String.format("%s joined the chat!", nickname);
                broadcast(new Message(joinedMessage, nickname));
                String message;
                while ((message = in.readLine()) != null) {
                    Message messageObject = new Message(message, nickname);
                    databaseService.saveMessage(messageObject);
                    broadcast(messageObject);
                }
            } catch (Exception e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                connections.remove(this);
                if (in != null) {
                    in.close();
                }
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

        public String getToken() {
            return token;
        }
    }


    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
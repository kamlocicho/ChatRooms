package com.kamlocicho;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        System.out.println("Server is defined.");
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
            }
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
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
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable {
        private final Socket client;
        private BufferedReader in;
        private PrintWriter out;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                String nickname = in.readLine();
                System.out.println(nickname + " connected to the server.");
                broadcast(nickname + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " changed his nickname to: " + messageSplit[1]);
                            System.out.println(nickname + " changed his nickname to: " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Nickname changed successfully");
                        } else {
                            out.println("No nickname provided");
                        }
                        // todo: handle nickname change
                    } else if (message.startsWith("/quit ")) {
                        broadcast(nickname + " - leaves the chat.");
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + message);
                    }
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
                if (in != null) {
                    in.close();
                }
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ingore
            }
        }
    }


    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
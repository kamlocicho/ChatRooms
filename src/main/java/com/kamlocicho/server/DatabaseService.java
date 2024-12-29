package com.kamlocicho.server;

import com.kamlocicho.common.Message;

import java.sql.*;

public class DatabaseService {

    private Connection connection;

    public DatabaseService() {
        System.out.println("Connecting to database...");

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
            Statement statement = connection.createStatement();
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY,
                        message VARCHAR(255),
                        timestamp VARCHAR(255),
                        username VARCHAR(255)
                    )
                    """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Message[] getRecentMessages(int count) {
        Message[] messages = new Message[count];
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM messages ORDER BY id DESC LIMIT ?")) {
            statement.setInt(1, count);
            ResultSet resultSet = statement.executeQuery();
            int i = 0;
            while (resultSet.next()) {
                messages[i] = new Message(resultSet.getInt("id"), resultSet.getString("message"), resultSet.getString("timestamp"), resultSet.getString("username"));
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public void saveMessage(Message message) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (message, timestamp, username) VALUES (?, ?, ?)")) {
            statement.setString(1, message.message());
            statement.setString(2, message.timestamp());
            statement.setString(3, message.username());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

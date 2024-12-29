package com.kamlocicho.common;

import java.time.LocalDateTime;

public record Message(int id, String message, String timestamp, String username) {
    public Message {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("Timestamp cannot be null or blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
    }

    public Message(String message, String username) {
        this(-1, message, LocalDateTime.now().toString(), username);
    }
    public Message(String message, String timestamp, String username) {
        this(-1, message, timestamp, username);
    }
}

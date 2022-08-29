package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class database {
    public static Connection connection;

    static{
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:guessTheEmoji.db");
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS emojis(_id INTEGER PRIMARY KEY, emoji TEXT NOT NULL, answer TEXT NOT NULL, text_to_send TEXT)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

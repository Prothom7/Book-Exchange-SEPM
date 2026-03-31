package com.example.book_exchange_sepm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Order(121)
public class ChatSchemaConstraintInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatSchemaConstraintInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ChatSchemaConstraintInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        String productName;
        try (Connection connection = dataSource.getConnection()) {
            productName = connection.getMetaData().getDatabaseProductName();
        }

        if (productName == null || !productName.toLowerCase().contains("postgres")) {
            return;
        }

        // Legacy schema compatibility:
        // old versions had chat_messages.receiver_id NOT NULL, but the current model is room-based.
        // Keep the column (if present) but make it nullable so inserts succeed.
        jdbcTemplate.execute("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'chat_messages'
                      AND column_name = 'receiver_id'
                ) THEN
                    ALTER TABLE chat_messages ALTER COLUMN receiver_id DROP NOT NULL;
                END IF;
            END $$;
            """);

        log.info("Synchronized chat_messages legacy receiver_id nullability for room-based chat model");
    }
}

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
@Order(122)
public class UserSchemaCompatibilityInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSchemaCompatibilityInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public UserSchemaCompatibilityInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
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

        jdbcTemplate.execute("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'users'
                      AND column_name = 'delivery_request_status'
                ) THEN
                    UPDATE users
                    SET delivery_request_status = 'NONE'
                    WHERE delivery_request_status IS NULL;
                END IF;
            END $$;
            """);

        log.info("Backfilled users.delivery_request_status with NONE for legacy rows");
    }
}
